package me.rutgersdirect.rudirect.helper;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import me.rutgersdirect.rudirect.BusConstants;
import me.rutgersdirect.rudirect.api.NextBusAPI;
import me.rutgersdirect.rudirect.ui.BusTimesActivity;

public class ShowBusStopsAndTimesHelper extends AsyncTask<Object, Void, Object[][]> {
    private String tag;
    private Activity activity;

    protected Object[][] doInBackground(Object... objects) {
        tag = (String) objects[0];
        activity = (Activity) objects[1];
        Object[][] busStopTitlesAndTimes = {NextBusAPI.getBusStopTitles(tag), NextBusAPI.getBusStopTimes(tag)};
        return busStopTitlesAndTimes;
    }

    protected void onPostExecute(Object[][] titlesAndTimes) {
        if (activity instanceof BusTimesActivity) {
            // Update bus stop titles and times
            ((BusTimesActivity) activity).setListView(titlesAndTimes[0], titlesAndTimes[1]);
        }
        else {
            // Start new activity to display bus stop titles and times
            Intent intent = new Intent(activity, BusTimesActivity.class);
            intent.putExtra(BusConstants.BUS_TAG_MESSAGE, tag);
            intent.putExtra(BusConstants.BUS_STOP_TITLES_MESSAGE, titlesAndTimes[0]);
            intent.putExtra(BusConstants.BUS_STOP_TIMES_MESSAGE, titlesAndTimes[1]);
            activity.startActivity(intent);
        }
    }
}