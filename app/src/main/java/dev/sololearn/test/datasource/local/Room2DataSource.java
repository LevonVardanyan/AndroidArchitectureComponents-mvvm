package dev.sololearn.test.datasource.local;

import android.content.Context;

import java.util.List;

import androidx.lifecycle.LiveData;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.datamodel.local.ArticlesDao;
import dev.sololearn.test.datamodel.local.ArticlesDataBase;
import dev.sololearn.test.util.MyExecutor;

/**
 * local source implementation for Room, this class implements BaseLocalDataSource and gives API
 * for working with room
 */
public class Room2DataSource implements BaseLocalDataSource {

    private static Room2DataSource articlesLocalSource;
    private ArticlesDao articlesDao;


    public static Room2DataSource getInstance(Context context) {
        if (articlesLocalSource == null) {
            synchronized (Room2DataSource.class) {
                if (articlesLocalSource == null) {
                    articlesLocalSource = new Room2DataSource(context);
                }
            }
        }
        return articlesLocalSource;
    }

    private Room2DataSource(Context context) {
        articlesDao = ArticlesDataBase.getInstance(context).getArticlesDao();
    }

    @Override
    public void insert(Article article) {
        article.lastUpdateTime = System.currentTimeMillis();
        MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.DB, () -> articlesDao.insert(article));
    }

    @Override
    public void insert(Article article, Runnable callback) {
        article.lastUpdateTime = System.currentTimeMillis();
        MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.DB, () -> {
            articlesDao.insert(article);
            MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.UI, callback::run);
        });
    }

    @Override
    public void remove(Article article) {
        MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.DB, () -> articlesDao.delete(article));
    }

    @Override
    public LiveData<List<Article>> getPinnedArticles() {
        return articlesDao.getPinnedArticles(true);
    }

    @Override
    public LiveData<Integer> getPinnedItemsCount() {
        return articlesDao.getPinnedItemsCount(true);
    }


    @Override
    public void getArticles(GetDataCallback getDataCallback) {
        MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.DB, () -> {
            List<Article> articles = articlesDao.getAllArticles();
            MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.UI, () -> {
                if (!articles.isEmpty()) {
                    getDataCallback.onSuccess(articles);
                }
            });

        });
    }

}
