package dev.sololearn.test.feed;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import dev.sololearn.test.datamodel.local.Article;

/**
 * diff callback for adapters that contains itemsList of Articles
 */
public class ArticleItemDiffCallback extends DiffUtil.Callback {

    private List<Article> oldList;
    private List<Article> newList;

    public ArticleItemDiffCallback(List<Article> oldList, List<Article> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).id.equals(newList.get(newItemPosition).id);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return newList.get(newItemPosition).publicationDate;
    }
}
