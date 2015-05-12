package com.example.recyclerview.app;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class StickyHeaderLayout extends RecyclerView.LayoutManager {
    public interface IHeaderPosition {
        int getHeaderPosition(int position);
    }

    public StickyHeaderLayout(IHeaderPosition headerPosition) {
        super();

        mHeaderPosition = headerPosition;
    }

    private IHeaderPosition mHeaderPosition;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        if (IHeaderPosition.class.isAssignableFrom(newAdapter.getClass())) {
            mHeaderPosition = (IHeaderPosition) newAdapter;
        }
        removeAllViews();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        if (getItemCount() == 0) {
            return;
        }

        if (state.getItemCount() < mTopLayoutPosition) {
            mTopLayoutPosition = 0;
            mTopLayoutOffset = 0;
        }

        int pos = mTopLayoutPosition;
        int watermark = mTopLayoutOffset;
        while (watermark < getHeight() && pos < state.getItemCount()) {
            View view = recycler.getViewForPosition(pos);
            addView(view);
            measureChildWithMargins(view, 0, 0);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            int height = getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
            int l = getPaddingLeft();
            int t = watermark;
            int r = l + getDecoratedMeasuredWidth(view);
            int b = watermark + height;

            layoutDecorated(view, l + params.leftMargin, t + params.topMargin,
                    r - params.rightMargin, b - params.bottomMargin);

            watermark += height;
            pos++;
        }

        if (watermark < getHeight()) {
            int empty = getHeight() - watermark;
            if (empty + mTopLayoutOffset < 0) {
                offsetChildrenVertical(-empty);
                mTopLayoutOffset += empty;
            } else {
                int offset = -mTopLayoutOffset;
                mTopLayoutOffset = 0;
                while (empty > offset && mTopLayoutPosition > 0) {
                    mTopLayoutPosition--;
                    View view = recycler.getViewForPosition(mTopLayoutPosition);
                    addView(view, 0);
                    measureChildWithMargins(view, 0, 0);

                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
                    int height = getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
                    int l = getPaddingLeft();
                    int t = -offset - height;
                    int r = l + getDecoratedMeasuredWidth(view);
                    int b = -offset;

                    layoutDecorated(view, l + params.leftMargin, t + params.topMargin,
                            r - params.rightMargin, b - params.bottomMargin);

                    offset += height;
                    if (empty < offset) {
                        mTopLayoutOffset = empty - offset;
                    }
                }
                if (offset != 0) {
                    offsetChildrenVertical(-offset);
                }
            }
        }

        recycleInvisible(recycler);
        adjustHeaderPosition(recycler);

        if (mLayoutAfterHeader && mHeaderPosition != null) {
            int headerPos = mHeaderPosition.getHeaderPosition(mTopLayoutPosition);
            if (headerPos < mTopLayoutPosition) {
                View headerView = null;
                for (int i = 0; i < getChildCount(); i++) {
                    View v = getChildAt(i);
                    if (getPosition(v) == headerPos) {
                        headerView = v;
                        break;
                    }
                }
                if (headerView == null) {
                    headerView = recycler.getViewForPosition(headerPos);
                    measureChildWithMargins(headerView, 0, 0);
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) headerView.getLayoutParams();
                    int height = getDecoratedMeasuredHeight(headerView) + params.topMargin + params.bottomMargin;
                    recycler.recycleView(headerView);
                    scrollVerticallyBy(-height, recycler, state);
                } else {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) headerView.getLayoutParams();
                    int height = getDecoratedMeasuredHeight(headerView) + params.topMargin + params.bottomMargin;

                    scrollVerticallyBy(-height, recycler, state);
                }
            }
        }
        mLayoutAfterHeader = false;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }

        if (dy < 0) {
            if (mTopLayoutPosition == 0 && mTopLayoutOffset == 0) {
                return 0;
            }
        }
        else if (dy > 0) {
            if (mTopLayoutPosition + getChildCount() >= state.getItemCount()) {
                View view = getChildAt(getChildCount() - 1);
                if (getPosition(view) == mDetachedHeaderPos) {
                    if (getChildCount() >= 2) {
                        view = getChildAt(getChildCount() - 2);
                    }
                }
                if (getPosition(view) == state.getItemCount() - 1) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
                    int height = getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
                    if (view.getTop() + height <= getHeight()) {
                        return 0;
                    }
                }
            }
        }

        if (mDetachedHeaderPos >= 0) {
            if (mDetachedHeaderPos < mTopLayoutPosition) {
                detachAndScrapViewAt(getChildCount() - 1, recycler);
            } else {
                View header = getChildAt(getChildCount() - 1);
                detachViewAt(getChildCount() - 1);
                attachView(header, 0);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) header.getLayoutParams();
                int height = getDecoratedMeasuredHeight(header) + params.topMargin + params.bottomMargin;
                int l = getPaddingLeft();
                int t = mTopLayoutOffset;
                int r = l + getDecoratedMeasuredWidth(header);
                int b = t + height;

                layoutDecorated(header, l + params.leftMargin, t + params.topMargin,
                        r - params.rightMargin, b - params.bottomMargin);
            }
        }

        int offsetRequested = -dy;
        int offsetPerformed = 0;

        offsetChildrenVertical(offsetRequested);

        if (offsetRequested > 0 ) { // scroll down
            View view = getChildAt(0);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            int top = view.getTop() - params.topMargin;
            while (top > 0 && mTopLayoutPosition > 0) {
                mTopLayoutPosition--;
                view = recycler.getViewForPosition(mTopLayoutPosition);
                addView(view, 0);
                measureChildWithMargins(view, 0, 0);
                int height = getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
                int l = getPaddingLeft();
                int t = top - height;
                int r = l + getDecoratedMeasuredWidth(view);
                int b = top;

                layoutDecorated(view, l + params.leftMargin, t + params.topMargin,
                        r - params.rightMargin, b - params.bottomMargin);

                top -= height;
            }
            if (top > 0) {
                offsetPerformed = offsetRequested - top;
                offsetChildrenVertical(-top);
            } else {
                offsetPerformed = offsetRequested;
            }
        }
        else if (offsetRequested < 0) { // scroll up
            int lastPos = mTopLayoutPosition + getChildCount() - 1;
            View view = getChildAt(getChildCount() - 1);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            int bottom = view.getTop() + getDecoratedMeasuredHeight(view) + params.bottomMargin;
            while (bottom < getHeight() && lastPos < (state.getItemCount() - 1)) {
                lastPos++;
                view = recycler.getViewForPosition(lastPos);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                int height = getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
                int l = getPaddingLeft();
                int t = bottom;
                int r = l + getDecoratedMeasuredWidth(view);
                int b = bottom + height;

                layoutDecorated(view, l + params.leftMargin, t + params.topMargin,
                        r - params.rightMargin, b - params.bottomMargin);

                bottom += height;
            }
            if (bottom < getHeight()) {
                int empty = bottom - getHeight();
                if (empty > offsetRequested) {
                    offsetPerformed = offsetRequested - empty;
                }
                if (offsetPerformed != offsetRequested) {
                    offsetChildrenVertical(offsetPerformed - offsetRequested);
                }
            } else {
                offsetPerformed = offsetRequested;
            }
        }

        if (offsetPerformed != 0) {
            recycleInvisible(recycler);
        }
        adjustHeaderPosition(recycler);

        return -offsetPerformed;
    }

    private void recycleInvisible(RecyclerView.Recycler recycler) {
        while (getChildCount() > 0) {
            View view = getChildAt(0);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            int height = getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
            if ((view.getTop() - params.topMargin + height) < 0) {
                mTopLayoutPosition++;
                removeView(view);
                recycler.recycleView(view);
            } else {
                mTopLayoutOffset = view.getTop() - params.topMargin;
                break;
            }
        }
        while (getChildCount() > 0) {
            View view = getChildAt(getChildCount() - 1);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            if (view.getTop() - params.topMargin > getHeight()) {
                removeView(view);
                recycler.recycleView(view);
            } else {
                break;
            }
        }
    }

    private void adjustHeaderPosition(RecyclerView.Recycler recycler) {
        if (mHeaderPosition == null) {
            return;
        }

        mDetachedHeaderPos = mHeaderPosition.getHeaderPosition(mTopLayoutPosition);
        View header = null;
        if (mDetachedHeaderPos == mTopLayoutPosition) {
            header = getChildAt(0);
            detachViewAt(0);
            attachView(header);
        } else {
            header = recycler.getViewForPosition(mDetachedHeaderPos);
            addView(header);
            measureChildWithMargins(header, 0, 0);
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) header.getLayoutParams();
        int height = getDecoratedMeasuredHeight(header) + params.topMargin + params.bottomMargin;
        int pos = 0;
        int space = 0;
        while (pos < getChildCount() - 1) {
            View view = getChildAt(pos);
            int adapterPos = getPosition(view);
            int headerPos = mHeaderPosition.getHeaderPosition(adapterPos);
            space = view.getTop() - params.topMargin;
            if (space > height) {
                break;
            }
            if (headerPos != mDetachedHeaderPos) {
                break;
            }
            pos++;
        }
        int l = getPaddingLeft();
        int t = space > height ? 0 : space - height;
        int r = l + getDecoratedMeasuredWidth(header);
        int b = t + height;

        layoutDecorated(header, l + params.leftMargin, t + params.topMargin,
                r - params.rightMargin, b - params.bottomMargin);
    }

    @Override
    public void scrollToPosition(int position) {
        mTopLayoutPosition = position;
        mTopLayoutOffset = 0;
        mLayoutAfterHeader = true;
        requestLayout();
    }
