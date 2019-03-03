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
    int getPinnedItemsCountSync(boolean pinned);

    /**
     * delete single article from db
     * @param article deleting article
     */
    @Delete
    void delete(Article article);


}
