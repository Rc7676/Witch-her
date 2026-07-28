#!/usr/bin/env python3
"""Catch unresolved Compose/Kotlin references before the slow Android build.

The :app module can only be compiled where the Android SDK is reachable, so a
missing import otherwise costs a full CI round trip. This is a heuristic
scanner, not a compiler: it flags capitalized identifiers that are called or
dereferenced but are neither imported, declared elsewhere in the same package,
nor on the builtin allowlist.

Usage: python3 tools/check_imports.py [source_root ...]
"""

import re
import sys
from pathlib import Path

# Names available without an import (kotlin.*, kotlin.collections.*, java.lang.*)
BUILTINS = {
    "Any", "Array", "Boolean", "Byte", "Char", "CharSequence", "Comparable",
    "Double", "Enum", "Exception", "Float", "IllegalArgumentException",
    "IllegalStateException", "Int", "IntArray", "Iterable", "LinkedHashMap",
    "List", "Long", "Map", "Math", "MutableList", "MutableMap", "MutableSet",
    "Number", "Nothing", "Pair", "Result", "RuntimeException", "Set", "Short",
    "String", "StringBuilder", "System", "Throwable", "Triple", "Unit",
    "ArrayList", "HashMap", "HashSet", "Regex", "Sequence", "Thread",
    "IndexOutOfBoundsException", "NullPointerException", "Error",
}

DECL_RE = re.compile(
    r"^\s*(?:@\w+\s+)*"
    r"(?:public |internal |private |abstract |sealed |open |data |value |inline |const |expect |actual )*"
    r"(?:class|object|interface|enum class|fun|val|var|typealias)\s+([A-Z]\w*)",
    re.MULTILINE,
)
# SCREAMING_SNAKE names are enum entries or constants, never Compose calls.
CONSTANT_RE = re.compile(r"^[A-Z][A-Z0-9_]*$")
IMPORT_RE = re.compile(r"^import\s+([\w.]+)(?:\s+as\s+(\w+))?", re.MULTILINE)
PACKAGE_RE = re.compile(r"^package\s+([\w.]+)", re.MULTILINE)
# Capitalized identifier used as a call or receiver, not preceded by a dot.
USE_RE = re.compile(r"(?<![\w.])([A-Z]\w*)\s*(?=[.(])")
STRING_RE = re.compile(r'"""(?:.|\n)*?"""|"(?:\\.|[^"\\])*"', re.MULTILINE)
COMMENT_RE = re.compile(r"//[^\n]*|/\*(?:.|\n)*?\*/", re.MULTILINE)


def strip_noise(text: str) -> str:
    """Remove comments and string literals so their contents aren't scanned."""
    text = COMMENT_RE.sub("", text)
    return STRING_RE.sub('""', text)


def scan(roots):
    files = []
    for root in roots:
        files.extend(sorted(Path(root).rglob("*.kt")))
    if not files:
        print(f"check_imports: no Kotlin files under {', '.join(map(str, roots))}")
        return 1

    # Top-level declarations per package: same-package symbols need no import.
    package_decls: dict[str, set[str]] = {}
    sources: dict[Path, str] = {}
    for path in files:
        raw = path.read_text(encoding="utf-8")
        sources[path] = raw
        body = strip_noise(raw)
        pkg_match = PACKAGE_RE.search(body)
        pkg = pkg_match.group(1) if pkg_match else ""
        package_decls.setdefault(pkg, set()).update(DECL_RE.findall(body))

    problems = []
    for path in files:
        body = strip_noise(sources[path])
        pkg_match = PACKAGE_RE.search(body)
        pkg = pkg_match.group(1) if pkg_match else ""

        known = set(BUILTINS) | package_decls.get(pkg, set())
        for dotted, alias in IMPORT_RE.findall(body):
            known.add(alias or dotted.rsplit(".", 1)[-1])
            # A star import makes everything in that package available; we
            # cannot resolve it, so treat the file as unverifiable.
            if dotted.endswith("*"):
                known.add("*")
        if "*" in known:
            continue

        # Names bound inside this file (generics, local classes, enum entries).
        known.update(DECL_RE.findall(body))

        seen = set()
        for line_no, line in enumerate(body.splitlines(), start=1):
            for name in USE_RE.findall(line):
                if name in known or name in seen or CONSTANT_RE.match(name):
                    continue
                seen.add(name)
                problems.append((path, line_no, name))

    if problems:
        print("check_imports: possibly unresolved references\n")
        for path, line_no, name in problems:
            print(f"  {path}:{line_no}  {name}")
        print(
            f"\n{len(problems)} suspicious reference(s). Add the missing import, "
            "or update BUILTINS in tools/check_imports.py if this is a false positive."
        )
        return 1

    print(f"check_imports: {len(files)} file(s) OK")
    return 0


if __name__ == "__main__":
    roots = sys.argv[1:] or ["app/src/main", "core/src/main"]
    sys.exit(scan(roots))
