#!/usr/bin/env python3
"""Strict Python replica of the Java CanonicalJson algorithm (sdk/.../CanonicalJson.java).

This is NOT json.dumps — it mirrors the Java source character by character so we can
prove (in this JDK-less environment) that the Java implementation emits exactly the
same bytes Python's json.dumps(sort_keys=True, separators=(',',':')) produces.

The CI job compiles and runs real JUnit tests against these same vectors on a runner
that has a JDK; here we cross-check the algorithm and the sign/verify round-trip.
"""

_SIMPLE_ESCAPES = {
    '"': '\\"', "\\": "\\\\", "\b": "\\b", "\f": "\\f",
    "\n": "\\n", "\r": "\\r", "\t": "\\t",
}


def _quote(s: str) -> str:
    """Mirror CanonicalJson.quote(StringBuilder, String)."""
    out = ['"']
    for ch in s:
        if ch in _SIMPLE_ESCAPES:
            out.append(_SIMPLE_ESCAPES[ch])
        elif 0x20 <= ord(ch) <= 0x7E:
            out.append(ch)
        else:
            # Java iterates UTF-16 code units; for non-BMP it emits each surrogate
            # as its own \\uXXXX (same as Python ensure_ascii=True on a str).
            cp = ord(ch)
            if cp <= 0xFFFF:
                out.append("\\u%04x" % cp)
            else:
                cp -= 0x10000
                hi = 0xD800 + (cp >> 10)
                lo = 0xDC00 + (cp & 0x3FF)
                out.append("\\u%04x\\u%04x" % (hi, lo))
    out.append('"')
    return "".join(out)


def _value(v) -> str:
    if v is None:
        return "null"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, str):
        return _quote(v)
    if isinstance(v, int):
        return str(v)
    raise TypeError("unsupported type: %r" % type(v))


def canonical(obj: dict) -> str:
    """Mirror CanonicalJson.canonical(Map): sorted keys, no spaces, ensure_ascii."""
    parts = []
    for k in sorted(obj.keys()):
        parts.append(_quote(k) + ":" + _value(obj[k]))
    return "{" + ",".join(parts) + "}"


def canonical_bytes(obj: dict) -> bytes:
    return canonical(obj).encode("utf-8")
