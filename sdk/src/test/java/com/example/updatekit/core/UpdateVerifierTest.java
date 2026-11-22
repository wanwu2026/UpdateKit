package com.example.updatekit.core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 真 JVM 上的验签闭环测试：用运行时生成的 RSA 密钥对签名 → 验证，
 * 证明 CanonicalJson + SignatureVerifier + Base32 三者协同正确（尤其是中文规范字节）。
 */
public class UpdateVerifierTest {

    private static Map<String, Object> sampleManifest() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", "my_app");
        m.put("version", "1.2.0");
        m.put("version_code", 12);
        m.put("channel", "stable");
        m.put("signer_fingerprint", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        m.put("apk_url", "https://cdn/app.apk");
        m.put("sha256", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        m.put("notes", "修复中文乱码问题");
        return m;
    }

    private static Map<String, Object> sampleNotice() {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", 1);
        n.put("product", "my_app");
        n.put("title", "版本公告");
        n.put("content", "中文内容不乱码 😀");
        n.put("level", "urgent");
        n.put("force_popup", true);
        n.put("published_at", 1730000000000L);
        return n;
    }

    private static String sign(Map<String, Object> body, java.security.PrivateKey key) throws Exception {
        java.security.Signature s = java.security.Signature.getInstance("SHA256withRSA");
        s.initSign(key);
        s.update(CanonicalJson.toBytes(body));
        return Base32.encode(s.sign());
    }

    @Test public void manifestVerifyRoundTrip() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        UpdateVerifier v = new UpdateVerifier(kp.getPublic());
        Map<String, Object> m = sampleManifest();
        assertTrue(v.verifyManifest(m, sign(m, kp.getPrivate())));
        // 篡改后必须失败
        Map<String, Object> bad = new LinkedHashMap<>(m);
        bad.put("notes", "被篡改");
        assertFalse(v.verifyManifest(bad, sign(m, kp.getPrivate())));
    }

    @Test public void noticeVerifyRoundTrip() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        UpdateVerifier v = new UpdateVerifier(kp.getPublic());
        Map<String, Object> n = sampleNotice();
        // 公告签名体只取六字段（与服务端 canonical 一致）
        Map<String, Object> body = Json.noticeSignatureBody(n);
        assertTrue(v.verifyNotice(n, sign(body, kp.getPrivate())));
        // 改 id 不影响验签（id 不进签名体）
        Map<String, Object> n2 = new LinkedHashMap<>(n);
        n2.put("id", 999);
        assertTrue(v.verifyNotice(n2, sign(body, kp.getPrivate())));
        // 改正文则失败
        Map<String, Object> bad = new LinkedHashMap<>(n);
        bad.put("content", "钓鱼内容");
        assertFalse(v.verifyNotice(bad, sign(body, kp.getPrivate())));
    }

    @Test public void base32RoundTrip() {
        byte[] data = "hello world 中文".getBytes(StandardCharsets.UTF_8);
        byte[] dec = Base32.decode(Base32.encode(data));
        org.junit.Assert.assertArrayEquals(data, dec);
    }

    @Test public void fingerprintCaseInsensitive() {
        assertTrue(UpdateVerifier.fingerprintMatches("ABCDEF", "abcdef"));
        assertTrue(UpdateVerifier.fingerprintMatches("aa:bb:cc", "aabbcc"));
        assertFalse(UpdateVerifier.fingerprintMatches("aabb", "ccdd"));
    }
}
