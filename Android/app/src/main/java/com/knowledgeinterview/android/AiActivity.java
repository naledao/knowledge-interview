package com.knowledgeinterview.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


/** AI 模块使用独立 WebView，与知识题库的本地资源及 API 拦截隔离。 */
public class AiActivity extends Activity {
    private static final String AI_HOST = "frp.kangnasi.xyz";
    private static final int PRIMARY = Color.rgb(49, 94, 210);
    private WebView webView;
    private ProgressBar progress;
    private TextView errorMessage;
    private boolean applyingProxy;
    private int proxyGeneration;
    private LinearLayout errorPanel;
    private static final String AI_URL = "https://frp.kangnasi.xyz:4400/#t=c0b77c9dca887f35c7eca68a6da3f520";
    private android.app.Dialog authDialog;
    private AiCredentialStore credentialStore;
    private AiCredentialStore.Credentials pendingCredentials;
    private boolean authAttempted;
    private boolean mainLoadFailed;
    private int appliedProxyPort = -1;
    private boolean proxyRouteDirty;
    private static java.lang.ref.WeakReference<AiActivity> activeInstance = new java.lang.ref.WeakReference<>(null);

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        credentialStore = new AiCredentialStore(this);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(4), 0, dp(4), 0);
        toolbar.addView(action("‹ 题库", v -> returnToKnowledge()));
        TextView title = new TextView(this);
        title.setText("AI 助手");
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        toolbar.addView(action("刷新", v -> reloadPage()));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));
        View separator = new View(this);
        separator.setBackgroundColor(Color.rgb(226, 233, 245));
        root.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));

        FrameLayout content = new FrameLayout(this);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setBlockNetworkLoads(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!request.isForMainFrame()) return !"https".equalsIgnoreCase(uri.getScheme());
                if (isAllowed(uri.toString())) return false;
                Toast.makeText(AiActivity.this, "已阻止跳转到其他网站", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                handleAuthentication(handler, host, realm);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                mainLoadFailed = false;
                errorPanel.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mainLoadFailed && isAllowed(url) && pendingCredentials != null && authDialog == null) {
                    AiCredentialStore.Credentials accepted = pendingCredentials;
                    pendingCredentials = null;
                    if (!credentialStore.save(accepted)) {
                        Toast.makeText(AiActivity.this, "登录成功，但无法安全保存账号，下次可能需要重新登录", Toast.LENGTH_LONG).show();
                    }
                }
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    mainLoadFailed = true;
                    pendingCredentials = null;
                    showLoadError();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame() && isAllowed(request.getUrl().toString()) && response.getStatusCode() == 401 && authAttempted) {
                    credentialStore.clear();
                }
                if (request.isForMainFrame()) {
                    mainLoadFailed = true;
                    pendingCredentials = null;
                    showLoadError();
                }
            }
        });
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(24), dp(24), dp(24), dp(24));
        errorPanel.setBackgroundColor(Color.WHITE);
        TextView message = new TextView(this);
        errorMessage = message;
        message.setText("暂时无法连接 AI 服务\n请检查网络或代理设置后重试");
        message.setGravity(Gravity.CENTER);
        message.setTextSize(16);
        message.setTextColor(Color.rgb(71, 85, 105));
        errorPanel.addView(message);
        errorPanel.addView(action("重新连接", v -> loadAddress()));
        errorPanel.setVisibility(View.GONE);
        content.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(PRIMARY));
        progress.setVisibility(View.GONE);
        content.addView(progress, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        activeInstance = new java.lang.ref.WeakReference<>(this);

        applyNetworkRoute(() -> {
            if (state == null || webView.restoreState(state) == null) openConfiguredAddress();
        });
    }

    private Button action(String label, View.OnClickListener listener) {
        Button button = new Button(this, null, android.R.attr.borderlessButtonStyle);
        button.setText(label);
        button.setTextColor(PRIMARY);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinWidth(dp(48));
        button.setMinimumWidth(dp(48));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setOnClickListener(listener);
        return button;
    }

    private static boolean isAllowed(String address) {
        if (address == null || address.isEmpty()) return false;
        Uri uri = Uri.parse(address);
        return "https".equalsIgnoreCase(uri.getScheme()) && AI_HOST.equalsIgnoreCase(uri.getHost())
                && uri.getPort() == 4400 && uri.getUserInfo() == null;
    }

    private void handleAuthentication(HttpAuthHandler handler, String host, String realm) {
        if (!(AI_HOST.equalsIgnoreCase(host) || (AI_HOST + ":4400").equalsIgnoreCase(host))
                || !"Antigravity".equals(realm) || isFinishing() || isDestroyed()) {
            handler.cancel();
            return;
        }
        if (authDialog != null && authDialog.isShowing()) {
            handler.cancel();
            return;
        }
        if (!authAttempted) {
            AiCredentialStore.Credentials saved = credentialStore.read();
            if (saved != null) {
                authAttempted = true;
                handler.proceed(saved.username, saved.password);
                return;
            }
        } else {
            // 同一加载过程再次收到认证挑战，停止自动重试并要求重新输入。
            pendingCredentials = null;
            credentialStore.clear();
        }
        showAuthenticationDialog(handler, host, realm);
    }

    private void showAuthenticationDialog(HttpAuthHandler handler, String host, String realm) {
        if (!(AI_HOST.equalsIgnoreCase(host) || (AI_HOST + ":4400").equalsIgnoreCase(host))
                || !"Antigravity".equals(realm) || isFinishing() || isDestroyed()) {
            handler.cancel();
            return;
        }
        if (authDialog != null && authDialog.isShowing()) {
            handler.cancel();
            return;
        }
        progress.setVisibility(View.GONE);
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.Theme_KnowledgeInterview_AiDialog);
        authDialog = dialog;
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        android.content.Context context = dialog.getContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(24), dp(24), dp(22));
        card.setBackground(authShape(Color.WHITE, 20, Color.TRANSPARENT));
        card.setFocusableInTouchMode(true);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = authText(context, "AI", 18, PRIMARY, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(authShape(Color.rgb(239, 246, 255), 13, Color.TRANSPARENT));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.rightMargin = dp(14);
        header.addView(icon, iconParams);
        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(authText(context, "登录 AI 助手", 20, Color.rgb(15, 23, 42), true));
        TextView subtitle = authText(context, "安全连接 · AI 服务", 12, Color.rgb(100, 116, 139), false);
        authAdd(heading, subtitle, 4, -2);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(header);

        TextView description = authText(context, "请输入 AI 服务的账号和密码，登录后即可开始对话。", 14, Color.rgb(100, 116, 139), false);
        description.setLineSpacing(dp(3), 1f);
        authAdd(card, description, 18, -2);
        authAdd(card, authText(context, "用户名", 13, Color.rgb(51, 65, 85), true), 22, -2);
        EditText username = authInput(context, "请输入用户名", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        username.setSaveEnabled(false);
        username.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        authAdd(card, username, 8, dp(48));
        authAdd(card, authText(context, "密码", 13, Color.rgb(51, 65, 85), true), 16, -2);
        EditText password = authInput(context, "请输入密码", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSaveEnabled(false);
        password.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        authAdd(card, password, 8, dp(48));
        TextView hint = authText(context, "登录成功后将加密保存在本机，下次自动登录。", 12, Color.rgb(100, 116, 139), false);
        authAdd(card, hint, 14, -2);

        LinearLayout actions = new LinearLayout(context);
        Button cancel = authButton(context, "取消", false);
        Button login = authButton(context, "登录", true);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        cancelParams.rightMargin = dp(12);
        actions.addView(cancel, cancelParams);
        actions.addView(login, new LinearLayout.LayoutParams(0, dp(46), 1));
        authAdd(card, actions, 24, -2);
        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(card, new android.widget.ScrollView.LayoutParams(-1, -2));
        dialog.setContentView(scroll);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(d -> { handler.cancel(); showLoadError(); });
        dialog.setOnDismissListener(d -> {
            password.setText("");
            if (authDialog == dialog) authDialog = null;
        });
        cancel.setOnClickListener(v -> dialog.cancel());
        login.setOnClickListener(v -> {
            if (username.getText().length() == 0) { username.setError("请输入用户名"); username.requestFocus(); return; }
            if (password.getText().length() == 0) { password.setError("请输入密码"); password.requestFocus(); return; }
            String account = username.getText().toString();
            String secret = password.getText().toString();
            authAttempted = true;
            pendingCredentials = new AiCredentialStore.Credentials(account, secret);
            dialog.dismiss();
            progress.setVisibility(View.VISIBLE);
            handler.proceed(account, secret);
        });
        username.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) return false;
            password.requestFocus();
            return true;
        });
        password.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
            login.performClick();
            return true;
        });
        card.requestFocus();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.40f);
            int available = getResources().getDisplayMetrics().widthPixels - dp(40);
            window.setLayout(Math.min(dp(400), available), -2);
            window.setGravity(Gravity.CENTER);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private android.graphics.drawable.GradientDrawable authShape(int color, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private TextView authText(android.content.Context context, String value, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private void authAdd(LinearLayout parent, View child, int margin, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, height);
        params.topMargin = dp(margin);
        parent.addView(child, params);
    }

    private EditText authInput(android.content.Context context, String hint, int inputType) {
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setHint(hint);
        input.setTextSize(15);
        input.setTextColor(Color.rgb(30, 41, 59));
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setPadding(dp(14), 0, dp(14), 0);
        android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, authShape(Color.WHITE, 11, PRIMARY));
        background.addState(new int[]{}, authShape(Color.rgb(248, 250, 252), 11, Color.rgb(203, 213, 225)));
        input.setBackground(background);
        return input;
    }

    private Button authButton(android.content.Context context, String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : Color.rgb(71, 85, 105));
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
        button.setMinimumHeight(0);
        button.setMinHeight(0);
        button.setStateListAnimator(null);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(primary ? 0x33FFFFFF : 0x15315ED2),
                authShape(primary ? PRIMARY : Color.rgb(241, 245, 249), 11, Color.TRANSPARENT), null));
        return button;
    }

    private void showLoadError() {
        progress.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }

    /** 必须等待 WebView 代理设置生效，之后才能发起网页请求。 */
    private void applyNetworkRoute(Runnable ready) {
        if (applyingProxy || isFinishing() || isDestroyed()) return;
        applyingProxy = true;
        proxyRouteDirty = false;
        int generation = ++proxyGeneration;
        webView.getSettings().setBlockNetworkLoads(true);
        progress.setVisibility(View.VISIBLE);
        boolean enabled = TrojanProxyManager.loadConfig(this).enabled;
        boolean supported = androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.PROXY_OVERRIDE);
        final int routePort = enabled ? TrojanProxyManager.ensureLocalProxyPort(this) : 0;
        Runnable complete = () -> {
            if (isFinishing() || isDestroyed() || generation != proxyGeneration) return;
            applyingProxy = false;
            if (proxyRouteDirty) {
                applyNetworkRoute(ready);
                return;
            }
            appliedProxyPort = routePort;
            errorMessage.setText("暂时无法连接 AI 服务\n请检查网络或代理设置后重试");
            errorPanel.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            webView.getSettings().setBlockNetworkLoads(false);
            android.util.Log.i("AiNetwork", enabled ? "Trojan WebView proxy applied" : "Built-in proxy disabled");
            ready.run();
        };
        if (enabled && !supported) {
            proxyError("当前 Android WebView 不支持内置代理，请更新系统 WebView 后重试。");
            return;
        }
        try {
            if (enabled) {
                int port = routePort;
                if (port <= 0) {
                    proxyError("内置代理启动失败，请返回题库检查代理设置后重试。");
                    return;
                }
                androidx.webkit.ProxyConfig config = new androidx.webkit.ProxyConfig.Builder()
                        .addProxyRule("http://127.0.0.1:" + port)
                        .removeImplicitRules()
                        .build();
                androidx.webkit.ProxyController.getInstance().setProxyOverride(config, this::runOnUiThread, complete);
            } else if (supported) {
                androidx.webkit.ProxyController.getInstance().clearProxyOverride(this::runOnUiThread, complete);
            } else {
                complete.run();
            }
        } catch (RuntimeException error) {
            proxyError("无法应用内置代理设置，请检查代理配置及 WebView 版本。");
            return;
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && applyingProxy && generation == proxyGeneration) {
                proxyError("代理配置超时，请重试。");
            }
        }, 10000);
    }

    private void proxyError(String message) {
        ++proxyGeneration;
        appliedProxyPort = -1;
        applyingProxy = false;
        webView.getSettings().setBlockNetworkLoads(true);
        errorMessage.setText(message + "\n代理开启时不会自动回退直连。");
        showLoadError();
    }

    private void loadAddress() {
        applyNetworkRoute(this::openConfiguredAddress);
    }

    private void openConfiguredAddress() {
        authAttempted = false;
        pendingCredentials = null;
        mainLoadFailed = false;
        webView.loadUrl(AI_URL);
    }

    private void reloadPage() {
        if (errorPanel.getVisibility() == View.VISIBLE || webView.getUrl() == null) loadAddress();
        else applyNetworkRoute(() -> {
            authAttempted = false;
            pendingCredentials = null;
            mainLoadFailed = false;
            webView.reload();
        });
    }

    /** 将题库切换至前台，不结束 AI Activity，保留原 WebView 和会话。 */
    private void returnToKnowledge() {
        android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(webView.getWindowToken(), 0);
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    static void onProxyConfigurationChanging() {
        AiActivity activity = activeInstance.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        activity.proxyRouteDirty = true;
        activity.webView.getSettings().setBlockNetworkLoads(true);
    }

    static void onProxyConfigurationChanged() {
        AiActivity activity = activeInstance.get();
        if (activity != null) activity.syncNetworkRoute();
    }

    /** 普通页面切换不刷新；只有代理配置变化时才重新应用网络路由。 */
    private void syncNetworkRoute() {
        if (applyingProxy || isFinishing() || isDestroyed()) return;
        if (!proxyRouteDirty) {
            if (appliedProxyPort < 0) return;
            int currentPort = TrojanProxyManager.loadConfig(this).enabled
                    ? TrojanProxyManager.ensureLocalProxyPort(this) : 0;
            if (currentPort == appliedProxyPort) return;
        }
        boolean needsLoad = webView.getUrl() == null || errorPanel.getVisibility() == View.VISIBLE;
        applyNetworkRoute(() -> {
            if (needsLoad) openConfiguredAddress();
        });
    }

    @Override
    public void onBackPressed() {
        if (errorPanel.getVisibility() != View.VISIBLE && webView.canGoBack()) webView.goBack();
        else returnToKnowledge();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        webView.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncNetworkRoute();
    }

    @Override
    protected void onDestroy() {
        if (activeInstance.get() == this) activeInstance.clear();
        ++proxyGeneration;
        pendingCredentials = null;
        if (authDialog != null) authDialog.cancel();
        webView.stopLoading();
        if (webView.getParent() instanceof ViewGroup) ((ViewGroup) webView.getParent()).removeView(webView);
        webView.destroy();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
