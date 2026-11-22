package com.example.updatekit.core;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 极简 HTTP GET：带 Bearer API Key，返回统一包络里的 data 对象。 */
public final class ApiClient {
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutMs;

    public ApiClient(String baseUrl, String apiKey, int timeoutMs) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
    }

    /** 返回 data 对象；无数据时返回 null。 */
    public JSONObject get(String path, String query) throws Exception {
        String url = baseUrl + path + (query == null || query.isEmpty() ? "" : "?" + query);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            int status = conn.getResponseCode();
            InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            String text = readAll(is);
            if (status < 200 || status >= 300) {
                throw new ApiException("HTTP " + status + (text.isEmpty() ? "" : ": " + text), status);
            }
            JSONObject resp = new JSONObject(text);
            if (!resp.optBoolean("ok", false)) {
                throw new ApiException("服务端返回 ok=false: " + resp.optString("meta", ""), status);
            }
            if (resp.isNull("data")) return null;
            return resp.getJSONObject("data");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    public static final class ApiException extends Exception {
        public final int status;
        public ApiException(String message, int status) { super(message); this.status = status; }
        public ApiException(String message) { this(message, -1); }
    }
}
