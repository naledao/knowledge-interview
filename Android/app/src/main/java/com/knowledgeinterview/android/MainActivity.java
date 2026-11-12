package com.knowledgeinterview.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.InputType;


import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "KnowledgeWebView";
    private static final String HOST = "appassets.androidplatform.net";
    private static final String DEFAULT_BACKEND = "http://frp.kangnasi.xyz:9881";
    private static final String SETTINGS_FILE = "backend_settings";
    private static final String BACKEND_ADDRESS_KEY = "backend_address";
    private WebView webView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // 标识内置安卓客户端，使网页导航仅在安卓端显示服务器设置入口。
        settings.setUserAgentString(settings.getUserAgentString() + " KnowledgeInterviewAndroid/1.0");
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
                // 使用受限的内部导航协议打开原生设置，避免向沙箱 iframe 注入 JavaScript 接口。
                String currentUrl = view.getUrl();
                Uri origin = currentUrl == null ? null : Uri.parse(currentUrl);
                if (request.isForMainFrame() && origin != null
                        && "https".equalsIgnoreCase(origin.getScheme()) && HOST.equalsIgnoreCase(origin.getHost())
                        && "knowledgeinterview".equalsIgnoreCase(uri.getScheme())
                        && "settings".equalsIgnoreCase(uri.getHost()) && "/backend".equals(uri.getPath())) {
                    showBackendAddressDialog();
                    return true;
                }
                return !("https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost()));
            }
        });
        // 网页导航已经包含服务器设置入口，不再额外占用一整行原生工具栏。
        setContentView(webView);
        webView.loadUrl("https://" + HOST + "/");
    }

    // 使用 SharedPreferences 持久化后端地址，应用重启后继续生效。
    private String backendAddress() {
        return getSharedPreferences(SETTINGS_FILE, MODE_PRIVATE)
                .getString(BACKEND_ADDRESS_KEY, DEFAULT_BACKEND);
    }

    // 使用自定义浅色圆角卡片，与知识题库移动端的视觉风格保持一致。
    private void showBackendAddressDialog() {
        Dialog dialog = new Dialog(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackground(dialogShape(Color.WHITE, 20, Color.TRANSPARENT));

        // 图标、标题和简短说明组成统一的卡片头部。
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_manage);
        icon.setColorFilter(Color.rgb(37, 99, 235));
        icon.setPadding(dp(11), dp(11), dp(11), dp(11));
        icon.setBackground(dialogShape(Color.rgb(239, 246, 255), 12, Color.TRANSPARENT));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.rightMargin = dp(12);
        header.addView(icon, iconParams);
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("服务器设置");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(15, 23, 42));
        heading.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("配置知识题库的后端连接");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(100, 116, 139));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        heading.addView(subtitle, subtitleParams);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(header);

        TextView label = new TextView(this);
        label.setText("后端服务器地址");
        label.setTextSize(14);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(Color.rgb(51, 65, 85));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(25);
        card.addView(label, labelParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setTextSize(15);
        input.setTextColor(Color.rgb(30, 41, 59));
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setHint("http://192.168.1.100:8080");
        input.setText(backendAddress());
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(13), 0, dp(13), 0);
        input.setBackground(dialogShape(Color.rgb(248, 250, 252), 11, Color.rgb(203, 213, 225)));
        input.setOnFocusChangeListener((view, focused) -> input.setBackground(dialogShape(
                Color.rgb(248, 250, 252), 11,
                focused ? Color.rgb(59, 130, 246) : Color.rgb(203, 213, 225))));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(50));
        inputParams.topMargin = dp(10);
        card.addView(input, inputParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView cancel = new TextView(this);
        cancel.setText("取消");
        cancel.setTextSize(14);
        cancel.setTypeface(null, Typeface.BOLD);
        cancel.setTextColor(Color.rgb(71, 85, 105));
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(dialogShape(Color.rgb(241, 245, 249), 10, Color.TRANSPARENT));
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(dp(88), dp(44));
        cancelParams.rightMargin = dp(10);
        actions.addView(cancel, cancelParams);
        TextView save = new TextView(this);
        save.setText("保存地址");
        save.setTextSize(14);
        save.setTypeface(null, Typeface.BOLD);
        save.setTextColor(Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setBackground(dialogShape(Color.rgb(37, 99, 235), 10, Color.TRANSPARENT));
        actions.addView(save, new LinearLayout.LayoutParams(dp(108), dp(44)));
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.topMargin = dp(23);
        card.addView(actions, actionsParams);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String address = normalizeBackendAddress(input.getText().toString());
            if (address == null) {
                input.setError("请输入有效的 HTTP/HTTPS 地址，仅允许主机及可选端口");
                input.requestFocus();
                return;
            }
            getSharedPreferences(SETTINGS_FILE, MODE_PRIVATE).edit()
                    .putString(BACKEND_ADDRESS_KEY, address).apply();
            dialog.dismiss();
            if (webView != null) webView.reload();
            Toast.makeText(this, "服务器地址已保存", Toast.LENGTH_SHORT).show();
        });

        dialog.setContentView(card);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(430)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.38f;
            window.setAttributes(attributes);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    // 统一输入框、提示区和操作按钮的背景、圆角及描边。
    private GradientDrawable dialogShape(int fill, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1), stroke);
        return shape;
    }

    // 后端地址仅允许协议、主机及端口，避免路径与 API 路由重复拼接。
    private static String normalizeBackendAddress(String raw) {
        String value = raw.trim();
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return null;
            if (uri.getHost() == null || uri.getHost().isEmpty() || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) return null;
            String path = uri.getRawPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) return null;
            if (uri.getPort() == 0 || uri.getPort() > 65535) return null;
            return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        } catch (URISyntaxException error) {
            return null;
        }
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
        String endpoint = backendAddress() + uri.getEncodedPath();
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
            Log.w(TAG, "Backend unavailable at configured address", error);
            return response(502, "Bad Gateway", "application/json", "{\"message\":\"Backend unavailable. Check the Android backend address setting.\"}");
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