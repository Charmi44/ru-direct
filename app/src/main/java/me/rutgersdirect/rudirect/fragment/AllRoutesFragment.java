package me.rutgersdirect.rudirect.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Map;

import me.rutgersdirect.rudirect.R;
import me.rutgersdirect.rudirect.adapter.BusRouteAdapter;
import me.rutgersdirect.rudirect.api.NextBusAPI;
import me.rutgersdirect.rudirect.ui.view.DividerItemDecoration;

public class AllRoutesFragment extends BaseRouteFragment {

    private RecyclerView allBusesRecyclerView;

    // Sets up the bus routes
    private class UpdateAllRoutesTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... voids) {
            NextBusAPI.saveBusStops(mainActivity);
            return null;
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        }

        @Override
        protected void onPostExecute(Void v) {
            if (!isNetworkAvailable()) {
                errorView.setVisibility(View.VISIBLE);
                errorView.setText("Unable to get routes - check your Internet connection and try again.");
            } else {
                errorView.setVisibility(View.GONE);
                allBusesRecyclerView.setAdapter(
                        new BusRouteAdapter(getBusRoutes(), mainActivity, AllRoutesFragment.this));
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    // Sets up the RecyclerView
    public void updateAllRoutes() {
        new UpdateAllRoutesTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_all_routes, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSwipeRefreshLayout();
        errorView = (TextView) mainActivity.findViewById(R.id.all_buses_error);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllRoutes();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            mSwipeRefreshLayout.setRefreshing(true);
            updateAllRoutes();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Set up RecyclerView
    private void setupRecyclerView() {
        // Initialize recycler view
        allBusesRecyclerView = (RecyclerView) mainActivity.findViewById(R.id.all_buses_recyclerview);
        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(mainActivity);
        allBusesRecyclerView.setLayoutManager(layoutManager);
        // Setup layout
        allBusesRecyclerView.addItemDecoration(new DividerItemDecoration(mainActivity, LinearLayoutManager.VERTICAL));
        // Set adapter
        allBusesRecyclerView.setAdapter(new BusRouteAdapter(getBusRoutes(), mainActivity, this));
    }

    // Set up SwipeRefreshLayout
    private void setupSwipeRefreshLayout() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) mainActivity.findViewById(R.id.all_buses_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateAllRoutes();
            }
        });
        mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
    }

    // Returns an array of bus route names
    public String[] getBusRoutes() {
        Map<String, ?> busesToTagsMap = mainActivity.getSharedPreferences(
                getString(R.string.buses_to_tags_key), Context.MODE_PRIVATE).getAll();
        Object[] busNamesObj = busesToTagsMap.keySet().toArray();
        String[] busNames = Arrays.copyOf(busNamesObj, busNamesObj.length, String[].class);
        Arrays.sort(busNames);
        return busNames;
    }
}