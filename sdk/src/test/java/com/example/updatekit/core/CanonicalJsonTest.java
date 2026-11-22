package com.example.updatekit.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** 规范 JSON 字节级用例——与 tools/canonical_java.py 的向量一致（CI 真实 JVM 上跑）。 */
public class CanonicalJsonTest {

    private static void assertCanonical(String expected, Map<String, ?> obj) {
        assertEquals(expected, CanonicalJson.canonical(obj));
    }

    @Test public void chineseEscaped() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", "中文");
        assertCanonical("{\"k\":\"\\u4e2d\\u6587\"}", m);
    }

    @Test public void emojiSurrogatePair() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", "emoji 😀");
        assertCanonical("{\"k\":\"emoji \\ud83d\\ude00\"}", m);
    }

    @Test public void controlAndDelEscaped() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", "tab\tnewline\nDEL\u007f");
        assertCanonical("{\"k\":\"tab\\tnewline\\nDEL\\u007f\"}", m);
    }

    @Test public void sortedKeysAndTypes() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("b", 2);
        m.put("a", 1);
        m.put("c", true);
        m.put("d", null);
        assertCanonical("{\"a\":1,\"b\":2,\"c\":true,\"d\":null}", m);
    }

    @Test public void signatureBodyNoticeSixFields() {
        Map<String, Object> full = new LinkedHashMap<>();
        full.put("product", "my_app");
        full.put("title", "版本公告");
        full.put("content", "中文内容不乱码");
        full.put("level", "urgent");
        full.put("force_popup", true);
        full.put("published_at", 1730000000000L);
        full.put("id", 1); // 不进签名体
        Map<String, Object> body = Json.noticeSignatureBody(full);
        String canonical = CanonicalJson.canonical(body);
        // 关键：中文被转义、六字段、id 不在其中
        org.junit.Assert.assertTrue(canonical.contains("\\u7248\\u672c\\u516c\\u544a"));
        org.junit.Assert.assertFalse(canonical.contains("id"));
        org.junit.Assert.assertEquals(6, body.size());
    }

    @Test public void bytesAreUtf8() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", "中文");
        byte[] b = CanonicalJson.toBytes(m);
        assertEquals("{\"k\":\"\\u4e2d\\u6587\"}", new String(b, StandardCharsets.UTF_8));
    }
}
