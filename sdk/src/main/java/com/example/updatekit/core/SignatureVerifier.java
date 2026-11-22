package com.example.updatekit.core;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/** 清单/公告验签：只持有公钥，不接触私钥。 */
public final class SignatureVerifier {
    private static final String ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGO = "RSA";

    private SignatureVerifier() {}

    /** 从 X.509 SPKI DER 的 Base64 文本加载公钥。 */
    public static PublicKey publicKeyFromBase64(String base64) throws SignatureException {
        try {
            byte[] der = Base64.decode(base64);
            return KeyFactory.getInstance(KEY_ALGO).generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new SignatureException("公钥格式错误: " + e.getMessage(), e);
        }
    }

    /** 验签：data 为规范 JSON 字节（CanonicalJson.toBytes），sig 为 Base32 编码的签名。 */
    public static boolean verify(byte[] data, String sigBase32, PublicKey key) {
        try {
            byte[] sig = Base32.decode(sigBase32);
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(key);
            verifier.update(data);
            return verifier.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    public static final class SignatureException extends Exception {
        public SignatureException(String message, Throwable cause) { super(message, cause); }
        public SignatureException(String message) { super(message); }
    }
}
