package com.example.updatekit.model;

import java.util.Map;

/** 一条已验签的更新清单（字段与服务端 manifest 一一对应）。 */
public final class UpdateInfo {
    public final String product;
    public final String version;
    public final long versionCode;
    public final String channel;
    public final String signerFingerprint;
    public final String apkUrl;
    public final String sha256;
    public final String notes;
    public final Map<String, Object> raw;

    public UpdateInfo(Map<String, Object> m) {
        this.product = str(m, "product");
        this.version = str(m, "version");
        this.versionCode = num(m, "version_code");
        this.channel = str(m, "channel");
        this.signerFingerprint = str(m, "signer_fingerprint");
        this.apkUrl = str(m, "apk_url");
        this.sha256 = str(m, "sha256");
        this.notes = str(m, "notes");
        this.raw = m;
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : String.valueOf(v);
    }

    private static long num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return v == null ? 0 : Long.parseLong(String.valueOf(v)); }
        catch (NumberFormatException e) { return 0; }
    }
}
