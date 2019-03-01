package dev.sololearn.test.feed;

import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

public class UpdateArticlesCallback implements ListUpdateCallback {
    int firstInsert = -1;
    RecyclerView.Adapter adapter = null;
    void bind(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
    }
    public void onChanged(int position, int count, Object payload) {
        adapter.notifyItemRangeChanged(position, count, payload);
    }
    public void onInserted(int position, int count) {
        if (firstInsert == -1 || firstInsert > position) {
            firstInsert = position;
        }
        adapter.notifyItemRangeInserted(position, count);
    }
    public void onMoved(int fromPosition, int toPosition) {
        adapter.notifyItemMoved(fromPosition, toPosition);
    }
    public void onRemoved(int position, int count) {
        adapter.notifyItemRangeRemoved(position, count);
    }
}
