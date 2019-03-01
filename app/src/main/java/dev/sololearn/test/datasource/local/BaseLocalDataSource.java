package dev.sololearn.test.datasource.local;

import java.util.List;

import androidx.lifecycle.LiveData;
import dev.sololearn.test.callback.DeleteDBCallback;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.datamodel.local.Article;

/**
 * Base API for working with local sources, every local source must implement this interface
 */
public interface BaseLocalDataSource {
    /**
     * insert article
     * @param article inserting article
     */
    void insert(Article article);

    /**
     * insert article and return in  callback
     * @param article inserting article
     */
    void insert(Article article, Runnable callback);


    /**
     * remove article
     * @param article removing article
     */
    void remove(Article article);

    /**
     * @return pinned articles list, list where every Article.pinned == true
     */
    LiveData<List<Article>> getPinnedArticles();

    /**
     *
     */
    LiveData<Integer> getPinnedItemsCount();

    /**
     * get all articles
     * @param getDataCallback result callback
     */
    void getArticles(GetDataCallback getDataCallback);

    /**
     * delete from local source
     * @param deleteDBCallback result callback
     */
    void deleteCachedArticles(DeleteDBCallback deleteDBCallback);
}
