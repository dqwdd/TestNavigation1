package net.megastudy.testnavigation1;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;

import com.google.android.material.resources.MaterialResources;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as a
 * bottom sheet.
 */
public class SideSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

  /**
   * Callback for monitoring events about bottom sheets.
   */
  public abstract static class SideSheetCallback {

    /**
     * Called when the bottom sheet changes its state.
     *
     * @param bottomSheet The bottom sheet view.
     * @param newState The new state. This will be one of {@link #STATE_DRAGGING}, {@link
     * #STATE_SETTLING}, {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, {@link #STATE_HIDDEN},
     * or {@link #STATE_HALF_EXPANDED}.
     */
    public abstract void onStateChanged(@NonNull View bottomSheet,
        @State int newState);

    /**
     * Called when the bottom sheet is being dragged.
     *
     * @param bottomSheet The bottom sheet view.
     * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset increases
     * as this bottom sheet is moving upward. From 0 to 1 the sheet is between collapsed and
     * expanded states and from -1 to 0 it is between hidden and collapsed states.
     */
    public abstract void onSlide(@NonNull View bottomSheet, float slideOffset);
  }

  /**
   * The bottom sheet is dragging.
   */
  public static final int STATE_DRAGGING = 1;

  /**
   * The bottom sheet is settling.
   */
  public static final int STATE_SETTLING = 2;

  /**
   * The bottom sheet is expanded.
   */
  public static final int STATE_EXPANDED = 3;

  /**
   * The bottom sheet is collapsed.
   */
  public static final int STATE_COLLAPSED = 4;

  /**
   * The bottom sheet is hidden.
   */
  public static final int STATE_HIDDEN = 5;

  /**
   * The bottom sheet is half-expanded (used when mFitToContents is false).
   */
  public static final int STATE_HALF_EXPANDED = 6;

  /**
   * @hide
   */
  @RestrictTo(LIBRARY_GROUP)
  @IntDef({
      STATE_EXPANDED,
      STATE_COLLAPSED,
      STATE_DRAGGING,
      STATE_SETTLING,
      STATE_HIDDEN,
      STATE_HALF_EXPANDED
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface State {

  }

  /**
   * Peek at the 16:9 ratio keyline of its parent.
   *
   * <p>This can be used as a parameter for {@link #setPeekWidth(int)}. {@link #getPeekWidth()}
   * will return this when the value is set.
   */
  public static final int PEEK_WIDTH_AUTO = -1;

  /**
   * This flag will preserve the peekHeight int value on configuration change.
   */
  public static final int SAVE_PEEK_WIDTH = 0x1;

  /**
   * This flag will preserve the fitToContents boolean value on configuration change.
   */
  public static final int SAVE_FIT_TO_CONTENTS = 0x2;

  /**
   * This flag will preserve the hideable boolean value on configuration change.
   */
  public static final int SAVE_HIDEABLE = 0x4;

  /**
   * This flag will preserve the skipCollapsed boolean value on configuration change.
   */
  public static final int SAVE_SKIP_COLLAPSED = 0x8;

  /**
   * This flag will preserve all aforementioned values on configuration change.
   */
  public static final int SAVE_ALL = -1;

  /**
   * This flag will not preserve the aforementioned values set at runtime if the view is destroyed
   * and recreated. The only value preserved will be the positional state, e.g. collapsed, hidden,
   * expanded, etc. This is the default behavior.
   */
  public static final int SAVE_NONE = 0;

  private int saveFlags = SAVE_NONE;

  private static final float HIDE_THRESHOLD = 0.5f;

  private static final float HIDE_FRICTION = 0.1f;

  private static final int CORNER_ANIMATION_DURATION = 500;

  private boolean fitToContents = true;

  private float maximumVelocity;

  /**
   * Peek height set by the user.
   */
  private int peekWidth;

  /**
   * Whether or not to use automatic peek height.
   */
  private boolean peekWidthAuto;

  /**
   * Minimum peek height permitted.
   */
  private int peekWidthMin;

  /**
   * True if Behavior has a non-null value for the @shapeAppearance attribute
   */
  private boolean shapeThemingEnabled;

  private MaterialShapeDrawable materialShapeDrawable;

  /**
   * Default Shape Appearance to be used in bottomsheet
   */
  private ShapeAppearanceModel shapeAppearanceModelDefault;

  @Nullable
  private ValueAnimator interpolatorAnimator;

  private static final int DEF_STYLE_RES = R.style.Widget_Design_BottomSheet_Modal;

  int fitToContentsOffset;

  int halfExpandedOffset;

  int collapsedOffset;

  boolean hideable;

  private boolean skipCollapsed;

  @State
  int state = STATE_COLLAPSED;

  ViewDragHelper viewDragHelper;

  private boolean ignoreEvents;

  private int lastNestedScrollDx;

  private boolean nestedScrolled;

  int parentWidth;
  int parentHeight;

  WeakReference<V> viewRef;

  WeakReference<View> nestedScrollingChildRef;

  private SideSheetCallback callback;

  private VelocityTracker velocityTracker;

  int activePointerId;

  private int initialX;

  boolean touchingScrollingChild;

  private Map<View, Integer> importantForAccessibilityMap;

  /**
   * 내가 한 거
   * 시작
   * */

//  @Nullable private WeakReference<View> coplanarSiblingViewRef;
//  @IdRes
//  private int coplanarSiblingViewId = View.NO_ID;
//
//  private void maybeAssignCoplanarSiblingViewBasedId(@NonNull CoordinatorLayout parent) {
//    if (coplanarSiblingViewRef == null && coplanarSiblingViewId != View.NO_ID) {
//      View coplanarSiblingView = parent.findViewById(coplanarSiblingViewId);
//      if (coplanarSiblingView != null) {
//        this.coplanarSiblingViewRef = new WeakReference<>(coplanarSiblingView);
//      }
//    }
//  }
//  /**
//   * Set the sibling view to use for coplanar sheet expansion. If a coplanar sibling has previously
//   * been set either by this method or via {@link #setCoplanarSiblingViewId(int)}, that reference
//   * will be cleared in favor of this new coplanar sibling reference.
//   *
//   * @param coplanarSiblingView the sibling view to squash during coplanar expansion
//   */
//  public void setCoplanarSiblingView(@Nullable View coplanarSiblingView) {
//    this.coplanarSiblingViewId = View.NO_ID;
//    if (coplanarSiblingView == null) {
//      clearCoplanarSiblingView();
//    } else {
//      this.coplanarSiblingViewRef = new WeakReference<>(coplanarSiblingView);
//      // Request layout to make the new view take effect.
//      if (viewRef != null) {
//        View view = viewRef.get();
//        if (ViewCompat.isLaidOut(view)) {
//          view.requestLayout();
//        }
//      }
//    }
//  }
//
//  /**
//   * Set the sibling id to use for coplanar sheet expansion. If a coplanar sibling has previously
//   * been set either by this method or via {@link #setCoplanarSiblingView(View)}, that View
//   * reference will be cleared in favor of this new coplanar sibling reference.
//   *
//   * @param coplanarSiblingViewId the id of the coplanar sibling
//   */
//  public void setCoplanarSiblingViewId(@IdRes int coplanarSiblingViewId) {
//    this.coplanarSiblingViewId = coplanarSiblingViewId;
//    // Clear any potential coplanar sibling view to make sure that we use this view id rather than
//    // an existing coplanar sibling view.
//    clearCoplanarSiblingView();
//    // Request layout to find the view and trigger a layout pass.
//    if (viewRef != null) {
//      View view = viewRef.get();
//      if (coplanarSiblingViewId != View.NO_ID && ViewCompat.isLaidOut(view)) {
//        view.requestLayout();
//      }
//    }
//  }
//  /** Returns the sibling view that is used for coplanar sheet expansion. */
//  @Nullable
//  public View getCoplanarSiblingView() {
//    return coplanarSiblingViewRef != null ? coplanarSiblingViewRef.get() : null;
//  }
//
//  private void clearCoplanarSiblingView() {
//    if (this.coplanarSiblingViewRef != null) {
//      this.coplanarSiblingViewRef.clear();
//    }
//    this.coplanarSiblingViewRef = null;
//  }



  /**
   * 내가 한 거
   * 끝
   * */

  public SideSheetBehavior() {
  }

  public SideSheetBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SideSheetBehavior_Layout);
    this.shapeThemingEnabled = a.hasValue(R.styleable.SideSheetBehavior_Layout_shapeAppearance);
    boolean hasBackgroundTint = a.hasValue(R.styleable.SideSheetBehavior_Layout_backgroundTint);
    if (hasBackgroundTint) {
      ColorStateList bottomSheetColor =
          MaterialResources.getColorStateList(
              context, a, R.styleable.SideSheetBehavior_Layout_backgroundTint);
      createMaterialShapeDrawable(context, attrs, hasBackgroundTint, bottomSheetColor);
    } else {
      createMaterialShapeDrawable(context, attrs, hasBackgroundTint);
    }
    createShapeValueAnimator();


    //SideSheetBehavior_Layout_coplanarSiblingViewId 이거 검색해보기

//    if (a.hasValue(R.styleable.SideSheetBehavior_Layout_coplanarSiblingViewId)) {
//      setCoplanarSiblingViewId(
//              a.getResourceId(R.styleable.SideSheetBehavior_Layout_coplanarSiblingViewId, View.NO_ID));
//    }
//    if (a.hasValue(com.google.android.material.R.styleable.SideSheetBehavior_Layout_coplanarSiblingViewId)) {
//      setCoplanarSiblingViewId(
//              a.getResourceId(com.google.android.material.R.styleable.SideSheetBehavior_Layout_coplanarSiblingViewId, View.NO_ID));
//    }


    TypedValue value = a.peekValue(R.styleable.SideSheetBehavior_Layout_behavior_peekWidth);
    if (value != null && value.data == PEEK_WIDTH_AUTO) {
      setPeekWidth(value.data);
    } else {
      setPeekWidth(
          a.getDimensionPixelSize(
              R.styleable.SideSheetBehavior_Layout_behavior_peekWidth, PEEK_WIDTH_AUTO));
    }
    setHideable(a.getBoolean(R.styleable.SideSheetBehavior_Layout_behavior_hideable, false));
    setFitToContents(
        a.getBoolean(R.styleable.SideSheetBehavior_Layout_behavior_fitToContents, true));
    setSkipCollapsed(
        a.getBoolean(R.styleable.SideSheetBehavior_Layout_behavior_skipCollapsed, false));
    setSaveFlags(a.getInt(R.styleable.SideSheetBehavior_Layout_side_behavior_saveFlags, SAVE_NONE));
    a.recycle();
    ViewConfiguration configuration = ViewConfiguration.get(context);
    maximumVelocity = configuration.getScaledMaximumFlingVelocity();
  }

  @Override
  public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
    return new SavedState(super.onSaveInstanceState(parent, child), this);
  }

  @Override
  public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(parent, child, ss.getSuperState());
    // Restore Optional State values designated by saveFlags
    restoreOptionalState(ss);
    // Intermediate states are restored as collapsed state
    if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
      this.state = STATE_COLLAPSED;
    } else {
      this.state = ss.state;
    }
  }

  @Override
  public void onAttachedToLayoutParams(@NonNull LayoutParams layoutParams) {
    super.onAttachedToLayoutParams(layoutParams);
    // These may already be null, but just be safe, explicitly assign them. This lets us know the
    // first time we layout with this behavior by checking (viewRef == null).
    viewRef = null;
    viewDragHelper = null;
  }

  @Override
  public void onDetachedFromLayoutParams() {
    super.onDetachedFromLayoutParams();
    // Release references so we don't run unnecessary codepaths while not attached to a view.
    viewRef = null;
    viewDragHelper = null;
  }

  @Override
  public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
    if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
      child.setFitsSystemWindows(true);
    }
    // Only set MaterialShapeDrawable as background if shapeTheming is enabled, otherwise will
    // default to android:background declared in styles or layout.
    if (shapeThemingEnabled && materialShapeDrawable != null) {
      ViewCompat.setBackground(child, materialShapeDrawable);
    }

    if (viewRef == null) {
      // First layout with this behavior.
      peekWidthMin = parent.getResources().getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min);
      viewRef = new WeakReference<>(child);
    }
    if (viewDragHelper == null) {
      viewDragHelper = ViewDragHelper.create(parent, dragCallback);
    }

    int savedLeft = child.getLeft();
    // First let the parent lay it out
    parent.onLayoutChild(child, layoutDirection);
    // Offset the bottom sheet
    parentWidth = parent.getWidth();
    parentHeight = parent.getHeight();
    fitToContentsOffset = Math.max(0, parentWidth - child.getWidth());
    halfExpandedOffset = parentWidth / 2;
    calculateCollapsedOffset();

    if (state == STATE_EXPANDED) {
      ViewCompat.offsetLeftAndRight(child, getExpandedOffset());
    } else if (state == STATE_HALF_EXPANDED) {
      ViewCompat.offsetLeftAndRight(child, halfExpandedOffset);
    } else if (hideable && state == STATE_HIDDEN) {
      ViewCompat.offsetLeftAndRight(child, parentWidth);
    } else if (state == STATE_COLLAPSED) {
      ViewCompat.offsetLeftAndRight(child, collapsedOffset);
    } else if (state == STATE_DRAGGING || state == STATE_SETTLING) {
      ViewCompat.offsetLeftAndRight(child, savedLeft - child.getLeft());
    }

    nestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
    return true;
  }

  @Override
  public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
    if (!child.isShown()) {
      ignoreEvents = true;
      return false;
    }
    int action = event.getActionMasked();
    // Record the velocity
    if (action == MotionEvent.ACTION_DOWN) {
      reset();
    }
    if (velocityTracker == null) {
      velocityTracker = VelocityTracker.obtain();
    }
    velocityTracker.addMovement(event);
    switch (action) {
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        touchingScrollingChild = false;
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        // Reset the ignore flag
        if (ignoreEvents) {
          ignoreEvents = false;
          return false;
        }
        break;
      case MotionEvent.ACTION_DOWN:
        int initialY = (int) event.getY();
        this.initialX = (int) event.getX();
        // Only intercept nested scrolling events here if the view not being moved by the
        // ViewDragHelper.
        if (state != STATE_SETTLING) {
          View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
          if (scroll != null && parent.isPointInChildBounds(scroll, this.initialX, initialY)) {
            activePointerId = event.getPointerId(event.getActionIndex());
            touchingScrollingChild = true;
          }
        }
        ignoreEvents =
            activePointerId == MotionEvent.INVALID_POINTER_ID
                && !parent.isPointInChildBounds(child, this.initialX, initialY);
        break;
      default: // fall out
    }
    if (!ignoreEvents
        && viewDragHelper != null
        && viewDragHelper.shouldInterceptTouchEvent(event)) {
      return true;
    }
    // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
    // it is not the top most view of its parent. This is not necessary when the touch event is
    // happening over the scrolling content as nested scrolling logic handles that case.
    View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
    return action == MotionEvent.ACTION_MOVE
        && scroll != null
        && !ignoreEvents
        && state != STATE_DRAGGING
        && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY())
        && viewDragHelper != null
        && Math.abs(initialX - event.getX()) > viewDragHelper.getTouchSlop();
  }

  @Override
  public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
    if (!child.isShown()) {
      return false;
    }
    int action = event.getActionMasked();
    if (state == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
      return true;
    }
    if (viewDragHelper != null) {
      viewDragHelper.processTouchEvent(event);
    }
    // Record the velocity
    if (action == MotionEvent.ACTION_DOWN) {
      reset();
    }
    if (velocityTracker == null) {
      velocityTracker = VelocityTracker.obtain();
    }
    velocityTracker.addMovement(event);
    // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
    // to capture the bottom sheet in case it is not captured and the touch slop is passed.
    if (action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
      if (Math.abs(initialX - event.getX()) > viewDragHelper.getTouchSlop()) {
        viewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
      }
    }
    return !ignoreEvents;
  }

  @Override
  public boolean onStartNestedScroll(
      @NonNull CoordinatorLayout coordinatorLayout,
      @NonNull V child,
      @NonNull View directTargetChild,
      @NonNull View target,
      int axes,
      int type) {
    lastNestedScrollDx = 0;
    nestedScrolled = false;
    return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
  }

  @Override
  public void onNestedPreScroll(
      @NonNull CoordinatorLayout coordinatorLayout,
      @NonNull V child,
      @NonNull View target,
      int dx,
      int dy,
      @NonNull int[] consumed,
      int type) {
    if (type == ViewCompat.TYPE_NON_TOUCH) {
      // Ignore fling here. The ViewDragHelper handles it.
      return;
    }
    View scrollingChild = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
    if (target != scrollingChild) {
      return;
    }
    int currentTop = child.getTop();
    int newTop = currentTop - dy;
    if (dy > 0) { // Upward
      if (newTop < getExpandedOffset()) {
        consumed[1] = currentTop - getExpandedOffset();
        ViewCompat.offsetTopAndBottom(child, -consumed[1]);
        setStateInternal(STATE_EXPANDED);
      } else {
        consumed[1] = dy;
        ViewCompat.offsetTopAndBottom(child, -dy);
        setStateInternal(STATE_DRAGGING);
      }
    } else if (dy < 0) { // Downward
      if (!target.canScrollVertically(-1)) {
        if (newTop <= collapsedOffset || hideable) {
          consumed[1] = dy;
          ViewCompat.offsetTopAndBottom(child, -dy);
          setStateInternal(STATE_DRAGGING);
        } else {
          consumed[1] = currentTop - collapsedOffset;
          ViewCompat.offsetTopAndBottom(child, -consumed[1]);
          setStateInternal(STATE_COLLAPSED);
        }
      }
    }
    dispatchOnSlide(child.getTop());
    lastNestedScrollDx = dx;
    nestedScrolled = true;
  }

  @Override
  public void onStopNestedScroll(
      @NonNull CoordinatorLayout coordinatorLayout,
      @NonNull V child,
      @NonNull View target,
      int type) {
    if (child.getTop() == getExpandedOffset()) {
      setStateInternal(STATE_EXPANDED);
      return;
    }
    if (nestedScrollingChildRef == null
        || target != nestedScrollingChildRef.get()
        || !nestedScrolled) {
      return;
    }
    int top;
    int targetState;
    if (lastNestedScrollDx > 0) {
      top = getExpandedOffset();
      targetState = STATE_EXPANDED;
    } else if (hideable && shouldHide(child, getXVelocity())) {
      top = parentHeight;
      targetState = STATE_HIDDEN;
    } else if (lastNestedScrollDx == 0) {
      int currentTop = child.getTop();
      if (fitToContents) {
        if (Math.abs(currentTop - fitToContentsOffset) < Math.abs(currentTop - collapsedOffset)) {
          top = fitToContentsOffset;
          targetState = STATE_EXPANDED;
        } else {
          top = collapsedOffset;
          targetState = STATE_COLLAPSED;
        }
      } else {
        if (currentTop < halfExpandedOffset) {
          if (currentTop < Math.abs(currentTop - collapsedOffset)) {
            top = 0;
            targetState = STATE_EXPANDED;
          } else {
            top = halfExpandedOffset;
            targetState = STATE_HALF_EXPANDED;
          }
        } else {
          if (Math.abs(currentTop - halfExpandedOffset) < Math.abs(currentTop - collapsedOffset)) {
            top = halfExpandedOffset;
            targetState = STATE_HALF_EXPANDED;
          } else {
            top = collapsedOffset;
            targetState = STATE_COLLAPSED;
          }
        }
      }
    } else {
      top = collapsedOffset;
      targetState = STATE_COLLAPSED;
    }
    if (viewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
      setStateInternal(STATE_SETTLING);
      ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
    } else {
      setStateInternal(targetState);
    }
    nestedScrolled = false;
  }

  @Override
  public boolean onNestedPreFling(
      @NonNull CoordinatorLayout coordinatorLayout,
      @NonNull V child,
      @NonNull View target,
      float velocityX,
      float velocityY) {
    if (nestedScrollingChildRef != null) {
      return target == nestedScrollingChildRef.get()
          && (state != STATE_EXPANDED
          || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
    } else {
      return false;
    }
  }

  /**
   * @return whether the height of the expanded sheet is determined by the height of its contents,
   * or if it is expanded in two stages (half the height of the parent container, full height of
   * parent container).
   */
  public boolean isFitToContents() {
    return fitToContents;
  }

  /**
   * Sets whether the height of the expanded sheet is determined by the height of its contents, or
   * if it is expanded in two stages (half the height of the parent container, full height of parent
   * container). Default value is true.
   *
   * @param fitToContents whether or not to fit the expanded sheet to its contents.
   */
  public void setFitToContents(boolean fitToContents) {
    if (this.fitToContents == fitToContents) {
      return;
    }
    this.fitToContents = fitToContents;

    // If sheet is already laid out, recalculate the collapsed offset based on new setting.
    // Otherwise, let onLayoutChild handle this later.
    if (viewRef != null) {
      calculateCollapsedOffset();
    }
    // Fix incorrect expanded settings depending on whether or not we are fitting sheet to contents.
    setStateInternal((this.fitToContents && state == STATE_HALF_EXPANDED) ? STATE_EXPANDED : state);
  }

  /**
   * Sets the height of the bottom sheet when it is collapsed.
   *
   * @param peekWidth The height of the collapsed bottom sheet in pixels, or {@link
   * #PEEK_WIDTH_AUTO} to configure the sheet to peek automatically at 16:9 ratio keyline.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
   */
  public final void setPeekWidth(int peekWidth) {
    setPeekWidth(peekWidth, false);
  }

  /**
   * Sets the height of the bottom sheet when it is collapsed while optionally animating between the
   * old height and the new height.
   *
   * @param peekWidth The height of the collapsed bottom sheet in pixels, or {@link
   * #PEEK_WIDTH_AUTO} to configure the sheet to peek automatically at 16:9 ratio keyline.
   * @param animate Whether to animate between the old height and the new height.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
   */
  public final void setPeekWidth(int peekWidth, boolean animate) {
    boolean layout = false;
    if (peekWidth == PEEK_WIDTH_AUTO) {
      if (!peekWidthAuto) {
        peekWidthAuto = true;
        layout = true;
      }
    } else if (peekWidthAuto || this.peekWidth != peekWidth) {
      peekWidthAuto = false;
      this.peekWidth = Math.max(peekWidth, 0);
      layout = true;
    }
    // If sheet is already laid out, recalculate the collapsed offset based on new setting.
    // Otherwise, let onLayoutChild handle this later.
    if (layout && viewRef != null) {
      calculateCollapsedOffset();
      if (state == STATE_COLLAPSED) {
        V view = viewRef.get();
        if (view != null) {
          if (animate) {
            startSettlingAnimationPendingLayout(state);
          } else {
            view.requestLayout();
          }
        }
      }
    }
  }

  /**
   * Gets the height of the bottom sheet when it is collapsed.
   *
   * @return The height of the collapsed bottom sheet in pixels, or {@link #PEEK_WIDTH_AUTO} if the
   * sheet is configured to peek automatically at 16:9 ratio keyline
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_peekHeight
   */
  public final int getPeekWidth() {
    return peekWidthAuto ? PEEK_WIDTH_AUTO : peekWidth;
  }

  /**
   * Sets whether this bottom sheet can hide when it is swiped down.
   *
   * @param hideable {@code true} to make this bottom sheet hideable.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
   */
  public void setHideable(boolean hideable) {
    if (this.hideable != hideable) {
      this.hideable = hideable;
      if (!hideable && state == STATE_HIDDEN) {
        // Lift up to collapsed state
        setState(STATE_COLLAPSED);
      }
    }
  }

  /**
   * Gets whether this bottom sheet can hide when it is swiped down.
   *
   * @return {@code true} if this bottom sheet can hide.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
   */
  public boolean isHideable() {
    return hideable;
  }

  /**
   * Sets whether this bottom sheet should skip the collapsed state when it is being hidden after it
   * is expanded once. Setting this to true has no effect unless the sheet is hideable.
   *
   * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
   */
  public void setSkipCollapsed(boolean skipCollapsed) {
    this.skipCollapsed = skipCollapsed;
  }

  /**
   * Sets whether this bottom sheet should skip the collapsed state when it is being hidden after it
   * is expanded once.
   *
   * @return Whether the bottom sheet should skip the collapsed state.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
   */
  public boolean getSkipCollapsed() {
    return skipCollapsed;
  }

  /**
   * Sets save flags to be preserved in bottomsheet on configuration change.
   *
   * @param flags bitwise int of {@link #SAVE_PEEK_WIDTH}, {@link #SAVE_FIT_TO_CONTENTS}, {@link
   * #SAVE_HIDEABLE}, {@link #SAVE_SKIP_COLLAPSED}, {@link #SAVE_ALL} and {@link #SAVE_NONE}.
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_saveFlags
   * @see #getSaveFlags()
   */
  public void setSaveFlags(int flags) {
    this.saveFlags = flags;
  }

  /**
   * Returns the save flags.
   *
   * @attr ref com.google.android.material.R.styleable#BottomSheetBehavior_Layout_behavior_saveFlags
   * @see #setSaveFlags(int)
   */
  public int getSaveFlags() {
    return this.saveFlags;
  }

  /**
   * Sets a callback to be notified of bottom sheet events.
   *
   * @param callback The callback to notify when bottom sheet events occur.
   */
  public void setSideSheetCallback(
      SideSheetCallback callback) {
    this.callback = callback;
  }

  /**
   * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
   * animation.
   *
   * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, {@link #STATE_HIDDEN},
   * or {@link #STATE_HALF_EXPANDED}.
   */
  public final void setState(@State int state) {
    @State int previousState = this.state;
    if (state == this.state) {
      return;
    }
    if (viewRef == null) {
      // The view is not laid out yet; modify mState and let onLayoutChild handle it later
      if (state == STATE_COLLAPSED
          || state == STATE_EXPANDED
          || state == STATE_HALF_EXPANDED
          || (hideable && state == STATE_HIDDEN)) {
        this.state = state;
      }
      return;
    }
    startSettlingAnimationPendingLayout(state);
    updateDrawableOnStateChange(state, previousState);
  }

  private void startSettlingAnimationPendingLayout(@State int state) {
    final V child = viewRef.get();
    if (child == null) {
      return;
    }
    // Start the animation; wait until a pending layout if there is one.
    ViewParent parent = child.getParent();
    if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
      final int finalState = state;
      child.post(
          new Runnable() {
            @Override
            public void run() {
              startSettlingAnimation(child, finalState);
            }
          });
    } else {
      startSettlingAnimation(child, state);
    }
  }

  /**
   * Gets the current state of the bottom sheet.
   *
   * @return One of {@link #STATE_EXPANDED}, {@link #STATE_HALF_EXPANDED}, {@link #STATE_COLLAPSED},
   * {@link #STATE_DRAGGING}, {@link #STATE_SETTLING}, or {@link #STATE_HALF_EXPANDED}.
   */
  @State
  public final int getState() {
    return state;
  }

  void setStateInternal(@State int state) {
    int previousState = this.state;

    if (this.state == state) {
      return;
    }
    this.state = state;

    if (viewRef == null) {
      return;
    }

    View bottomSheet = viewRef.get();
    if (bottomSheet == null) {
      return;
    }

    if (state == STATE_HALF_EXPANDED || state == STATE_EXPANDED) {
      updateImportantForAccessibility(true);
    } else if (state == STATE_HIDDEN || state == STATE_COLLAPSED) {
      updateImportantForAccessibility(false);
    }

    ViewCompat.setImportantForAccessibility(
        bottomSheet, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    bottomSheet.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

    updateDrawableOnStateChange(state, previousState);
    if (callback != null) {
      callback.onStateChanged(bottomSheet, state);
    }
  }

  private void updateDrawableOnStateChange(@State int state,
      @State int previousState) {
    if (materialShapeDrawable != null) {
      // If the BottomSheetBehavior's state is set directly to STATE_EXPANDED from
      // STATE_HIDDEN or STATE_COLLAPSED, bypassing  STATE_DRAGGING, the corner transition animation
      // will not be triggered automatically, so we will trigger it here.
      if (state == STATE_EXPANDED
          && (previousState == STATE_HIDDEN || previousState == STATE_COLLAPSED)
          && interpolatorAnimator != null
          && interpolatorAnimator.getAnimatedFraction() == 1) {
        interpolatorAnimator.reverse();
      }
      if (state == STATE_DRAGGING
          && previousState == STATE_EXPANDED && interpolatorAnimator != null) {
        interpolatorAnimator.start();
      }
    }
  }

  private void calculateCollapsedOffset() {
    int peek;
    if (peekWidthAuto) {
      peek = Math.max(peekWidthMin, parentWidth - parentWidth * 9 / 16);
    } else {
      peek = peekWidth;
    }

    if (fitToContents) {
      collapsedOffset = Math.max(parentWidth - peek, fitToContentsOffset);
    } else {
      collapsedOffset = parentWidth - peek;
    }
  }

  private void reset() {
    activePointerId = ViewDragHelper.INVALID_POINTER;
    if (velocityTracker != null) {
      velocityTracker.recycle();
      velocityTracker = null;
    }
  }

  private void restoreOptionalState(
      SavedState ss) {
    if (this.saveFlags == SAVE_NONE) {
      return;
    }
    if (this.saveFlags == SAVE_ALL || (this.saveFlags & SAVE_PEEK_WIDTH) == SAVE_PEEK_WIDTH) {
      this.peekWidth = ss.peekWidth;
    }
    if (this.saveFlags == SAVE_ALL
        || (this.saveFlags & SAVE_FIT_TO_CONTENTS) == SAVE_FIT_TO_CONTENTS) {
      this.fitToContents = ss.fitToContents;
    }
    if (this.saveFlags == SAVE_ALL || (this.saveFlags & SAVE_HIDEABLE) == SAVE_HIDEABLE) {
      this.hideable = ss.hideable;
    }
    if (this.saveFlags == SAVE_ALL
        || (this.saveFlags & SAVE_SKIP_COLLAPSED) == SAVE_SKIP_COLLAPSED) {
      this.skipCollapsed = ss.skipCollapsed;
    }
  }

  boolean shouldHide(View child, float xvel) {
    if (skipCollapsed) {
      return true;
    }
    if (child.getLeft() < collapsedOffset) {
      // It should not hide, but collapse.
      return false;
    }
    final float newLeft = child.getLeft() + xvel * HIDE_FRICTION;
    return Math.abs(newLeft - collapsedOffset) / (float) peekWidth > HIDE_THRESHOLD;
  }

  @VisibleForTesting
  View findScrollingChild(View view) {
    if (ViewCompat.isNestedScrollingEnabled(view)) {
      return view;
    }
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0, count = group.getChildCount(); i < count; i++) {
        View scrollingChild = findScrollingChild(group.getChildAt(i));
        if (scrollingChild != null) {
          return scrollingChild;
        }
      }
    }
    return null;
  }

  private void createMaterialShapeDrawable(
      Context context, AttributeSet attrs, boolean hasBackgroundTint) {
    this.createMaterialShapeDrawable(context, attrs, hasBackgroundTint, null);
  }

  private void createMaterialShapeDrawable(
      Context context,
      AttributeSet attrs,
      boolean hasBackgroundTint,
      @Nullable ColorStateList bottomSheetColor) {
    if (this.shapeThemingEnabled) {

      this.shapeAppearanceModelDefault =
          new ShapeAppearanceModel();

      this.materialShapeDrawable = new MaterialShapeDrawable(shapeAppearanceModelDefault);

      if (hasBackgroundTint && bottomSheetColor != null) {
        materialShapeDrawable.setFillColor(bottomSheetColor);
      } else {
        // If the tint isn't set, use the theme default background color.
        TypedValue defaultColor = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorBackground, defaultColor, true);
        materialShapeDrawable.setTint(defaultColor.data);
      }
    }
  }

  private void createShapeValueAnimator() {
    interpolatorAnimator = ValueAnimator.ofFloat(0f, 1f);
    interpolatorAnimator.setDuration(CORNER_ANIMATION_DURATION);
    interpolatorAnimator.addUpdateListener(
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            float value = (float) animation.getAnimatedValue();
            if (materialShapeDrawable != null) {
              materialShapeDrawable.setInterpolation(value);
            }
          }
        });
  }

  private float getXVelocity() {
    if (velocityTracker == null) {
      return 0;
    }
    velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
    return velocityTracker.getXVelocity(activePointerId);
  }

  private int getExpandedOffset() {
    return fitToContents ? fitToContentsOffset : 0;
  }

  void startSettlingAnimation(View child, int state) {
    int left;
    if (state == STATE_COLLAPSED) {
      left = collapsedOffset;
    } else if (state == STATE_HALF_EXPANDED) {
      left = halfExpandedOffset;
      if (fitToContents && left <= fitToContentsOffset) {
        // Skip to the expanded state if we would scroll past the height of the contents.
        state = STATE_EXPANDED;
        left = fitToContentsOffset;
      }
    } else if (state == STATE_EXPANDED) {
      left = getExpandedOffset();
    } else if (hideable && state == STATE_HIDDEN) {
      left = parentWidth;
    } else {
      throw new IllegalArgumentException("Illegal state argument: " + state);
    }
    if (viewDragHelper.smoothSlideViewTo(child, left, child.getTop())) {
      setStateInternal(STATE_SETTLING);
      ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
    } else {
      setStateInternal(state);
    }
  }

  private final ViewDragHelper.Callback dragCallback =
      new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
          if (state == STATE_DRAGGING) {
            return false;
          }
          if (touchingScrollingChild) {
            return false;
          }
          if (state == STATE_EXPANDED && activePointerId == pointerId) {
            View scroll = nestedScrollingChildRef != null ? nestedScrollingChildRef.get() : null;
            if (scroll != null && scroll.canScrollHorizontally(-1)) {
              // Let the content scroll up
              return false;
            }
          }
          return viewRef != null && viewRef.get() == child;
        }

        @Override
        public void onViewPositionChanged(
            @NonNull View changedView, int left, int top, int dx, int dy) {
          dispatchOnSlide(left);
        }

        @Override
        public void onViewDragStateChanged(int state) {
          if (state == ViewDragHelper.STATE_DRAGGING) {
            setStateInternal(STATE_DRAGGING);
          }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
          int left;
          @State int targetState;
          if (xvel < 0) { // Moving left
            if (fitToContents) {
              left = fitToContentsOffset;
              targetState = STATE_EXPANDED;
            } else {
              int currentLeft = releasedChild.getLeft();
              if (currentLeft > halfExpandedOffset) {
                left = halfExpandedOffset;
                targetState = STATE_HALF_EXPANDED;
              } else {
                left = 0;
                targetState = STATE_EXPANDED;
              }
            }
          } else if (hideable
              && shouldHide(releasedChild, xvel)
              && (releasedChild.getLeft() > collapsedOffset || Math.abs(yvel) < Math.abs(xvel))) {
            // Hide if we shouldn't collapse and the view was either released low or it was a
            // vertical swipe.
            left = parentWidth;
            targetState = STATE_HIDDEN;
          } else if (xvel == 0.f || Math.abs(yvel) > Math.abs(xvel)) {
            // If the Y velocity is 0 or the swipe was mostly horizontal indicated by the X velocity
            // being greater than the Y velocity, settle to the nearest correct height.
            int currentLeft = releasedChild.getLeft();
            if (fitToContents) {
              if (Math.abs(currentLeft - fitToContentsOffset)
                  < Math.abs(currentLeft - collapsedOffset)) {
                left = fitToContentsOffset;
                targetState = STATE_EXPANDED;
              } else {
                left = collapsedOffset;
                targetState = STATE_COLLAPSED;
              }
            } else {
              if (currentLeft < halfExpandedOffset) {
                if (currentLeft < Math.abs(currentLeft - collapsedOffset)) {
                  left = 0;
                  targetState = STATE_EXPANDED;
                } else {
                  left = halfExpandedOffset;
                  targetState = STATE_HALF_EXPANDED;
                }
              } else {
                if (Math.abs(currentLeft - halfExpandedOffset)
                    < Math.abs(currentLeft - collapsedOffset)) {
                  left = halfExpandedOffset;
                  targetState = STATE_HALF_EXPANDED;
                } else {
                  left = collapsedOffset;
                  targetState = STATE_COLLAPSED;
                }
              }
            }
          } else {
            left = collapsedOffset;
            targetState = STATE_COLLAPSED;
          }
          if (viewDragHelper.settleCapturedViewAt(left, releasedChild.getTop())) {
            setStateInternal(STATE_SETTLING);
            if (targetState == STATE_EXPANDED && interpolatorAnimator != null) {
              interpolatorAnimator.reverse();
            }
            ViewCompat.postOnAnimation(
                releasedChild, new SettleRunnable(releasedChild, targetState));
          } else {
            if (targetState == STATE_EXPANDED && interpolatorAnimator != null) {
              interpolatorAnimator.reverse();
            }
            setStateInternal(targetState);
          }
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
          return child.getTop();
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
          return MathUtils.clamp(
              left, getExpandedOffset(), hideable ? parentHeight : collapsedOffset);
        }


        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
          if (hideable) {
            return parentWidth;
          } else {
            return collapsedOffset;
          }
        }
      };

  void dispatchOnSlide(int left) {
    View bottomSheet = viewRef.get();
    if (bottomSheet != null && callback != null) {
      if (left > collapsedOffset) {
        callback.onSlide(
            bottomSheet, (float) (collapsedOffset - left) / (parentWidth - collapsedOffset));
      } else {
        callback.onSlide(
            bottomSheet,
            (float) (collapsedOffset - left) / (collapsedOffset - getExpandedOffset()));
      }
    }
  }


  @VisibleForTesting
  int getPeekWidthMin() {
    return peekWidthMin;
  }

  private class SettleRunnable implements Runnable {

    private final View view;

    @State
    private final int targetState;

    SettleRunnable(View view, @State int targetState) {
      this.view = view;
      this.targetState = targetState;
    }

    @Override
    public void run() {
      if (viewDragHelper != null && viewDragHelper.continueSettling(true)) {
        ViewCompat.postOnAnimation(view, this);
      } else {
        if (state == STATE_SETTLING) {
          setStateInternal(targetState);
        }
      }
    }
  }

  /**
   * State persisted across instances
   */
  protected static class SavedState extends AbsSavedState {

    @State
    final int state;
    int peekWidth;
    boolean fitToContents;
    boolean hideable;
    boolean skipCollapsed;

    public SavedState(Parcel source) {
      this(source, null);
    }

    public SavedState(Parcel source, ClassLoader loader) {
      super(source, loader);
      //noinspection ResourceType
      state = source.readInt();
      peekWidth = source.readInt();
      fitToContents = source.readInt() == 1;
      hideable = source.readInt() == 1;
      skipCollapsed = source.readInt() == 1;
    }

    public SavedState(Parcelable superState, SideSheetBehavior behavior) {
      super(superState);
      this.state = behavior.state;
      this.peekWidth = behavior.peekWidth;
      this.fitToContents = behavior.fitToContents;
      this.hideable = behavior.hideable;
      this.skipCollapsed = behavior.skipCollapsed;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(state);
      out.writeInt(peekWidth);
      out.writeInt(fitToContents ? 1 : 0);
      out.writeInt(hideable ? 1 : 0);
      out.writeInt(skipCollapsed ? 1 : 0);
    }

    public static final Creator<SavedState> CREATOR =
        new ClassLoaderCreator<SavedState>() {
          @Override
          public SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new SavedState(in, loader);
          }

          @Override
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in, null);
          }

          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }

  /**
   * A utility function to get the {@link SideSheetBehavior} associated with the {@code view}.
   *
   * @param view The {@link View} with {@link SideSheetBehavior}.
   * @return The {@link SideSheetBehavior} associated with the {@code view}.
   */
  @SuppressWarnings("unchecked")
  public static <V extends View> SideSheetBehavior<V> from(V view) {
    ViewGroup.LayoutParams params = view.getLayoutParams();
    if (!(params instanceof LayoutParams)) {
      throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
    }
    CoordinatorLayout.Behavior behavior = ((LayoutParams) params).getBehavior();
    if (!(behavior instanceof SideSheetBehavior)) {
      throw new IllegalArgumentException("The view is not associated with BottomSheetBehavior");
    }
    return (SideSheetBehavior<V>) behavior;
  }

  private void updateImportantForAccessibility(boolean expanded) {
    if (viewRef == null) {
      return;
    }

    ViewParent viewParent = viewRef.get().getParent();
    if (!(viewParent instanceof CoordinatorLayout)) {
      return;
    }

    CoordinatorLayout parent = (CoordinatorLayout) viewParent;
    final int childCount = parent.getChildCount();
    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && expanded) {
      if (importantForAccessibilityMap == null) {
        importantForAccessibilityMap = new HashMap<>(childCount);
      } else {
        // The important for accessibility values of the child views have been saved already.
        return;
      }
    }

    for (int i = 0; i < childCount; i++) {
      final View child = parent.getChildAt(i);
      if (child == viewRef.get()) {
        continue;
      }

      if (!expanded) {
        if (importantForAccessibilityMap != null
            && importantForAccessibilityMap.containsKey(child)) {
          // Restores the original important for accessibility value of the child view.
          ViewCompat.setImportantForAccessibility(child, importantForAccessibilityMap.get(child));
        }
      } else {
        // Saves the important for accessibility value of the child view.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          importantForAccessibilityMap.put(child, child.getImportantForAccessibility());
        }

        ViewCompat.setImportantForAccessibility(
            child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
      }
    }

    if (!expanded) {
      importantForAccessibilityMap = null;
    }
  }
}
