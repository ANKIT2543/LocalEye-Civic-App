package com.example.realproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {

    private List<String> trendingList;
    private OnTrendingItemClickListener listener;

    public interface OnTrendingItemClickListener {
        void onCategoryClick(String category);
    }

    public TrendingAdapter(List<String> trendingList, OnTrendingItemClickListener listener) {
        this.trendingList = trendingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String title = trendingList.get(position);
        holder.tvTitle.setText(title);

        // Trigger the click listener when the pill is tapped
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(title));
    }

    @Override
    public int getItemCount() {
        return trendingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Binding only the title, since we removed the fake subtitle
            tvTitle = itemView.findViewById(R.id.tvTrendingTitle);
        }
    }
}