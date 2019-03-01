package dev.sololearn.test.callback;

import java.util.List;

import dev.sololearn.test.datamodel.local.Article;


/**
 * Callback for making requests to remote or local and getting result articles list
 */
public interface GetDataCallback {

	void onSuccess(List<Article> articles);
	void onFailure(String errorMessage);
}
