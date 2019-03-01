package dev.sololearn.test.feed;

import android.view.View;

import dev.sololearn.test.datamodel.local.Article;


/**
 * This event triggers when user clicks on some article, We passed the clicked article and also passed the array of views
 * which must be shared to other activity.  I think this is the limitation of android shared elements
 * transition framework
 */
public class OpenArticleEvent {

	private View[] sharedViews;
	private Article article;

	public OpenArticleEvent(Article article, View[] sharedViews) {
		this.sharedViews = sharedViews;
		this.article = article;
	}

	public View[] getSharedViews() {
		return sharedViews;
	}

	public Article getArticle() {
		return article;
	}

}
