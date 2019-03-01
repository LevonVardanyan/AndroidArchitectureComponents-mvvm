package dev.sololearn.test.feed;

import android.app.Activity;
import android.content.IntentFilter;
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
import com.paginate.Paginate;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import dev.sololearn.test.GlideApp;
import dev.sololearn.test.R;
import dev.sololearn.test.databinding.FeedFragmentBinding;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.feed.pinned.PinnedItemsAdapter;
import dev.sololearn.test.openarticle.OpenArticleFragment;
import dev.sololearn.test.util.AnimationUtils;
import dev.sololearn.test.util.Constants;
import dev.sololearn.test.util.NetworkStateReceiver;
import dev.sololearn.test.util.OffsetDecoration;
import dev.sololearn.test.util.Utils;
import dev.sololearn.test.util.ViewModelFactory;

public class FeedFragment extends Fragment implements View.OnClickListener {
    static final String TAG = "feedFragment";
    public final static int FEED_STYLE_LIST = 0;
    public final static int FEED_STYLE_GRID = 1;

    private FeedFragmentBinding binding;
    private FeedViewModel feedViewModel;
    private FeedArticlesAdapter adapter;
    private PinnedItemsAdapter pinnedItemsAdapter;

    private int feedStyle;
    private SharedPreferences sharedPreferences;
    private OffsetDecoration feedItemsOffsetDecoration;

    private RequestManager glideRequestManager;
    private NetworkStateReceiver networkStateReceiver;

    private OpenArticleFragment openArticleFragment;

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

        binding.executePendingBindings();

