package dev.sololearn.test.feed;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import dev.sololearn.test.GlideApp;
import dev.sololearn.test.R;
import dev.sololearn.test.databinding.FeedFragmentBinding;
import dev.sololearn.test.feed.pinned.PinnedItemsAdapter;
import dev.sololearn.test.pagedscroll.PagedScroll;
import dev.sololearn.test.util.AnimationUtils;
import dev.sololearn.test.util.Constants;
import dev.sololearn.test.util.NetworkStateReceiver;
import dev.sololearn.test.util.OffsetDecoration;
import dev.sololearn.test.util.Utils;

public class FeedFragment extends Fragment implements View.OnClickListener {
    static final String TAG = "feedFragment";
    final static int FEED_STYLE_LIST = 0;
    private final static int FEED_STYLE_GRID = 1;

    private FeedFragmentBinding binding;
    private FeedViewModel feedViewModel;
    private FeedArticlesAdapter feedItemsAdapter;
    private PinnedItemsAdapter pinnedItemsAdapter;

    private int feedStyle;
    private boolean isPinnedPanelOpened;
    private boolean isFirstOpen = true;
    private SharedPreferences sharedPreferences;
    private OffsetDecoration feedItemsListOffsetDecoration;
    private OffsetDecoration feedItemsGridOffsetDecoration;

    private RequestManager glideRequestManager;

