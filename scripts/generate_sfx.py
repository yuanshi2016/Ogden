#!/usr/bin/env python3
"""Generate Chinese motivational sound-effect mp3s via the local Qwen TTS WebUI API.

Mirrors scripts/generate_word_audio.py: same ffmpeg post-processing, but for a
handful of short encouragement phrases instead of per-word accents.

Requires the WebUI running with --api. Edit sfx_manifest.json to change phrases,
speaker, or tone. Files land where the app expects them:
    app/src/main/assets/audio/sfx/{name}.mp3

Examples:
    python scripts/generate_sfx.py            # generate all missing sfx
    python scripts/generate_sfx.py --force    # overwrite existing
    python scripts/generate_sfx.py --dry-run
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
MANIFEST = HERE / "sfx_manifest.json"


def af_chain(extra: str = "") -> str:
    trim = ("silenceremove=start_periods=1:start_duration=0:start_threshold=-45dB,"
            "areverse,"
            "silenceremove=start_periods=1:start_duration=0:start_threshold=-45dB,"
            "areverse")
    return trim + (("," + extra) if extra else "")


def peak_gain_db(path: str, target_peak_db: float) -> float:
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


def generate(api_base: str, model: str, text: str, instruct: str,
             speaker: str, language: str, retries: int = 3) -> bytes:
    payload = {"model_name": model, "text": text, "instruct": instruct,
               "speaker": speaker, "language": language}
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
    ap.add_argument("--force", action="store_true", help="overwrite existing files")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--retries", type=int, default=3)
    args = ap.parse_args()

    m = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    api_base = m["api_base"].rstrip("/")
    model = m["model_name"]
    speaker = m["speaker"]
    language = m.get("language") or "auto"
    instruct = m["instruct"]
    bitrate = m.get("mp3_bitrate", "48k")
    target_peak = float(m.get("target_peak_db", -6.0))
    out_dir = ROOT / m["out_dir"]

    done = skipped = failed = 0
    for item in m["sfx"]:
        name, text = item["name"], item["text"]
        out_path = out_dir / f"{name}.mp3"
        if out_path.exists() and not args.force:
            skipped += 1
            print(f"[skip] {name}")
            continue
        if args.dry_run:
            print(f"[dry-run] {name} -> {out_path.relative_to(ROOT)}")
            continue
        out_dir.mkdir(parents=True, exist_ok=True)
        t0 = time.time()
        try:
            wav = generate(api_base, model, text, instruct, speaker, language,
                           retries=args.retries)
            to_mp3(wav, out_path, bitrate, target_peak)
            d = duration(out_path)
            done += 1
            print(f"[ok] {name:10s} {time.time()-t0:4.1f}s dur={d:.2f}s "
                  f"-> {out_path.relative_to(ROOT)}")
        except Exception as exc:  # noqa: BLE001
            failed += 1
            print(f"[fail] {name:10s} {exc}")

    print(f"\ndone={done} skipped={skipped} failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
