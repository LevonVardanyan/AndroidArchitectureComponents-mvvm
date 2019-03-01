package dev.sololearn.test.datamodel.local;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Article.class}, version = 1, exportSchema = false)
public abstract class ArticlesDataBase extends RoomDatabase {
	private static ArticlesDataBase articlesDataBase;

	public abstract ArticlesDao getArticlesDao();

	public static ArticlesDataBase getInstance(@NonNull Context context) {
		if (articlesDataBase == null) {
			synchronized (ArticlesDataBase.class) {
				if (articlesDataBase == null) {
					articlesDataBase = Room.databaseBuilder(context.getApplicationContext(),
							ArticlesDataBase.class, "articles_db").build();
				}
			}
		}
		return articlesDataBase;
	}
}
