package com.example.luminaai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.helpers.TimeUtils;
import com.example.luminaai.models.BookmarkItem;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    private List<BookmarkItem> bookmarkList;
    private OnBookmarkListener listener;

    public interface OnBookmarkListener {
        void onRemoveBookmark(BookmarkItem item, int position);
        void onOpenChatSession(BookmarkItem item);
    }

    public BookmarkAdapter(List<BookmarkItem> bookmarkList, OnBookmarkListener listener) {
        this.bookmarkList = bookmarkList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookmarkItem item = bookmarkList.get(position);
        holder.tvTitle.setText(item.getSessionTitle());
        holder.tvContent.setText(item.getMessageText());
        holder.tvTime.setText(TimeUtils.getTimeAgo(item.getCreatedAt()));

        // Bấm vào nút hủy bookmark
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveBookmark(item, position);
            }
        });

        // Bấm vào toàn bộ thẻ để trỏ đến đoạn chat đó
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenChatSession(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookmarkList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBookmarkSessionTitle);
            tvContent = itemView.findViewById(R.id.tvBookmarkContent);
            tvTime = itemView.findViewById(R.id.tvBookmarkTime);
            btnRemove = itemView.findViewById(R.id.btnRemoveBookmark);
        }
    }
}