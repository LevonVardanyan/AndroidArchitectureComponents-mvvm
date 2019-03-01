package dev.sololearn.test.datamodel.local;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Article is our model, it can be created from net as well as from db
 * DB also saves rows of articles
 * From remote call we can get list of Articles
 */

@Entity(tableName = "articles_table")
public class Article implements Parcelable {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String id = "";

    @Nullable
    @SerializedName("webTitle")
    public String title;

    @Nullable
    @SerializedName("webUrl")
    public String webUrl;

    @Nullable
    @SerializedName("sectionName")
    public String category;

    @Nullable
    @TypeConverters(Converters.class)
    @SerializedName("webPublicationDate")
    public String publicationDate;

    public boolean pinned;
    @ColumnInfo(name = "is_offline")
    public boolean savedForOffline;

    @ColumnInfo(name = "last_updated")
    public long lastUpdateTime;

    @Nullable
    @SerializedName("fields")
    @Embedded
    public ArticleFields articleFields;

    public Article() {
    }

    public void set(@NonNull Article nextNewItem) {
        this.id = nextNewItem.id;
        this.title = nextNewItem.title;
        this.webUrl = nextNewItem.webUrl;
        this.category = nextNewItem.category;
        this.publicationDate = nextNewItem.publicationDate;
        if (articleFields != null && nextNewItem.articleFields != null) {
            articleFields.articleThumbnail = nextNewItem.articleFields.articleThumbnail;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof Article) {
            return id.equals(((Article) obj).id);
        }
        return false;
    }

    @Nullable
    public String constructArticleThumbnailPath() {
        if (articleFields != null && (articleFields.articleThumbnailPath == null ||
                TextUtils.isEmpty(articleFields.articleThumbnailPath))) {
            articleFields.articleThumbnailPath = id.substring(id.lastIndexOf("/") + 1) + "_thumbnail";
        }
        return articleFields == null ? null : articleFields.articleThumbnailPath;
    }

    @NonNull
    public static DiffUtil.ItemCallback<Article> DIFF_CALLBACK = new DiffUtil.ItemCallback<Article>() {

        @Override
        public boolean areItemsTheSame(@NonNull Article oldItem, @NonNull Article newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Article oldItem, @NonNull Article newItem) {
            return oldItem.equals(newItem);
        }
    };


    protected Article(@NonNull Parcel in) {
        id = in.readString();
        title = in.readString();
        webUrl = in.readString();
        category = in.readString();
        pinned = in.readByte() != 0;
        savedForOffline = in.readByte() != 0;
        articleFields = in.readParcelable(ArticleFields.class.getClassLoader());
    }

    public static final Creator<Article> CREATOR = new Creator<Article>() {
        @Override
        public Article createFromParcel(Parcel in) {
            return new Article(in);
        }

        @Override
        public Article[] newArray(int size) {
            return new Article[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(webUrl);
        dest.writeString(category);
        dest.writeByte((byte) (pinned ? 1 : 0));
        dest.writeByte((byte) (savedForOffline ? 1 : 0));
        dest.writeParcelable(articleFields, flags);
    }
}
