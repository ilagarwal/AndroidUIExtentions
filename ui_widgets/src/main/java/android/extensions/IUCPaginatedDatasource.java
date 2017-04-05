package android.extensions;

import java.util.ArrayList;


/**
 * UCPaginatedList uses this interface to provide data to recycler view for next page.
 * It does this in two steps raw data to build the page, and parsedData to render each row
 */

public interface IUCPaginatedDatasource {

    /**
     * Delegate to start getting data based on page number.
     * Raw data is passed back in recievedDataSuccess or recievedDataError to UCPaginatedList.
     */
    void fetchNextPage(int page); // Todo : Move recievedDataSuccess recievedDataError to callbacks


    /**
     * Delegate to processed data, to render each row.
     * Returns process data to UCPaginatedList
     */
    ArrayList<Object> parseDataArray(ArrayList<Object> data);
}
