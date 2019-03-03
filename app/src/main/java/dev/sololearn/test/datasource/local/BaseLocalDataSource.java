package dev.sololearn.test.datasource.local;

import java.util.List;

import androidx.lifecycle.LiveData;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.callback.GetItemsCountCallback;
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
     * @return pinned articles list, list where every Article.pinned == true
     */
    LiveData<List<Article>> getPinnedArticles();

    /**
     *
     */
    void getPinnedItemsCount(GetItemsCountCallback getItemsCountCallback);

    /**
     * get all articles
     * @param getDataCallback result callback
     */
    void getArticles(GetDataCallback getDataCallback);

}
