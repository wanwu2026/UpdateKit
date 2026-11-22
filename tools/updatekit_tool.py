#!/usr/bin/env python3
"""UpdateKit 配套工具：密钥生成 / 造签名清单与公告 / 校验。

与 OpenHub 的 plugins.cardkey.signing 完全同构：
  RSA-2048 / SHA-256 (RSASSA-PKCS1-v1_5) / RFC4648 Base32 无填充。
私钥只留服务器或测试目录，绝不进客户端。

子命令：
  keygen [--out DIR]                       生成 private.pem + public_key_base64.txt
  sign-manifest --private-key P ...        造一条已签名更新清单（JSON，含 _signature）
  sign-notice --private-key P ...          造一条已签名公告（JSON，含 signature/id）
  check FILE --public-key-base64 B ...     验签（--notice 时按六字段）
  vectors                                  输出规范 JSON 测试向量（JSON，供 Java 单测比对）
"""
import argparse
import base64
import json
import subprocess
import sys
import tempfile
from pathlib import Path

ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"


# ---------- 编码 ----------

def b32encode(data: bytes) -> str:
    bits = "".join(format(b, "08b") for b in data)
    bits += "0" * ((-len(bits)) % 5)
    return "".join(ALPHABET[int(bits[i:i + 5], 2)] for i in range(0, len(bits), 5))


def b32decode(text: str) -> bytes:
    s = text.strip().upper().replace("=", "").replace("-", "").replace(" ", "")
    bits = "".join(format(ALPHABET.index(c), "05b") for c in s)
    return bytes(int(bits[i:i + 8], 2) for i in range(0, len(bits) - 7, 8))


def canonical(obj: dict) -> bytes:
    """与服务端/Java CanonicalJson 一致的规范字节。"""
    return json.dumps(obj, sort_keys=True, separators=(",", ":")).encode("utf-8")


# ---------- 密码学 ----------

def _openssl(args, data=None):
    return subprocess.run(["openssl"] + args, input=data, capture_output=True, check=True).stdout


def gen_keypair(out_dir: str):
    d = Path(out_dir)
    d.mkdir(parents=True, exist_ok=True)
    priv = d / "private.pem"
    _openssl(["genrsa", "-out", str(priv), "2048"])
    der = _openssl(["rsa", "-in", str(priv), "-pubout", "-outform", "DER"])
    pub = base64.b64encode(der).decode("ascii")
    (d / "public_key_base64.txt").write_text(pub)
    return str(priv), pub


def public_b64(priv_path: str) -> str:
    der = _openssl(["rsa", "-in", priv_path, "-pubout", "-outform", "DER"])
    return base64.b64encode(der).decode("ascii")


def sign_bytes(data: bytes, priv_path: str) -> str:
    return b32encode(_openssl(["dgst", "-sha256", "-sign", priv_path], data))


def verify_bytes(data: bytes, sig_b32: str, pub_b64: str) -> bool:
    with tempfile.TemporaryDirectory() as td:
        pub = Path(td) / "pub.der"
        sig = Path(td) / "sig.bin"
        dat = Path(td) / "data.bin"
        pub.write_bytes(base64.b64decode(pub_b64))
        sig.write_bytes(b32decode(sig_b32))
        dat.write_bytes(data)
        r = subprocess.run(["openssl", "dgst", "-sha256", "-verify", str(pub),
                            "-signature", str(sig), str(dat)], capture_output=True)
        return r.returncode == 0


# ---------- 签名体 ----------

NOTICE_FIELDS = ("product", "title", "content", "level", "force_popup", "published_at")


def sign_manifest(priv, product, version, version_code, fingerprint, apk_url, sha256, notes, channel):
    m = dict(product=product, version=version, version_code=int(version_code), channel=channel,
             signer_fingerprint=fingerprint, apk_url=apk_url, sha256=sha256, notes=notes)
    m["_signature"] = sign_bytes(canonical(m), priv)
    return m


def sign_notice(priv, product, title, content, level, force, published_at):
    n = dict(product=product, title=title, content=content, level=level,
             force_popup=force in (True, "true", "1", "yes"), published_at=int(published_at))
    n["signature"] = sign_bytes(canonical(n), priv)
    n["id"] = 1
    return n


def check(obj: dict, pub_b64: str, sig_field: str, notice: bool = False) -> bool:
    data = dict(obj)
    sig = data.pop(sig_field)
    if notice:
        data = {k: data.get(k) for k in NOTICE_FIELDS}
    return verify_bytes(canonical(data), sig, pub_b64)


# ---------- CLI ----------

def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    g = sub.add_parser("keygen")
    g.add_argument("--out", default=".verify_keys")

    sm = sub.add_parser("sign-manifest")
    sm.add_argument("--private-key", required=True)
    sm.add_argument("--product", default="my_app")
    sm.add_argument("--version", default="1.2.0")
    sm.add_argument("--version-code", type=int, default=12)
    sm.add_argument("--fingerprint", default="a" * 64)
    sm.add_argument("--apk-url", default="https://cdn/app.apk")
    sm.add_argument("--sha256", default="b" * 64)
    sm.add_argument("--notes", default="修复若干问题")
    sm.add_argument("--channel", default="stable")

    sn = sub.add_parser("sign-notice")
    sn.add_argument("--private-key", required=True)
    sn.add_argument("--product", default="my_app")
    sn.add_argument("--title", default="版本公告")
    sn.add_argument("--content", default="中文内容不乱码")
    sn.add_argument("--level", default="info")
    sn.add_argument("--force", default="false")
    sn.add_argument("--published-at", type=int, default=1730000000000)

    c = sub.add_parser("check")
    c.add_argument("json_file")
    c.add_argument("--public-key-base64", required=True)
    c.add_argument("--sig-field", default="signature")
    c.add_argument("--notice", action="store_true")

    sub.add_parser("vectors")

    a = p.parse_args()
    if a.cmd == "keygen":
        priv, pub = gen_keypair(a.out)
        print("private:", priv)
        print("public_key_base64:", pub)
    elif a.cmd == "sign-manifest":
        m = sign_manifest(a.private_key, a.product, a.version, a.version_code,
                          a.fingerprint, a.apk_url, a.sha256, a.notes, a.channel)
        print(json.dumps(m, ensure_ascii=False, indent=2))
    elif a.cmd == "sign-notice":
        n = sign_notice(a.private_key, a.product, a.title, a.content,
                        a.level, a.force, a.published_at)
        print(json.dumps(n, ensure_ascii=False, indent=2))
    elif a.cmd == "check":
        ok = check(json.loads(Path(a.json_file).read_text()), a.public_key_base64,
                   a.sig_field, a.notice)
        print("signature valid:", ok)
        sys.exit(0 if ok else 1)
    elif a.cmd == "vectors":
        cases = ["中文", "emoji 😀", "tab\tnewline\n", "quote\"backslash\\",
                 "DEL\x7f", "ctrl\x01", "mix 中文😀 ok", "é ñ ü", "／",
                 "product:my_app", "simple", ""]
        out = [{"obj": {"k": s}, "canonical": canonical({"k": s}).decode("utf-8")} for s in cases]
        print(json.dumps(out, ensure_ascii=False))


if __name__ == "__main__":
    main()
