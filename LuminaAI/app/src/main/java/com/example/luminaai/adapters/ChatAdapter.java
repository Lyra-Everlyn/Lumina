package com.example.luminaai.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.adapters.holders.ChatViewHolder;
import com.example.luminaai.models.ChatMessage;
import com.example.luminaai.R;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {
    private List<ChatMessage> messages;
    private OnBookmarkClickListener listener;
    private Markwon markwon;

    public interface OnBookmarkClickListener {
        void onBookmarkClick(ChatMessage message, int position);
    }

    public ChatAdapter(List<ChatMessage> messages, OnBookmarkClickListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (markwon == null) {
            markwon = Markwon.builder(parent.getContext())
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(JLatexMathPlugin.create(30f, new JLatexMathPlugin.BuilderConfigure() {
                        @Override
                        public void configureBuilder(JLatexMathPlugin.Builder builder) {
                            builder.inlinesEnabled(true);
                        }
                    }))
                    .build();
        }

        LinearLayout rowContainer = new LinearLayout(parent.getContext());
        rowContainer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowContainer.setOrientation(LinearLayout.VERTICAL);
        rowContainer.setPadding(24, 12, 24, 12);

        TextView textView = new TextView(parent.getContext());
        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setMaxWidth(800);
        textView.setLayoutParams(textParams);
        textView.setPadding(32, 24, 32, 24);
        textView.setTextSize(14f);

        ImageView bookmarkIcon = new ImageView(parent.getContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
        iconParams.topMargin = 8;
        bookmarkIcon.setLayoutParams(iconParams);
        bookmarkIcon.setPadding(16, 16, 16, 16);

        android.util.TypedValue outValue = new android.util.TypedValue();
        parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        bookmarkIcon.setBackgroundResource(outValue.resourceId);

        rowContainer.addView(textView);
        rowContainer.addView(bookmarkIcon);

        return new ChatViewHolder(rowContainer, textView, bookmarkIcon);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        String rawText = msg.getMessage();
        if (rawText == null) rawText = "";
        String processedText = rawText;

        try {
            processedText = rawText.replaceAll("(?<!\\$)\\$([^\\$]+?)\\$(?!\\$)", "\\$\\$$1\\$\\$");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (markwon != null) {
            try {
                markwon.setMarkdown(holder.textView, processedText);
            } catch (Exception e) {
                holder.textView.setText(processedText + "\n\n[Lỗi Markwon: " + e.getMessage() + "]");
            }
        } else {
            holder.textView.setText(processedText);
        }

        GradientDrawable bubbleBg = new GradientDrawable();
        bubbleBg.setShape(GradientDrawable.RECTANGLE);
        bubbleBg.setCornerRadius(40f);

        LinearLayout rowContainer = (LinearLayout) holder.itemView;

        if (msg.isUser()) {
            rowContainer.setGravity(Gravity.END);
            bubbleBg.setColor(Color.parseColor("#99F6E4"));
            holder.textView.setTextColor(Color.parseColor("#134E4A"));
            holder.bookmarkIcon.setVisibility(View.GONE);
        } else {
            rowContainer.setGravity(Gravity.START);
            bubbleBg.setColor(Color.parseColor("#FFFFFF"));
            holder.textView.setTextColor(Color.parseColor("#1F2937"));

            holder.bookmarkIcon.setVisibility(View.VISIBLE);

            if (msg.isBookmarked()) {
                holder.bookmarkIcon.setImageResource(R.drawable.ic_bookmark);
            } else {
                holder.bookmarkIcon.setImageResource(R.drawable.ic_unbookmark);
            }

            holder.bookmarkIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookmarkClick(msg, position);
                }
            });
        }
        holder.textView.setBackground(bubbleBg);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}