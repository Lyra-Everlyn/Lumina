package com.example.luminaai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.models.ChatSession;
import com.example.luminaai.helpers.TimeUtils;

import java.util.List;

public class RecentChatAdapter extends RecyclerView.Adapter<RecentChatAdapter.ViewHolder> {
    private List<ChatSession> sessionList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChatSession session);
    }

    public RecentChatAdapter(List<ChatSession> sessionList, OnItemClickListener listener) {
        this.sessionList = sessionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = sessionList.get(position);
        holder.tvTitle.setText(session.getSessionTitle());
        holder.tvSubject.setText(session.getSubjectName());

        holder.tvTime.setText(TimeUtils.getTimeAgo(session.getCreatedAt()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(session);
        });
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvSubject = itemView.findViewById(R.id.tvSessionSubject);
            tvTime = itemView.findViewById(R.id.tvSessionTime);
        }
    }
}