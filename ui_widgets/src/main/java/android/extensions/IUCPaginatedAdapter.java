package android.extensions;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * UCPaginatedList uses this interface to render rows of the list
 * All methods are anologous to RecyclerView Adapter methods.
 */
public interface IUCPaginatedAdapter {
    RecyclerView.ViewHolder onCreateViewHolder(int viewType, ViewGroup parent, ArrayList<Object> data);

    void onBindViewHolder(RecyclerView.ViewHolder holder, int position, ArrayList<Object> data);

    int getItemViewType(ArrayList<Object> data, int position);

}
