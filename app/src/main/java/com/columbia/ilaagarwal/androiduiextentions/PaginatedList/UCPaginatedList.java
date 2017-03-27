package com.columbia.ilaagarwal.androiduiextentions.PaginatedList;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.columbia.ilaagarwal.androiduiextentions.R;

import java.util.ArrayList;

/**
 * Extends RecyclerView list to support -
 * 1. Pull to refresh for first page of the list
 * 2. Pagination and delegate methods to fetch next page - IUCPaginatedDatasource
 * 3. fetches next page once mItemsOffsetBeforeNextPage appear from bottom of the screen
 * 4. Empty view in case of no data
 */

public class  UCPaginatedList extends RelativeLayout {

    private static final int PAGINATION_COUNT = 10;

    //UI
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SwipeRefreshLayout mEmptyView;
    private String mEmptyStateText;
    private ProgressBar mProgressBar;
    private TextView mEmptyViewTextView;
    private LinearLayoutManager mLinearLayoutManager;

    //Pagination
    private int mItemsOffsetBeforeNextPage = 1;
    private int mPageNumber = 0;
    private boolean mNoMoreData;

    // Adapter
    private UCPaginatedAdapter mAdapter;
    private IUCPaginatedAdapter mAdapterDelegate;
    private IUCPaginatedDatasource mDatasourceDelegate;

    //API
    private boolean mDataFetchInProgress;
    private ArrayList<Object> mData;

    // Context
    private Context mContext;

    public UCPaginatedList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        inflateUI();
    }

    public UCPaginatedList addAdapterDelegate(IUCPaginatedAdapter adapterDelegate) {
        mAdapterDelegate = adapterDelegate;
        return this;
    }

    public UCPaginatedList addDataSourceDelegate(IUCPaginatedDatasource dataSourceDelegate) {
        mDatasourceDelegate = dataSourceDelegate;
        return this;
    }

    public UCPaginatedList addEmptyStateText(String emptyStateText) {
        mEmptyStateText = emptyStateText;
        return this;
    }

    public UCPaginatedList addItemsOffsetBeforeNextPage(int itemsOffsetBeforeNextPage) {
        mItemsOffsetBeforeNextPage = itemsOffsetBeforeNextPage;
        return this;
    }

    public void initialize() {
        initEmptyView();
        initRecycler();
        initSwipeRefresh();
        mProgressBar.setVisibility(View.VISIBLE);
        fetchData(0);
    }

    public void recievedDataSuccess(ArrayList<Object> data, int page) {
        mEmptyView.setVisibility(View.GONE);
        if (mData == null) {
            mData = new ArrayList<>();
        }
        mDataFetchInProgress = false;
        if (data == null || data.size() < PAGINATION_COUNT) {
            if (page == 0 && (data == null || data.size() == 0)) {
                mEmptyView.setVisibility(View.VISIBLE);
            }
            mNoMoreData = true;
        }

        if (page == 0) {
            mData.clear();
        }

        mData.addAll(mDatasourceDelegate.parseDataArray(data));

        mPageNumber = mPageNumber + 1;

        if (mAdapter == null) {
            mAdapter = new UCPaginatedAdapter(mData, mAdapterDelegate);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }

        mSwipeRefreshLayout.setVisibility(View.VISIBLE); // why this ?
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyView.setRefreshing(false);
    }

    public void receivedDataError() {
        mDataFetchInProgress = false;
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyView.setRefreshing(false);
    }

    private void inflateUI() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.paginated_recycler, this, true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mEmptyView = (SwipeRefreshLayout) view.findViewById(R.id.empty_view);
        mEmptyViewTextView = (TextView) mEmptyView.findViewById(R.id.no_data_message);
    }

    public void initSwipeRefresh() {
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchData(0);
            }
        });
        mSwipeRefreshLayout.setVisibility(View.GONE);
    }

    public void initRecycler() {
        mLinearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,
                false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mLinearLayoutManager.findLastCompletelyVisibleItemPosition()
                        + mItemsOffsetBeforeNextPage > recyclerView.getAdapter().getItemCount()) {
                    fetchData(mPageNumber);
                }
            }
        });
    }

    public void initEmptyView() {
        mEmptyViewTextView.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        mEmptyViewTextView.setText(mEmptyStateText);
        mEmptyView.setVisibility(View.GONE);
        mEmptyView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchData(0);
            }
        });
    }

    private void fetchData(int page) {
        if (mDataFetchInProgress || mNoMoreData) {
            return;
        }

        mDataFetchInProgress = true;

        mDatasourceDelegate.fetchNextPage(page);
        if (!mSwipeRefreshLayout.isRefreshing() && !mEmptyView.isRefreshing()) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }
}
