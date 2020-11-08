package com.enroute.lasttake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ManagerContainer extends Fragment {

    BottomNavigationView navigationView;
    Fragment newFragment;
    Fragment currentFragment;
    FragmentManager manager;
    TextView businessID;
    Global global;
    FloatingActionButton settings;
    FirebaseDatabase database;
    String name,address;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.manager_container,container,false);

        global =((Global)getContext().getApplicationContext());

        ManagerTracking managerTracking=new ManagerTracking();
        ManagerOrders managerOrders=new ManagerOrders();
        ManagerDriverList managerDriverList=new ManagerDriverList();

        navigationView=view.findViewById(R.id.id_managerNavView);
        manager=getActivity().getSupportFragmentManager();
        manager.beginTransaction().add(R.id.id_manager_layout,managerOrders,"2").hide(managerOrders).commit();
        manager.beginTransaction().add(R.id.id_manager_layout,managerDriverList,"3").hide(managerDriverList).commit();
        manager.beginTransaction().add(R.id.id_manager_layout,managerTracking,"1").commit();
        currentFragment=managerTracking;
        navigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId()){
                case R.id.manager_nav_track_drivers:
                    newFragment=managerTracking;
                    break;
                case R.id.manager_nav_orders:
                    newFragment=managerOrders;
                    break;
                case R.id.manager_nav_driver_list:
                    newFragment=managerDriverList;
                    break;
            }
            manager.beginTransaction().hide(currentFragment).show(newFragment).commit();
            currentFragment=newFragment;
            return true;
        });

        navigationView.setSelectedItemId(R.id.manager_nav_track_drivers);

        database=FirebaseDatabase.getInstance();

        businessID=view.findViewById(R.id.id_managerBusinessID);
        businessID.setText("Business ID: "+ global.getBusinessID());

        database.getReference().child("buisnesses").child(global.getBusinessID()).child("business_name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                name=dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        database.getReference().child("buisnesses").child(global.getBusinessID()).child("business_address").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                address=dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        settings=view.findViewById(R.id.id_manager_settings);
        settings.setOnClickListener(v -> {
            View layout=inflater.inflate(R.layout.manager_settings,container,false);
            Button signOut=layout.findViewById(R.id.id_sign_out);
            Button save=layout.findViewById(R.id.id_save);
            Button cancel=layout.findViewById(R.id.id_cancel);

            TextInputLayout name=layout.findViewById(R.id.id_businessName_setting);
            TextInputLayout address=layout.findViewById(R.id.id_businessAddress_setting);

            name.getEditText().setText(this.name);
            address.getEditText().setText(this.address);

            RadioGroup colors=layout.findViewById(R.id.id_color_options);
            if(global.getColorBlind().equals("normal"))
                colors.check(R.id.id_color_normal);
            else if(global.getColorBlind().equals("prot"))
                colors.check(R.id.id_color_prot);
            else if(global.getColorBlind().equals("deut"))
                colors.check(R.id.id_color_deut);

            AlertDialog settingsDialog =new AlertDialog.Builder(getContext())
                    .setTitle("Settings")
                    .setView(layout)
                    .create();

            signOut.setOnClickListener(v1 -> {
                MainActivity.signOut();
                settingsDialog.dismiss();
            });

            cancel.setOnClickListener(v12 -> settingsDialog.cancel());

            save.setOnClickListener(v13 -> {
                boolean valid=true;
                if(name.getEditText().getText().length()==0){
                    name.setError("Field can't be empty");
                    valid=false;
                }else name.setError(null);

                if (address.getEditText().getText().length()==0){
                    address.setError("Field can't be empty");
                    valid=false;
                }else address.setError(null);

                if(valid){
                    this.name=name.getEditText().getText().toString();
                    this.address=address.getEditText().getText().toString();
                    saveNameAndAddress(name.getEditText().getText().toString(),address.getEditText().getText().toString());

                    if(colors.getCheckedRadioButtonId()==R.id.id_color_normal){
                        global.setColorBlind("normal");
                    }else if(colors.getCheckedRadioButtonId()==R.id.id_color_prot){
                        global.setColorBlind("prot");
                    }else if(colors.getCheckedRadioButtonId()==R.id.id_color_deut){
                        global.setColorBlind("deut");
                    }
                    getFragmentManager().beginTransaction().detach(this).attach(this).commit();
                    settingsDialog.dismiss();
                }
            });
            settingsDialog.show();
        });

        return view;
    }

    private void saveNameAndAddress(String name,String address){
        database.getReference().child("businesses").child(global.getBusinessID()).child("business_name").setValue(name);
        database.getReference().child("businesses").child(global.getBusinessID()).child("business_address").setValue(address);
    }
}
