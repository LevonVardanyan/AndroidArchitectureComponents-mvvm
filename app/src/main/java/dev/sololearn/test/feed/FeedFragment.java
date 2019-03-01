package dev.sololearn.test.feed;

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

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import dev.sololearn.test.GlideApp;
import dev.sololearn.test.R;
import dev.sololearn.test.callback.DeleteDBCallback;
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

    public static final String TAG = "feedFragment";

    FeedFragmentBinding binding;
    FeedViewModel feedViewModel;
    private FeedArticlesAdapter adapter;
    private PinnedItemsAdapter pinnedItemsAdapter;

    private ViewStyle viewStyle;
    private SharedPreferences sharedPreferences;
    private OffsetDecoration feedItemsOffsetDecoration;
//    private OffsetDecoration pinnedItemsOffsetDecoration;

    private RequestManager glideRequestManager;
    private NetworkStateReceiver networkStateReceiver;

    private StaggeredGridLayoutManager staggeredGridLayoutManager;
    private LinearLayoutManager linearLayoutManager;
    private OpenArticleFragment openArticleFragment;

    private int test;
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
        feedViewModel = obtainViewModel(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int marginStartEnd = getResources().getDimensionPixelSize(R.dimen.feed_item_margin_start_end);
        feedItemsOffsetDecoration = new OffsetDecoration(
                marginStartEnd, marginStartEnd, 0,
                getResources().getDimensionPixelSize(R.dimen.home_page_articles_margin_bottom), false);
        viewStyle = ViewStyle.getFromInt(sharedPreferences.getInt(Constants.HOME_PAGE_VIEW_STYLE,
                Utils.isScreenLargeOrXLarge(getResources()) ? ViewStyle.STAGGERED.value : ViewStyle.LIST.value));

        binding.setViewmodel(feedViewModel);
        glideRequestManager = GlideApp.with(this);

        feedViewModel.getOpenArticleEvent().observe(this, this::openArticle);
        binding.executePendingBindings();

        cleanCache(savedInstanceState);
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

    private static FeedViewModel obtainViewModel(Fragment fragment) {
        ViewModelFactory factory = ViewModelFactory.getInstance(fragment.getActivity().getApplication());

        return ViewModelProviders.of(fragment, factory).get(FeedViewModel.class);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.home_page_menu, menu);
        menu.findItem(R.id.switch_style).setVisible(!Utils.isScreenLargeOrXLarge(getResources()));
        return;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem switchStyle = menu.findItem(R.id.switch_style);
        if (viewStyle == ViewStyle.LIST) {
            switchStyle.setIcon(R.drawable.ic_feed_style_list);
        } else {
            switchStyle.setIcon(R.drawable.ic_feed_style_grid);
        }
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_style:
                if (viewStyle == ViewStyle.LIST) {
                    viewStyle = ViewStyle.STAGGERED;
                    staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                            getResources().getInteger(R.integer.staggered_style_column_count),
                            StaggeredGridLayoutManager.VERTICAL);
                    staggeredGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                    binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
                } else {
                    viewStyle = ViewStyle.LIST;
                    binding.articlesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                }
                adapter.setViewStyle(viewStyle);
                binding.articlesRecyclerView.setAdapter(adapter);
                sharedPreferences.edit().putInt(Constants.HOME_PAGE_VIEW_STYLE, viewStyle.value()).apply();
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
                case NONE:
                    break;
            }
        });
    }

    private void pinUnpin(Article article) {
        feedViewModel.pinUnArticle(article, () -> feedViewModel.getPinnedItemsCount().observe(FeedFragment.this, integer -> {
            feedViewModel.getPinnedItemsCount().removeObservers(FeedFragment.this);
            if (getActivity() == null) {
                return;
            }
            if (article.pinned && integer == 1) {
                if (Utils.isLandscape(getActivity())) {
                    AnimationUtils.showAnimateViewWidth(binding.pinnedItemsContainer,
                            getResources().getDimensionPixelSize(R.dimen.pinned_items_container_width));
                } else {
                    AnimationUtils.showAnimateViewHeight(binding.pinnedItemsContainer,
                            getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height));
                }
            } else if (!article.pinned && integer == 0) {
                if (Utils.isLandscape(getActivity())) {
                    AnimationUtils.hideAnimateViewWidth(binding.pinnedItemsContainer,
                            getResources().getDimensionPixelSize(R.dimen.pinned_items_container_width));
                } else {
                    AnimationUtils.hideAnimateViewHeight(binding.pinnedItemsContainer,
                            getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height));
                }
            }
        }));
    }

    /*
   Before loading data, first we need remove unused data from DB
    */
    private void cleanCache(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            feedViewModel.resetPageCounter(new DeleteDBCallback() {
                @Override
                public void onSuccess() {
                    setupFeedAdapter();
                    setupPinnedItems();
                }

                @Override
                public void onFailure() {
                }
            });
        } else {
            setupFeedAdapter();
            setupPinnedItems();
        }
    }

    private void setupFeedAdapter() {
        adapter = new FeedArticlesAdapter(feedViewModel, this, glideRequestManager, new ArrayList<>(0));
        adapter.setViewStyle(viewStyle);
        if (viewStyle == ViewStyle.LIST) {
            linearLayoutManager = new LinearLayoutManager(getActivity());
            binding.articlesRecyclerView.setLayoutManager(linearLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsOffsetDecoration);
            }
        } else {
            staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                    getResources().getInteger(R.integer.staggered_style_column_count),
                    StaggeredGridLayoutManager.VERTICAL);
            staggeredGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            binding.articlesRecyclerView.setLayoutManager(staggeredGridLayoutManager);
            if (binding.articlesRecyclerView.getItemDecorationCount() == 0) {
                binding.articlesRecyclerView.addItemDecoration(feedItemsOffsetDecoration);
            }
        }
        binding.articlesRecyclerView.setAdapter(adapter);
        binding.articlesRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && feedViewModel.isNewArticlesAvailable()) {
                    feedViewModel.setIsNewArticlesAvailable(false);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Utils.checkInternetConnection(getActivity())) {
                    if (!binding.articlesRecyclerView.canScrollVertically(1)) {
                        feedViewModel.addMoreArticles();
                    }

                }
            }
        });
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
            feedViewModel.getShowPinnedContainer().set(articles.isEmpty());
            binding.pinnedItemsRecyclerView.smoothScrollToPosition(0);
        });
        binding.pinnedItemsRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupObserversAndStart() {
        feedViewModel.getNewestArticle().observe(this, newestArticleDate ->
                sharedPreferences.edit().putString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE,
                        newestArticleDate).apply());
        feedViewModel.getIsNewArticlesAvailable().observe(this, aBoolean -> {
            if (aBoolean) {
                binding.newArticlesAvailable.animate().alpha(1f).start();
            } else {
                binding.newArticlesAvailable.animate().alpha(0).start();
            }
        });
        feedViewModel.getPinnedItemsCount().observe(this, integer -> {
            int panelHeight = getResources().getDimensionPixelSize(R.dimen.pinned_items_container_height);
            int panelWidth = getResources().getDimensionPixelSize(R.dimen.pinned_items_container_width);
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
                hideNoInternet();
            }

            @Override
            public void onNetworkDisconnected(NetworkStateReceiver receiver) {
                showNoInternet();
                binding.newArticlesAvailable.animate().alpha(0).start();

            }
        });
        getActivity().registerReceiver(networkStateReceiver, new IntentFilter(Constants.CONNECTIVITY_CHANGE_ACTION));
    }

    private void openArticle(OpenArticleEvent openArticleEvent) {

        View[] sharedViews = openArticleEvent.getSharedViews();
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        Bundle arguments = openArticleFragment.getArguments();
        if (arguments == null) {
            arguments = new Bundle();
        }
        arguments.putParcelable(Constants.EXTRA_ARTICLE, openArticleEvent.getArticle());
        arguments.putString(Constants.EXTRA_TRANSITION_NAME_THUMB, ViewCompat.getTransitionName(sharedViews[0]));
        if (openArticleFragment.getArguments() == null) {
            openArticleFragment.setArguments(arguments);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sharedViews != null) {
            for (View view : sharedViews) {
                fragmentTransaction.addSharedElement(view, ViewCompat.getTransitionName(view));
            }
            fragmentTransaction.replace(R.id.open_article_container, openArticleFragment,
                    OpenArticleFragment.TAG).commit();
        } else {
            fragmentTransaction.replace(R.id.open_article_container, openArticleFragment,
                    OpenArticleFragment.TAG).commit();
        }
    }

    private void showNoInternet() {
        binding.noInternetConnection.animate().alpha(1f).start();
    }

    private void hideNoInternet() {
        binding.noInternetConnection.animate().alpha(0f).start();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.new_articles_available:
                binding.newArticlesAvailable.animate().alpha(0).start();
                binding.articlesRecyclerView.smoothScrollToPosition(0);
                break;
        }
    }

    /*
      Feed style can be changed to List or STAGGERED
       */
    public enum ViewStyle {
        LIST(1), STAGGERED(2);
        int value;

        ViewStyle(int value) {
            this.value = value;
        }

        int value() {
            return value;
        }

        static ViewStyle getFromInt(int value) {
            switch (value) {
                case 1:
                    return LIST;
                case 2:
                    return STAGGERED;
                default:
                    return LIST;
            }
        }
    }

}
