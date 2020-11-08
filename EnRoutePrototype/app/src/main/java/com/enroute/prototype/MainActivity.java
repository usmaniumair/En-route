package com.enroute.prototype;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    //Backend
    private GoogleSignInAccount account;
    private String userID=null;
    private GoogleSignInClient signInClient;
    private FirebaseAuth auth;
    private SignInButton googleSignInButton;
    private FirebaseUser user;

    //Sign in screen
    private Button signInButton;
    private Button createAccountButton;
    private EditText email;
    private EditText password;
    private ConstraintLayout signInScreen;

    //Signed in screen
    private ConstraintLayout mainScreen;
    private TextView emailText;
    private TextView userIDText;
    private Button signOut;
    private Switch duty;
    private DatabaseReference onDuty;
    private TextView onDutyText;
    private DatabaseReference data;
    private EditText dataText;
    private Button addData;
    private TextView dataAdded;

    //Database
    private FirebaseDatabase database;

    //Map
    private MapView mapView;
    private GoogleMap gMap;
    private DatabaseReference location;
    private TextView latText,longText;
    private Button addPin;
    private Marker marker;

    @Override
    protected void onStart() {
        super.onStart();
        account=GoogleSignIn.getLastSignedInAccount(this);
        if(account!=null){
            AuthCredential credential= GoogleAuthProvider.getCredential(account.getIdToken(),null);
            auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        userID=account.getId();
                        emailText.setText(account.getEmail());
                        signInScreen.setVisibility(View.GONE);
                        userIDText.setText(userID);
                        mainScreen.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        user = auth.getCurrentUser();
        if(user!=null){
            userID=user.getUid();
            emailText.setText(user.getEmail());
            signInScreen.setVisibility(View.GONE);
            userIDText.setText(userID);
            mainScreen.setVisibility(View.VISIBLE);
        }
        if(userID!=null)
            addDataListeners();
        else{
            signInButton.setVisibility(View.VISIBLE);
            mainScreen.setVisibility(View.GONE);
        }
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleSignInButton=findViewById(R.id.id_googleSignIn);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = signInClient.getSignInIntent();
                startActivityForResult(signInIntent, 0);
            }
        });

        signInButton=findViewById(R.id.id_signIn);
        createAccountButton=findViewById(R.id.id_createAccount);
        email=findViewById(R.id.id_loginEmail);
        password=findViewById(R.id.id_loginPassword);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(email.getText().toString().length()>0 && password.getText().toString().length()>0)
                    signIn(email.getText().toString(),password.getText().toString());
            }
        });
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(email.getText().toString().length()>0 && password.getText().toString().length()>0)
                    createAccount(email.getText().toString(),password.getText().toString());
            }
        });

        signInScreen=findViewById(R.id.id_signInScreen);
        mainScreen=findViewById(R.id.id_mainScreen);

        emailText=findViewById(R.id.id_email);
        userIDText=findViewById(R.id.id_userID);

        signOut=findViewById(R.id.id_signOut);
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInClient.signOut();
                auth.signOut();

                mainScreen.setVisibility(View.GONE);
                signInScreen.setVisibility(View.VISIBLE);
            }
        });

        database=FirebaseDatabase.getInstance();

        duty=findViewById(R.id.id_duty);
        duty.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    onDuty.setValue(true);
                }else{
                    onDuty.setValue(false);
                }
            }
        });

        onDutyText=findViewById(R.id.id_onDutyText);

        dataText=findViewById(R.id.id_dataText);
        dataText.clearFocus();
        dataAdded=findViewById(R.id.id_data);
        addData=findViewById(R.id.id_addData);
        addData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.setValue(dataText.getText().toString());
            }
        });

        mapView=findViewById(R.id.id_mapView);
        mapView.onCreate(savedInstanceState);
        latText=findViewById(R.id.id_lat);
        longText=findViewById(R.id.id_long);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap=googleMap;
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    gMap.setMyLocationEnabled(true);
                    gMap.getUiSettings().setMyLocationButtonEnabled(true);
                    LocationServices.getFusedLocationProviderClient(MainActivity.this).getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            updateLocation(location);
                        }
                    });
                    gMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
                        @Override
                        public void onMyLocationClick(@NonNull Location location) {
                            updateLocation(location);
                        }
                    });
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},0);
                }
            }
        });

        addPin=findViewById(R.id.id_addPin);
        addPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng marker=new LatLng((double)Math.round(randNum(-90,90) * 100000d) / 100000d,(double)Math.round(randNum(-180,180) * 100000d) / 100000d);
                if(MainActivity.this.marker!=null)
                    MainActivity.this.marker.remove();
                MainActivity.this.marker=gMap.addMarker(new MarkerOptions().position(marker).title("Randon Marker"));
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker,0));
                Location location=new Location("");
                location.setLatitude(marker.latitude);
                location.setLongitude(marker.longitude);
                updateLocation(location);
            }
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("765389432783-17ur2256q7d7qsjv7pbqhjtjeqt82mp8.apps.googleusercontent.com")
                .requestId()
                .requestEmail()
                .build();
        signInClient=GoogleSignIn.getClient(this,gso);

        auth=FirebaseAuth.getInstance();
    }

    public double randNum(int min,int max){
        return (Math.random()*max)+min;
    }

    public void updateLocation(Location location){
        if(location!=null && this.location!=null){
            this.location.child("lat").setValue(location.getLatitude());
            this.location.child("long").setValue(location.getLongitude());
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (permissions.length == 1 && permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gMap.setMyLocationEnabled(true);
                gMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        }
    }

    public void addDataListeners(){
        onDuty=database.getReference().child("users").child(userID).child("duty");
        onDuty.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean value=dataSnapshot.getValue(Boolean.class);
                if(value!=null) {
                    onDutyText.setText("On Duty: " + Boolean.toString(value));
                    duty.setChecked(value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        data=database.getReference().child("users").child(userID).child("data");
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String data=dataSnapshot.getValue(String.class);
                if(data!=null)
                    dataAdded.setText("Data Added: "+data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        location=database.getReference().child("users").child(userID).child("location");
        location.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Double value=dataSnapshot.getValue(Double.class);
                if(value!=null)
                    if(dataSnapshot.getKey().equals("lat"))
                        latText.setText("Latitude: "+value);
                    else longText.setText("Longitude: "+value);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Double value=dataSnapshot.getValue(Double.class);
                if(value!=null)
                    if(dataSnapshot.getKey().equals("lat"))
                        latText.setText("Latitude: "+value);
                    else longText.setText("Longitude: "+value);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                account = task.getResult(ApiException.class);
                userID = account.getId();
                signInScreen.setVisibility(View.GONE);
                emailText.setText(account.getEmail());
                userIDText.setText(userID);
                mainScreen.setVisibility(View.VISIBLE);
                addDataListeners();
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(MainActivity.this,"Google Sign In Failed",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void createAccount(String email,String password){
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    user = auth.getCurrentUser();
                    userID=user.getUid();
                    signInScreen.setVisibility(View.GONE);
                    emailText.setText(user.getEmail());
                    userIDText.setText(userID);
                    mainScreen.setVisibility(View.VISIBLE);
                    addDataListeners();
                }
            }
        });
    }

    public void signIn(String email,String password){
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    user = auth.getCurrentUser();
                    userID=user.getUid();
                    onDuty=database.getReference().child("users").child(userID).child("duty");
                    signInScreen.setVisibility(View.GONE);
                    emailText.setText(user.getEmail());
                    userIDText.setText(userID);
                    mainScreen.setVisibility(View.VISIBLE);
                    addDataListeners();
                }
            }
        });
    }
}
