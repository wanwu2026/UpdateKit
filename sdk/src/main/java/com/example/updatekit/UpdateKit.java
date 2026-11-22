package com.example.updatekit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import com.example.updatekit.core.ApiClient;
import com.example.updatekit.core.SignatureVerifier;
import com.example.updatekit.core.UpdateVerifier;
import com.example.updatekit.model.Notice;
import com.example.updatekit.model.UpdateInfo;

/**
 * UpdateKit 单例入口：查更新 / 拉公告，两者都先验签再返回。
 *
 * 配置在 init() 里给一次：
 *   UpdateKit.init(new Config(context)
 *       .baseUrl("https://your.openhub/api/v1")
 *       .apiKey("ohk_xxx")
 *       .publicKeyBase64("...")
 *       .product("my_app"));
 */
public final class UpdateKit {
    private static UpdateKit instance;

    private final Context appContext;
    private final String baseUrl;
    private final String apiKey;
    private final String product;
    private final PublicKey publicKey;
    private final UpdateVerifier verifier;
    private final ApiClient api;
    private final SharedPreferences prefs;
    private final int timeoutMs;

    public enum Level { INFO, WARN, URGENT }

    private UpdateKit(Config c) throws Exception {
        this.appContext = c.context.getApplicationContext();
        this.baseUrl = c.baseUrl;
        this.apiKey = c.apiKey;
        this.product = c.product;
        this.publicKey = SignatureVerifier.publicKeyFromBase64(c.publicKeyBase64);
        this.verifier = new UpdateVerifier(publicKey);
        this.timeoutMs = c.timeoutMs > 0 ? c.timeoutMs : 8000;
        this.api = new ApiClient(baseUrl, apiKey, this.timeoutMs);
        this.prefs = appContext.getSharedPreferences("updatekit_prefs", Context.MODE_PRIVATE);
    }

    public static void init(Config c) throws Exception {
        instance = new UpdateKit(c);
    }

    public static UpdateKit get() {
        if (instance == null) throw new IllegalStateException("请先调用 UpdateKit.init()");
        return instance;
    }

    public UpdateVerifier verifier() { return verifier; }
    public String product() { return product; }
    public Context context() { return appContext; }

    /** 查更新：验签通过后返回 UpdateInfo；无更新返回 null；验签失败抛 VerifyFailed。 */
    public UpdateInfo checkUpdate(int currentVersionCode) throws Exception {
        String query = "product=" + enc(product) + "&current_version_code=" + currentVersionCode
                + "&channel=" + enc("stable");
        JSONObject data = api.get("/update/check", query);
        if (data == null) return null;
        if (!data.optBoolean("has_update", false)) return null;
        JSONObject m = data.optJSONObject("manifest");
        if (m == null) return null;
        String sig = data.optString("manifest_signature", "");
        if (!verifier.verifyManifest(com.example.updatekit.core.Json.toMap(m), sig)) {
            throw new VerifyFailed("更新清单验签失败");
        }
        return new UpdateInfo(com.example.updatekit.core.Json.toMap(m));
    }

    /** 拉公告（增量）：验签逐条过滤，失败的直接丢弃；返回未读公告。 */
    public List<Notice> fetchNotices() throws Exception {
        long since = prefs.getLong("last_notice_ts", 0L);
        String query = "product=" + enc(product) + "&since=" + since;
        JSONObject data = api.get("/announce/list", query);
        List<Notice> out = new ArrayList<>();
        if (data == null) return out;
        JSONArray arr = data.optJSONArray("notices");
        if (arr == null) return out;
        long maxTs = since;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String sig = o.optString("signature", "");
            Map<String, Object> m = com.example.updatekit.core.Json.toMap(o);
            if (!verifier.verifyNotice(m, sig)) continue; // 验签失败=丢弃，不信任
            Notice n = new Notice(m);
            out.add(n);
            if (n.publishedAt > maxTs) maxTs = n.publishedAt;
        }
        if (maxTs > since) prefs.edit().putLong("last_notice_ts", maxTs).apply();
        return out;
    }

    /** 标记公告已读（按 published_at 水位；一般 fetchNotices 已自动推进）。 */
    public void markNoticesRead(long publishedAt) {
        long cur = prefs.getLong("last_notice_ts", 0L);
        if (publishedAt > cur) prefs.edit().putLong("last_notice_ts", publishedAt).apply();
    }

    private static String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    public static final class VerifyFailed extends Exception {
        public VerifyFailed(String message) { super(message); }
    }

    /** 链式配置。 */
    public static final class Config {
        final Context context;
        String baseUrl = "";
        String apiKey = "";
        String product = "";
        String publicKeyBase64 = "";
        int timeoutMs = 8000;

        public Config(Context context) { this.context = context; }
        public Config baseUrl(String v) { this.baseUrl = v; return this; }
        public Config apiKey(String v) { this.apiKey = v; return this; }
        public Config product(String v) { this.product = v; return this; }
        public Config publicKeyBase64(String v) { this.publicKeyBase64 = v; return this; }
        public Config timeoutMs(int v) { this.timeoutMs = v; return this; }
    }
}
