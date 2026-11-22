package com.example.updatekit.core;

import java.security.PublicKey;
import java.util.Map;

/**
 * 验签编排（纯逻辑，不依赖 Android，便于单测与 Python 交叉验证）。
 * - 更新清单：对完整 manifest 重建规范 JSON 验签
 * - 公告：只取六个稳定字段重建规范 JSON 验签
 */
public final class UpdateVerifier {
    private final PublicKey publicKey;

    public UpdateVerifier(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /** 验更新清单签名：data 是响应里的完整 manifest 对象。 */
    public boolean verifyManifest(Map<String, Object> manifest, String signatureBase32) {
        if (manifest == null || signatureBase32 == null) return false;
        byte[] canonical = CanonicalJson.toBytes(manifest);
        return SignatureVerifier.verify(canonical, signatureBase32, publicKey);
    }

    /** 验公告签名：只取六字段，id/signature 不进签名体。 */
    public boolean verifyNotice(Map<String, Object> notice, String signatureBase32) {
        if (notice == null || signatureBase32 == null) return false;
        Map<String, Object> body = Json.noticeSignatureBody(notice);
        byte[] canonical = CanonicalJson.toBytes(body);
        return SignatureVerifier.verify(canonical, signatureBase32, publicKey);
    }

    /** 下载后的第二道校验：APK 签名证书指纹与清单一致（大小写不敏感）。 */
    public static boolean fingerprintMatches(String actualFingerprint, String expected) {
        if (actualFingerprint == null || expected == null) return false;
        // 两边都去冒号：有的工具把证书指纹显示成 aa:bb:cc 形式，签名体里是无分隔 hex
        return actualFingerprint.replace(":", "").equalsIgnoreCase(expected.replace(":", ""));
    }
}
