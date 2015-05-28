package me.rutgersdirect.rudirect.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import me.rutgersdirect.rudirect.BusConstants;
import me.rutgersdirect.rudirect.R;
import me.rutgersdirect.rudirect.model.BusStop;
import me.rutgersdirect.rudirect.ui.adapter.BusStopAdapter;
import me.rutgersdirect.rudirect.ui.helper.ShowBusStopsHelper;

public class BusStopsActivity extends AppCompatActivity {
    public static boolean active; // Whether or not the activity is active
    public static int expBusStopIndex; // Index of the bus stop to be expanded
    public static int lastExpBusStopIndex; // Index of the last bus stop expanded
    public static boolean isExpBusStopIndexExpanded; // Is the bus stop expanded already?
    private String busTag; // Bus tag that the bus stops are being shown for
    private Handler refreshHandler; // Handles auto refresh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_stops);

        // Gets the bus tag, stop titles, and stop times
        Intent intent = getIntent();
        busTag = intent.getStringExtra(BusConstants.BUS_TAG_MESSAGE);
        String[] busStopTitles = intent.getStringArrayExtra(BusConstants.BUS_STOP_TITLES_MESSAGE);
        int[][] busStopTimes = (int[][]) intent.getExtras().getSerializable(BusConstants.BUS_STOP_TIMES_MESSAGE);

        // Sets the title to the name of the bus
        SharedPreferences tagsToBusesPref = getSharedPreferences(getString(R.string.tags_to_buses_key), Context.MODE_PRIVATE);
        setTitle(tagsToBusesPref.getString(busTag, "Bus Stops"));

        // Setup the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Sets up the list view
        expBusStopIndex = -1;
        isExpBusStopIndexExpanded = false;
        setListView(busStopTitles, busStopTimes);

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long myLong) {
                lastExpBusStopIndex = expBusStopIndex;
                expBusStopIndex = myItemInt;
                updateBusTimes();
            }
        });
    }

    // Updates the list view with bus stop titles and times
    public void setListView(String[] titles, int[][] times) {
        ArrayList<BusStop> buses = new ArrayList<>(titles.length);
        for (int i = 0; i < titles.length; i++) {
            buses.add(new BusStop(busTag, titles[i], times[i]));
        }

        ListView busTimesList = (ListView) findViewById(android.R.id.list);
        if (busTimesList.getAdapter() == null) {
            BusStopAdapter adapter = new BusStopAdapter(getApplicationContext(), R.layout.list_bus_stops, buses);
            busTimesList.setAdapter(adapter);
        } else {
            ((BusStopAdapter) busTimesList.getAdapter()).refill(buses);
        }
    }

    // Updates the bus times
    private void updateBusTimes() {
        new ShowBusStopsHelper().execute(busTag, this, getApplicationContext());
    }

    @Override
    public void onResume() {
        // Auto refreshes times every 60 seconds
        refreshHandler = new Handler();
        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateBusTimes();
                refreshHandler.postDelayed(this, 60000);
            }
        }, 60000);
        active = true; // Activity is active

        super.onResume();
    }

    @Override
    protected void onPause() {
        active = false; // Activity is not active
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.abc_shrink_fade_out_from_bottom);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bus_stops, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here
        int id = item.getItemId();
        if (id == R.id.refresh) {
            updateBusTimes();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
