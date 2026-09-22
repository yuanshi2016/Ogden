#!/usr/bin/env python3
"""Generate example-sentence mp3s for every word via the local Qwen TTS WebUI API.

Reads example sentences from app/src/main/assets/ogden_words.json and writes:
    app/src/main/assets/audio/examples/{us,uk}/{slug}.mp3
where slug mirrors MainActivity.localAudioPath() (lowercase, non-alnum -> "_").

Requests are strictly sequential (single process): the TTS server serialises them anyway.

Examples:
    python scripts/generate_example_audio.py                  # generate missing us+uk
    python scripts/generate_example_audio.py --only us        # US only
    python scripts/generate_example_audio.py --word have      # one word
    python scripts/generate_example_audio.py --limit 10       # first N files, to smoke-test
    python scripts/generate_example_audio.py --force          # overwrite existing
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

from generate_word_audio import audio_filename, duration, generate, to_mp3

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
MANIFEST = HERE / "example_audio_manifest.json"
WORDS = ROOT / "app/src/main/assets/ogden_words.json"
ACCENTS = {"us": "instruct_us", "uk": "instruct_uk"}


def load_examples() -> list[tuple[str, str]]:
    """(word, example) pairs from the word bank, deduped by output slug."""
    data = json.loads(WORDS.read_text(encoding="utf-8-sig"))
    out: list[tuple[str, str]] = []
    seen: set[str] = set()
    for item in data:
        word, example = item.get("w"), item.get("ex")
        if not word or not example:
            continue
        slug = audio_filename(example)
        if slug and slug not in seen:
            seen.add(slug)
            out.append((word, example))
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", default=str(MANIFEST))
    ap.add_argument("--only", choices=["us", "uk"], help="generate one accent only")
    ap.add_argument("--word", action="append", help="generate a single word (repeatable)")
    ap.add_argument("--limit", type=int, help="stop after N generated files")
    ap.add_argument("--force", action="store_true", help="overwrite existing files")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    m = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    api_base = m["api_base"].rstrip("/")
    model = m["model_name"]
    speaker = m["speaker"]
    language = m.get("language") or "english"
    bitrate = m.get("mp3_bitrate", "48k")
    target_peak = float(m.get("target_peak_db", -6.0))
    lo = float(m.get("min_duration", 0.8))
    hi = float(m.get("max_duration", 15.0))
    accents = [args.only] if args.only else list(ACCENTS)

    pairs = load_examples()
    if args.word:
        wanted = {w.lower() for w in args.word}
        pairs = [(w, ex) for w, ex in pairs if w.lower() in wanted]

    done = skipped = failed = 0
    errors: list[str] = []
    for word, text in pairs:
        for accent in accents:
            instruct = m[ACCENTS[accent]]
            out_dir = ROOT / m[f"{accent}_dir"]
            out_path = out_dir / f"{audio_filename(text)}.mp3"
            if out_path.exists() and not args.force:
                skipped += 1
                continue
            if args.dry_run:
                print(f"[dry-run] {accent} {word:16s} {text!r} -> {out_path.relative_to(ROOT)}")
                continue
            if args.limit and done >= args.limit:
                print(f"limit {args.limit} reached")
                return 0
            out_dir.mkdir(parents=True, exist_ok=True)
            t0 = time.time()
            try:
                wav = generate(api_base, model, text, instruct, speaker, language)
                to_mp3(wav, out_path, bitrate, target_peak)
                d = duration(out_path)
                done += 1
                flag = "" if lo <= d <= hi else "  (out-of-range kept)"
                print(f"[ok] {accent:2s} {word:16s} {time.time()-t0:4.1f}s "
                      f"dur={d:.2f}s -> {out_path.relative_to(ROOT)}{flag}")
            except Exception as exc:  # noqa: BLE001
                failed += 1
                errors.append(f"{accent} {word}: {exc}")
                print(f"[fail] {accent:2s} {word:16s} {exc}")

    print(f"\ndone={done} skipped={skipped} failed={failed}")
    if errors:
        print("failures:")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
