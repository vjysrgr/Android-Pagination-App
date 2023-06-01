package com.skycore.task.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.skycore.task.R;
import com.skycore.task.models.Businesses;
import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RestaurantAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Businesses> listBusiness;
    private final Activity mActivity;

    // for load more
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private OnLoadMoreListener onLoadMoreListener;

    // before loading more.
    private boolean isLoading;
    private final int visibleThreshold = 1;
    private int lastVisibleItem, totalItemCount;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }


    //constructor
    public RestaurantAdapter(Context context, List<Businesses> myDataset, RecyclerView recyclerView) {

        mActivity = (Activity) context;
        listBusiness = myDataset;

        // load more
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });
    }

    // Create view according to item
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_restaurants, parent, false);
            return new ViewHolderRow(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_progress_bar, parent, false);
            return new ViewHolderLoading(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ViewHolderRow) {
            Businesses map = listBusiness.get(position);

            ViewHolderRow userViewHolder = (ViewHolderRow) holder;

            userViewHolder.tv_name.setText(map.getName());
            userViewHolder.tv_address.setText(map.getLocation().getAddress1());
            userViewHolder.tv_open_close.setText(map.getIs_closed() ? "Closed" : "Open");
            userViewHolder.tv_rating.setText(map.getRating());
            if (!map.getImage_url().isEmpty()) {
                Picasso.with(mActivity)
                        .load(map.getImage_url())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        //.memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        //.networkPolicy(NetworkPolicy.NO_CACHE)
                        .into(userViewHolder.img_product);
            }

        } else if (holder instanceof ViewHolderLoading) {
            ViewHolderLoading loadingViewHolder = (ViewHolderLoading) holder;
            loadingViewHolder.progress_bar.setIndeterminate(true);
        }

    }

    @Override
    public int getItemCount() {
        return listBusiness == null ? 0 : listBusiness.size();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
        this.onLoadMoreListener = mOnLoadMoreListener;
    }

    @Override
    public int getItemViewType(int position) {
        return listBusiness.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    public void setLoaded() {
        isLoading = false;
    }

    private static class ViewHolderLoading extends RecyclerView.ViewHolder {
        public ProgressBar progress_bar;

        public ViewHolderLoading(View view) {
            super(view);
            progress_bar = view.findViewById(R.id.progress_bar);
        }
    }

    public static class ViewHolderRow extends RecyclerView.ViewHolder {
        public TextView tv_name, tv_address, tv_open_close, tv_rating;
        public ImageView img_product;


        public ViewHolderRow(View v) {
            super(v);
            tv_name = v.findViewById(R.id.tv_name);
            tv_address = v.findViewById(R.id.tv_address);
            tv_open_close = v.findViewById(R.id.tv_open_close);
            tv_rating = v.findViewById(R.id.tv_rating);
            img_product = v.findViewById(R.id.img_product);
        }

    }

}