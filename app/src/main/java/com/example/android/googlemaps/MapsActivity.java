package com.example.android.googlemaps;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    double latitude, longitude;
    TextView textView;
    ArrayList<LatLng> latLngArrayList = new ArrayList<>();
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        textView = (TextView) findViewById(R.id.addtext);

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            } else {
                checklocationpermission();
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        String addresslocation = null;
        if (mLocation != null) {
            latitude = mLocation.getLatitude();
            longitude = mLocation.getLongitude();
            LatLng loc = new LatLng(latitude, longitude);
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                addresslocation = address + ", " + city + ", " + state + ", " + country + ", " + postalCode;
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMap.addMarker(new MarkerOptions().position(loc).title(addresslocation));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 11));
            StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
            sb.append("location=" + latitude + "," + longitude);
            sb.append("&types=" + "restaurant");
            sb.append("&radius=5000");
            sb.append("&sensor=true");
            sb.append("&key=" + "YOUR_KEY_HERE");
            new PlacesNear().execute(new String(sb.toString()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private String directionUrl(LatLng destination) {
        StringBuilder stringBuilderdirection = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        stringBuilderdirection.append("&origin=" + mLocation.getLatitude() + "," + mLocation.getLongitude());
        stringBuilderdirection.append("&destination=" + destination.latitude + "," + destination.longitude);
        stringBuilderdirection.append("&sensor=false");
        stringBuilderdirection.append("&mode=driving");
        String direction = stringBuilderdirection.toString();
        return direction;
    }


    private void checklocationpermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setTitle("Location Permission Needed").setMessage("App needs permission")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        }).create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private class PlacesNear extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            String url = strings[0];
            String result = null;
            String inputline;

            try {
                URL myurl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) myurl.openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(15000);
                connection.setConnectTimeout(15000);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                int responsecode = connection.getResponseCode();
                if (responsecode != 200) {
                    InputStreamReader inputStreamReader_error = new InputStreamReader(connection.getErrorStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader_error);
                    StringBuilder builder = new StringBuilder();
                    while ((inputline = bufferedReader.readLine()) != null) {
                        builder.append(inputline);
                    }
                    bufferedReader.close();
                    inputStreamReader_error.close();
                    result = builder.toString();
                } else {
                    InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    StringBuilder builder = new StringBuilder();
                    while ((inputline = bufferedReader.readLine()) != null) {
                        builder.append(inputline);
                    }
                    bufferedReader.close();
                    inputStreamReader.close();
                    result = builder.toString();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                JSONArray jsonArray = jsonObject.getJSONArray("results");

                for (int i = 0; i < jsonArray.length(); i++) {
                    Double lat = Double.valueOf(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat"));
                    Double longi = Double.valueOf(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng"));
                    String placename = jsonArray.getJSONObject(i).getString("name");
                    String vicinity = jsonArray.getJSONObject(i).getString("vicinity");
                    String near = lat + " " + longi + " " + placename + " " + vicinity;
                    LatLng latLng1 = new LatLng(lat, longi);
                    mMap.addMarker(new MarkerOptions().position(latLng1).title(placename));
                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            LatLng latLngdest = marker.getPosition();
                            String title = marker.getTitle();
                            LatLng latLngdestination = new LatLng(latLngdest.latitude, latLngdest.longitude);
                            textView.setText(title);
                            String direct = directionUrl(latLngdestination);
                            new RouteClass().execute(new String(direct.toString()));
                            return true;
                        }
                    });

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class RouteClass extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            String url = strings[0];
            String result = null;
            String inputline;

            try {
                URL myurl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) myurl.openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(15000);
                connection.setConnectTimeout(15000);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                int responsecode = connection.getResponseCode();
                if (responsecode != 200) {
                    InputStreamReader inputStreamReader_error = new InputStreamReader(connection.getErrorStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader_error);
                    StringBuilder builder = new StringBuilder();
                    while ((inputline = bufferedReader.readLine()) != null) {
                        builder.append(inputline);
                    }
                    bufferedReader.close();
                    inputStreamReader_error.close();
                    result = builder.toString();
                } else {
                    InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    StringBuilder builder = new StringBuilder();
                    while ((inputline = bufferedReader.readLine()) != null) {
                        builder.append(inputline);
                    }
                    bufferedReader.close();
                    inputStreamReader.close();
                    result = builder.toString();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                GoogleMapOptions options = new GoogleMapOptions();
                options.liteMode(true).mapToolbarEnabled(true);
                Double lat = null, longi = null;
                JSONObject jsonObejct = new JSONObject(s);
                ArrayList points = null;
                PolylineOptions lineoptions = null;
                MarkerOptions markeroptions = new MarkerOptions();

                JSONArray jsonAray = jsonObejct.getJSONArray("routes");
                for (int i = 0; i < jsonAray.length(); i++) {
                    points = new ArrayList();
                    lineoptions = new PolylineOptions();
                    JSONArray resultarray = jsonAray.getJSONObject(i).getJSONArray("legs").getJSONObject(i).getJSONArray("steps");
                    for (i = 0; i < resultarray.length(); i++) {
                        if (i < resultarray.length() - 1) {
                            lat = (Double) resultarray.getJSONObject(i).getJSONObject("start_location").get("lat");
                            longi = (Double) resultarray.getJSONObject(i).getJSONObject("start_location").get("lng");
                        } else {
                            lat = (Double) resultarray.getJSONObject(i).getJSONObject("end_location").get("lat");
                            longi = (Double) resultarray.getJSONObject(i).getJSONObject("end_location").get("lng");
                        }
                        String latlongthmgs = lat + " " + longi;
                        LatLng position = new LatLng(lat, longi);
                        points.add(position);
                    }
                    lineoptions.addAll(points);
                    lineoptions.width(12);
                    lineoptions.color(Color.RED);
                    lineoptions.geodesic(true);
                    mMap.addPolyline(lineoptions);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(s);
        }
    }

}


