package dev.sololearn.test.datamodel.local;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/**
 * this class is part of articles response
 * different class because server response structure made this way
 */
public class ArticleFields implements Parcelable {

	@Nullable
	@SerializedName("thumbnail")
	@ColumnInfo(name = "thumbnail_url")
	public String articleThumbnail;

	@Nullable
	@ColumnInfo(name = "thumbnail_path")
	public String articleThumbnailPath;

	private ArticleFields(Parcel in) {
		articleThumbnail = in.readString();
		articleThumbnailPath = in.readString();
	}

	ArticleFields() {
	}

	public static final Creator<ArticleFields> CREATOR = new Creator<ArticleFields>() {
		@Override
		public ArticleFields createFromParcel(Parcel in) {
			return new ArticleFields(in);
		}

		@Override
		public ArticleFields[] newArray(int size) {
			return new ArticleFields[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeString(articleThumbnail);
		dest.writeString(articleThumbnailPath);
	}
}
