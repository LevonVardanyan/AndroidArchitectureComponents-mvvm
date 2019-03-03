package dev.sololearn.test.feed.pinned;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import dev.sololearn.test.R;
import dev.sololearn.test.databinding.PinnedItemBinding;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.feed.AdapterOfArticlesDiffCallback;
import dev.sololearn.test.feed.ClickArticleEvent;
import dev.sololearn.test.util.Constants;

/**
 * this adapter manages pinned items
 */
public class PinnedItemsAdapter extends RecyclerView.Adapter<PinnedItemsAdapter.PinnedItemViewHolder> {

    private List<Article> items;
    private RequestManager requestManager;
    private MutableLiveData<ClickArticleEvent> clickArticle;

    public PinnedItemsAdapter(MutableLiveData<ClickArticleEvent> clickArticle,
                              RequestManager requestManager) {
        this.requestManager = requestManager;
        this.clickArticle = clickArticle;
        this.items = new ArrayList<>(0);
    }

    public void setItems(List<Article> items) {
        AdapterOfArticlesDiffCallback pinnedItemDiffCallback = new AdapterOfArticlesDiffCallback(this.items, items);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(pinnedItemDiffCallback);

        this.items.clear();
        this.items.addAll(items);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PinnedItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PinnedItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.pinned_item, parent, false);
        return new PinnedItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PinnedItemViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        Article article = items.get(pos);
        holder.binding.setArticle(article);
        holder.binding.pinnedArticleThumbnail.setTransitionName(Constants.PINNED_ARTICLE_IMAGE_TRANSACTION_NAME + position);
        holder.itemView.setOnClickListener(
                v -> clickArticle.setValue(new ClickArticleEvent(article, new View[]{holder.binding.pinnedArticleThumbnail})));
        holder.binding.executePendingBindings();
        if (article != null && article.articleFields != null && article.articleFields.articleThumbnailPath != null) {
            File file = new File(holder.binding.getRoot().getContext().getFilesDir(),
                    article.articleFields.articleThumbnailPath);
            requestManager.load(Uri.fromFile(file)).diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true).apply(RequestOptions
                    .placeholderOf(R.drawable.image_place_holder)).into(holder.binding.pinnedArticleThumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PinnedItemViewHolder extends RecyclerView.ViewHolder {

        PinnedItemBinding binding;

        PinnedItemViewHolder(PinnedItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
