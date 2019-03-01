package dev.sololearn.test.feed;

import android.app.Application;
import android.preference.PreferenceManager;
import android.view.View;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dev.sololearn.test.callback.DeleteDBCallback;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.repository.ArticlesRepository;
import dev.sololearn.test.util.Constants;
import dev.sololearn.test.util.MyExecutor;

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

    private ArticlesRepository articlesRepository;

    private MutableLiveData<List<Article>> items = new MutableLiveData<>();

    private ObservableBoolean isDataLoading = new ObservableBoolean();
    ObservableBoolean isEmpty = new ObservableBoolean();
    private ObservableBoolean showPinnedContainer = new ObservableBoolean();

    private MutableLiveData<String> isErrorLoadMessage = new MutableLiveData<>();

    private MutableLiveData<OpenArticleEvent> openArticleEvent = new MutableLiveData<>();

    private MutableLiveData<Article> newestArticle = new MutableLiveData<>();
    private MutableLiveData<Boolean> isNewArticlesAvailable = new MutableLiveData<>();

    private Runnable checkForNewArticlesRunnable = new Runnable() {
        @Override
        public void run() {
            String newestDate = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext())
                    .getString(Constants.PREF_NEWEST_ARTICLE_PUBLICATION_DATE, "");
            articlesRepository.refreshArticles(newestDate, new GetDataCallback() {
                        @Override
                        public void onSuccess(List<Article> articles) {
                            MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.UI, () -> {
                                checkForNewItems(articles);
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {}
                    }
            );
            MyExecutor.getInstance().lunchOnRefresh(this, 30000);
        }
    };

    public FeedViewModel(ArticlesRepository articlesRepository, Application application) {
        super(application);
        this.articlesRepository = articlesRepository;
    }

    void updateItem(Article article) {
        List<Article> list = items.getValue();
        if (list != null) {
            int pos = list.indexOf(article);
            if (pos >= 0) {
                Article changedArticle = list.get(pos);
                assert changedArticle != null;
                changedArticle.pinned = article.pinned;
                changedArticle.savedForOffline = article.savedForOffline;
            }
        }
    }

    private void checkForNewItems(List<Article> newArticles) {
        List<Article> currentItems = items.getValue();
        if (currentItems != null) {
            boolean isSomeItemAdded = false;
            for (int i = newArticles.size() - 1; i >= 0; i--) {
                Article nextNewItem = newArticles.get(i);
                int index = currentItems.indexOf(nextNewItem);
                if (index < 0) {
                    currentItems.add(0, nextNewItem);
                    isSomeItemAdded = true;
                } else {
                    currentItems.get(index).copyRemote(nextNewItem);
                }
            }
            items.setValue(currentItems);
            if (isSomeItemAdded) {
                isNewArticlesAvailable.setValue(true);
            }
        }
    }

    void startOnline() {
        isDataLoading.set(true);
        articlesRepository.getRemoteArticlesPage(new GetDataCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                List<Article> currentList = items.getValue();
                if (currentList == null) {
                    items.setValue(articles);
                } else {
                    currentList.addAll(articles);
                    items.setValue(currentList);
                }
                isDataLoading.set(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                isDataLoading.set(false);
            }
        });
        MyExecutor.getInstance().lunchOnRefresh(checkForNewArticlesRunnable, 30000);
    }

    void startOffline() {
        isDataLoading.set(true);
        articlesRepository.getLocalAllArticles(new GetDataCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                items.setValue(articles);
                isDataLoading.set(false);
                if (items.getValue().size() == 0) {
                    isEmpty.set(true);
                }
            }

            @Override
            public void onFailure(String errorMessage) {

            }
        });

    }

    public void addMoreArticles() {
        articlesRepository.getRemoteArticlesPage(new GetDataCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                items.getValue().addAll(articles);
                items.setValue(items.getValue());
            }

            @Override
            public void onFailure(String errorMessage) {

            }
        });
    }

    void pinUnArticle(Article article, Runnable callback) {
        if (article != null) {
            if (article.pinned) {
                articlesRepository.unpinArticle(article, callback);
            } else {
                articlesRepository.pinArticle(article, callback);
                articlesRepository.saveArticleForOffline(article);
            }
            updateItem(article);
        }
    }

    void saveArticleForOffline(Article article) {
        if (article != null && !article.savedForOffline) {
            articlesRepository.saveArticleForOffline(article);
            updateItem(article);
        }
    }

    void stopChecking() {
        MyExecutor.getInstance().getRefreshExecutor().removeCallbacks(checkForNewArticlesRunnable);
    }

    @Nullable
    LiveData<List<Article>> getItems() {
        return items;
    }

    @Nullable
    LiveData<List<Article>> getPinnedItems() {
        return articlesRepository.getPinnedItems();
    }

    LiveData<Integer> getPinnedItemsCount() {
        return articlesRepository.getPinnedItemsCount();
    }

    public void resetPageCounter(@NonNull DeleteDBCallback deleteDBCallback) {
        articlesRepository.reset(deleteDBCallback);
    }


    public MutableLiveData<Boolean> getIsNewArticlesAvailable() {
        return isNewArticlesAvailable;
    }

    public boolean isNewArticlesAvailable() {
        return isNewArticlesAvailable.getValue() == null ? false : isNewArticlesAvailable.getValue();
    }

    public void setIsNewArticlesAvailable(boolean isAvailable) {
        isNewArticlesAvailable.setValue(isAvailable);
    }

    MutableLiveData<String> getNewestArticle() {
        return articlesRepository.getNewestArticle();
    }

    public void onArticleClick(@NonNull Article article, @Nullable View[] sharedViews) {
        openArticleEvent.setValue(new OpenArticleEvent(article, sharedViews));
    }

    @NonNull
    public ObservableBoolean getShowPinnedContainer() {
        return showPinnedContainer;
    }

    @NonNull
    public ObservableBoolean getIsDataLoading() {
        return isDataLoading;
    }

    public ObservableBoolean getIsEmpty() {
        return isEmpty;
    }

    MutableLiveData<OpenArticleEvent> getOpenArticleEvent() {
        return openArticleEvent;
    }
}
