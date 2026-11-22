package com.example.updatekit.core;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/** RFC 4648 Base32（A-Z2-7，无 '=' 填充）。签名编码格式，与服务端一致。 */
public final class Base32 {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {}

    public static String encode(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(ALPHABET.charAt((buffer >>> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        return sb.toString();
    }

    /** 解码，容忍大小写、'-'、空白与 '='。 */
    public static byte[] decode(String text) {
        if (text == null) throw new IllegalArgumentException("Base32 输入为 null");
        String s = text.trim().toUpperCase(Locale.US)
                .replace("=", "").replace("-", "").replace(" ", "")
                .replace("\r", "").replace("\n", "");
        if (s.isEmpty()) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream((s.length() * 5) / 8 + 1);
        int buffer = 0, bitsLeft = 0;
        for (int i = 0; i < s.length(); i++) {
            int idx = ALPHABET.indexOf(s.charAt(i));
            if (idx < 0) throw new IllegalArgumentException("非法 Base32 字符: " + s.charAt(i));
            buffer = (buffer << 5) | idx;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >>> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
