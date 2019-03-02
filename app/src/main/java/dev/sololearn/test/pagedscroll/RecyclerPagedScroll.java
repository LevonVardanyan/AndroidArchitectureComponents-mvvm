package dev.sololearn.test.pagedscroll;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * For recyclerView paging scroll handling
 * Checks if during scrolling remains loadingThreshold count of items, then calls callback's onLoadMore
 * method
 * If loadForFirstTime is true, then after object creation callback's onLoadMore method will be called one time
 */
public class RecyclerPagedScroll extends PagedScroll{
    private RecyclerView recyclerView;
    private PagedScroll.Callback callback;
    private int loadingThreshold;
    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            checkEndOffset();
        }
    };

    public RecyclerPagedScroll(RecyclerView recyclerView, PagedScroll.Callback callback, int loadingThreshold,
                               boolean loadForFirstTime) {
        this.recyclerView = recyclerView;
        this.callback = callback;
        this.loadingThreshold = loadingThreshold;
        recyclerView.setOnScrollListener(onScrollListener);

        if (loadForFirstTime) {
            callback.onLoadMore();
        }
    }

    void checkEndOffset() {
        int visibleItemCount = this.recyclerView.getChildCount();
        int totalItemCount = this.recyclerView.getLayoutManager().getItemCount();
        int firstVisibleItemPosition;
        if (this.recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            firstVisibleItemPosition = ((LinearLayoutManager)this.recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            if (!(this.recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager)) {
                throw new IllegalStateException("LayoutManager needs to subclass LinearLayoutManager or StaggeredGridLayoutManager");
            }

            if (this.recyclerView.getLayoutManager().getChildCount() > 0) {
                firstVisibleItemPosition = ((StaggeredGridLayoutManager)this.recyclerView.getLayoutManager()).findFirstVisibleItemPositions((int[])null)[0];
            } else {
                firstVisibleItemPosition = 0;
            }
        }

        if ((totalItemCount - visibleItemCount <= firstVisibleItemPosition + this.loadingThreshold) && !this.callback.isLoading()) {
            this.callback.onLoadMore();
        }
    }

    public static class Builder {
        RecyclerView recyclerView;
        PagedScroll.Callback callback;
        int loadingThreshold = 5;
        boolean loadForFirstTime;

        public Builder(RecyclerView recyclerView, PagedScroll.Callback callback) {
            this.recyclerView = recyclerView;
            this.callback = callback;
        }

        public Builder setLoadingThreshold(int loadingThreshold) {
            this.loadingThreshold = loadingThreshold;
            return this;
        }

        public Builder setLoadForFirstTime(boolean loadForFirstTime) {
            this.loadForFirstTime = loadForFirstTime;
            return this;
        }

        public PagedScroll build() {
            if (this.recyclerView.getAdapter() == null) {
                throw new IllegalStateException("Adapter needs to be set!");
            } else if (this.recyclerView.getLayoutManager() == null) {
                throw new IllegalStateException("LayoutManager needs to be set on the RecyclerView");
            } else {
                return new RecyclerPagedScroll(this.recyclerView, this.callback, this.loadingThreshold, this.loadForFirstTime);
            }
        }
    }
}
