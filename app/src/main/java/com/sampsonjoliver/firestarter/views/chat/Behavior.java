package com.sampsonjoliver.firestarter.views.chat;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;

public class Behavior extends CoordinatorLayout.Behavior<RelativeLayout> {
    private float translationY;

    public Behavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        updateViewTranslationForSnackbar(parent, child, true);
        return false;
    }

    private void updateViewTranslationForSnackbar(CoordinatorLayout parent, final View view, boolean animationAllowed) {
        final float targetTransY = getFabTranslationYForSnackbar(parent, view);
        if (translationY == targetTransY) {
            // We're already at (or currently animating to) the target value, return...
            return;
        }

        final float currentTransY = view.getTranslationY();

        view.clearAnimation();

        if (animationAllowed && view.isShown() && Math.abs(currentTransY - targetTransY) > (view.getHeight() * 0.667f)) {
            // If the FAB will be travelling by more than 2/3 of its height, let's animate it instead
            view.animate().translationY(targetTransY).setInterpolator(new FastOutSlowInInterpolator());
        } else {
            view.setTranslationY(targetTransY);
        }

        translationY = targetTransY;
    }

    private float getFabTranslationYForSnackbar(CoordinatorLayout parent, View fab) {
        float minOffset = 0;
        final List<View> dependencies = parent.getDependencies(fab);
        for (int i = 0, z = dependencies.size(); i < z; i++) {
            final View view = dependencies.get(i);
            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset,
                        ViewCompat.getTranslationY(view) - view.getHeight());
            }
        }

        return minOffset;
    }
}