package dev.sololearn.test.repository;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.callback.GetItemsCountCallback;
import dev.sololearn.test.callback.RefreshListCallback;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.datamodel.remote.RequestConstants;
import dev.sololearn.test.datamodel.remote.apimodel.ArticleResponse;
import dev.sololearn.test.datamodel.remote.apimodel.ArticlesData;
import dev.sololearn.test.datasource.local.BaseLocalDataSource;
import dev.sololearn.test.datasource.remote.BaseRemoteDataSource;
import dev.sololearn.test.util.Constants;
import dev.sololearn.test.util.MyExecutor;

/**
 * Repository class which have only BaseDataSources, here we don't have any coupling with DataSources implementations
 * Try to programming for interfaces not for implementations))
 * Here we have one local data source and one remote, and they must be passed from outside
 * this is the good example of DependencyInjection
 * We made this class as singleton because all app must have one repository
 */
public class ArticlesRepository {

    private static ArticlesRepository articlesRepository;
    private BaseLocalDataSource localDataSource;
    private BaseRemoteDataSource remoteDataSource;

    public static ArticlesRepository getInstance(BaseLocalDataSource localDataSource,
                                                 BaseRemoteDataSource remoteDataSource) {
        if (articlesRepository == null) {
            synchronized (ArticlesRepository.class) {
                if (articlesRepository == null) {
                    articlesRepository = new ArticlesRepository(localDataSource, remoteDataSource);
                }
            }
        }
        return articlesRepository;
    }

    private ArticlesRepository(BaseLocalDataSource localDataSource,
                               BaseRemoteDataSource remoteDataSource) {
        this.localDataSource = localDataSource;
        this.remoteDataSource = remoteDataSource;
    }

    public LiveData<List<Article>> getPinnedItems() {
        return localDataSource.getPinnedArticles();
    }

    public void updateItem(List<Article> articles, Article newArticle) {
        if (articles != null) {
            int pos = articles.indexOf(newArticle);
            if (pos >= 0) {
                Article changedArticle = articles.get(pos);
                assert changedArticle != null;
                changedArticle.pinned = newArticle.pinned;
                changedArticle.savedForOffline = newArticle.savedForOffline;
            }
        }
    }


    public void refreshArticles(String newestArticleDate, RefreshListCallback refreshListCallback) {
        MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.NETWORK, () -> {
            ArticleResponse articleResponse = remoteDataSource.getArticlesFromDateSync(newestArticleDate, 1);
            if (articleResponse != null && articleResponse.articlesData != null &&
                    RequestConstants.RESULT_OK.equalsIgnoreCase(articleResponse.articlesData.status)) {
                ArticlesData articlesData = articleResponse.articlesData;
                long remainPageCount = articlesData.pagesCount - 1;
                List<Article> newArticles = new ArrayList<>(articlesData.articleList);
                int page = 2;
                while (page < remainPageCount) {
                    articleResponse = remoteDataSource.getArticlesFromDateSync(newestArticleDate, page);
                    if (articleResponse != null && articleResponse.articlesData != null &&
                            RequestConstants.RESULT_OK.equalsIgnoreCase(articleResponse.articlesData.status)) {
                        newArticles.addAll(articleResponse.articlesData.articleList);
                    }
                    page++;
                }
                boolean isSomethingAdded = newArticles.size() > 1;
                if (isSomethingAdded) {
                    localDataSource.insert(newArticles);
                }
                MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.UI, () ->
                        refreshListCallback.onDataRefreshed(isSomethingAdded));
            }
        });
    }

    public void saveArticleForOffline(Article article) {
        article.savedForOffline = true;
        localDataSource.insert(article);
    }

    public void addMorePage(Runnable endActionCallback) {
        remoteDataSource.getArticles(new GetDataCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                localDataSource.insert(articles);
                endActionCallback.run();
            }

            @Override
            public void onFailure(String errorMessage) {

            }
        });
    }

    public MutableLiveData<String> getNewestArticle() {
        return remoteDataSource.getNewestArticle();
    }

    public void pinArticle(Article article) {
        article.pinned = true;
        localDataSource.insert(article);
    }

    public void getPinnedItemsCount(GetItemsCountCallback getItemsCountCallback) {
        localDataSource.getPinnedItemsCount(getItemsCountCallback);
    }

    public boolean isNewerArticleExist(String lastArticleDate) {
        return remoteDataSource.isNewerArticleExist(lastArticleDate);
    }

    public void unpinArticle(Article article) {
        article.pinned = false;
        localDataSource.insert(article);
    }

    public LiveData<PagedList<Article>> getLocalArticles(PagedList.BoundaryCallback<Article> callback) {
        PagedList.Config config = new PagedList.Config.Builder().setPageSize(Constants.PAGE_SIZE).build();

        return new LivePagedListBuilder<>(localDataSource.getArticles(), config)
                .setBoundaryCallback(callback).build();
    }

    public void reset(Runnable endActionCallback) {
        remoteDataSource.reset();
        localDataSource.reset(endActionCallback);
    }

}
