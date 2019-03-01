package dev.sololearn.test.datamodel.remote.apimodel;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

/**
 * response from server
 */
public class ArticleResponse {

	@Nullable
	@SerializedName("response")
	public ArticlesData articlesData;
}
