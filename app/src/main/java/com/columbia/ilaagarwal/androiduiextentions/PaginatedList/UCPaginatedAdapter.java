package com.columbia.ilaagarwal.androiduiextentions.PaginatedList;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by ilaagarwal on 25/03/17.
 * Implement delegate IUCPaginatedAdapter to render rows
 */

class UCPaginatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private IUCPaginatedAdapter mAdapterDelegate;
    private ArrayList<Object> mData;

    public UCPaginatedAdapter(ArrayList<Object> data, IUCPaginatedAdapter adapterDelegate) {
        mData = data;
        mAdapterDelegate = adapterDelegate;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mAdapterDelegate != null) {
            return mAdapterDelegate.onCreateViewHolder(viewType, parent, mData);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mAdapterDelegate != null) {
            mAdapterDelegate.onBindViewHolder(holder, position, mData);
        }
    }

    @Override
    public int getItemCount() {
        if (mData == null) {
            return 0;
        } else {
            return mData.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mAdapterDelegate != null) {
            return mAdapterDelegate.getItemViewType(mData, position);
        }
        return -1;
    }
}