package com.knowledgeinterview.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "KnowledgeWebView";
    private static final String HOST = "appassets.androidplatform.net";
    private static final String BACKEND = "http://127.0.0.1:8080";
    private WebView webView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return intercept(request);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost()));
            }
        });
        setContentView(webView);
        webView.loadUrl("https://" + HOST + "/");
    }

    private WebResourceResponse intercept(WebResourceRequest request) {
        Uri uri = request.getUrl();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !HOST.equalsIgnoreCase(uri.getHost())) return null;
        String path = uri.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";
        if ("/api".equals(path) || path.startsWith("/api/")) return proxyApi(request, uri);
        if (path.contains("..") || path.contains("\\")) return response(404, "Not Found", "text/plain", "Invalid asset path");
        try {
            return asset(path);
        } catch (IOException error) {
            String last = path.substring(path.lastIndexOf('/') + 1);
            if (request.isForMainFrame() && !last.contains(".")) {
                try { return asset("/index.html"); } catch (IOException ignored) { }
            }
            Log.w(TAG, "Asset not found: " + path);
            return response(404, "Not Found", "text/plain", "Asset not found");
        }
    }

    private WebResourceResponse asset(String path) throws IOException {
        String mime = mime(path);
        String encoding = (mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json")) ? "UTF-8" : null;
        return new WebResourceResponse(mime, encoding, getAssets().open("www" + path));
    }

    private WebResourceResponse proxyApi(WebResourceRequest request, Uri uri) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return response(405, "Method Not Allowed", "text/plain", "Only GET is supported by this API bridge");
        }
        String endpoint = BACKEND + uri.getEncodedPath();
        if (uri.getEncodedQuery() != null) endpoint += "?" + uri.getEncodedQuery();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(10000);
            for (Map.Entry<String, String> header : request.getRequestHeaders().entrySet()) {
                if ("Authorization".equalsIgnoreCase(header.getKey()) || "Accept".equalsIgnoreCase(header.getKey())) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            int status = connection.getResponseCode();
            if (status < 200 || (status >= 300 && status < 400) || status > 599) {
                return response(502, "Bad Gateway", "text/plain", "Unexpected backend HTTP status");
            }
            InputStream source = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (source != null) {
                try (InputStream input = source) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = input.read(buffer)) != -1) bytes.write(buffer, 0, length);
                }
            }
            String contentType = connection.getContentType();
            String type = contentType == null ? "application/json" : contentType.split(";")[0].trim();
            String reason = connection.getResponseMessage();
            if (reason == null || reason.isEmpty()) reason = "Backend Response";
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-store");
            return new WebResourceResponse(type, "UTF-8", status, reason, headers, new ByteArrayInputStream(bytes.toByteArray()));
        } catch (IOException error) {
            Log.w(TAG, "Backend unavailable on localhost:8080", error);
            return response(502, "Bad Gateway", "application/json", "{\"message\":\"Backend unavailable. Start the backend on port 8080.\"}");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static WebResourceResponse response(int status, String reason, String type, String content) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-store");
        return new WebResourceResponse(type, "UTF-8", status, reason, headers,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String mime(String path) {
        String p = path.toLowerCase(java.util.Locale.ROOT);
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "application/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json") || p.endsWith(".webmanifest")) return "application/json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".ico")) return "image/x-icon";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".woff")) return "font/woff";
        if (p.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}