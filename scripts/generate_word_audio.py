#!/usr/bin/env python3
"""Generate US/UK pronunciation mp3s for new vocabulary via the local Qwen TTS WebUI API.

Requires the WebUI running with --api (e.g. `python launch.py --api`).
Requests are strictly sequential (single process): the TTS server serialises them anyway.

Edit `word_audio_manifest.json` to add/remove words, switch speaker, or tweak the
accent prompts. Files land where the app expects them:
    app/src/main/assets/audio/{us,uk}/{word}.mp3

Examples:
    python scripts/generate_word_audio.py                 # generate missing us+uk
    python scripts/generate_word_audio.py --only us       # US only
    python scripts/generate_word_audio.py --word hello    # one word
    python scripts/generate_word_audio.py --force         # overwrite existing
    python scripts/generate_word_audio.py --greedy        # deterministic, avoids rambling
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import requests

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
MANIFEST = HERE / "word_audio_manifest.json"
ACCENTS = {"us": "instruct_us", "uk": "instruct_uk"}


def audio_filename(word: str) -> str:
    """Mirror MainActivity.localAudioPath()'s file naming."""
    return re.sub(r"[^a-z0-9]+", "_", word.lower()).strip("_")


def af_chain(extra: str = "") -> str:
    # trim leading/trailing silence below -45 dB, then optional extra filter
    trim = ("silenceremove=start_periods=1:start_duration=0:start_threshold=-45dB,"
            "areverse,"
            "silenceremove=start_periods=1:start_duration=0:start_threshold=-45dB,"
            "areverse")
    return trim + (("," + extra) if extra else "")


def peak_gain_db(path: str, target_peak_db: float) -> float:
    """Measure peak of the trimmed audio and return the gain to hit target_peak_db."""
    r = subprocess.run(
        ["ffmpeg", "-hide_banner", "-i", path, "-af", af_chain("volumedetect"),
         "-f", "null", "-"], capture_output=True, text=True)
    m = re.search(r"max_volume: ([-\d.]+) dB", r.stderr)
    if not m:
        return 0.0
    gain = target_peak_db - float(m.group(1))
    return max(-30.0, min(30.0, gain))


def to_mp3(wav_bytes: bytes, out_path: Path, bitrate: str,
           target_peak_db: float = -6.0) -> None:
    with tempfile.TemporaryDirectory() as td:
        src = os.path.join(td, "in.wav")
        with open(src, "wb") as f:
            f.write(wav_bytes)
        gain = peak_gain_db(src, target_peak_db)
        subprocess.run(
            ["ffmpeg", "-y", "-hide_banner", "-loglevel", "error", "-i", src,
             "-af", af_chain(f"volume={gain:.2f}dB"), "-ar", "24000", "-ac", "1",
             "-codec:a", "libmp3lame", "-b:a", bitrate, str(out_path)],
            check=True,
        )


def duration(path: Path) -> float:
    r = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=nw=1:nk=1", str(path)],
        capture_output=True, text=True)
    return float(r.stdout.strip())


def generate(api_base: str, model: str, word: str, instruct: str,
             speaker: str, language: str, sample: bool | None = None,
             retries: int = 3) -> bytes:
    payload = {"model_name": model, "text": word, "instruct": instruct,
               "speaker": speaker, "language": language}
    if sample is not None:
        payload["do_sample"] = sample
    last: Exception | None = None
    for attempt in range(1, retries + 1):
        try:
            r = requests.post(f"{api_base}/custom-voice", json=payload, timeout=1800)
            r.raise_for_status()
            return base64.b64decode(r.json()["audio_files_base64"][0])
        except Exception as exc:  # noqa: BLE001
            last = exc
            time.sleep(2 * attempt)
    raise last  # type: ignore[misc]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", default=str(MANIFEST))
    ap.add_argument("--only", choices=["us", "uk"], help="generate one accent only")
    ap.add_argument("--word", help="generate a single word (repeatable)", action="append")
    ap.add_argument("--force", action="store_true", help="overwrite existing files")
    ap.add_argument("--greedy", action="store_true",
                    help="deterministic decoding (do_sample=false); avoids TTS rambling")
    ap.add_argument("--retries", type=int, default=4,
                    help="attempts per file when the duration guard rejects the result")
    ap.add_argument("--limit", type=int, help="stop after N generated files")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    m = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    api_base = m["api_base"].rstrip("/")
    model = m["model_name"]
    speaker = m["speaker"]
    language = m.get("language") or "english"
    bitrate = m.get("mp3_bitrate", "48k")
    target_peak = float(m.get("target_peak_db", -6.0))
    lo = float(m.get("min_duration", 0.20))
    hi = float(m.get("max_duration", 1.60))
    overrides = m.get("overrides", {})
    accents = [args.only] if args.only else list(ACCENTS)
    words = m["words"]
    if args.word:
        wanted = {w.lower() for w in args.word}
        words = [w for w in words if w.lower() in wanted]

    done = skipped = failed = retried = 0
    errors: list[str] = []
    for word in words:
        for accent in accents:
            instruct = overrides.get(word, {}).get(accent) or m[ACCENTS[accent]]
            out_dir = ROOT / m[f"{accent}_dir"]
            out_path = out_dir / f"{audio_filename(word)}.mp3"
            if out_path.exists() and not args.force:
                skipped += 1
                continue
            if args.dry_run:
                print(f"[dry-run] {accent} {word} -> {out_path.relative_to(ROOT)}")
                continue
            if args.limit and done >= args.limit:
                print(f"limit {args.limit} reached")
                return 0
            out_dir.mkdir(parents=True, exist_ok=True)
            t0 = time.time()
            attempts = 1 if args.greedy else args.retries
            try:
                d = None
                for attempt in range(attempts):
                    wav = generate(api_base, model, word, instruct, speaker, language,
                                   sample=False if args.greedy else None)
                    to_mp3(wav, out_path, bitrate, target_peak)
                    d = duration(out_path)
                    if lo <= d <= hi:
                        break
                    retried += 1
                done += 1
                flag = "" if lo <= d <= hi else "  (out-of-range kept)"
                print(f"[ok] {accent:2s} {word:16s} {time.time()-t0:4.1f}s "
                      f"dur={d:.2f}s -> {out_path.relative_to(ROOT)}{flag}")
            except Exception as exc:  # noqa: BLE001
                failed += 1
                errors.append(f"{accent} {word}: {exc}")
                print(f"[fail] {accent:2s} {word:16s} {exc}")

    print(f"\ndone={done} skipped={skipped} failed={failed} retried={retried}")
    if errors:
        print("failures:")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
