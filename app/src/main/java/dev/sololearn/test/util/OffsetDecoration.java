package dev.sololearn.test.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OffsetDecoration extends RecyclerView.ItemDecoration {

	private int marginLeft;
	private int marginRight;
	private int marginTop;
	private int marginBottom;

	private boolean bigMarginForLastItem;

	public OffsetDecoration() {
	}

	public OffsetDecoration(int marginLeft, int marginRight, int marginTop, int marginBottom, boolean bigMarginForLastItem) {
		this.marginLeft = marginLeft;
		this.marginRight = marginRight;
		this.marginTop = marginTop;
		this.marginBottom = marginBottom;
		this.bigMarginForLastItem = bigMarginForLastItem;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
							   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);
		if (bigMarginForLastItem && parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
			outRect.set(marginLeft, marginTop, marginRight, 5 * marginBottom);
		} else {
			outRect.set(marginLeft, marginTop, marginRight, marginBottom);
		}
	}
}
