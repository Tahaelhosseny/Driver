package uber.example.uber.driver;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DriverMap extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks ,GoogleApiClient.OnConnectionFailedListener ,RoutingListener ,com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private List<Polyline> polylines;
    private GoogleApiClient mGoogleApiClient;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    MediaPlayer bus_start;
    MediaPlayer end;
    LatLng schoolLatLang;
    String id;
    String busId;
    List<LatLng> points;
    HashMap<String, Marker> markers;
    ImageView start;
    Marker bus;
    private FusedLocationProviderClient mFusedLocationClient;
    GoogleApiClient getmGoogleApiClient ;
    Location mLastLocation ;
    LocationRequest locationRequest ;

    ProgressDialog mProgressDialog ;

    DatabaseReference childrenRef = FirebaseDatabase.getInstance().getReference().child("SchoolBus").child("Children");
    HashMap <String ,String > hashMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        points = new ArrayList<>();
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        busId = intent.getStringExtra("busId");
        markers = new HashMap<>();
        polylines = new ArrayList<>();
        start = (ImageView) findViewById(R.id.startImageView);
        bus_start = MediaPlayer.create(this, R.raw.bus_start);
        end = MediaPlayer.create(this, R.raw.bus);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Getting Yor Data");
        mProgressDialog.setMessage("wait until getting your data");
        mProgressDialog.setCanceledOnTouchOutside(false);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        schoolLatLang = new LatLng(29.8080425,39.8548355);
        mMap.addMarker(new MarkerOptions().position(schoolLatLang).title("OurSchool")).setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.school));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(schoolLatLang));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
        points.add(schoolLatLang);
        buildGooGleApiClient();
        bus =mMap.addMarker(new MarkerOptions().position(schoolLatLang).title("Our Bus").icon(BitmapDescriptorFactory.fromResource(R.mipmap.bus_icon)));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);


    }





    private void makeRequest()
    {

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String link = "http://bus.smartapp-eg.com/api/bus/get_driver_route?busId=" + busId;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, link, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 1; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        LatLng latLng = new LatLng(Double.valueOf(jsonObject.get("child_lat").toString()), Double.valueOf(jsonObject.get("child_lang").toString()));
                        points.add(latLng);
                        hashMap.put(jsonObject.getString("child_name"),"true");
                        Marker marker =mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.home)).title(jsonObject.get("child_name").toString()));
                        markers.put(jsonObject.get("child_name").toString(), marker);
                    }
                    driverProprties();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                childrenRef.setValue(hashMap);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mProgressDialog.dismiss();
                start.setClickable(true);
                Toast.makeText(getApplicationContext(), error.getMessage().toString(), Toast.LENGTH_SHORT).show();
            }
        });


        requestQueue.add(stringRequest);



    }


    @Override
    public void onLocationChanged(Location location)
    {
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        DatabaseReference locationDatabaseReference = FirebaseDatabase.getInstance().getReference("SchoolBus").child("Buses").child("Bus1").child("Location");
        locationDatabaseReference.setValue(String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude()));
        bus.setPosition(latLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
    }





    @Override
    public void onRoutingFailure(RouteException e)
    {
        mProgressDialog.dismiss();
        Toast.makeText(getApplicationContext(), "Please Press Start Button Agin" + e.getMessage(), Toast.LENGTH_LONG).show();
        start.setClickable(true);
    }

    @Override
    public void onRoutingStart() {

    }


    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex)
    {

        mProgressDialog.dismiss();
        if (polylines.size() > 0) {
            for (Polyline poly : polylines)
            {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {
            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(Color.RED);
            polyOptions.width(4);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
        }

    }

    @Override
    public void onRoutingCancelled() {

    }


    private void driverProprties() {
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(points).build();
        routing.execute();
    }

    public void stratTrip(View view)
    {
        bus_start.start();
        mProgressDialog.show();
        makeRequest();
        start.setClickable(false);
        DatabaseReference childrenDatabaseReference = FirebaseDatabase.getInstance().getReference("SchoolBus").child("Children");

        childrenDatabaseReference.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                if(dataSnapshot.getValue().toString().equals("false"))
                {
                    try {
                        String str = dataSnapshot.getKey().toString();
                        Marker marker = markers.get(str);
                        marker.remove();
                        markers.remove(str);
                        if(markers.size()==0)
                        {
                            mMap.clear();
                            DatabaseReference startn = FirebaseDatabase.getInstance().getReference("SchoolBus").child("Buses").child("Bus1").child("Status");
                            startn.setValue("false");
                            start.setClickable(true);
                        }
                    }catch (Exception e)
                    {
                    }
                }

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot)
            {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference start = FirebaseDatabase.getInstance().getReference("SchoolBus").child("Buses").child("Bus1").child("Status");
        start.setValue("true");
        bus = mMap.addMarker(new MarkerOptions().position(schoolLatLang).title("Our Bus").icon(BitmapDescriptorFactory.fromResource(R.mipmap.bus_icon)));
    }


    protected synchronized void buildGooGleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(Bundle bundle)
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient , locationRequest ,  this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
