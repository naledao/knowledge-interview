package com.knowledgeinterview.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** 更新流程统一的蓝白圆角弹窗，保持与应用内服务器设置相同的视觉风格。 */
final class UpdateDialogs {
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int DARK = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(100, 116, 139);

    private UpdateDialogs() { }

    /** 长时间操作保持进度卡片可见，完成、失败或取消结束后由调用方关闭。 */
    static final class Busy {
        final Dialog dialog;
        private final Activity activity;
        private final LinearLayout card;
        private final TextView status;
        private final ProgressBar bar;
        private final TextView actionButton;

        Busy(Activity activity, Dialog dialog, LinearLayout card, TextView status, ProgressBar bar, TextView actionButton) {
            this.activity = activity;
            this.dialog = dialog;
            this.card = card;
            this.status = status;
            this.bar = bar;
            this.actionButton = actionButton;
        }

        void update(String description, int percent) {
            if (!dialog.isShowing()) return;
            status.setText(description);
            bar.setIndeterminate(percent < 0);
            if (percent >= 0) MotionHelper.animateProgress(bar, percent);
        }

        /** 下载校验完成后切换为“立即安装”主按钮状态并附带轻弹反馈。 */
        void markReadyToInstall(String description, Runnable onInstall) {
            if (!dialog.isShowing()) return;
            status.setText(description);
            bar.setIndeterminate(false);
            MotionHelper.animateProgress(bar, 100);
            if (actionButton != null) {
                actionButton.setEnabled(true);
                actionButton.setAlpha(1f);
                actionButton.setText("立即安装");
                actionButton.setTextColor(Color.WHITE);
                actionButton.setBackground(MotionHelper.ripple(0x33FFFFFF, shape(activity, BLUE, 10, Color.TRANSPARENT)));
                actionButton.setOnClickListener(v -> MotionHelper.animateDialogExit(dialog, card, onInstall));
                MotionHelper.animateBounce(actionButton);
            }
        }

        void dismiss() {
            MotionHelper.animateDialogExit(dialog, card);
        }
    }

    /** 版本提示采用独立版本信息卡和主次操作按钮，不再使用系统默认 AlertDialog。 */
    static Dialog confirm(Activity activity, String current, String latest, Runnable accept) {
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, "发现新版本", "新的应用版本已准备就绪");
        LinearLayout versions = new LinearLayout(activity);
        versions.setOrientation(LinearLayout.VERTICAL);
        versions.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
        versions.setBackground(shape(activity, Color.rgb(248, 250, 255), 13, Color.rgb(219, 234, 254)));
        TextView currentText = text(activity, "当前版本  v" + current, 13, MUTED, false);
        TextView latestText = text(activity, "最新版本  v" + latest, 17, BLUE, true);
        LinearLayout.LayoutParams latestParams = new LinearLayout.LayoutParams(-1, -2);
        latestParams.topMargin = dp(activity, 9);
        versions.addView(currentText);
        versions.addView(latestText, latestParams);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(-1, -2);
        versionParams.topMargin = dp(activity, 24);
        card.addView(versions, versionParams);

        LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView later = button(activity, "稍后", false);
        TextView install = button(activity, "立即更新", true);
        LinearLayout.LayoutParams laterParams = new LinearLayout.LayoutParams(dp(activity, 88), dp(activity, 44));
        laterParams.rightMargin = dp(activity, 10);
        actions.addView(later, laterParams);
        actions.addView(install, new LinearLayout.LayoutParams(dp(activity, 110), dp(activity, 44)));
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.topMargin = dp(activity, 25);
        card.addView(actions, actionsParams);
        later.setOnClickListener(v -> MotionHelper.animateDialogExit(dialog, card));
        install.setOnClickListener(v -> MotionHelper.animateDialogExit(dialog, card, accept));
        show(activity, dialog, card, true);
        return dialog;
    }

    /** 请求未完成时显示持续动画；下载时可取消，但取消后继续等待请求收尾。 */
    static Busy busy(Activity activity, String title, String subtitle, Runnable onCancel) {
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, title, subtitle);
        TextView status = text(activity, "请稍候，正在处理请求…", 13, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(activity, 25);
        card.addView(status, statusParams);
        ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setIndeterminateTintList(ColorStateList.valueOf(BLUE));
        bar.setProgressTintList(ColorStateList.valueOf(BLUE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(219, 234, 254)));
        bar.setIndeterminate(true);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(activity, 7));
        barParams.topMargin = dp(activity, 16);
        card.addView(bar, barParams);
        TextView cancel = null;
        if (onCancel != null) {
            cancel = button(activity, "取消下载", false);
            LinearLayout actions = new LinearLayout(activity);
            actions.setGravity(Gravity.END);
            actions.addView(cancel, new LinearLayout.LayoutParams(dp(activity, 104), dp(activity, 44)));
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
            actionsParams.topMargin = dp(activity, 23);
            card.addView(actions, actionsParams);
            final TextView cancelBtn = cancel;
            cancelBtn.setOnClickListener(v -> {
                cancelBtn.setEnabled(false);
                cancelBtn.setAlpha(0.65f);
                cancelBtn.setText("正在取消");
                status.setText("正在取消下载，请稍候…");
                bar.setIndeterminate(true);
                onCancel.run();
            });
        }
        show(activity, dialog, card, false);
        return new Busy(activity, dialog, card, status, bar, cancel);
    }

    private static LinearLayout card(Activity activity, String title, String subtitle) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 22), dp(activity, 24), dp(activity, 22), dp(activity, 22));
        card.setBackground(shape(activity, Color.WHITE, 20, Color.TRANSPARENT));
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text(activity, "↑", 24, BLUE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(activity, Color.rgb(239, 246, 255), 12, Color.TRANSPARENT));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 44));
        iconParams.rightMargin = dp(activity, 12);
        header.addView(icon, iconParams);
        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(text(activity, title, 19, DARK, true));
        TextView note = text(activity, subtitle, 12, MUTED, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(activity, 4);
        heading.addView(note, noteParams);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(header);
        return card;
    }

    private static TextView button(Activity activity, String label, boolean primary) {
        TextView button = text(activity, label, 14, primary ? Color.WHITE : Color.rgb(71, 85, 105), true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(MotionHelper.ripple(
                primary ? 0x33FFFFFF : 0x182563EB,
                shape(activity, primary ? BLUE : Color.rgb(241, 245, 249), 10, Color.TRANSPARENT)
        ));
        button.setClickable(true);
        MotionHelper.attachPressScale(button);
        return button;
    }

    private static TextView text(Activity activity, String value, int size, int color, boolean bold) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private static GradientDrawable shape(Activity activity, int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(activity, radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(activity, 1), stroke);
        return drawable;
    }

    private static int dp(Activity activity, float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static void show(Activity activity, Dialog dialog, LinearLayout content, boolean cancelable) {
        dialog.setContentView(content);
        dialog.setCancelable(cancelable);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 32), dp(activity, 430)), WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.38f;
            window.setAttributes(attributes);
        }
        MotionHelper.animateDialogEnter(content);
    }
}