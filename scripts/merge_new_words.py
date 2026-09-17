#!/usr/bin/env python3
"""Merge scripts/new_words.json into app/src/main/assets/ogden_words.json.

The 850 original entries are left byte-for-byte untouched; entries already in
the file whose "w" matches a new word are replaced (so renaming/re-coding works).
Edit new_words.json (add/remove/change words) then re-run this script.

    python scripts/merge_new_words.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
NEW_WORDS = HERE / "new_words.json"
TARGET = ROOT / "app" / "src" / "main" / "assets" / "ogden_words.json"


def split_objects(raw: str) -> list[str]:
    """Split a JSON array file into its top-level object source texts, verbatim."""
    start, end = raw.index("["), raw.rindex("]")
    inner = raw[start + 1:end]
    objs: list[str] = []
    depth = 0
    in_str = False
    esc = False
    cur: list[str] = []
    for ch in inner:
        if in_str:
            cur.append(ch)
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
            cur.append(ch)
        elif ch == "{":
            depth += 1
            cur.append(ch)
        elif ch == "}":
            depth -= 1
            cur.append(ch)
            if depth == 0:
                objs.append("".join(cur))
                cur = []
        elif depth > 0:
            cur.append(ch)
    return objs


def dump_entry(e: dict) -> str:
    s = ",\n".join('                  ' + json.dumps(x, ensure_ascii=False) for x in e["s"])
    return (
        "    {\n"
        f'        "w":  {json.dumps(e["w"], ensure_ascii=False)},\n'
        f'        "c":  {json.dumps(e["c"], ensure_ascii=False)},\n'
        f'        "zh":  {json.dumps(e["zh"], ensure_ascii=False)},\n'
        f'        "en":  {json.dumps(e["en"], ensure_ascii=False)},\n'
        f'        "ex":  {json.dumps(e["ex"], ensure_ascii=False)},\n'
        f'        "exz":  {json.dumps(e["exz"], ensure_ascii=False)},\n'
        '        "s":  [\n'
        f"{s}\n"
        "              ]\n"
        "    }"
    )


def obj_word(text: str) -> str | None:
    try:
        return json.loads(text).get("w", "").lower()
    except Exception:  # noqa: BLE001
        return None


def main() -> int:
    new = json.loads(NEW_WORDS.read_text(encoding="utf-8"))

    raw = TARGET.read_text(encoding="utf-8-sig")
    objs = split_objects(raw)
    new_words = {e["w"].lower() for e in new}
    original = [o for o in objs if obj_word(o) not in new_words]
    removed = len(objs) - len(original)

    new_texts = [dump_entry(e) for e in new]
    body = ",\n".join(["    " + o for o in original] + new_texts)
    out = "[\n" + body + "\n]\n"
    TARGET.write_text("\ufeff" + out, encoding="utf-8")
    print(f"original kept: {len(original)}  supplement removed: {removed}  added: {len(new)}")
    print(f"total now: {len(original) + len(new)}  -> {TARGET.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
