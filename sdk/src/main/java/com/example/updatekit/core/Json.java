package com.example.updatekit.core;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** JSONObject → Map 转换（保留 org.json 的数值/布尔类型，供 CanonicalJson 重建字节）。 */
public final class Json {
    private Json() {}

    public static Map<String, Object> toMap(JSONObject o) {
        Map<String, Object> m = new HashMap<>();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            Object v = o.get(k);
            m.put(k, v == JSONObject.NULL ? null : v);
        }
        return m;
    }

    /** 公告签名体只含六个稳定字段（与服务端 announce.plugin.canonical 一致）。 */
    public static Map<String, Object> noticeSignatureBody(Map<String, Object> full) {
        String[] fields = {"product", "title", "content", "level", "force_popup", "published_at"};
        Map<String, Object> m = new HashMap<>();
        for (String f : fields) m.put(f, full.get(f));
        return m;
    }
}
