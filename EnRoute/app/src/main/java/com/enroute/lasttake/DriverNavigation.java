package com.enroute.lasttake;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.msdkui.common.measurements.UnitSystem;
import com.here.msdkui.guidance.GuidanceEstimatedArrivalView;
import com.here.msdkui.guidance.GuidanceEstimatedArrivalViewPresenter;
import com.here.msdkui.guidance.GuidanceManeuverData;
import com.here.msdkui.guidance.GuidanceManeuverListener;
import com.here.msdkui.guidance.GuidanceManeuverPresenter;
import com.here.msdkui.guidance.GuidanceManeuverView;
import com.here.msdkui.guidance.GuidanceNextManeuverPresenter;
import com.here.msdkui.guidance.GuidanceNextManeuverView;
import com.here.msdkui.guidance.GuidanceSpeedLimitView;
import com.here.msdkui.guidance.GuidanceSpeedPresenter;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DriverNavigation extends Fragment {

    private View view;

    private Map map;
    private ArrayList<MapObject> mapObjects;
    private java.util.Map<Integer,Boolean> alerts=new TreeMap<>();
    private SmsManager smsManager;
    private RecyclerView ordersRecycler;
    private TextView noOrders;
    private ArrayList<DeliveryOrder> orders=new ArrayList<>();
    private DeliveryOrder order;
    private FirebaseDatabase database;
    private DatabaseReference databaseOrders;
    private ValueEventListener updateDriver;
    private String phoneNumber;
    private SmsBroadcastReceiver smsBroadcastReceiver;
    private LinearLayout finishOrderLayout;

    private GuidanceManeuverPresenter guidanceManeuverPresenter;
    private GuidanceSpeedPresenter guidanceSpeedPresenter;
    private GuidanceNextManeuverPresenter guidanceNextManeuverPresenter;
    private GuidanceEstimatedArrivalViewPresenter guidanceEstimatedArrivalViewPresenter;
    private GuidanceSpeedLimitView speedLimitView;

    private boolean paused;
    private NavigationManager navigationManager;
    private PositioningManager posManager;
    private PositioningManager.OnPositionChangedListener positionListener;

    private Driver driver;

    private Global global;

    private FloatingActionButton changeSettings;
    static String driverMessage = "default";

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (posManager != null)
            posManager.removeListener(positionListener);
        map = null;

        try {
            getContext().unregisterReceiver(smsBroadcastReceiver);
        }catch (IllegalArgumentException e){

        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.driver_navigation,container,false);

        global =((Global)getContext().getApplicationContext());

        driverMessage="Hi, my name is "+global.getName()+" and I have just left with your order.\n\n";

        smsManager= SmsManager.getDefault();

        ordersRecycler=view.findViewById(R.id.id_driverOrdersRecycler);
        ordersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersRecycler.setAdapter(new DriverDeliveryOrderAdapter(getContext()));
        noOrders=view.findViewById(R.id.id_noOrders);

        database=FirebaseDatabase.getInstance();

        databaseOrders=database.getReference().child("businesses").child(global.getBusinessID()).child("orders");

        databaseOrders.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                orders.clear();
                for(DataSnapshot data:dataSnapshot.getChildren())
                    orders.add(data.getValue(DeliveryOrder.class));
                noOrders.setVisibility(orders.isEmpty()?View.VISIBLE:View.GONE);
                ordersRecycler.getAdapter().notifyDataSetChanged();
                if(!orders.contains(order) && order!=null && !order.getOrderID().equals("None")){
                    exitRoute();
                    new AlertDialog.Builder(getContext())
                            .setTitle("Order Cancelled")
                            .setMessage("Your current order has been cancelled by your manager.")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        changeSettings=view.findViewById(R.id.id_settingsButton);
        changeSettings.setOnClickListener(v -> {
            View view=inflater.inflate(R.layout.driver_settings,container,false);
            EditText customMessage=view.findViewById(R.id.id_initialMessage);
            RadioGroup colors=view.findViewById(R.id.id_color_options);
            if(global.getColorBlind().equals("normal"))
                colors.check(R.id.id_color_normal);
            else if(global.getColorBlind().equals("prot"))
                colors.check(R.id.id_color_prot);
            else if(global.getColorBlind().equals("deut"))
                colors.check(R.id.id_color_deut);
            AlertDialog settings=new AlertDialog.Builder(getContext())
                    .setTitle("Change Initial Text Message")
                    .setView(view)
                    .setPositiveButton("Save", (dialog, which) -> {
                        driverMessage = customMessage.getText().toString();
                        if(colors.getCheckedRadioButtonId()==R.id.id_color_normal){
                            global.setColorBlind("normal");
                        }else if(colors.getCheckedRadioButtonId()==R.id.id_color_prot){
                            global.setColorBlind("prot");
                        }else if(colors.getCheckedRadioButtonId()==R.id.id_color_deut){
                            global.setColorBlind("deut");
                        }
                        getFragmentManager().beginTransaction().detach(this).attach(this).commit();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .create();
            view.findViewById(R.id.id_sign_out).setOnClickListener(v1 ->{
                MainActivity.signOut();
                settings.dismiss();
            });
            customMessage.setText(driverMessage);
            settings.show();
        });

        updateDriver=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Driver> drivers=new ArrayList<>();
                for(DataSnapshot data:dataSnapshot.getChildren()) {
                    Driver dummyDriver=data.getValue(Driver.class);
                    if(driver.equals(dummyDriver))
                        drivers.add(driver);
                    else drivers.add(dummyDriver);
                }
                database.getReference().child("businesses").child(global.getBusinessID()).child("drivers").setValue(drivers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        finishOrderLayout=view.findViewById(R.id.id_finish_order_layout);

        final SupportMapFragment mapFragment=(SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.id_manager_map);

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "com.enroute.lasttake.MapService");

        if (success) {
            new Thread(() -> mapFragment.init(error -> {
                if (error == OnEngineInitListener.Error.NONE) {
                    MapEngine.getInstance().onResume();
                    map = mapFragment.getMap();
                    map.setProjectionMode(Map.Projection.MERCATOR);

                    posManager=PositioningManager.getInstance();
                    navigationManager=NavigationManager.getInstance();
                    navigationManager.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DYNAMIC);
                    navigationManager.setSpeedWarningEnabled(true);

                    positionListener = new PositioningManager.OnPositionChangedListener() {
                        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
                            if(map!=null)
                                map.setCenter(position.getCoordinate(), Map.Animation.LINEAR);
                            new Thread(() -> updateDriverLocation(position.getCoordinate())).start();

                        }
                        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) { }
                    };

                    posManager.addListener(new WeakReference<>(positionListener));
                    posManager.enableProbeDataCollection(true);
                    posManager.setMapMatchingEnabled(true);
                    posManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
                    mapFragment.getPositionIndicator().setVisible(true);

                    map.setCenter(posManager.getLastKnownPosition().getCoordinate(), Map.Animation.NONE);

                    navigationManager.setMap(map);

                    GeoCoordinate coordinate=posManager.getLastKnownPosition().getCoordinate();
                    DriverLatLng current=new DriverLatLng(coordinate.getLatitude(),coordinate.getLongitude());
                    driver=new Driver(global.getName(),global.getUserID(),current,new DeliveryOrder());
                    addDriverToDatabase(driver);
                } else {
                    Log.d("here",error.getDetails());
                }
            })).start();
        }

        smsBroadcastReceiver=new SmsBroadcastReceiver(phoneNumber,"SMS@CARRIS");
        getContext().registerReceiver(smsBroadcastReceiver,new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        return view;
    }

    private void updateDriverLocation(GeoCoordinate coordinate){
        driver.setLocation(coordinate.getLatitude(),coordinate.getLongitude());
        updateDriverInDatabase();
    }

    private void addDriverToDatabase(Driver driver){
        database.getReference()
                .child("businesses")
                .child(global.getBusinessID())
                .child("drivers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ArrayList<Driver> drivers=new ArrayList<>();
                        for(DataSnapshot data:dataSnapshot.getChildren())
                            drivers.add(data.getValue(Driver.class));
                        if(!drivers.contains(driver)) {
                            drivers.add(driver);
                            database.getReference().child("businesses").child(global.getBusinessID()).child("drivers").setValue(drivers);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void updateDriverInDatabase(){
        new Thread(() ->{
                database.getReference()
                .child("businesses")
                .child(global.getBusinessID())
                .child("drivers")
                .removeEventListener(updateDriver);

                database.getReference()
                        .child("businesses")
                        .child(global.getBusinessID())
                        .child("drivers")
                        .addListenerForSingleValueEvent(updateDriver);
        }).start();
    }

    private void getRoute(final GeoCoordinate fromCoords, String destination){
        new GeocodeRequest(destination).setSearchArea(fromCoords, 10000).execute((results, errorCode) -> {
            if (errorCode == ErrorCode.NONE && results.size()>0) {
                getRoute(fromCoords, new GeoCoordinate(results.get(0).getLocation().getCoordinate().getLatitude(), results.get(0).getLocation().getCoordinate().getLongitude()));
            }else Toast.makeText(getContext(),"Bad Location",Toast.LENGTH_LONG).show();
        });
    }

    private void getRoute(GeoCoordinate fromCoords,GeoCoordinate destination){
        final CoreRouter router=new CoreRouter();
        final RoutePlan routePlan=new RoutePlan();
        routePlan.addWaypoint(new RouteWaypoint(fromCoords));
        routePlan.addWaypoint(new RouteWaypoint(destination));

        mapObjects=new ArrayList<>();
        Image image=new Image();
        try {
            image.setImageResource(R.drawable.ic_route_start);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MapMarker startMarker=new MapMarker(fromCoords)
                .setDraggable(false)
                .setDescription("Start")
                .setIcon(image);
        try {
            image.setImageResource(R.drawable.ic_route_end);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MapMarker endMarker=new MapMarker(destination)
                .setDraggable(false)
                .setDescription("End")
                .setIcon(image);

        mapObjects.add(startMarker);
        mapObjects.add(endMarker);

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routeOptions.setRouteCount(5);

        routePlan.setRouteOptions(routeOptions);
        new Thread(() -> router.calculateRoute(routePlan,new RouteListener())).start();
    }

    private void attachPresentersAndStartRoute(MapRoute mapRoute){
        GuidanceManeuverView maneuverView = view.findViewById(R.id.id_guidanceManeuverView);
        maneuverView.setUnitSystem(UnitSystem.IMPERIAL_US);
        guidanceManeuverPresenter = new GuidanceManeuverPresenter(getContext(), navigationManager, mapRoute.getRoute());
        guidanceManeuverPresenter.addListener(new GuidanceManeuverListener() {
            @Override
            public void onDataChanged(@Nullable GuidanceManeuverData guidanceManeuverData) {
                if (guidanceManeuverData == null)
                    maneuverView.setViewState(GuidanceManeuverView.State.UPDATING);
                else
                    maneuverView.setViewState(new GuidanceManeuverView.State(guidanceManeuverData));
            }

            @Override
            public void onDestinationReached() {
                finishOrderLayout.setVisibility(View.VISIBLE);
                Button finishOrder=view.findViewById(R.id.id_finish_order);
                finishOrder.setOnClickListener(v -> {
                    finishOrder();
                    finishOrderLayout.setVisibility(View.GONE);
                });
            }
        });

        GuidanceEstimatedArrivalView estimatedArrivalView=view.findViewById(R.id.id_estimatedArrivalView);
        estimatedArrivalView.setUnitSystem(UnitSystem.IMPERIAL_US);
        guidanceEstimatedArrivalViewPresenter = new GuidanceEstimatedArrivalViewPresenter(navigationManager);
        guidanceEstimatedArrivalViewPresenter.addListener(guidanceEstimatedArrivalViewData -> {
            estimatedArrivalView.setEstimatedArrivalData(guidanceEstimatedArrivalViewData);
            if (guidanceEstimatedArrivalViewData.getDuration() != null) {
                if (guidanceEstimatedArrivalViewData != null) {
                    sendAlerts(phoneNumber, guidanceEstimatedArrivalViewData.getDuration() / 60);
                }
                if (guidanceEstimatedArrivalViewData.getEta() != null && order.isInProgress()) {
                    order.setEta(DateFormat.getTimeInstance(DateFormat.SHORT).format(guidanceEstimatedArrivalViewData.getEta()));
                    if(orders.indexOf(order)>=0)
                        orders.set(orders.indexOf(order),order);
                    updateDatabaseOrders();
                }
            }
        });

        GuidanceNextManeuverView nextManeuverView=view.findViewById(R.id.id_nextManeuverView);
        nextManeuverView.setUnitSystem(UnitSystem.IMPERIAL_US);
        guidanceNextManeuverPresenter = new GuidanceNextManeuverPresenter(getContext(), navigationManager, mapRoute.getRoute());
        guidanceNextManeuverPresenter.addListener(nextManeuverView::setNextManeuverData);

        speedLimitView=view.findViewById(R.id.id_speedLimitView);
        speedLimitView.setUnitSystem(UnitSystem.IMPERIAL_US);
        guidanceSpeedPresenter=new GuidanceSpeedPresenter(navigationManager,posManager);
        guidanceSpeedPresenter.addListener(speedLimitView::setCurrentSpeedData);

        Button exitRoute=view.findViewById(R.id.id_exitRoute);
        exitRoute.setOnClickListener(v -> {
            exitRoute();
            finishOrderLayout.setVisibility(View.GONE);
        });

        Button delayAlert=view.findViewById(R.id.id_delay);
        delayAlert.setOnClickListener(v -> sendText("I am currently delayed. Your order may be later than initially calculated."));

        startRoute(mapRoute);
    }

    private void sendAlerts(String number,int eta){
        for(java.util.Map.Entry<Integer,Boolean> entry : alerts.entrySet()) {
            if(eta<=entry.getKey() && !entry.getValue()){
                sendText("Your delivery is "+entry.getKey()+" minutes away.");
                alerts.put(entry.getKey(),true);
            }
        }
    }

    private void sendText(String content){
        smsManager.sendTextMessage(phoneNumber,null,content,null,null);
    }

    private void startRoute(MapRoute mapRoute){
        mapObjects.add(mapRoute);
        map.addMapObjects(mapObjects);

        navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);

        guidanceManeuverPresenter.resume();
        guidanceEstimatedArrivalViewPresenter.resume();
        guidanceNextManeuverPresenter.resume();
        guidanceSpeedPresenter.resume();

        ordersRecycler.setVisibility(View.GONE);
        changeSettings.setVisibility(View.GONE);

        navigationManager.startNavigation(mapRoute.getRoute());
        //navigationManager.simulate(mapRoute.getRoute(),20);

        driverMessage+="The current ETA is: "+order.getEta()+"\n\n";
        driverMessage+="Please respond with the times at which you wish to receive alerts, separated by spaces. Ex: \"5 10 15\" will send alerts when your order is 5, 10, and 15 minutes away.";
        sendText(driverMessage);
        smsBroadcastReceiver.setOnRecievedListener(text -> {
            if(phoneNumber!=null){
                String[] times=text.split(" ");
                for(String time:times){
                    alerts.put(Integer.parseInt(time),false);
                }
                smsBroadcastReceiver.setOnRecievedListener(text1 -> {
                    if(phoneNumber!=null){
                        if(text1.toLowerCase().trim().equals("eta")){
                            sendText("The ETA of your order is: "+order.getEta());
                        }
                    }
                });
            }
        });
    }

    private void finishOrder(){
        orders.remove(order);
        driver.setCurrentOrder(order=new DeliveryOrder());
        updateDriverInDatabase();
        ordersRecycler.getAdapter().notifyDataSetChanged();
        updateDatabaseOrders();
        exitRoute();
    }

    private void exitRoute(){
        order.setInProgress(false);
        order.setEta("None");
        if(orders.indexOf(order)>=0)
            orders.set(orders.indexOf(order),order);
        driver.setCurrentOrder(new DeliveryOrder());
        updateDatabaseOrders();

        phoneNumber=null;

        ordersRecycler.setVisibility(View.VISIBLE);
        changeSettings.setVisibility(View.VISIBLE);

        navigationManager.stop();
        guidanceEstimatedArrivalViewPresenter.pause();
        guidanceNextManeuverPresenter.pause();
        guidanceSpeedPresenter.pause();
        guidanceManeuverPresenter.pause();
        map.removeMapObjects(mapObjects);
    }

    private void updateDatabaseOrders(){
        databaseOrders.setValue(orders);
        /*databaseOrders.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<DeliveryOrder> orders=new ArrayList<>();
                for(DataSnapshot snapshot:dataSnapshot.getChildren()){
                    DeliveryOrder dummyOrder=snapshot.getValue(DeliveryOrder.class);
                    if(dummyOrder.equals(order)){
                        if(!order.getOrderID().equals("None"))
                            orders.add(order);
                    }else orders.add(dummyOrder);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/
    }

    private class RouteListener implements CoreRouter.Listener {

        @Override
        public void onProgress(int i) {

        }

        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {
            // If the route was calculated successfully
            if (error == RoutingError.NONE) {
                MapRoute mapRoute = new MapRoute(routeResult.get(0).getRoute())
                        .setTrafficEnabled(true)
                        .setTraveledColor(Color.GRAY);

                //map.zoomTo(routeResult.get(0).getRoute().getBoundingBox(), Map.Animation.LINEAR, Map.MOVE_PRESERVE_ORIENTATION);
                attachPresentersAndStartRoute(mapRoute);
            }
        }
    }

    public class DriverDeliveryOrderAdapter extends RecyclerView.Adapter<DriverDeliveryOrderAdapter.MyViewHolder>{
        Context context;
        DriverDeliveryOrderAdapter(Context context){
            this.context=context;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_orders_layout,parent,false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            DeliveryOrder o=orders.get(position);
            if(o!=null) {
                holder.orderID.setText(o.getOrderID().equals("") ? "NONE" : o.getOrderID());
                holder.destination.setText(o.getDestinationName());
                holder.phoneNumber.setText(o.getPhoneNumber());
                holder.acceptOrder.setEnabled(!o.isInProgress());
                holder.acceptOrder.setText(o.isInProgress()?"In Progress":"Accept Order");
                holder.acceptOrder.setOnClickListener(v -> {
                        phoneNumber = orders.get(position).getPhoneNumber();
                        smsBroadcastReceiver=new SmsBroadcastReceiver(phoneNumber,"SMS@CARRIS");
                        orders.get(position).setInProgress(true);
                        driver.setCurrentOrder(orders.get(position));
                        order=orders.get(position);
                        updateDriverInDatabase();
                        getRoute(posManager.getLastKnownPosition().getCoordinate(), order.getDestinationName());
                        updateDatabaseOrders();
                    }
                );
            }
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            TextView orderID,destination,phoneNumber;
            Button acceptOrder;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                orderID=itemView.findViewById(R.id.adapter_id_orderID);
                destination=itemView.findViewById(R.id.adapter_id_destination);
                phoneNumber=itemView.findViewById(R.id.adapter_id_phoneNumber);
                acceptOrder=itemView.findViewById(R.id.adapter_id_acceptOrder);
            }
        }
    }
}
