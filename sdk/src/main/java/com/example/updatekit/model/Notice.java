package com.example.updatekit.model;

import java.util.Map;

/** 一条已验签的公告。id 不进签名体，只做已读去重。 */
public final class Notice {
    public final String id;
    public final String product;
    public final String title;
    public final String content;
    public final String level;
    public final boolean forcePopup;
    public final long publishedAt;
    public final String signature;
    public final Map<String, Object> raw;

    public Notice(Map<String, Object> m) {
        this.id = String.valueOf(m.get("id") == null ? "" : m.get("id"));
        this.product = str(m, "product");
        this.title = str(m, "title");
        this.content = str(m, "content");
        this.level = str(m, "level");
        Object f = m.get("force_popup");
        this.forcePopup = f instanceof Boolean ? (Boolean) f : Boolean.parseBoolean(String.valueOf(f));
        this.publishedAt = num(m, "published_at");
        this.signature = str(m, "signature");
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
