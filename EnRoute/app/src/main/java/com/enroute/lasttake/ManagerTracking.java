package com.enroute.lasttake;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ManagerTracking extends Fragment {

    private GoogleMap gMap;
    private MapView mapView;

    private ArrayList<Driver> drivers=new ArrayList<>();
    private FirebaseDatabase database;

    private Global global;

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @SuppressLint("MissingPermission")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manager_tracking,container,false);

        global=((Global)getContext().getApplicationContext());

        mapView = view.findViewById(R.id.id_manager_map);
        if (mapView != null){
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(googleMap -> {
                gMap = googleMap;
                gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                gMap.setMyLocationEnabled(true);
                gMap.getUiSettings().setMapToolbarEnabled(false);
                gMap.getUiSettings().setMyLocationButtonEnabled(true);
                gMap.setMapStyle(new MapStyleOptions(getResources().getString(R.string.manager_map_style)));
                LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                if (location != null) {
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(17)
                            .bearing(0)
                            .build();
                    gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            });
        }

        database=FirebaseDatabase.getInstance();

        addDriverListListener();
        drawDriversOnMap();

        return view;
    }

    private void addDriverListListener(){
        database.getReference().child("businesses").child(global.getBusinessID()).child("drivers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                drivers.clear();
                for(DataSnapshot data:dataSnapshot.getChildren())
                    drivers.add(data.getValue(Driver.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private synchronized void drawDriversOnMap(){
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(gMap!=null) {
                    getActivity().runOnUiThread(() -> gMap.clear());
                    for (Driver driver : drivers) {
                        if(!driver.getCurrentOrder().getPhoneNumber().equals("None"))
                            getActivity().runOnUiThread(() -> gMap.addMarker(driver.getMapObject()).showInfoWindow());
                    }
                }
            }
        },0,1000);
    }

}
