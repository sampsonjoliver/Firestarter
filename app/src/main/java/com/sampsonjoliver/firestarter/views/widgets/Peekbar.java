package com.sampsonjoliver.firestarter.views.widgets;

import android.animation.Animator;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

public class Peekbar extends FrameLayout {

    static final int ANIMATION_DURATION = 250;
    static final int ANIMATION_FADE_DURATION = 180;

    public Peekbar(Context context) {
        super(context);
    }

    public Peekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Peekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                setupView();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });
    }

    final void setupView() {
        final ViewGroup.LayoutParams lp = getLayoutParams();

        if (lp instanceof CoordinatorLayout.LayoutParams) {
            // If our LayoutParams are from a CoordinatorLayout, we'll setup our Behavior

            final PeekDismissBehavior behavior = new PeekDismissBehavior();
            behavior.setStartAlphaSwipeDistance(0.1f);
            behavior.setEndAlphaSwipeDistance(0.6f);
            behavior.setSwipeDirection(PeekDismissBehavior.SWIPE_DIRECTION_BOTTOM_TO_TOP);
            behavior.setListener(new PeekDismissBehavior.OnDismissListener() {
                @Override
                public void onDismiss(View view) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onDragStateChanged(int state) {

                }
            });
            ((CoordinatorLayout.LayoutParams) lp).setBehavior(behavior);
        }
    }

    private void animateViewIn() {
        animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f)
                .translationY(0)
                .setDuration(ANIMATION_DURATION)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {}
                    @Override
                    public void onAnimationCancel(Animator animation) {}
                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
    }

    private void animateViewOut() {
        animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(0f)
                .translationY(-this.getHeight())
                .setDuration(ANIMATION_DURATION)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}
                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
    }

    public final void hideView() {
        if (shouldAnimate() && getVisibility() == View.VISIBLE) {
            animateViewOut();
        } else {
            // If anims are disabled or the view isn't visible, just call back now
            onViewHidden();
        }
    }

    public final void showView() {
        if (shouldAnimate() && getVisibility() != View.VISIBLE) {
            animateViewIn();
        } else {
            // If anims are disabled or the view isn't visible, just call back now
            onViewShown();
        }
    }

    public boolean shouldAnimate() {
        return true;
    }

    private void onViewShown() {

    }

    private void onViewHidden() {

    }

    private static ViewGroup findSuitableParent(View view) {
        ViewGroup fallback = null;
        do {
            if (view instanceof CoordinatorLayout) {
                // We've found a CoordinatorLayout, use it
                return (ViewGroup) view;
            } else if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    // If we've hit the decor content view, then we didn't find a CoL in the
                    // hierarchy, so use it.
                    return (ViewGroup) view;
                } else {
                    // It's not the content view but we'll use it as our fallback
                    fallback = (ViewGroup) view;
                }
            }

            if (view != null) {
                // Else, we will loop and crawl up the view hierarchy and try to find a parent
                final ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);

        // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
        return fallback;
    }
}
