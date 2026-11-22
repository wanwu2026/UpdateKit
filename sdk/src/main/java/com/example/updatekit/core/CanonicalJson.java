package com.example.updatekit.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规范化 JSON：字节级复刻 Python 的
 *   json.dumps(obj, sort_keys=True, separators=(",", ":"))   # ensure_ascii 默认 True
 *
 * 服务端（OpenHub）用它对清单/公告签名，客户端必须用完全相同的字节重建才能验签通过。
 * 三个要点：① key 按字典序排 ② 无空格分隔 ③ 非 ASCII 转成小写 unicode 转义序列（含代理对）。
 *
 * 只支持签名体里出现的标量类型：String / Number(Integer、Long) / Boolean / null。
 */
public final class CanonicalJson {
    private CanonicalJson() {}

    public static byte[] toBytes(Map<String, ?> obj) {
        return canonical(obj).getBytes(StandardCharsets.UTF_8);
    }

    public static String canonical(Map<String, ?> obj) {
        List<String> keys = new ArrayList<>(obj.keySet());
        // Python sort_keys 按 Unicode 码位排；签名体字段均为 ASCII，Java 的 UTF-16 顺序等价
        java.util.Collections.sort(keys);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String k : keys) {
            if (!first) sb.append(',');
            first = false;
            quote(sb, k);
            sb.append(':');
            appendValue(sb, obj.get(k));
        }
        return sb.append('}').toString();
    }

    private static void appendValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            quote(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v) ? "true" : "false");
        } else if (v instanceof Integer || v instanceof Long || v instanceof Short) {
            // Python json.dumps 整数为纯十进制，无 .0、无前导零
            sb.append(v.toString());
        } else if (v instanceof Number) {
            // 浮点走 Python repr 语义较复杂；签名体是整数时间戳/版本号，不该走到这里。
            // 仍给出确定输出（去掉末尾 .0），但生产签名体请保持整数。
            double d = ((Number) v).doubleValue();
            if (d == Math.rint(d) && !Double.isInfinite(d)) {
                sb.append(Long.toString((long) d));
            } else {
                sb.append(v.toString());
            }
        } else {
            throw new IllegalArgumentException("CanonicalJson 不支持的类型: " + v.getClass());
        }
    }

    /** 按 Python ensure_ascii=True 的规则转义字符串（含引号与反斜杠）。 */
    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c >= 0x20 && c <= 0x7E) {
                        sb.append(c);
                    } else if (c <= 0xFFFF) {
                        appendUnicodeEscape(sb, c);
                    } else {
                        // 超出 BMP：Java char 是 UTF-16，代理对各自转义，正是 Python 的行为
                        appendUnicodeEscape(sb, c); // high surrogate
                        // low surrogate 会在下一次循环处理
                    }
            }
        }
        sb.append('"');
    }

    private static void appendUnicodeEscape(StringBuilder sb, char c) {
        sb.append("\\u");
        String hex = Integer.toHexString(c);
        for (int i = hex.length(); i < 4; i++) sb.append('0');
        sb.append(hex); // Integer.toHexString 为小写，与 Python 一致
    }
}
