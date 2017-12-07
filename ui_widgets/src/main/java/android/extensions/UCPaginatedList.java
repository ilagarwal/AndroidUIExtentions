package android.extensions;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.urbanclap.android.extension.R;

import java.util.ArrayList;

/**
 * Extends RecyclerView list to support -
 * 1. Pull to refresh for first page of the list
 * 2. Pagination and delegate methods to fetch next page - IUCPaginatedDatasource
 * 3. fetches next page once mItemsOffsetBeforeNextPage appear from bottom of the screen
 * 4. Empty view in case of no data
 */

public class UCPaginatedList extends RelativeLayout {

    public static final int NO_NEXT_PAGE = -1;

    private static final int PAGINATION_COUNT = 10;
    private static final int TOP_PADDING = 8;

    //UI
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SwipeRefreshLayout mEmptyView;
    private String mEmptyStateText;
    private ProgressBar mProgressBar;
    private TextView mEmptyViewTextView;
    private LinearLayoutManager mLinearLayoutManager;
    private int mEmptyTextColor = Color.BLACK;
    private boolean showTopPadding;

    //Pagination
    private int mItemsOffsetBeforeNextPage = 1;
    private int mPageNumber = 0;

    // Adapter
    private UCPaginatedAdapter mAdapter;

    // Delegates
    private IUCPaginatedAdapter mAdapterDelegate;
    private IUCPaginatedDatasource mDatasourceDelegate;
    private IUCDatasourceComparison mComparisonDelegate;

    //API
    private boolean mDataFetchInProgress;
    private ArrayList<Object> mData;

    private boolean initialized = false;

    public UCPaginatedList(Context context) {
        super(context);
        init(null);
    }