        initPinnedContainer();
        setupFeedAdapter();
        setupPinnedItems();
        setupObserversAndStart();

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkNetwork();
        initOpenArticleFragment();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (networkStateReceiver != null) {
            getActivity().unregisterReceiver(networkStateReceiver);
        }
        feedViewModel.stopChecking();
    }

    private static FeedViewModel obtainViewModel(FragmentActivity activity) {
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
        return ViewModelProviders.of(activity, factory).get(FeedViewModel.class);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.home_page_menu, menu);
        menu.findItem(R.id.switch_style).setVisible(!Utils.isScreenLargeOrXLarge(getResources()));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem switchStyle = menu.findItem(R.id.switch_style);
        switchStyle.setIcon(feedStyle == FEED_STYLE_LIST ? R.drawable.ic_feed_style_list : R.drawable.ic_feed_style_grid);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_style:
                if (feedStyle == FEED_STYLE_LIST) {
                    feedStyle = FEED_STYLE_GRID;
                    StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                            getResources().getInteger(R.integer.staggered_style_column_count),
                            StaggeredGridLayoutManager.VERTICAL);
                    binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
                } else {
                    feedStyle = FEED_STYLE_LIST;
                    binding.articlesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                }
                adapter.setFeedStyle(feedStyle);
                binding.articlesRecyclerView.setAdapter(adapter);
                sharedPreferences.edit().putInt(Constants.HOME_PAGE_VIEW_STYLE, feedStyle).apply();
                getActivity().invalidateOptionsMenu();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onBackPressed() {
        if (openArticleFragment.isAdded()) {
            openArticleFragment.onBackPressed();
            return true;
        }
        return false;
    }

    private void initOpenArticleFragment() {
        openArticleFragment = (OpenArticleFragment) getActivity().getSupportFragmentManager().findFragmentByTag(OpenArticleFragment.TAG);
        if (openArticleFragment == null) {
            openArticleFragment = new OpenArticleFragment();
        }
        openArticleFragment.setOnActionListener((currentArticle, action) -> {
            switch (action) {
                case PIN_UNPIN:
                    pinUnpin(currentArticle);
                    break;
                case SAVE:
                    feedViewModel.saveArticleForOffline(currentArticle);
                    break;
            }
        });
    }

    private void initPinnedContainer() {
        feedViewModel.getPinnedItemsCount().observe(this, integer -> {
            int panelHeight = getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height);
            int panelWidth = getResources().getDimensionPixelSize(R.dimen.pinned_items_landscape_container_width);
            ViewGroup.LayoutParams layoutParams = binding.pinnedItemsContainer.getLayoutParams();
            if (integer == 0) {
                if (Utils.isLandscape(getActivity())) {
                    layoutParams.width = 0;
                } else {
                    layoutParams.height = 0;
                }
            } else {
                if (Utils.isLandscape(getActivity())) {
                    layoutParams.width = panelWidth;
                } else {
                    layoutParams.height = panelHeight;
                }
            }
            binding.pinnedItemsContainer.setLayoutParams(layoutParams);
        });
    }

    private void pinUnpin(Article article) {
        feedViewModel.pinUnPinArticle(article, () -> feedViewModel.getPinnedItemsCount().observe(FeedFragment.this, integer -> {
            feedViewModel.getPinnedItemsCount().removeObservers(FeedFragment.this);
            if (getActivity() == null) {
                return;
            }
            int panelHeight = getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height);
            int panelWidth = getResources().getDimensionPixelSize(R.dimen.pinned_items_landscape_container_width);
            if (article.pinned && integer == 1) {
                if (Utils.isLandscape(getActivity())) {
                    AnimationUtils.showAnimateViewWidth(binding.pinnedItemsContainer, panelWidth);
                } else {
                    AnimationUtils.showAnimateViewHeight(binding.pinnedItemsContainer, panelHeight);
                }
            } else if (!article.pinned && integer == 0) {
                if (Utils.isLandscape(getActivity())) {
                    AnimationUtils.hideAnimateViewWidth(binding.pinnedItemsContainer, panelWidth);
                } else {
                    AnimationUtils.hideAnimateViewHeight(binding.pinnedItemsContainer, panelHeight);
                }
            }
        }));
    }

    private void setupFeedAdapter() {
        adapter = new FeedArticlesAdapter(getActivity(), feedViewModel.getOpenArticleEvent(), glideRequestManager);
        adapter.setFeedStyle(feedStyle);
        if (feedStyle == FEED_STYLE_LIST) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            binding.articlesRecyclerView.setLayoutManager(linearLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsOffsetDecoration);
            }
        } else {
            StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                    getResources().getInteger(R.integer.staggered_style_column_count),
                    StaggeredGridLayoutManager.VERTICAL);
            binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsOffsetDecoration);
            }
        }
        binding.articlesRecyclerView.setAdapter(adapter);
        Paginate.with(binding.articlesRecyclerView, feedViewModel.getLoadMoreCallback())
                .addLoadingListItem(false).build();
        feedViewModel.getItems().observe(this, articles -> adapter.setItems(articles));
    }

    private void setupPinnedItems() {
        pinnedItemsAdapter = new PinnedItemsAdapter(feedViewModel, new ArrayList<>(0), glideRequestManager);
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
        feedViewModel.getOpenArticleEvent().observe(this, this::openArticle);
        feedViewModel.getNewestArticle().observe(this, newestArticleDate ->
                sharedPreferences.edit().putString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE,
                        newestArticleDate).apply());
        feedViewModel.getIsNewArticlesAvailable().observe(this, aBoolean -> {
            if (aBoolean) {
                AnimationUtils.showViewWithAlphaAnimation(binding.newArticlesAvailable);
            } else {
                AnimationUtils.hideViewWithAlphaAnimation(binding.newArticlesAvailable);
            }
        });

        if (Utils.checkInternetConnection(getActivity())) {
            feedViewModel.startOnline();
        } else {
            feedViewModel.startOffline();
        }
    }

    private void checkNetwork() {
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(new NetworkStateReceiver.NetworkStateListener() {
            @Override
            public void onNetworkAvailable(NetworkStateReceiver receiver) {
                if (feedViewModel.isEmpty.get()) {
                    setupFeedAdapter();
                }
                AnimationUtils.hideViewWithAlphaAnimation(binding.noInternetConnection);
            }

            @Override
            public void onNetworkDisconnected(NetworkStateReceiver receiver) {
                AnimationUtils.showViewWithAlphaAnimation(binding.noInternetConnection);
                AnimationUtils.hideViewWithAlphaAnimation(binding.newArticlesAvailable);

            }
        });
        getActivity().registerReceiver(networkStateReceiver, new IntentFilter(Constants.CONNECTIVITY_CHANGE_ACTION));
    }

    private void openArticle(ClickArticleEvent clickArticleEvent) {
        View[] sharedViews = clickArticleEvent.getSharedViews();
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        Bundle arguments = openArticleFragment.getArguments();
        if (arguments == null) {
            arguments = new Bundle();
        }
        arguments.putParcelable(Constants.EXTRA_ARTICLE, clickArticleEvent.getArticle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sharedViews != null) {
            arguments.putString(Constants.EXTRA_TRANSITION_NAME_THUMB, ViewCompat.getTransitionName(sharedViews[0]));

            for (View view : sharedViews) {
                fragmentTransaction.addSharedElement(view, ViewCompat.getTransitionName(view));
            }
            fragmentTransaction.replace(R.id.open_article_container, openArticleFragment,
                    OpenArticleFragment.TAG);
        }
        if (openArticleFragment.getArguments() == null) {
            openArticleFragment.setArguments(arguments);
        }
        fragmentTransaction.replace(R.id.open_article_container, openArticleFragment,
                OpenArticleFragment.TAG).commit();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.new_articles_available:
                binding.newArticlesAvailable.animate().alpha(0).start();
                binding.articlesRecyclerView.scrollToPosition(0);
                break;
        }
    }
}
