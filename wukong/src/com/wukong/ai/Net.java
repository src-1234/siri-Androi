package com.wukong.ai;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class Net {
    public static String post(String json) {
        JSONObject r = new JSONObject();
        try {
            JSONObject o = new JSONObject(json);
            String url = o.getString("url");
            String token = o.optString("token");
            JSONObject body = o.getJSONObject("body");
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(30000);
            c.setReadTimeout(120000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + token);
            c.setRequestProperty("User-Agent", "WukongApp/1.0");
            byte[] data = body.toString().getBytes("UTF-8");
            c.getOutputStream().write(data);
            c.getOutputStream().flush();
            int code = c.getResponseCode();
            InputStream in = (code >= 400) ? c.getErrorStream() : c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            if (in != null) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            }
            r.put("ok", code >= 200 && code < 300);
            r.put("status", code);
            r.put("text", bo.toString("UTF-8"));
        } catch (Exception e) {
            try {
                r.put("ok", false);
                r.put("status", 0);
                r.put("error", String.valueOf(e.getMessage()));
            } catch (Exception ignored) { }
        }
        return r.toString();
    }
}
