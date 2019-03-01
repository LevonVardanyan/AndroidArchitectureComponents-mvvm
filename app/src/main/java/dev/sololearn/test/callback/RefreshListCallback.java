package dev.sololearn.test.callback;

import java.util.List;

import dev.sololearn.test.datamodel.local.Article;

public interface RefreshListCallback {
    void onDataRefreshed(List<Article> result, boolean isNewItemAdded);

}
