package dev.sololearn.test.feed.pinned;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import dev.sololearn.test.R;
import dev.sololearn.test.databinding.PinnedItemBinding;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.feed.ArticleItemDiffCallback;
import dev.sololearn.test.feed.FeedViewModel;
import dev.sololearn.test.util.Constants;

/**
 * this adapter manages pinned items
 */
public class PinnedItemsAdapter extends RecyclerView.Adapter<PinnedItemsAdapter.PinnedItemViewHolder> {

    private List<Article> items;
    private FeedViewModel feedViewModel;
    private RequestManager requestManager;

    public PinnedItemsAdapter(FeedViewModel feedViewModel, List<Article> items,
                              RequestManager requestManager) {
        this.feedViewModel = feedViewModel;
        this.items = items;
        this.requestManager = requestManager;
    }

    public void setItems(List<Article> items) {
        ArticleItemDiffCallback pinnedItemDiffCallback = new ArticleItemDiffCallback(this.items, items);
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
        PinnedItemViewHolder pinnedItemViewHolder = new PinnedItemViewHolder(binding);
        ArticleActionListener articleActionListener = article ->
                feedViewModel.onArticleClick(article, new View[]{binding.pinnedArticleThumbnail});
        binding.setListener(articleActionListener);
        return pinnedItemViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PinnedItemViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        Article article = items.get(pos);
        holder.binding.setArticle(article);
        holder.binding.pinnedArticleThumbnail.setTransitionName(Constants.PINNED_ARTICLE_IMAGE_TRANSACTION_NAME + position);
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

    public interface ArticleActionListener {
        void onArticleClicked(Article article);
    }
}