    private StaggeredGridLayoutManager staggeredGridLayoutManager;
    private LinearLayoutManager linearLayoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.feed_fragment, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) {
            return;
        }
        feedViewModel = FeedActivity.obtainViewModel(getActivity());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int marginStartEnd = getResources().getDimensionPixelSize(R.dimen.feed_item_margin_start_end);
        feedItemsListOffsetDecoration = new OffsetDecoration(
                marginStartEnd, marginStartEnd, 0,
                getResources().getDimensionPixelSize(R.dimen.home_page_articles_margin_bottom), false);
        feedItemsGridOffsetDecoration = new OffsetDecoration(
                marginStartEnd, marginStartEnd, 0,
                getResources().getDimensionPixelSize(R.dimen.home_page_articles_margin_bottom), false);
        feedStyle = sharedPreferences.getInt(Constants.HOME_PAGE_VIEW_STYLE,
                Utils.isScreenLargeOrXLarge(getResources()) ? FEED_STYLE_GRID : FEED_STYLE_LIST);

        binding.setViewmodel(feedViewModel);
        glideRequestManager = GlideApp.with(this);

        initPinnedContainer();
        setupFeedItemsAdapter();
        setupPinnedItemsAdapter();
        setupObserversAndStart();

        feedViewModel.startPeriodicChecking();
        setHasOptionsMenu(true);
    }

    @Override
    public void onStop() {
        super.onStop();

        feedViewModel.stopChecking();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.home_page_menu, menu);
        menu.findItem(R.id.switch_style).setVisible(!Utils.isScreenLargeOrXLarge(getResources()));
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem switchStyle = menu.findItem(R.id.switch_style);
        switchStyle.setIcon(feedStyle == FEED_STYLE_LIST ? R.drawable.ic_feed_style_list : R.drawable.ic_feed_style_grid);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_style:
                int[] visibleItemPos = new int[getResources().getInteger(R.integer.staggered_style_column_count)];
                if (feedStyle == FEED_STYLE_LIST) {
                    feedStyle = FEED_STYLE_GRID;
                    visibleItemPos[0] = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                    staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                            getResources().getInteger(R.integer.staggered_style_column_count),
                            StaggeredGridLayoutManager.VERTICAL);
                    binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
                } else {
                    feedStyle = FEED_STYLE_LIST;
                    staggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(visibleItemPos);
                    linearLayoutManager = new LinearLayoutManager(getActivity());
                    binding.articlesRecyclerView.setLayoutManager(linearLayoutManager);
                }
                feedItemsAdapter.setFeedStyle(feedStyle);
                binding.articlesRecyclerView.setAdapter(feedItemsAdapter);
                binding.articlesRecyclerView.scrollToPosition(visibleItemPos[0]);
                sharedPreferences.edit().putInt(Constants.HOME_PAGE_VIEW_STYLE, feedStyle).apply();
                if (getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPinnedContainer() {
        feedViewModel.getPinnedItemsCount(count -> {
            if (getActivity() == null) {
                return;
            }
            int panelHeight = getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height);
            int panelWidth = getResources().getDimensionPixelSize(R.dimen.pinned_items_landscape_container_width);
            ViewGroup.LayoutParams layoutParams = binding.pinnedItemsContainer.getLayoutParams();
            int width = count == 0 ? 0 : panelWidth;
            int height = count == 0 ? 0 : panelHeight;
            if (Utils.isLandscape(getActivity())) {
                layoutParams.width = width;
            } else {
                layoutParams.height = height;
            }
            if (isFirstOpen) {
                isPinnedPanelOpened = count > 0;
            }
            binding.pinnedItemsContainer.setLayoutParams(layoutParams);
        });
    }

    private void animatePinPanelIfNeed(int count) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        int panelHeight = getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height);
        int panelWidth = getResources().getDimensionPixelSize(R.dimen.pinned_items_landscape_container_width);
        if (count == 1 && !isPinnedPanelOpened) {
            isPinnedPanelOpened = true;
            if (Utils.isLandscape(activity)) {
                AnimationUtils.showAnimateViewWidth(binding.pinnedItemsContainer, panelWidth);
            } else {
                AnimationUtils.showAnimateViewHeight(binding.pinnedItemsContainer, panelHeight);
            }
        } else if (count == 0 && isPinnedPanelOpened) {
            isPinnedPanelOpened = false;
            if (Utils.isLandscape(activity)) {
                AnimationUtils.hideAnimateViewWidth(binding.pinnedItemsContainer, panelWidth);
            } else {
                AnimationUtils.hideAnimateViewHeight(binding.pinnedItemsContainer, panelHeight);
            }
        }
    }

    private void setupFeedItemsAdapter() {
        if (feedItemsAdapter == null) {
            feedItemsAdapter = new FeedArticlesAdapter(getActivity(), feedViewModel.getOpenArticleEvent(), glideRequestManager);
            feedItemsAdapter.setFeedStyle(feedStyle);
        }
        if (feedStyle == FEED_STYLE_LIST) {
            linearLayoutManager = new LinearLayoutManager(getActivity());
            binding.articlesRecyclerView.setLayoutManager(linearLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsListOffsetDecoration);
            }
        } else {
            staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                    getResources().getInteger(R.integer.staggered_style_column_count),
                    StaggeredGridLayoutManager.VERTICAL);
            binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0 && !Utils.isLandscape(getActivity())) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsGridOffsetDecoration);
            }
        }
        binding.articlesRecyclerView.setAdapter(feedItemsAdapter);
    }

    private void setupPinnedItemsAdapter() {
        if (pinnedItemsAdapter == null) {
            pinnedItemsAdapter = new PinnedItemsAdapter(feedViewModel.getOpenArticleEvent(), glideRequestManager);
        }
        int orientation = Utils.isLandscape(getActivity()) ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        binding.pinnedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),
                orientation, false));
        binding.pinnedItemsRecyclerView.setAdapter(pinnedItemsAdapter);
        feedViewModel.getPinnedItems().observe(getViewLifecycleOwner(), articles -> {
            pinnedItemsAdapter.setItems(articles);

            binding.pinnedItemsRecyclerView.scrollTo(0, 0);
            animatePinPanelIfNeed(articles.size());
        });
        binding.pinnedItemsRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupObserversAndStart() {
        feedViewModel.getNewestArticle().observe(getViewLifecycleOwner(), newestArticleDate ->
                sharedPreferences.edit().putString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE,
                        newestArticleDate).apply());
        feedViewModel.getIsNewArticlesAvailable().observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                AnimationUtils.showViewWithAlphaAnimation(binding.newArticlesAvailable);
            }
        });

        feedViewModel.getNetworkState().observe(getViewLifecycleOwner(), networkState -> {
            int currentState = networkState.getNetworkState();
            if (currentState == NetworkStateReceiver.NetworkState.NETWORK_CONNECTED) {
                int itemSize = feedViewModel.getItemsSize();
                feedViewModel.getItems().observe(getViewLifecycleOwner(), articles -> {
                    if (articles != null) {
                        feedItemsAdapter.setItems(articles);
                    }
                });
                PagedScroll.with(binding.articlesRecyclerView, feedViewModel.getLoadMorePagedCallback())
                        .setLoadingThreshold(5).setLoadForFirstTime(itemSize == 0).build();
                AnimationUtils.hideViewWithAlphaAnimation(binding.noInternetConnection);
            } else {
                feedViewModel.getItems().removeObservers(getViewLifecycleOwner());
                feedViewModel.getOfflineItems().observe(getViewLifecycleOwner(), articles -> {
                    if (articles != null) {
                        feedItemsAdapter.setItems(articles);
                    }
                });
                AnimationUtils.showViewWithAlphaAnimation(binding.noInternetConnection);
                AnimationUtils.hideViewWithAlphaAnimation(binding.newArticlesAvailable);
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.new_articles_available:
                AnimationUtils.hideViewWithAlphaAnimation(binding.newArticlesAvailable);
                feedViewModel.getIsNewArticlesAvailable().setValue(false);
                binding.articlesRecyclerView.smoothScrollToPosition(0);
                break;
        }
    }
}
