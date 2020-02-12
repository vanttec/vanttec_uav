package com.vanttec.uav;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCircleClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;


public class MapsActivity extends FragmentActivity
        implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        OnMapReadyCallback,
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        OnMarkerClickListener,
        OnMarkerDragListener,
        OnCircleClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = MapsActivity.class.getName();

    //constants
    static  final int LOCATE_ME = 0;
    static  final int LOCATE_DRONE = 1;
    static  final int SET_DEST = 2;
    static  final int SET_DEST_MANUALLY = 3;
    static  final int RETURN_HOME = 4;
    static  final int ADD_GEOFENCE = 5;
    static  final int ADD_GEOFENCE_MANUALLY = 6;

    /**cetec
     * lat lng
     * 25.65094662530861 -100.2913697557484
     * **/

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    //UI variables
    private Spinner sp_settings;
    private LinearLayout lyt_dest, lyt_dest_2;
    private Button btn_start, btn_stop, btn_clear, btn_add, btn_config, btn_upload;
    private FloatingActionButton btn_camera;

    //Google vars
    private GoogleMap mMap;

    static final LatLng cetec = new LatLng(25.65094662530861, -100.2913697557484);
    private LatLng MyLatLng, droneLatLng; //phone, drone location

    private Marker droneMarker;
    private List<Marker> markers = new ArrayList<Marker>();

    private Boolean isAddManually = false;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;
    private FusedLocationProviderClient fusedLocationClient;

    //Geofence variables
    private GeofencingClient geofencingClient;
    private List<Geofence> geoList = new ArrayList<Geofence>();
    private double GEOFENCE_RADIUS = 100;
    private Marker geofenceMarker;
    private Circle geoFenceLimits;

    //Drone
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();
    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator missionOperator;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            MyLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                            moveToLocation(MyLatLng);
                        }
                    }
                });

        initUI();
        //geofencingClient = LocationServices.getGeofencingClient(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    private void initUI() {
        //spinner dropdown
        sp_settings = (Spinner) findViewById(R.id.sp_settings);
        ArrayAdapter<String> sp_adapter = new ArrayAdapter<String>(
                MapsActivity.this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.map_settings));
        sp_adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sp_settings.setAdapter(sp_adapter);
        sp_settings.setOnItemSelectedListener(this);

        //Buttons set destination
        lyt_dest = (LinearLayout) findViewById(R.id.lyt_dest);
        lyt_dest_2 = (LinearLayout) findViewById(R.id.lyt_dest_2);

        btn_add = (Button) findViewById(R.id.add);
        btn_clear = (Button) findViewById(R.id.clear);
        btn_stop = (Button) findViewById(R.id.stop);
        btn_start = (Button) findViewById(R.id.start);
        btn_config = (Button) findViewById(R.id.configuration);
        btn_upload = (Button) findViewById(R.id.upload);

        btn_add.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_clear.setOnClickListener(this);
        btn_config.setOnClickListener(this);
        btn_upload.setOnClickListener(this);

        btn_camera = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        btn_camera.setOnClickListener(this);
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
        //mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMarkerDragListener(this);


        enableMyLocation();

        //init Marker
        /*MarkerOptions a = new MarkerOptions().position(cetec).title("Drone").icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_pin));
        //Icons made by <a href="https://www.flaticon.com/authors/smashicons" title="Smashicons">Smashicons</a> from <a href="https://www.flaticon.com/" title="Flaticon"> www.flaticon.com</a>
        droneMarker = mMap.addMarker(a);*/

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCircleClickListener(this);
        Log.d(TAG, "onMapsReady");
        initFlightController();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this,
                "Current phone location\n" +
                        "lat: " + location.getLatitude() + "\n" +
                        "long: " + location.getLongitude(),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add: {
                if (isAddManually) {
                    openDialogDest();
                } else {
                    addMarker(MyLatLng);
                }
                break;
            }
            case R.id.clear: {
                clearMarkers();
                break;
            }
            case R.id.configuration: {
                Log.d(TAG, "configuration btn pressed");
                showSettingDialog();
                break;
            }
            case R.id.upload: {
                Log.d(TAG, "upload btn pressed");
                //uploadWayPointMission();
                break;
            }
            case R.id.start:{
                Log.d(TAG, "start btn pressed");
                //startWaypointMission();
                break;
            }
            case R.id.stop:{
                Log.d(TAG, "stop btn pressed");
                //stopWaypointMission();
                break;
            }
            case R.id.floatingActionButton: {
                finish();
                /*Intent intent = new Intent(this, DefaultLayoutActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);*/
            }
        }
    }

    //Spinning selector
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        lyt_dest.setVisibility(View.GONE);
        lyt_dest_2.setVisibility(View.GONE);
        switch (position) {
            case LOCATE_ME: {
                // Move the camera instantly to My location with a zoom of 17.
                moveToLocation(MyLatLng);
                break;
            }
            case LOCATE_DRONE: {
                moveToLocation(droneLatLng);
                break;
            }
            case SET_DEST: {
                lyt_dest.setVisibility(View.VISIBLE);
                lyt_dest_2.setVisibility(View.VISIBLE);
                isAddManually = false;
                break;
            }
            case SET_DEST_MANUALLY: {
                lyt_dest.setVisibility(View.VISIBLE);
                lyt_dest_2.setVisibility(View.VISIBLE);
                isAddManually = true;
                break;
            }
            case ADD_GEOFENCE:{
                removeGeofence();
                addGeoMarker(MyLatLng);
                break;
            }
            case ADD_GEOFENCE_MANUALLY:{
                openDialogGeofence();
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //moveToLocation(MyLatLng);
    }

    public void moveToLocation(LatLng loc) {
        if(loc != null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,17));
    }

    public void addMarker(LatLng latlng) {
        int stageNumber = markers.size() + 1;
        MarkerOptions a = new MarkerOptions()
                .position(latlng)
                .draggable(true)
                .title("Stage " + stageNumber);

        Marker m = mMap.addMarker(a);
        m.setPosition(latlng);

        markers.add(m);
        moveToLocation(latlng);
    }

    public void addGeoMarker(LatLng latlng) {
        if(geofenceMarker == null) {
            MarkerOptions a = new MarkerOptions()
                    .position(latlng)
                    .draggable(true)
                    .title("GEOFENCE");

            geofenceMarker = mMap.addMarker(a);
        }

        geofenceMarker.setPosition(latlng);
        moveToLocation(latlng);
    }

    public void clearMarkers() {
        for (int i = 0; i < markers.size(); i++) {
            Marker aux = markers.get(i);
            aux.remove();
        }

        markers.clear();
    }

    public void openDialogDest() {
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_destination, null);

        final EditText latText = (EditText) dialogView.findViewById(R.id.latitude);
        final EditText lngText = (EditText) dialogView.findViewById(R.id.longitude);
        final EditText altText = (EditText) dialogView.findViewById(R.id.altitude);
        Button btnAdd = (Button) dialogView.findViewById(R.id.add);

        mBuilder.setView(dialogView);
        final AlertDialog dialog = mBuilder.create();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!latText.getText().toString().isEmpty() && !lngText.getText().toString().isEmpty()) {
                    final Double lat = Double.parseDouble(latText.getText().toString());
                    final Double lng = Double.parseDouble(lngText.getText().toString());
                    final Double alt = Double.parseDouble(altText.getText().toString());
                    addMarker(new LatLng(lat, lng));
                    dialog.cancel();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Set a lat & lng to continue",
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        dialog.show();
    }

    public void openDialogGeofence() {
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_geofence, null);

        final EditText latText = (EditText) dialogView.findViewById(R.id.latitude);
        final EditText lngText = (EditText) dialogView.findViewById(R.id.longitude);
        final EditText radioText = (EditText) dialogView.findViewById(R.id.radio);
        Button btnAdd = (Button) dialogView.findViewById(R.id.add);

        mBuilder.setView(dialogView);
        final AlertDialog dialog = mBuilder.create();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!latText.getText().toString().isEmpty() && !lngText.getText().toString().isEmpty()) {
                    final Double lat = Double.parseDouble(latText.getText().toString());
                    final Double lng = Double.parseDouble(lngText.getText().toString());

                    if(!radioText.getText().toString().isEmpty()) {
                        final Double radio = Double.parseDouble(radioText.getText().toString());
                        GEOFENCE_RADIUS = radio;
                    }

                    addGeoMarker(new LatLng(lat, lng));
                    addGeofence();
                    dialog.cancel();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Set a lat & lng to continue",
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        dialog.show();
    }

    public void openDialogRadioGeofence() {
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_radio_geofence, null);

        final EditText radioText = (EditText) dialogView.findViewById(R.id.radio);
        Button btnAdd = (Button) dialogView.findViewById(R.id.add);
        Button btnRemove = (Button) dialogView.findViewById(R.id.remove);

        mBuilder.setView(dialogView);
        final AlertDialog dialog = mBuilder.create();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!radioText.getText().toString().isEmpty()) {
                    final Double radio = Double.parseDouble(radioText.getText().toString());
                    GEOFENCE_RADIUS = radio;
                    addGeofence();
                    dialog.cancel();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Set a valid Radio",
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                removeGeofence();
                geofenceMarker.remove();
                geofenceMarker = null;
            }
        });

        dialog.show();
    }

    //Draw the circle for now
    public void addGeofence() {
        Log.d(TAG, "drawGeofence()");
        removeGeofence();
        addGeofenceCircle();
    }

    //remove circle for now
    private void removeGeofence() {
        if ( geoFenceLimits != null ) {
            Log.d("call", "fence limits not null");
            geoFenceLimits.remove();
        }
    }

    private void addGeofenceCircle() {
        Toast.makeText(this, "pos: " + geofenceMarker.getPosition(), Toast.LENGTH_SHORT).show();
        CircleOptions circleOptions = new CircleOptions()
                .center( geofenceMarker.getPosition() )
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS );
        geoFenceLimits = mMap.addCircle( circleOptions );
        geoFenceLimits.setClickable(true);
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        Toast.makeText(this, "pos: " + marker.getPosition(), Toast.LENGTH_SHORT).show();
        if(marker.equals(geofenceMarker)) {
            Toast.makeText(this, "Geo Marker", Toast.LENGTH_SHORT).show();
            addGeofence();
        }

        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    @Override
    public void onCircleClick(Circle circle) {
        Toast.makeText(this, "Set Radio", Toast.LENGTH_SHORT).show();
        openDialogRadioGeofence();
    }

    //Drone Methods
    private void initFlightController() {
        Log.d(TAG, "initFlightController");
        FlightController mFlightController = DemoApplication.getFlightController();

        if (mFlightController != null) {
            //This method is called 10 times per second.
            mFlightController.setStateCallback(djiFlightControllerCurrentState -> {
                double droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                double droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                droneLatLng = new LatLng(droneLocationLat, droneLocationLng);
                Log.d(TAG, "onUpdate drone latlng" + droneLocationLat + " " + droneLocationLng);

                if(!Double.isNaN(droneLocationLat) || !Double.isNaN((droneLocationLng))){
                    Log.d(TAG, "update Drone Location");
                    updateDroneLocation();
                } else {
                    Log.d(TAG, "cant update Drone Location");
                }
            });
        }
    }

    private void updateDroneLocation() {
        Log.d(TAG, "creating marker");
        MarkerOptions a = new MarkerOptions()
                .position(droneLatLng)
                .title("Drone :)")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_pin));
        Log.d(TAG, "marker ready!");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                    Log.d(TAG, "marker remove");
                }
                droneMarker = mMap.addMarker(a);
                Log.d(TAG, "marker on Map");
            }
        });

        Toast.makeText(this, "drone \n" + droneLatLng, Toast.LENGTH_SHORT).show();
    }

    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_config, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }
        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
            .setTitle("")
            .setView(wayPointSettings)
            .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id) {

                    String altitudeString = wpAltitude_TV.getText().toString();
                    altitude = Integer.parseInt(nulltoIntegerDefault(altitudeString));
                    Log.e(TAG,"altitude " + altitude);
                    Log.e(TAG,"speed " + mSpeed);
                    Log.e(TAG, "mFinishedAction " + mFinishedAction);
                    Log.e(TAG, "mHeadingMode " + mHeadingMode);
                    configWayPointMission();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .create()
            .show();
    }

    String nulltoIntegerDefault(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val) {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){
        if (waypointMissionBuilder == null){
            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){
            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            Toast.makeText(this, "Set Waypoint attitude successfully", Toast.LENGTH_SHORT).show();
        }

        DJIError error = DemoApplication.getWaypointMissionControl().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            Toast.makeText(this, "loadWaypoint succeeded", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "loadWaypoint failed " + error.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }
}