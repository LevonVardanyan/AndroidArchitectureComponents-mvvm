package dev.sololearn.test.feed;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import dev.sololearn.test.R;
import dev.sololearn.test.databinding.FeedItemBinding;
import dev.sololearn.test.databinding.FeedItemGridBinding;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.util.Constants;

/**
 * this adapter will manage Articles in Feed
 */
public class FeedArticlesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private FeedViewModel feedViewModel;
    private LifecycleOwner lifecycleOwner;

    private RequestManager requestManager;
    private List<Article> items;
    private FeedFragment.ViewStyle viewStyle = FeedFragment.ViewStyle.LIST;

    public FeedArticlesAdapter(FeedViewModel feedViewModel, LifecycleOwner activity,
                               RequestManager requestManager, List<Article> items) {
        this.feedViewModel = feedViewModel;
        this.lifecycleOwner = activity;
        this.requestManager = requestManager;
        this.items = items;
    }

    public void setViewStyle(FeedFragment.ViewStyle viewStyle) {
        this.viewStyle = viewStyle;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        if (viewStyle == FeedFragment.ViewStyle.LIST) {
            FeedItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(
                    parent.getContext()), R.layout.feed_item, parent, false);
            ArticleActionListener articleActionListener = article -> {
                feedViewModel.onArticleClick(article, new View[]{binding.articleThumbnail});
            };
            ArticleViewHolder articleViewHolder = new ArticleViewHolder(binding);
            articleViewHolder.binding.setLifecycleOwner(lifecycleOwner);
            articleViewHolder.binding.setListener(articleActionListener);
            holder = articleViewHolder;
        } else {
            FeedItemGridBinding binding = DataBindingUtil.inflate(LayoutInflater.from(
                    parent.getContext()), R.layout.feed_item_grid, parent, false);
            ArticleActionListener articleActionListener = article -> {
                feedViewModel.onArticleClick(article, new View[]{binding.articleThumbnail});
            };
            ArticleGridViewHolder articleGridViewHolder = new ArticleGridViewHolder(binding);
            articleGridViewHolder.binding.setLifecycleOwner(lifecycleOwner);
            articleGridViewHolder.binding.setListener(articleActionListener);
            holder = articleGridViewHolder;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        Article article = items.get(pos);
        ImageView thumbnail;
        TextView title;
        TextView category;
        Context context;
        if (viewStyle == FeedFragment.ViewStyle.STAGGERED) {
            ArticleGridViewHolder articleGridViewHolder = (ArticleGridViewHolder) holder;
            articleGridViewHolder.binding.setArticle(article);
            thumbnail = articleGridViewHolder.binding.articleThumbnail;
            context = articleGridViewHolder.binding.getRoot().getContext();

        } else {
            ArticleViewHolder articleViewHolder = (ArticleViewHolder) holder;
            articleViewHolder.binding.setArticle(article);
            thumbnail = articleViewHolder.binding.articleThumbnail;
            context = articleViewHolder.binding.getRoot().getContext();
        }
        ViewCompat.setTransitionName(thumbnail,
                Constants.ARTICLE_IMAGE_TRANSACTION_NAME + position);
        if (article != null && article.articleFields != null) {
            if (article.savedForOffline && article.articleFields.articleThumbnailPath != null) {
                File cacheFile = new File(context.getFilesDir(), article.articleFields.articleThumbnailPath);
                requestManager.load(Uri.fromFile(cacheFile)).diskCacheStrategy(DiskCacheStrategy.NONE).apply(RequestOptions.placeholderOf(R.drawable.image_place_holder))
                        .skipMemoryCache(true).into(thumbnail);
            } else {
                requestManager.load(article.articleFields.articleThumbnail)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true).apply(RequestOptions.placeholderOf(R.drawable.image_place_holder))
                        .into(thumbnail);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Article> items) {
        ArticleItemDiffCallback pinnedItemDiffCallback = new ArticleItemDiffCallback(this.items, items);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(pinnedItemDiffCallback);

        this.items.clear();
        this.items.addAll(items);
        diffResult.dispatchUpdatesTo(this);
    }

    public void addItemsFromEnd(List<Article> items) {
        this.items.addAll(items);
    }

    public void addItemsFromStart(List<Article> items) {
        this.items.addAll(0, items);
    }

    public class ArticleViewHolder extends RecyclerView.ViewHolder {
        FeedItemBinding binding;

        public ArticleViewHolder(FeedItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public class ArticleGridViewHolder extends RecyclerView.ViewHolder {
        FeedItemGridBinding binding;

        public ArticleGridViewHolder(@NonNull FeedItemGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface ArticleActionListener {
        void onArticleClicked(Article article);
    }
}
