package com.skycore.task;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skycore.task.adapters.RestaurantAdapter;
import com.skycore.task.models.Businesses;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.skycore.task.Config.GET_NEAR_BY_RESTAURANTS;
import static com.skycore.task.Constants.API_KEY;

// Just added project to git

// this changes done from server

// try again

public class MainActivity extends AppCompatActivity {

    private final List<Businesses> rootList = new ArrayList<>();
    private RecyclerView rv_restaurants;
    private ProgressBar progress_bar;
    private TextView tv_not_found, tv_address;
    private AppCompatSeekBar seekBar;
    private RestaurantAdapter mAdapter;
    private int radius = 1000, offset = 0;
    private boolean isDefaultLocation = true;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    @SuppressLint("SetTextI18n")
    private void initViews() {
        rv_restaurants = findViewById(R.id.rv_restaurants);
        rv_restaurants.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
        progress_bar = findViewById(R.id.progress_bar);
        tv_not_found = findViewById(R.id.tv_not_found);
        TextView tv_default = findViewById(R.id.tv_default);
        tv_address = findViewById(R.id.tv_address);
        TextView tv_current_loc = findViewById(R.id.tv_current_loc);
        seekBar = findViewById(R.id.seekBar);
        TextView tv_km = findViewById(R.id.tv_km);

        seekBar.incrementProgressBy(1);
        tv_km.setText("1" + " " + getString(R.string.km));

        tv_default.setOnClickListener(s -> {
            isDefaultLocation = true;
            tv_address.setText("NYC");
            tv_not_found.setVisibility(View.GONE);
            refreshAndGetdata();
        });

        tv_current_loc.setOnClickListener(s -> {
            try {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                } else {
                    getLocation1();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        /* Seekbar
         * */
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    tv_km.setText("100" + " " + getString(R.string.m));
                } else {
                    tv_km.setText(progress + " " + getString(R.string.km));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                refreshAndGetdata();
            }
        });

        /*Default Api Call
         * */
        if (Util.isConnectingToInternet(this)) {
            getData();
        } else {
            Toast.makeText(this, getString(R.string.check_internet_connection), Toast.LENGTH_LONG).show();
        }

    }

    /*Clear data and get data from server
     * */
    private void refreshAndGetdata() {
        offset = 0;
        rootList.clear();
        mAdapter = null;
        rv_restaurants.setVisibility(View.GONE);
        if (seekBar.getProgress() == 0) {
            radius = 100;
        } else {
            radius = seekBar.getProgress() * 1000;
        }
        progress_bar.setVisibility(View.VISIBLE);
        seekBar.setEnabled(false);
        if (Util.isConnectingToInternet(getApplicationContext())) {
            getData();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.check_internet_connection), Toast.LENGTH_LONG).show();
        }
    }

    /*Get data from server
     * */
    private void getData() {
        String URL, str_location = "location=NYC", str_sort_by = "&sort_by=distance", str_term = "&term=restaurants";

        if (!isDefaultLocation) {    //Current location
            str_location = "latitude=" + latitude + "&longitude=" + longitude;
        }
        URL = GET_NEAR_BY_RESTAURANTS + str_location + str_sort_by + str_term + "&radius=" + radius + "&limit=15" + "&offset=" + offset;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                URL, null,
                response -> {
                    Log.d("### res:", response.toString());
                    List<Businesses> tempList = new ArrayList<>();
                    try {
                        JSONObject jsonObject = new JSONObject(response.toString());
                        JSONArray jsonArray = jsonObject.getJSONArray("businesses");
                        Type listType = new TypeToken<List<Businesses>>() {
                        }.getType();
                        Gson gson = new Gson();
                        tempList = gson.fromJson(jsonArray.toString(), listType);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (!tempList.isEmpty()) {
                        rootList.addAll(tempList);
                    }

                    updateDisplay();


                }, error -> {

        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", API_KEY);  // yelp API key
                return headers;
            }
        };

        AppController.getInstance().addToRequestQueue(jsonObjReq, "### MainActivity :");     // add in queue
    }

    /* Update recyclerview
     * */
    private void updateDisplay() {
        if (mAdapter != null) {     // for second api call in pagination
            mAdapter.notifyDataSetChanged();
            mAdapter.setLoaded();
        } else {
            mAdapter = new RestaurantAdapter(MainActivity.this, rootList, rv_restaurants);
            rv_restaurants.setAdapter(mAdapter);
            mAdapter.setLoaded();

            mAdapter.setOnLoadMoreListener(() -> {

                if (rootList.get(rootList.size() - 1) != null) {    // to avoid multiple progressbar at bottom
                    rootList.add(null);     // add progressbar
                    mAdapter.notifyItemInserted(rootList.size() - 1);
                    new Handler().postDelayed(() -> {
                        rootList.remove(rootList.size() - 1);   // remove progressbar
                        mAdapter.notifyItemRemoved(rootList.size());
                        int index = rootList.size();
                        offset = index + 1; // offset change
                        if (Util.isConnectingToInternet(this)) {
                            getData();
                        } else {
                            Toast.makeText(this, getString(R.string.check_internet_connection), Toast.LENGTH_LONG).show();
                        }

                    }, 1000);
                }

            });
        }

        if (!rootList.isEmpty()) {
            rv_restaurants.setVisibility(View.VISIBLE);
            tv_not_found.setVisibility(View.GONE);
        } else {
            tv_not_found.setVisibility(View.VISIBLE);
        }
        progress_bar.setVisibility(View.GONE);
        seekBar.setEnabled(true);   // to avoid multiple request

    }


    public void getLocation1() throws IOException {
        isDefaultLocation = false;
        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        if (gpsTracker.canGetLocation()) {
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();


            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String str_address = addresses.get(0).getAddressLine(0);
            Log.d("#### address:", str_address);
            tv_address.setText(str_address);


        } else {
            gpsTracker.showSettingsAlert();
        }
        refreshAndGetdata();
    }


}
