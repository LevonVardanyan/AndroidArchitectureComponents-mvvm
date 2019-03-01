package dev.sololearn.test.datamodel.local;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Dao for working with DB
 */
@Dao
public interface ArticlesDao {

    /**
     * insert single article to db
     * @param article inserting article
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Article article);

    /**
     * insert list of articles to db
     * @param articles inserting list
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(List<Article> articles);

    /**
     * get pinned articles list, sorted by time of inserting
     * @param pinned this will be passed true from caller
     * @return list of pinned articles, where every Article.pinned == true
     */
    @Query(value = "SELECT * FROM articles_table WHERE pinned = :pinned ORDER BY last_updated DESC ")
    LiveData<List<Article>> getPinnedArticles(boolean pinned);

    /**
     * get all articles from db
     * @return list of all articles from db, sorted by publication date
     */
    @Query(value = "SELECT * FROM articles_table ORDER BY publicationDate DESC")
    List<Article> getAllArticles();

    @Query(value = "SELECT Count(*) FROM articles_table WHERE pinned = :pinned")
    LiveData<Integer> getPinnedItemsCount(boolean pinned);

    /**
     * delete single article from db
     * @param article deleting article
     */
    @Delete
    void delete(Article article);

    /**
     * delete all article except pinned and saved articles
     * This method for clearing cached by app articles
     * @param pinned will be passed false, true if want to delete pinned
     * @param savedForOffline will be passed false, true if want to delete saves
     */
    @Query(value = "DELETE FROM articles_table WHERE pinned = :pinned AND is_offline = :savedForOffline")
    void delete(boolean pinned, boolean savedForOffline);


}
