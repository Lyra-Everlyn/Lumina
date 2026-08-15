package com.example.luminaai.adapters.holders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChatViewHolder extends RecyclerView.ViewHolder {
    public TextView textView;
    public ImageView bookmarkIcon;

    public ChatViewHolder(@NonNull View itemView, TextView textView, ImageView bookmarkIcon) {
        super(itemView);
        this.textView = textView;
        this.bookmarkIcon = bookmarkIcon;
    }
}