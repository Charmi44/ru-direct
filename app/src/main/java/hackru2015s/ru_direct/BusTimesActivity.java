package hackru2015s.ru_direct;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class BusTimesActivity extends ListActivity {

    private class SetupBusPredictions extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... strings) {
            return MainActivity.getJSON("http://runextbus.herokuapp.com/route/" + strings[0]);
        }

        protected void onPostExecute(String result) {
            ArrayList<String> busStopTitles = new ArrayList<String>();
            ArrayList<String> busStopTimes = new ArrayList<String>();
            try {
                JSONArray jArray = new JSONArray(result);
                for (int i = 0; i < jArray.length(); i++) {
                    String allTimes = "";
                    JSONObject stopObject = jArray.getJSONObject(i);
                    busStopTitles.add(stopObject.getString("title"));
                    JSONArray predictions = stopObject.getJSONArray("predictions");
                    for (int j = 0; j < predictions.length(); j++) {
                        JSONObject times = predictions.getJSONObject(j);
                        allTimes += times.getString("minutes");
                        if (j != predictions.length() - 1) {
                            allTimes += ", ";
                        }
                    }
                    busStopTimes.add(allTimes);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            String[] titles = busStopTitles.toArray(new String[busStopTitles.size()]);
            String[] times = busStopTimes.toArray(new String[busStopTimes.size()]);
            setListView(titles, times);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bus_times);

        Intent intent = getIntent();
        final String busName = intent.getStringExtra(MainActivity.BUS_NAME);
        String[] busStopTitles = intent.getStringArrayExtra(MainActivity.BUS_STOP_TITLES_MESSAGE);
        String[] busStopTimes = intent.getStringArrayExtra(MainActivity.BUS_STOP_TIMES_MESSAGE);

        setListView(busStopTitles, busStopTimes);

        // Setup refresh button
        final Button refresh = (Button) findViewById(R.id.refreshTimes);
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new SetupBusPredictions().execute(busName);
            }
        });
    }

    public void setListView(String[] titles, String[] times) {
        String[] buses = new String[titles.length];

        for (int i = 0; i < buses.length; i++) {
            buses[i] = titles[i] + "\n" + times[i] + " minutes";
        }

        ListView busTimesList = (ListView) findViewById(android.R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                R.layout.list_black_text, R.id.list_content, buses);
        busTimesList.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bus_times, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}