package dev.sololearn.test.pagedscroll;

import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerPagedScroll provider class, this will create instance of RecyclerPageScroll.Builder
 */
public class PagedScroll {

    public interface Callback {
        /**
         * Called when need load more page, e.g scrollPosition > firstVisiblePosition + loadingThreshold
         */
        void onLoadMore();

        /**
         * @return return true if page loading is in progress, else otherwise
         */
        boolean isLoading();
    }

    public static RecyclerPagedScroll.Builder with(RecyclerView recyclerView, Callback callback) {
        return new RecyclerPagedScroll.Builder(recyclerView, callback);
    }
}
