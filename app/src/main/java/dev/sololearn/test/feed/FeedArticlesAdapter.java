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
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import dev.sololearn.test.R;
import dev.sololearn.test.datamodel.local.Article;
import dev.sololearn.test.util.Constants;

/**
 * this adapter will manage Articles in Feed
 */
public class FeedArticlesAdapter extends RecyclerView.Adapter<FeedArticlesAdapter.ArticleItemViewHolder> {

    private RequestManager requestManager;
    private MutableLiveData<ClickArticleEvent> clickArticle;
    private Context context;
    private List<Article> items;
    private int feedStyle;

    FeedArticlesAdapter(Context context, MutableLiveData<ClickArticleEvent> clickArticle,
                        RequestManager requestManager) {
        this.context = context;
        this.requestManager = requestManager;
        this.clickArticle = clickArticle;
        this.items = new ArrayList<>(0);
    }

    void setFeedStyle(int feedStyle) {
        this.feedStyle = feedStyle;
    }

    @NonNull
    @Override
    public FeedArticlesAdapter.ArticleItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ArticleItemViewHolder viewHolder;
        View view = LayoutInflater.from(context).inflate(feedStyle == FeedFragment.FEED_STYLE_LIST ?
                R.layout.feed_item : R.layout.feed_item_grid, parent, false);
        viewHolder = new ArticleItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FeedArticlesAdapter.ArticleItemViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        Article article = items.get(pos);
        ViewCompat.setTransitionName(holder.thumbnail, Constants.ARTICLE_IMAGE_TRANSACTION_NAME + position);
        if (article != null && article.articleFields != null) {
            holder.title.setText(article.title);
            holder.category.setText(article.category);
            if (holder.publicationDate != null) {
                holder.publicationDate.setText(article.publicationDate);
            }
            holder.itemView.setOnClickListener(v -> clickArticle.setValue(new ClickArticleEvent(article, new View[]{holder.thumbnail})));
            if (article.savedForOffline && article.articleFields.articleThumbnailPath != null) {
                File cacheFile = new File(context.getFilesDir(), article.articleFields.articleThumbnailPath);
                requestManager.load(Uri.fromFile(cacheFile)).diskCacheStrategy(DiskCacheStrategy.NONE).apply(RequestOptions.placeholderOf(R.drawable.image_place_holder))
                        .skipMemoryCache(true).into(holder.thumbnail);
            } else {
                requestManager.load(article.articleFields.articleThumbnail).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true).apply(RequestOptions.placeholderOf(R.drawable.image_place_holder))
                        .into(holder.thumbnail);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void setItems(List<Article> items) {
        ArticleItemDiffCallback pinnedItemDiffCallback = new ArticleItemDiffCallback(this.items, items);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(pinnedItemDiffCallback);

        this.items.clear();
        this.items.addAll(items);
        diffResult.dispatchUpdatesTo(this);
    }

    class ArticleItemViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView category;
        @Nullable
        TextView publicationDate;

        ArticleItemViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.article_thumbnail);
            title = view.findViewById(R.id.article_title);
            category = view.findViewById(R.id.article_category);
            publicationDate = view.findViewById(R.id.article_publication_date);
        }
    }

    public interface ArticleActionListener {
        void onArticleClicked(Article article);
    }
}
