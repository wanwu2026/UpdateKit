#!/usr/bin/env python3
"""UpdateKit 核心逻辑验证（本机无 JDK，用 Python 严格复刻 Java 算法做交叉验证）。

覆盖：
1. Java CanonicalJson 复刻 vs Python json.dumps 字节级一致（含中文/emoji/控制字符/代理对）
2. Base32 编解码 round-trip（对齐 RFC4648）
3. 更新清单 / 公告 签名→验签端到端（含中文 notes，防止 ensure_ascii 不一致导致验签失败）
4. 篡改内容后验签必须失败
5. 公告签名体只取六字段（id 变更不影响验签）

CI 里另有 :sdk:testDebugUnitTest 跑真 Java JUnit；这里验证的是算法可移植性与签名闭环。
"""
import json
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import canonical_java as cj
import updatekit_tool as t

PASS = 0
FAIL = 0


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print("  PASS", name)
    else:
        FAIL += 1
        print("  FAIL", name, detail)


def test_canonical_crosscheck():
    print("[1] Java CanonicalJson 复刻 vs Python json.dumps（字节级）")
    cases = [
        {"k": "中文"}, {"k": "emoji 😀"}, {"k": "tab\tnewline\n"},
        {"k": 'quote"backslash\\'}, {"k": "DEL\x7f"}, {"k": "ctrl\x01"},
        {"k": "mix 中文😀 ok"}, {"k": "é ñ ü"}, {"k": "／"},
        {"k": "product:my_app"}, {"k": "simple"}, {"k": ""},
        {"b": 2, "a": 1, "c": True, "d": None},  # key 排序 + 类型
        {"product": "my_app", "version_code": 12, "force_popup": True},
    ]
    for obj in cases:
        java = cj.canonical_bytes(obj)
        py = t.canonical(obj)
        check("canonical " + repr(obj)[:40], java == py,
              "\n    java=%r\n    py  =%r" % (java, py))


def test_base32_roundtrip():
    print("[2] Base32 RFC4648 向量")
    vectors = [
        (b"", ""), (b"f", "MY======"), (b"fo", "MZXQ===="),
        (b"foo", "MZXW6==="), (b"foob", "MZXW6YQ="), (b"fooba", "MZXW6YTB"),
        (b"foobar", "MZXW6YTBOI======"),
    ]
    for raw, expected in vectors:
        enc = t.b32encode(raw)
        check("b32enc " + repr(raw), enc.rstrip("=") == expected.rstrip("="),
              "%r != %r" % (enc, expected))
        check("b32dec " + repr(raw), t.b32decode(enc) == raw)


def test_sign_verify():
    print("[3] 更新清单 / 公告 签名闭环（含中文）")
    with tempfile.TemporaryDirectory() as td:
        priv, pub = t.gen_keypair(td)
        # 更新清单
        m = t.sign_manifest(priv, "my_app", "1.2.0", 12, "a" * 64,
                            "https://cdn/app.apk", "b" * 64, "修复中文乱码问题", "stable")
        check("manifest sign valid", t.check(m, pub, "_signature"))
        bad = dict(m)
        bad["notes"] = "被篡改"
        check("manifest tamper rejected", not t.check(bad, pub, "_signature"))
        # 公告
        n = t.sign_notice(priv, "my_app", "版本公告", "中文内容不乱码", "urgent", "true", 1730000000000)
        check("notice sign valid", t.check(n, pub, "signature", notice=True))
        badn = dict(n)
        badn["content"] = "钓鱼内容"
        check("notice tamper rejected", not t.check(badn, pub, "signature", notice=True))
        # 公告六字段：改 id 不影响验签（id 不进签名体）
        n2 = dict(n)
        n2["id"] = 999
        check("notice id not signed", t.check(n2, pub, "signature", notice=True))
        # Java 复刻的规范字节 == 服务端规范字节（确保端到端同一字节）
        serv = t.canonical({k: n[k] for k in t.NOTICE_FIELDS})
        java = cj.canonical_bytes({k: n[k] for k in t.NOTICE_FIELDS})
        check("notice canonical java==server", java == serv)


def main():
    test_canonical_crosscheck()
    test_base32_roundtrip()
    test_sign_verify()
    print("\nRESULT: %d passed, %d failed" % (PASS, FAIL))
    sys.exit(0 if FAIL == 0 else 1)


if __name__ == "__main__":
    main()
