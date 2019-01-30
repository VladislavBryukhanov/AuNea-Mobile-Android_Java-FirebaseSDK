package com.example.nameless.autoupdating.common;

import android.util.Log;
import android.widget.AbsListView;

public abstract class InfiniteScroll implements AbsListView.OnScrollListener {

    private int currentPage = 0;
    private static boolean fetching = false;

    private int firstVisibleItem;
    private int visibleItemCount;
    private int totalItemCount;

    private int countOfOneDownload;

    public InfiniteScroll(int countOfOneDownload) {
        this.countOfOneDownload = countOfOneDownload;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
//        if (!fetching && firstVisibleItem + visibleItemCount + countOfOneDownload / 2 >= totalItemCount ) {
        if (!fetching && firstVisibleItem <= countOfOneDownload ) {
            Log.d("++++++++++++++++", ""+firstVisibleItem);
            Log.d("///////////////////", ""+visibleItemCount);
            Log.d("--------------", ""+totalItemCount);
            fetching = true;
            nextPage(++currentPage, firstVisibleItem);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
        this.visibleItemCount = visibleItemCount;
        this.totalItemCount = totalItemCount;
    }

    public abstract void nextPage(int page, int itemsCount);

    public static void setFetching(boolean state) {
        fetching = state;
    }
}
