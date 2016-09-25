package com.sampsonjoliver.firestarter.views.widgets;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An interaction behavior plugin for child views of {@link CoordinatorLayout} to provide support
 * for the 'swipe-to-dismiss' gesture.
 */
public class PeekDismissBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /**
     * A view is not currently being dragged or animating as a result of a fling/snap.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * A view is currently being dragged. The position is currently changing as a result
     * of user input or simulated user input.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    /**
     * A view is currently settling into place as a result of a fling or
     * predefined non-interactive motion.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    /** @hide */
    @IntDef({SWIPE_DIRECTION_TOP_TO_BOTTOM, SWIPE_DIRECTION_BOTTOM_TO_TOP, SWIPE_DIRECTION_ANY})
    @Retention(RetentionPolicy.SOURCE)
    private @interface SwipeDirection {}

    /**
     * Swipe direction that only allows swiping in the direction of start-to-end. That is
     * left-to-right in LTR, or right-to-left in RTL.
     */
    public static final int SWIPE_DIRECTION_TOP_TO_BOTTOM = 0;

    /**
     * Swipe direction that only allows swiping in the direction of end-to-start. That is
     * right-to-left in LTR or left-to-right in RTL.
     */
    public static final int SWIPE_DIRECTION_BOTTOM_TO_TOP = 1;

    /**
     * Swipe direction which allows swiping in either direction.
     */
    public static final int SWIPE_DIRECTION_ANY = 2;

    private static final float DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f;
    private static final float DEFAULT_ALPHA_START_DISTANCE = 0f;
    private static final float DEFAULT_ALPHA_END_DISTANCE = DEFAULT_DRAG_DISMISS_THRESHOLD;

    private ViewDragHelper mViewDragHelper;
    private OnDismissListener mListener;
    private boolean mIgnoreEvents;

    private float mSensitivity = 0f;
    private boolean mSensitivitySet;

    private int mSwipeDirection = SWIPE_DIRECTION_ANY;
    private float mDragDismissThreshold = DEFAULT_DRAG_DISMISS_THRESHOLD;
    private float mAlphaStartSwipeDistance = DEFAULT_ALPHA_START_DISTANCE;
    private float mAlphaEndSwipeDistance = DEFAULT_ALPHA_END_DISTANCE;

    /**
     * Callback interface used to notify the application that the view has been dismissed.
     */
    public interface OnDismissListener {
        /**
         * Called when {@code view} has been dismissed via swiping.
         */
        public void onDismiss(View view);

        /**
         * Called when the drag state has changed.
         *
         * @param state the new state. One of
         * {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         */
        public void onDragStateChanged(int state);
    }

    /**
     * Set the listener to be used when a dismiss event occurs.
     *
     * @param listener the listener to use.
     */
    public void setListener(OnDismissListener listener) {
        mListener = listener;
    }

    /**
     * Sets the swipe direction for this behavior.
     *
     * @param direction one of the {@link #SWIPE_DIRECTION_TOP_TO_BOTTOM},
     *                  {@link #SWIPE_DIRECTION_BOTTOM_TO_TOP} or {@link #SWIPE_DIRECTION_ANY}
     */
    public void setSwipeDirection(@SwipeDirection int direction) {
        mSwipeDirection = direction;
    }

    /**
     * Set the threshold for telling if a view has been dragged enough to be dismissed.
     *
     * @param distance a ratio of a view's width, values are clamped to 0 >= x <= 1f;
     */
    public void setDragDismissDistance(float distance) {
        mDragDismissThreshold = clamp(0f, distance, 1f);
    }

    /**
     * The minimum swipe distance before the view's alpha is modified.
     *
     * @param fraction the distance as a fraction of the view's width.
     */
    public void setStartAlphaSwipeDistance(float fraction) {
        mAlphaStartSwipeDistance = clamp(0f, fraction, 1f);
    }

    /**
     * The maximum swipe distance for the view's alpha is modified.
     *
     * @param fraction the distance as a fraction of the view's width.
     */
    public void setEndAlphaSwipeDistance(float fraction) {
        mAlphaEndSwipeDistance = clamp(0f, fraction, 1f);
    }

    /**
     * Set the sensitivity used for detecting the start of a swipe. This only takes effect if
     * no touch handling has occured yet.
     *
     * @param sensitivity Multiplier for how sensitive we should be about detecting
     *                    the start of a drag. Larger values are more sensitive. 1.0f is normal.
     */
    public void setSensitivity(float sensitivity) {
        mSensitivity = sensitivity;
        mSensitivitySet = true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            default:
                mIgnoreEvents = !parent.isPointInChildBounds(child,
                        (int) event.getX(), (int) event.getY());
                break;
        }

        if (mIgnoreEvents) {
            return false;
        }

        ensureViewDragHelper(parent);
        return mViewDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (mViewDragHelper != null) {
            mViewDragHelper.processTouchEvent(event);
            return true;
        }
        return false;
    }

    /**
     * Called when the user's input indicates that they want to swipe the given view.
     *
     * @param view View the user is attempting to swipe
     * @return true if the view can be dismissed via swiping, false otherwise
     */
    public boolean canSwipeDismissView(@NonNull View view) {
        return true;
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {
        private static final int INVALID_POINTER_ID = -1;

        private int mOriginalCapturedViewTop;
        private int mActivePointerId = INVALID_POINTER_ID;

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // Only capture if we don't already have an active pointer id
            return mActivePointerId == INVALID_POINTER_ID && canSwipeDismissView(child);
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            mActivePointerId = activePointerId;
            mOriginalCapturedViewTop = capturedChild.getTop();

            // The view has been captured, and thus a drag is about to start so stop any parents
            // intercepting
            final ViewParent parent = capturedChild.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mListener != null) {
                mListener.onDragStateChanged(state);
            }
        }

        @Override
        public void onViewReleased(View child, float xvel, float yvel) {
            // Reset the active pointer ID
            mActivePointerId = INVALID_POINTER_ID;

            final int childHeight = child.getHeight();
            int targetTop;
            boolean dismiss = false;

            if (shouldDismiss(child, yvel)) { // todo tweak to allow vertical dismiss
                targetTop = child.getTop() < mOriginalCapturedViewTop
                        ? mOriginalCapturedViewTop - childHeight
                        : mOriginalCapturedViewTop + childHeight;
                dismiss = true;
            } else {
                // Else, reset back to the original left
                targetTop = mOriginalCapturedViewTop;
            }

            if (mViewDragHelper.settleCapturedViewAt(child.getLeft(), targetTop)) {
                ViewCompat.postOnAnimation(child, new SettleRunnable(child, dismiss));
            } else if (dismiss && mListener != null) {
                mListener.onDismiss(child);
            }
        }

        private boolean shouldDismiss(View child, float yvel) {
            if (yvel != 0f) {
                if (mSwipeDirection == SWIPE_DIRECTION_ANY) {
                    // We don't care about the direction so return true
                    return true;
                } else if (mSwipeDirection == SWIPE_DIRECTION_TOP_TO_BOTTOM) {
                    // We only allow start-to-end swiping, so the fling needs to be in the
                    // correct direction
                    return yvel > 0f;
                } else if (mSwipeDirection == SWIPE_DIRECTION_BOTTOM_TO_TOP) {
                    // We only allow end-to-start swiping, so the fling needs to be in the
                    // correct direction
                    return yvel < 0f;
                }
            } else {
                final int distance = child.getTop() - mOriginalCapturedViewTop;
                final int thresholdDistance = Math.round(child.getHeight() * mDragDismissThreshold);
                return Math.abs(distance) >= thresholdDistance;
            }

            return false;
        }


        @Override
        public int getViewVerticalDragRange(View child) {
            return child.getHeight();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return child.getLeft();
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int min, max;

            if (mSwipeDirection == SWIPE_DIRECTION_TOP_TO_BOTTOM) {
                min = mOriginalCapturedViewTop;
                max = mOriginalCapturedViewTop + child.getHeight();
            } else if (mSwipeDirection == SWIPE_DIRECTION_BOTTOM_TO_TOP) {
                min = mOriginalCapturedViewTop - child.getHeight();
                max = mOriginalCapturedViewTop;
            } else {
                min = mOriginalCapturedViewTop - child.getHeight();
                max = mOriginalCapturedViewTop + child.getHeight();
            }

            return clamp(min, top, max);
        }

        @Override
        public void onViewPositionChanged(View child, int left, int top, int dx, int dy) {
            final float startAlphaDistance = mOriginalCapturedViewTop
                    + child.getHeight() * mAlphaStartSwipeDistance;
            final float endAlphaDistance = mOriginalCapturedViewTop
                    + child.getHeight() * mAlphaEndSwipeDistance;

            if (top <= startAlphaDistance) {
                ViewCompat.setAlpha(child, 1f);
            } else if (top >= endAlphaDistance) {
                ViewCompat.setAlpha(child, 0f);
            } else {
                // We're between the start and end distances
                final float distance = fraction(startAlphaDistance, endAlphaDistance, top);
                ViewCompat.setAlpha(child, clamp(0f, 1f - distance, 1f));
            }
        }
    };

    private void ensureViewDragHelper(ViewGroup parent) {
        if (mViewDragHelper == null) {
            mViewDragHelper = mSensitivitySet
                    ? ViewDragHelper.create(parent, mSensitivity, mDragCallback)
                    : ViewDragHelper.create(parent, mDragCallback);
        }
    }

    private class SettleRunnable implements Runnable {
        private final View mView;
        private final boolean mDismiss;

        SettleRunnable(View view, boolean dismiss) {
            mView = view;
            mDismiss = dismiss;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                if (mDismiss && mListener != null) {
                    mListener.onDismiss(mView);
                }
            }
        }
    }

    private static float clamp(float min, float value, float max) {
        return Math.min(Math.max(min, value), max);
    }

    private static int clamp(int min, int value, int max) {
        return Math.min(Math.max(min, value), max);
    }

    /**
     * Retrieve the current drag state of this behavior. This will return one of
     * {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
     *
     * @return The current drag state
     */
    public int getDragState() {
        return mViewDragHelper != null ? mViewDragHelper.getViewDragState() : STATE_IDLE;
    }

    /**
     * The fraction that {@code value} is between {@code startValue} and {@code endValue}.
     */
    static float fraction(float startValue, float endValue, float value) {
        return (value - startValue) / (endValue - startValue);
    }
}