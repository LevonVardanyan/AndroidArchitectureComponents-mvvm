package dev.sololearn.test.feed;

import android.app.Application;
import android.preference.PreferenceManager;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.callback.GetItemsCountCallback;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.pagedscroll.PagedScroll;
import dev.sololearn.test.repository.ArticlesRepository;
import dev.sololearn.test.util.Constants;
import dev.sololearn.test.util.MyExecutor;
import dev.sololearn.test.util.NetworkStateReceiver;
import dev.sololearn.test.util.PinUnPinPendingEvent;

/**
 * FeedViewModel manages all use cases in FeedsActivity
 * Can be started offline or online, if offline then data will be requested from DB,
 * from the server otherwise
 * Also viewmodel will check every 30 sec. is there any new article after our last remote request, and
 * if there is, then viewmodel will notify observers that data changed
 * ViewModel makes all his work on ArticlesRepository, that's mean that viewModel doesn't know about
 * data and how that data will be reached to him, only thing he does, calling methods on repository.
 */
public class FeedViewModel extends AndroidViewModel {


    private MutableLiveData<List<Article>> items = new MutableLiveData<>();
    private MutableLiveData<ClickArticleEvent> openArticleEvent = new MutableLiveData<>();
    private MutableLiveData<Boolean> isNewArticlesAvailable = new MutableLiveData<>();
    private ObservableBoolean isInitialLoading = new ObservableBoolean();
    private MutableLiveData<Boolean> pinUnPinAction = new MutableLiveData<>();
    private PinUnPinPendingEvent pinUnPinPendingEvent;
    private MutableLiveData<NetworkStateReceiver.NetworkState> networkState = new MutableLiveData<>();
    private ObservableBoolean isEmpty = new ObservableBoolean();
    private ArticlesRepository articlesRepository;
    private boolean isLoading;

    private MyExecutor myExecutor;


    private Runnable checkForNewArticlesRunnable = new Runnable() {
        @Override
        public void run() {
            String newestDate = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext())
                    .getString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE, "");
            articlesRepository.refreshArticles(newestDate, items.getValue(), (result, isNewItemAdded) -> {
                items.setValue(result);
                if (isNewItemAdded) {
                    isNewArticlesAvailable.setValue(true);
                }
            });
            myExecutor.lunchPeriodic(this, 30000);
        }
    };

    private PagedScroll.Callback loadMorePagedCallback = new PagedScroll.Callback() {
        @Override
        public void onLoadMore() {
            isLoading = true;
            loadMore();
        }

        @Override
        public boolean isLoading() {
            return isLoading;
        }
    };

    public FeedViewModel(ArticlesRepository articlesRepository, Application application) {
        super(application);
        this.articlesRepository = articlesRepository;
        myExecutor = MyExecutor.getInstance();
    }

    void startPeriodicChecking() {
        myExecutor.lunchPeriodic(checkForNewArticlesRunnable, 30000);
    }

    LiveData<List<Article>> getOfflineItems() {
        return articlesRepository.getLocalAllArticles();
    }

    private void loadMore() {
        if (items.getValue() == null || getItemsSize() == 0) {
            isInitialLoading.set(true);
            articlesRepository.reset();
        }
        articlesRepository.getRemoteArticlesPage(new GetDataCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                isLoading = false;
                List<Article> currentList = items.getValue();
                if (currentList == null) {
                    items.setValue(articles);
                } else {
                    currentList.addAll(articles);
                    items.setValue(currentList);
                }
                isInitialLoading.set(false);
                isEmpty.set(false);
            }

            @Override
            public void onFailure(String errorMessage) {

            }
        });
    }

    public void executrePendingPinUnPinArticle() {
        if (pinUnPinPendingEvent != null) {
            Article article = pinUnPinPendingEvent.getArticle();
            if (article.pinned) {
                articlesRepository.unpinArticle(article);
            } else {
                articlesRepository.pinArticle(article);
                articlesRepository.saveArticleForOffline(article);
            }
            articlesRepository.updateItem(items.getValue(), article);
            pinUnPinPendingEvent = null;
        }
    }

    public void saveArticleForOffline(Article article) {
        if (article != null && !article.savedForOffline) {
            articlesRepository.saveArticleForOffline(article);
            articlesRepository.updateItem(items.getValue(), article);
        }
    }

    void stopChecking() {
        myExecutor.getRefreshExecutor().removeCallbacks(checkForNewArticlesRunnable);
    }

    LiveData<List<Article>> getItems() {
        return items;
    }

    int getItemsSize() {
        return items.getValue() == null ? 0 : items.getValue().size();
    }

    public void setPendingPinUnPinAricle(Article pendingPinUnPinAricle, int action) {
        this.pinUnPinPendingEvent = new PinUnPinPendingEvent(pendingPinUnPinAricle, action);
        pinUnPinAction.setValue(true);
    }


    public PinUnPinPendingEvent getPinUnPinPendingEvent() {
        return pinUnPinPendingEvent;
    }

    LiveData<List<Article>> getPinnedItems() {
        return articlesRepository.getPinnedItems();
    }

    void getPinnedItemsCount(GetItemsCountCallback getItemsCountCallback) {
        articlesRepository.getPinnedItemsCount(getItemsCountCallback);
    }

    MutableLiveData<Boolean> getIsNewArticlesAvailable() {
        return isNewArticlesAvailable;
    }

    MutableLiveData<String> getNewestArticle() {
        return articlesRepository.getNewestArticle();
    }

    MutableLiveData<Boolean> getPinUnPinAction() {
        return pinUnPinAction;
    }

    @NonNull
    public ObservableBoolean getIsInitialLoading() {
        return isInitialLoading;
    }

    public ObservableBoolean getIsEmpty() {
        return isEmpty;
    }

    MutableLiveData<ClickArticleEvent> getOpenArticleEvent() {
        return openArticleEvent;
    }

    PagedScroll.Callback getLoadMorePagedCallback() {
        return loadMorePagedCallback;
    }

    public MutableLiveData<NetworkStateReceiver.NetworkState> getNetworkState() {
        return networkState;
    }

    public void setNetworkState(int networkState) {
        NetworkStateReceiver.NetworkState currentState = this.networkState.getValue();
        if (currentState == null) {
            currentState = new NetworkStateReceiver.NetworkState(networkState);
        } else {
            currentState.setNetworkState(networkState);
        }
        this.networkState.setValue(currentState);
    }

}
