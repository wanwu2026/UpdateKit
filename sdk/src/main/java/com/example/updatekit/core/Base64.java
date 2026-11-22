package com.example.updatekit.core;

/** 轻量 Base64 解码（仅解码 X.509 公钥的 Base64 文本用），避免依赖 android.util.Base64 之外的东西。 */
public final class Base64 {
    private static final String TAB =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private Base64() {}

    public static byte[] decode(String text) {
        if (text == null) return new byte[0];
        String s = text.replace("\r", "").replace("\n", " ").replace(" ", "")
                .replace("\t", "").replace("=", "");
        ByteArrayOutputStreamLite out = new ByteArrayOutputStreamLite((s.length() * 3) / 4 + 1);
        int buffer = 0, bits = 0;
        for (int i = 0; i < s.length(); i++) {
            int idx = TAB.indexOf(s.charAt(i));
            if (idx < 0) throw new IllegalArgumentException("非法 Base64 字符: " + s.charAt(i));
            buffer = (buffer << 6) | idx;
            bits += 6;
            if (bits >= 8) {
                out.write((buffer >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    /** 避免引入 java.io.ByteArrayOutputStream 的复杂度，用最小可变字节缓冲。 */
    private static final class ByteArrayOutputStreamLite {
        private byte[] buf;
        private int size;
        ByteArrayOutputStreamLite(int cap) { buf = new byte[Math.max(16, cap)]; }
        void write(int b) {
            if (size == buf.length) {
                byte[] next = new byte[buf.length * 2];
                System.arraycopy(buf, 0, next, 0, size);
                buf = next;
            }
            buf[size++] = (byte) b;
        }
        byte[] toByteArray() {
            byte[] r = new byte[size];
            System.arraycopy(buf, 0, r, 0, size);
            return r;
        }
    }
}