/*
    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller linearSmoothScroller =
                new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return StickyHeaderLayout.this.computeScrollVectorForPosition(targetPosition);
                    }
                };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        return new PointF(0, targetPosition > mTopLayoutPosition ? 1 : -1);
    }
*/
    int mTopLayoutPosition = 0;
    int mTopLayoutOffset = 0;
    boolean mLayoutAfterHeader;
    int mDetachedHeaderPos = -1;

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.mFirstVisiblePosition = mTopLayoutPosition;
        state.mFirstVisibleOffset = mTopLayoutOffset;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            mTopLayoutPosition = ss.mFirstVisiblePosition;
            mTopLayoutOffset = ss.mFirstVisibleOffset;
        }
    }

    static class SavedState implements Parcelable {

        int mFirstVisiblePosition;
        int mFirstVisibleOffset;

        public SavedState() {

        }

        SavedState(Parcel in) {
            mFirstVisiblePosition = in.readInt();
            mFirstVisibleOffset = in.readInt();
        }

        public SavedState(SavedState other) {
            mFirstVisiblePosition = other.mFirstVisiblePosition;
            mFirstVisibleOffset = other.mFirstVisibleOffset;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mFirstVisiblePosition);
            dest.writeInt(mFirstVisibleOffset);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    //private static final String TAG = "StickyHeaderLayout";
}
