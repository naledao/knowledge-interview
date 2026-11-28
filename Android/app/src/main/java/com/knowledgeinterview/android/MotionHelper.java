package com.knowledgeinterview.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ProgressBar;

/** 安卓原生层统一动效工具类：封装按压回弹、弹窗过渡、表单抖动、波纹与系统动画开关检测。 */
final class MotionHelper {
    static final Interpolator MICRO_INTERPOLATOR = new PathInterpolator(0.2f, 0f, 0f, 1f);
    static final Interpolator ENTER_INTERPOLATOR = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    static final Interpolator EXIT_INTERPOLATOR = new PathInterpolator(0.4f, 0f, 1f, 1f);
    static final Interpolator STANDARD_INTERPOLATOR = new PathInterpolator(0.2f, 0.8f, 0.2f, 1f);

    private static final int TAG_DISMISSING = 0x7f0b0001;
    private static final int TAG_PROGRESS_ANIMATOR = 0x7f0b0002;
    private static final int TAG_SHAKE_ANIMATOR = 0x7f0b0003;

    private MotionHelper() { }

    static boolean isAnimationsEnabled(Context context) {
        if (context == null) return true;
        try {
            float scale = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
            );
            return scale > 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    static Drawable ripple(int rippleColor, Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    /** 为按钮/卡片绑定按下微缩与松手回弹，不拦截点击事件。 */
    static void attachPressScale(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (!v.isEnabled()) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (isAnimationsEnabled(v.getContext())) {
                    v.animate().cancel();
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(100)
                            .setInterpolator(MICRO_INTERPOLATOR)
                            .start();
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (isAnimationsEnabled(v.getContext())) {
                    v.animate().cancel();
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(160)
                            .setInterpolator(ENTER_INTERPOLATOR)
                            .start();
                } else {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
            }
            return false;
        });
    }

    /** 弹窗卡片入场动画：微缩放 + 上浮淡入。 */
    static void animateDialogEnter(View card) {
        if (card == null) return;
        card.setTag(TAG_DISMISSING, Boolean.FALSE);
        card.animate().cancel();
        if (!isAnimationsEnabled(card.getContext())) {
            card.setAlpha(1f);
            card.setScaleX(1f);
            card.setScaleY(1f);
            card.setTranslationY(0f);
            return;
        }
        card.setAlpha(0f);
        card.setScaleX(0.94f);
        card.setScaleY(0.94f);
        card.setTranslationY(dp(card.getContext(), 16f));
        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(ENTER_INTERPOLATOR)
                .start();
    }

    /** 弹窗卡片退场动画：微缩淡出后再关闭弹窗。 */
    static void animateDialogExit(Dialog dialog, View card, Runnable onDismissed) {
        if (dialog == null || !dialog.isShowing()) {
            if (onDismissed != null) onDismissed.run();
            return;
        }
        if (card == null || !isAnimationsEnabled(card.getContext())) {
            safeDismiss(dialog);
            if (onDismissed != null) onDismissed.run();
            return;
        }
        if (Boolean.TRUE.equals(card.getTag(TAG_DISMISSING))) return;
        card.setTag(TAG_DISMISSING, Boolean.TRUE);
        card.animate().cancel();
        card.animate()
                .alpha(0f)
                .scaleX(0.94f)
                .scaleY(0.94f)
                .translationY(dp(card.getContext(), 10f))
                .setDuration(150)
                .setInterpolator(EXIT_INTERPOLATOR)
                .withEndAction(() -> {
                    safeDismiss(dialog);
                    if (onDismissed != null) onDismissed.run();
                })
                .start();
    }

    static void animateDialogExit(Dialog dialog, View card) {
        animateDialogExit(dialog, card, null);
    }

    private static void safeDismiss(Dialog dialog) {
        try {
            if (dialog.isShowing()) dialog.dismiss();
        } catch (RuntimeException ignored) { }
    }

    /** 面板平滑浮现：用于错误态或状态提示面板。 */
    static void animatePanelEnter(View panel) {
        if (panel == null) return;
        panel.animate().cancel();
        panel.setVisibility(View.VISIBLE);
        if (!isAnimationsEnabled(panel.getContext())) {
            panel.setAlpha(1f);
            panel.setTranslationY(0f);
            return;
        }
        panel.setAlpha(0f);
        panel.setTranslationY(dp(panel.getContext(), 12f));
        panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(ENTER_INTERPOLATOR)
                .start();
    }

    /** 轻弹反馈：用于状态切换（如刷新触发、下载完成切换立即安装）。 */
    static void animateBounce(View view) {
        if (view == null) return;
        if (!isAnimationsEnabled(view.getContext())) {
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.animate().cancel();
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        view.animate()
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(130)
                .setInterpolator(ENTER_INTERPOLATOR)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140)
                        .setInterpolator(MICRO_INTERPOLATOR)
                        .start())
                .start();
    }

    /** 表单校验失败时的水平轻抖动 + 轻触觉反馈。 */
    static void shake(View view) {
        if (view == null) return;
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (!isAnimationsEnabled(view.getContext())) return;
        Object previous = view.getTag(TAG_SHAKE_ANIMATOR);
        if (previous instanceof Animator) ((Animator) previous).cancel();
        float d8 = dp(view.getContext(), 8f);
        float d5 = dp(view.getContext(), 5f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                view, View.TRANSLATION_X, 0f, -d8, d8, -d5, d5, 0f
        );
        animator.setDuration(280);
        animator.setInterpolator(MICRO_INTERPOLATOR);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setTranslationX(0f);
                if (view.getTag(TAG_SHAKE_ANIMATOR) == animation) {
                    view.setTag(TAG_SHAKE_ANIMATOR, null);
                }
            }
        });
        view.setTag(TAG_SHAKE_ANIMATOR, animator);
        animator.start();
    }

    /** 平滑推进 ProgressBar 进度，自动取消未完成的旧动画避免跳动。 */
    static void animateProgress(ProgressBar bar, int target) {
        if (bar == null) return;
        int clamped = Math.min(100, Math.max(0, target));
        Object previous = bar.getTag(TAG_PROGRESS_ANIMATOR);
        if (previous instanceof Animator) ((Animator) previous).cancel();
        if (!isAnimationsEnabled(bar.getContext())) {
            bar.setProgress(clamped);
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(bar, "progress", bar.getProgress(), clamped);
        animator.setDuration(180);
        animator.setInterpolator(MICRO_INTERPOLATOR);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (bar.getTag(TAG_PROGRESS_ANIMATOR) == animation) {
                    bar.setTag(TAG_PROGRESS_ANIMATOR, null);
                }
            }
        });
        bar.setTag(TAG_PROGRESS_ANIMATOR, animator);
        animator.start();
    }

    /** 统一处理 Activity 切换转场，尊重系统动画开关。 */
    @SuppressWarnings("deprecation")
    static void applyTransition(Activity activity, int enterAnim, int exitAnim) {
        if (activity == null) return;
        if (!isAnimationsEnabled(activity)) {
            activity.overridePendingTransition(0, 0);
            return;
        }
        activity.overridePendingTransition(enterAnim, exitAnim);
    }

    /** Android 14+ 声明式转场辅助（用于 Activity 打开/关闭）。 */
    static void configureDeclarativeTransitions(Activity activity, int openEnter, int openExit, int closeEnter, int closeExit) {
        if (activity == null || Build.VERSION.SDK_INT < 34) return;
        if (!isAnimationsEnabled(activity)) return;
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, openEnter, openExit);
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, closeEnter, closeExit);
    }
}