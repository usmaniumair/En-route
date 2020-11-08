package com.enroute.lasttake;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ManagerDriverList extends Fragment {

    private RecyclerView driverRecycler;
    private ArrayList<Driver> driverList=new ArrayList<>();
    private FirebaseDatabase database;
    private TextView noDrivers;

    private Global global;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.manager_driver_list,container,false);

        global =((Global)getContext().getApplicationContext());

        driverRecycler =view.findViewById(R.id.id_driver_list);
        driverRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        driverRecycler.setAdapter(new ManagerDriverListAdapter(getContext()));
        noDrivers=view.findViewById(R.id.id_no_drivers);

        database=FirebaseDatabase.getInstance();

        DatabaseReference deliveryDrivers=database.getReference().child("businesses").child(global.getBusinessID()).child("drivers");
        deliveryDrivers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                driverList.clear();
                for(DataSnapshot data:dataSnapshot.getChildren())
                    driverList.add(data.getValue(Driver.class));

                noDrivers.setVisibility(driverList.isEmpty()?View.VISIBLE:View.GONE);
                driverRecycler.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    public class ManagerDriverListAdapter extends RecyclerView.Adapter<ManagerDriverList.ManagerDriverListAdapter.MyViewHolder>{
        Context context;
        ManagerDriverListAdapter(Context context){
            this.context=context;
        }

        @NonNull
        @Override
        public ManagerDriverList.ManagerDriverListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.manager_driver_list_layout,parent,false);
            return new ManagerDriverList.ManagerDriverListAdapter.MyViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ManagerDriverList.ManagerDriverListAdapter.MyViewHolder holder, int position) {
            Driver driver=driverList.get(position);
            holder.name.setText("Name: "+driver.getName());
            holder.order.setText("Current Order: "+driver.getCurrentOrder().getOrderID());
        }

        @Override
        public int getItemCount() {
            return driverList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            TextView name,order;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                name=itemView.findViewById(R.id.adapter_id_driver_name);
                order=itemView.findViewById(R.id.adapter_id_current_order);
            }
        }
    }
}
