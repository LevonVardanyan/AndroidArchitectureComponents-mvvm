package dev.sololearn.test.datamodel.remote.apimodel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import androidx.annotation.Nullable;
import dev.sololearn.test.datamodel.local.Article;

/**
 * data came from the server
 * will be included in ArticleResponse
 */
public class ArticlesData {

	@Nullable
	@SerializedName("status")
	public String status;

	@SerializedName("currentPage")
	public int currentPage;
	@SerializedName("pages")
	public long pagesCount;

	@Nullable
	@SerializedName("results")
	public List<Article> articleList;

}
