package dev.sololearn.test.datasource.remote;

import androidx.lifecycle.MutableLiveData;
import dev.sololearn.test.callback.GetDataCallback;
import dev.sololearn.test.datamodel.remote.apimodel.ArticleResponse;

/**
 * Base API for working with remote sources, every remote source must implement this interface
 */
public interface BaseRemoteDataSource {
    /**
     * get paged list of articles
     * @param page page number
     * @param getDataCallback result callback
     */
    void getArticlesPage(int page, GetDataCallback getDataCallback);

    /**
     * free for usage, can implement any kind of getting request
     * @param getDataCallback result callback
     */
    void getArticles(GetDataCallback getDataCallback);

    /**
     * get first page of articles
     * @param getDataCallback result callback
     */
    void getArticlesFirstPage(GetDataCallback getDataCallback);

    /**
     * get is new articles published after our last request
     * @param lastItemDate last saved article publicationDate
     * @return
     */
    boolean isNewerArticleExist(String lastItemDate);

    /**
     * for reset
     */
    void resetPageCounter();

    /**
     * this method must work synchronized, and returns the article list since date with pages
     * @param startDate since date
     * @param page page number
     * @return
     */
    ArticleResponse getArticlesFromDateSync(String startDate, int page);

    /**
     * can be used for returning some live data for observing, or can event return newest article from
     * remote if your server supports that request
     * @return
     */
    MutableLiveData<String> getNewestArticle();
}
