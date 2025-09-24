package com.example.dailywords;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<WordItem> historyList;

    public HistoryAdapter(List<WordItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        WordItem item = historyList.get(position);
        holder.tvHistoryWord.setText(item.getWord());
        holder.tvHistoryMeaning.setText(item.getMeaning());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryWord, tvHistoryMeaning;

        HistoryViewHolder(View itemView) {
            super(itemView);
            tvHistoryWord = itemView.findViewById(R.id.tvHistoryWord);
            tvHistoryMeaning = itemView.findViewById(R.id.tvHistoryMeaning);
        }
    }
}
