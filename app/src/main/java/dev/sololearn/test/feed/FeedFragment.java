package dev.sololearn.test.feed;

import android.app.Activity;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.transition.Fade;
import android.transition.TransitionInflater;
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
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
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
import dev.sololearn.test.util.ViewModelFactory;

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
    private OffsetDecoration feedItemsOffsetDecoration;

    private RequestManager glideRequestManager;
    private NetworkStateReceiver networkStateReceiver;
    private StaggeredGridLayoutManager staggeredGridLayoutManager;
    private LinearLayoutManager linearLayoutManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            setSharedElementEnterTransition(TransitionInflater.from(activity).inflateTransition(R.transition.image_transform));
            setSharedElementReturnTransition(TransitionInflater.from(activity).inflateTransition(R.transition.image_transform));
            setExitTransition(new Fade());
        }
    }

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
        feedViewModel = obtainViewModel(getActivity());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int marginStartEnd = getResources().getDimensionPixelSize(R.dimen.feed_item_margin_start_end);
        feedItemsOffsetDecoration = new OffsetDecoration(
                marginStartEnd, marginStartEnd, 0,
                getResources().getDimensionPixelSize(R.dimen.home_page_articles_margin_bottom), false);
        feedStyle = sharedPreferences.getInt(Constants.HOME_PAGE_VIEW_STYLE,
                Utils.isScreenLargeOrXLarge(getResources()) ? FEED_STYLE_GRID : FEED_STYLE_LIST);

        binding.setViewmodel(feedViewModel);
        glideRequestManager = GlideApp.with(this);

        initPinnedContainer();
        setupFeedItemsAdapter(true);
        setupPinnedItemsAdapter();
        setupObserversAndStart();

        if (isFirstOpen) {
            if (Utils.checkInternetConnection(getActivity())) {
                feedViewModel.startOnline();
            } else {
                feedViewModel.startOffline();
            }
        }
        feedViewModel.startPeriodicChecking();
        observePinChanges();
        setHasOptionsMenu(true);
        if (isFirstOpen) {
            isFirstOpen = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkNetwork();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (networkStateReceiver != null && getActivity() != null) {
            getActivity().unregisterReceiver(networkStateReceiver);
        }
        feedViewModel.stopChecking();
    }

    private static FeedViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(FeedViewModel.class);
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

    private void observePinChanges() {
        feedViewModel.getPinUnPinEvent().observe(this, aBoolean -> feedViewModel.getPinnedItemsCount(count -> {
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
        }));
    }

    private void setupFeedItemsAdapter(boolean regiesterPagedScroll) {
        feedItemsAdapter = new FeedArticlesAdapter(getActivity(), feedViewModel.getOpenArticleEvent(), glideRequestManager);
        feedItemsAdapter.setFeedStyle(feedStyle);
        if (feedStyle == FEED_STYLE_LIST) {
            linearLayoutManager = new LinearLayoutManager(getActivity());
            binding.articlesRecyclerView.setLayoutManager(linearLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsOffsetDecoration);
            }
        } else {
            staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                    getResources().getInteger(R.integer.staggered_style_column_count),
                    StaggeredGridLayoutManager.VERTICAL);
            binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsOffsetDecoration);
            }
        }
        binding.articlesRecyclerView.setAdapter(feedItemsAdapter);
        if (regiesterPagedScroll) {
            PagedScroll.with(binding.articlesRecyclerView, feedViewModel.getLoadMorePagedCallback())
                    .setLoadingThreshold(5).setLoadForFirstTime(isFirstOpen && Utils.checkInternetConnection(getActivity())).build();
        }
        feedViewModel.getItems().observe(this, articles -> {
            if (articles != null) {
                feedItemsAdapter.setItems(articles);
            }
        });
    }

    private void setupPinnedItemsAdapter() {
        pinnedItemsAdapter = new PinnedItemsAdapter(feedViewModel.getOpenArticleEvent(), glideRequestManager);
        int orientation = Utils.isLandscape(getActivity()) ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        binding.pinnedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),
                orientation, false));
        binding.pinnedItemsRecyclerView.setAdapter(pinnedItemsAdapter);
        feedViewModel.getPinnedItems().observe(this, articles -> {
            pinnedItemsAdapter.setItems(articles);

            binding.pinnedItemsRecyclerView.scrollTo(0, 0);
        });
        binding.pinnedItemsRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupObserversAndStart() {
        feedViewModel.getNewestArticle().observe(this, newestArticleDate ->
                sharedPreferences.edit().putString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE,
                        newestArticleDate).apply());
        feedViewModel.getIsNewArticlesAvailable().observe(this, aBoolean -> {
            if (aBoolean) {
                AnimationUtils.showViewWithAlphaAnimation(binding.newArticlesAvailable);
            }
        });
    }

    private void checkNetwork() {
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(new NetworkStateReceiver.NetworkStateListener() {
            @Override
            public void onNetworkAvailable(NetworkStateReceiver receiver) {
                feedViewModel.startOnline();
                setupFeedItemsAdapter(false);
                PagedScroll.with(binding.articlesRecyclerView, feedViewModel.getLoadMorePagedCallback())
                        .setLoadingThreshold(5).setLoadForFirstTime(true).build();
                AnimationUtils.hideViewWithAlphaAnimation(binding.noInternetConnection);
            }

            @Override
            public void onNetworkDisconnected(NetworkStateReceiver receiver) {
                AnimationUtils.showViewWithAlphaAnimation(binding.noInternetConnection);
                AnimationUtils.hideViewWithAlphaAnimation(binding.newArticlesAvailable);

            }
        });
        if (getActivity() != null) {
            getActivity().registerReceiver(networkStateReceiver, new IntentFilter(Constants.CONNECTIVITY_CHANGE_ACTION));
        }
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