    public UCPaginatedList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public UCPaginatedList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        inflateUI();
        parseAttributeSet(attrs);
        populateUI();
    }

    private void parseAttributeSet(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.UCPaginatedList);
        showTopPadding = ta.getBoolean(R.styleable.UCPaginatedList_showTopPadding, false);
        ta.recycle();
    }

    private void populateUI() {
        if (showTopPadding) {
            mRecyclerView.setPadding(0, Utils.dpToPx(getContext(), TOP_PADDING), 0, 0);
            mRecyclerView.setClipToPadding(false);
            mRecyclerView.setClipChildren(false);
        }
    }

    public UCPaginatedList addAdapterDelegate(IUCPaginatedAdapter adapterDelegate) {
        mAdapterDelegate = adapterDelegate;
        return this;
    }

    public UCPaginatedList addDataSourceDelegate(IUCPaginatedDatasource dataSourceDelegate) {
        mDatasourceDelegate = dataSourceDelegate;
        if (dataSourceDelegate instanceof IUCDatasourceComparison) {
            mComparisonDelegate = (IUCDatasourceComparison) dataSourceDelegate;
        }
        return this;
    }

    public UCPaginatedList addEmptyStateText(String emptyStateText) {
        mEmptyStateText = emptyStateText;
        return this;
    }

    public UCPaginatedList addEmptyStateTextColor(int emptyStateTextColor) {
        mEmptyTextColor = emptyStateTextColor;
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
        initialized = true;
    }

    public void startDataPopulation() {
        if (initialized) {
            fetchData(0);
        }
    }

    public void recievedDataSuccess(ArrayList<Object> data, int currentPage, int nextPage) {
        mEmptyView.setVisibility(View.GONE);

        if (mData == null) {
            mData = new ArrayList<>();
        }

        if (currentPage == 0) {
            mData.clear();
        }

        if (currentPage == 0 && (data == null || data.size() == 0)) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            if (mDatasourceDelegate != null) {
                mData.addAll(mDatasourceDelegate.parseDataArray(data));
            }

            if (nextPage != NO_NEXT_PAGE) {
                mPageNumber = nextPage;
            } else {
                if (data != null && data.size() > 0) {
                    mPageNumber = currentPage + 1;
                }
            }
        }

        refreshAdapater();

        refreshLoaderState();

        mDataFetchInProgress = false;
    }

    private void refreshAdapater() {
        if (mAdapter == null) {
            mAdapter = new UCPaginatedAdapter(mData, mAdapterDelegate);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void receivedDataError() {
        mDataFetchInProgress = false;
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyView.setRefreshing(false);
    }

    public boolean removeItem(Object genericItem) {
        if (mData != null && mData.contains(genericItem)) {
            mData.remove(genericItem);
            mAdapter.notifyDataSetChanged();
            resetEmptyViewPageNumberState();
            return true;
        }
        return false;
    }

    public
    @Nullable
    RecyclerView getRecyclerViewOnlySpecialNeeds() {
        return mRecyclerView;
    }

    public void refreshList(boolean fromPageZero) {
        if (fromPageZero) {
            fetchData(0);
        } else {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public int getItemPosition(@NonNull Object genericObject) {
        if (mData != null) {
            return mData.indexOf(genericObject);
        }
        return RecyclerView.NO_POSITION;
    }

    public boolean removeItemByReferenceId(String referenceId) {
        Object found = findItemByReferenceId(referenceId);
        if (found != null) {
            removeItem(found);
            return true;
        }
        return false;
    }

    public Object findItemByReferenceId(String referenceId) {
        if (mData == null || mData.size() == 0)
            return null;

        for (Object o : mData)
            if (mComparisonDelegate != null) {
                if (mComparisonDelegate.hasReferenceId(o, referenceId)) {
                    return o;
                }
            }

        return null;
    }

    public boolean updateItemByReferenceId(String referenceId, Object newItem) {
        Object found = findItemByReferenceId(referenceId);
        if (found != null) {
            updateItem(found, newItem);
            return true;
        }
        return false;
    }

    public boolean updateItem(Object oldItem, Object newItem) {
        if (mData != null) {
            int position = mData.indexOf(oldItem);
            if (position == -1) {
                return false;
            }
            mData.set(position, newItem);
            mAdapter.notifyItemChanged(position);
            resetEmptyViewPageNumberState();
            return true;
        }
        return false;
    }

    private void inflateUI() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = (LayoutInflater)
                getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(
                R.layout.paginated_recycler, this, true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mEmptyView = (SwipeRefreshLayout) view.findViewById(R.id.empty_view);
        mEmptyViewTextView = (TextView) mEmptyView.findViewById(R.id.no_data_message);
    }

    private void initSwipeRefresh() {
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchData(0);
            }
        });
        mSwipeRefreshLayout.setVisibility(View.GONE);
    }

    private void initRecycler() {
        mLinearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,
                false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mDataFetchInProgress) {
                    return;
                }
                if (dy > 0) {
                    int visibleItemCount = mLinearLayoutManager.getChildCount();
                    int totalItemCount = mLinearLayoutManager.getItemCount();
                    int pastVisibleItems = mLinearLayoutManager.findFirstVisibleItemPosition();
                    if ((visibleItemCount + pastVisibleItems) + mItemsOffsetBeforeNextPage
                            >= totalItemCount) {

                        fetchData(mPageNumber);
                    }
                }
            }
        });
    }

    private void initEmptyView() {
        mEmptyViewTextView.setTextColor(mEmptyTextColor);
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
        if (mDataFetchInProgress) {
            return;
        }

        mDataFetchInProgress = true;

        if (mDatasourceDelegate != null) {
            mDatasourceDelegate.fetchNextPage(page);
            if (!mSwipeRefreshLayout.isRefreshing() && !mEmptyView.isRefreshing()) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void resetEmptyViewPageNumberState() {
        if (mData != null && mData.size() == 0) {
            mPageNumber = 0;
            mEmptyView.setVisibility(View.VISIBLE);
        }

        if (mData != null && mData.size() != 0) {
            mEmptyView.setVisibility(View.GONE);

        }
    }

    public void overrideDataSource(ArrayList<Object> data, int skipToPage) {
        if (data == null || data.size() == 0) {
            return;
        }
        if (mData != null) {
            mData.clear();
            mData.addAll(data);
        } else {
            mData = new ArrayList<>();
            mData.addAll(data);
        }
        refreshAdapater();

        if (skipToPage > 0) {
            mPageNumber = skipToPage;
        }
        refreshLoaderState();
    }

    private void refreshLoaderState() {
        mSwipeRefreshLayout.setVisibility(View.VISIBLE); // why this ?
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyView.setRefreshing(false);
    }

}
