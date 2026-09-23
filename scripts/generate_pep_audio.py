#!/usr/bin/env python3
"""Generate US/UK mp3s for PEP curriculum words + phrases via local Qwen TTS.

Words  → app/src/main/assets/audio/{us,uk}/{slug}.mp3
Phrases → app/src/main/assets/audio/phrases/{us,uk}/{slug}.mp3

Slug mirrors Audio.kt localAudioPath (lowercase, non-alnum → "_").
Reuses generate_word_audio.generate / to_mp3 / duration / audio_filename.

Examples:
    python scripts/generate_pep_audio.py --dry-run
    python scripts/generate_pep_audio.py --limit 4 --only us
    python scripts/generate_pep_audio.py --words-only
    python scripts/generate_pep_audio.py --phrases-only --limit 2
    python scripts/generate_pep_audio.py --force
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from generate_word_audio import audio_filename, duration, generate, to_mp3

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
MANIFEST = HERE / "pep_audio_manifest.json"
ACCENTS = {"us": "instruct_us", "uk": "instruct_uk"}


def probe_tts(api_base: str) -> None:
    """Fail fast if the WebUI API is down (do not silently report success)."""
    base = api_base.rstrip("/")
    # Prefer /models (OpenAI-compatible); fall back to host /docs
    candidates = [
        f"{base}/models",
        "http://127.0.0.1:7860/docs",
    ]
    last_err: Exception | None = None
    for url in candidates:
        try:
            with urllib.request.urlopen(url, timeout=5) as r:
                if 200 <= r.status < 300:
                    print(f"[probe] TTS ok → {url} ({r.status})")
                    return
        except Exception as exc:  # noqa: BLE001
            last_err = exc
    raise SystemExit(
        f"TTS not reachable (tried {candidates}). "
        f"Start WebUI with --api. Last error: {last_err}"
    )


def load_curriculum(files: list[str]) -> tuple[list[str], list[str]]:
    """Return (words, phrases) deduped, preserving first-seen order."""
    words: list[str] = []
    phrases: list[str] = []
    seen_w: set[str] = set()
    seen_p: set[str] = set()

    for rel in files:
        path = ROOT / rel
        if not path.exists():
            print(f"[warn] missing {rel}", file=sys.stderr)
            continue
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        # pep_extra_words.json is a flat OgdenWord-like array
        if isinstance(data, list) and data and isinstance(data[0], dict) and "w" in data[0]:
            for item in data:
                w = (item.get("w") or "").strip()
                if w and w.lower() not in seen_w:
                    seen_w.add(w.lower())
                    words.append(w)
            continue
        units = data if isinstance(data, list) else data.get("units", [])
        for unit in units:
            for w in unit.get("words", []):
                if isinstance(w, str) and w.strip() and w.lower() not in seen_w:
                    seen_w.add(w.lower())
                    words.append(w.strip())
            for p in unit.get("phrases", []):
                en = p.get("en") if isinstance(p, dict) else p
                if isinstance(en, str) and en.strip():
                    slug = audio_filename(en)
                    if slug and slug not in seen_p:
                        seen_p.add(slug)
                        phrases.append(en.strip())
    return words, phrases


def is_single_word(text: str) -> bool:
    return bool(re.fullmatch(r"[A-Za-z][A-Za-z0-9-]*", text))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", default=str(MANIFEST))
    ap.add_argument("--only", choices=["us", "uk"], help="one accent only")
    ap.add_argument("--word", action="append", help="limit to these word keys (repeatable)")
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--greedy", action="store_true",
                    help="do_sample=false; more stable for short words")
    ap.add_argument("--retries", type=int, default=4)
    ap.add_argument("--limit", type=int, help="stop after N generated files")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--words-only", action="store_true")
    ap.add_argument("--phrases-only", action="store_true")
    ap.add_argument("--skip-probe", action="store_true")
    args = ap.parse_args()

    m = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    api_base = m["api_base"].rstrip("/")
    model = m["model_name"]
    speaker = m["speaker"]
    language = m.get("language") or "english"
    bitrate = m.get("mp3_bitrate", "48k")
    target_peak = float(m.get("target_peak_db", -6.0))
    word_lo = float(m.get("word_min_duration", 0.2))
    word_hi = float(m.get("word_max_duration", 1.6))
    phrase_lo = float(m.get("phrase_min_duration", 0.8))
    phrase_hi = float(m.get("phrase_max_duration", 15.0))
    accents = [args.only] if args.only else list(ACCENTS)

    words, phrases = load_curriculum(m["curriculum_files"])
    if args.word:
        wanted = {w.lower() for w in args.word}
        words = [w for w in words if w.lower() in wanted]
        # also keep phrases that contain those words? no — word filter is for words only

    jobs: list[tuple[str, str, Path, float, float]] = []
    # kind text out_path lo hi
    if not args.phrases_only:
        for w in words:
            if not is_single_word(w):
                # multiword keys (rare) go to phrase dirs so localAudioPath can find them
                for accent in accents:
                    out_dir = ROOT / m[f"phrase_{accent}_dir"]
                    out_path = out_dir / f"{audio_filename(w)}.mp3"
                    jobs.append(("phrase", w, out_path, phrase_lo, phrase_hi))
                continue
            for accent in accents:
                out_dir = ROOT / m[f"{accent}_dir"]
                out_path = out_dir / f"{audio_filename(w)}.mp3"
                jobs.append(("word", w, out_path, word_lo, word_hi))

    if not args.words_only:
        for text in phrases:
            for accent in accents:
                out_dir = ROOT / m[f"phrase_{accent}_dir"]
                out_path = out_dir / f"{audio_filename(text)}.mp3"
                jobs.append(("phrase", text, out_path, phrase_lo, phrase_hi))

    if not args.dry_run and not args.skip_probe:
        probe_tts(api_base)

    done = skipped = failed = retried = 0
    errors: list[str] = []
    for kind, text, out_path, lo, hi in jobs:
        accent = "us" if "/us/" in str(out_path).replace("\\", "/") or out_path.parts[-2] == "us" else "uk"
        # derive accent from parent folder name
        accent = out_path.parent.name  # us | uk
        instruct = m[ACCENTS[accent]]
        if out_path.exists() and not args.force:
            skipped += 1
            continue
        if args.dry_run:
            print(f"[dry-run] {kind:6s} {accent} {text!r} -> {out_path.relative_to(ROOT)}")
            continue
        if args.limit is not None and done >= args.limit:
            print(f"limit {args.limit} reached")
            break
        out_path.parent.mkdir(parents=True, exist_ok=True)
        t0 = time.time()
        attempts = 1 if args.greedy else args.retries
        try:
            d = None
            for attempt in range(attempts):
                wav = generate(
                    api_base, model, text, instruct, speaker, language,
                    sample=False if args.greedy else None,
                )
                to_mp3(wav, out_path, bitrate, target_peak)
                d = duration(out_path)
                if lo <= d <= hi:
                    break
                retried += 1
            done += 1
            flag = "" if d is not None and lo <= d <= hi else "  (out-of-range kept)"
            print(
                f"[ok] {kind:6s} {accent:2s} {time.time()-t0:5.1f}s "
                f"dur={d:.2f}s {text!r} -> {out_path.relative_to(ROOT)}{flag}"
            )
        except Exception as exc:  # noqa: BLE001
            failed += 1
            errors.append(f"{accent} {kind} {text!r}: {exc}")
            print(f"[fail] {accent:2s} {kind} {text!r}: {exc}")

    print(f"\ndone={done} skipped={skipped} failed={failed} retried={retried} jobs={len(jobs)}")
    if errors:
        print("failures:")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
