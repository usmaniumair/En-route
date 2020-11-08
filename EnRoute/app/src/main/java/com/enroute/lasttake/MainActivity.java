package com.enroute.lasttake;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends FragmentActivity {

    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
    };

    private SharedPreferences preferences;
    private static Global global;

    static FirebaseAuth auth;
    private FirebaseUser user;
    private GoogleSignInAccount account;

    private static FragmentManager manager;

    @Override
    public void onStart() {
        super.onStart();

        account=GoogleSignIn.getLastSignedInAccount(this);
        if(account!=null){
            AuthCredential credential= GoogleAuthProvider.getCredential(account.getIdToken(),null);
            auth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if(task.isSuccessful())
                    global.setUserID(account.getId());
            });
        }

        user = auth.getCurrentUser();
        if(user!=null)
            global.setUserID(user.getUid());

        preferences=getSharedPreferences("EnRoutePreferences",0);
        if(global.getUserID()!=null){
            global.setBusinessID(preferences.getString("business_id_"+global.getUserID(),null));
            global.setAccountType(preferences.getString("account_type_"+global.getUserID(),null));
            global.setName(preferences.getString("name_"+global.getUserID(),null));

        }
        global.setColorBlind(preferences.getString("color_blind","normal"));
        savePreferences();

        if(global.getUserID()==null || global.getAccountType()==null || global.getBusinessID()==null){
            showUI(new Login());
        }else{
            //account already signed in
            if(global.getAccountType().equals("driver"))
                showUI(new DriverNavigation());
            else showUI(new ManagerContainer());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        savePreferences();
    }

    private void savePreferences(){
        if(global.getUserID()!=null){
            SharedPreferences.Editor editor=preferences.edit();
            editor.putString("business_id_"+global.getUserID(), global.getBusinessID());
            editor.putString("account_type_"+global.getUserID(), global.getAccountType());
            editor.putString("name_"+global.getUserID(), global.getName());
            editor.putString("color_blind",global.getColorBlind());
            editor.apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        global =((Global)getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        manager=getSupportFragmentManager();

        auth=FirebaseAuth.getInstance();

        Fabric.with(this, new Crashlytics());
    }

    public static void showUI(Fragment fragment){
        FragmentTransaction transaction=manager.beginTransaction();
        transaction.replace(R.id.id_layout,fragment);
        transaction.commit();
    }

    public static void signOut(){
        auth.signOut();
        showUI(new Login());
        global.clear();
    }

    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (String permission : REQUIRED_SDK_PERMISSIONS) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED)
                missingPermissions.add(permission);
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            String[] permissions = missingPermissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(1, REQUIRED_SDK_PERMISSIONS, grantResults);
        }
    }
}
