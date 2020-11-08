package com.enroute.lasttake;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ManagerOrders extends Fragment {

    private View view;

    private RecyclerView ordersRecycler;
    private ArrayList<DeliveryOrder> orders=new ArrayList<>();
    private FirebaseDatabase database;
    private DatabaseReference databaseOrders;
    private int greenText,redText;

    private FloatingActionButton addOrder;

    private Global global;

    private TextView noOrders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.manager_orders,container,false);

        global =((Global)getContext().getApplicationContext());

        ordersRecycler=view.findViewById(R.id.id_managerOrdersRecycler);
        ordersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersRecycler.setAdapter(new ManagerDeliveryOrderAdapter(getContext()));
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        addOrder=view.findViewById(R.id.id_addOrder);
        addOrder.setOnClickListener(v -> {
            View view=inflater.inflate(R.layout.add_order_dialog,container,false);
            TextInputLayout address=view.findViewById(R.id.id_address);
            TextInputLayout phoneNumber=view.findViewById(R.id.id_phoneNumber);
            TextInputLayout orderID=view.findViewById(R.id.id_orderID);
            Button createOrder=view.findViewById(R.id.id_createOrder);
            Button cancel=view.findViewById(R.id.id_cancel);

            AlertDialog orderDialog =new AlertDialog.Builder(getContext())
                    .setTitle("Create a new Delivery Order")
                    .setView(view)
                    .create();

            createOrder.setOnClickListener(v12 -> {
                boolean invalid=false;
                DeliveryOrder newOrder=new DeliveryOrder(address.getEditText().getText().toString(),"+1"+phoneNumber.getEditText().getText().toString(),orderID.getEditText().getText().toString(),false,"None");
                if(orders.contains(newOrder) || newOrder.getOrderID().toLowerCase().equals("None")) {
                    orderID.setError("An order by this ID already exists");
                    invalid=true;
                }else orderID.setError(null);
                if(newOrder.getPhoneNumber().length()!=12){
                    phoneNumber.setError("Phone number length is invalid");
                    invalid=true;
                }else phoneNumber.setError(null);
                if(newOrder.getDestinationName().length()==0){
                    address.setError("Field can't be empty");
                    invalid=true;
                }else address.setError(null);
                if(!invalid) {
                    orders.add(newOrder);
                    updateDatabaseOrders();
                    orderDialog.dismiss();
                }
            });

            cancel.setOnClickListener(v1 -> orderDialog.cancel());

            orderDialog.show();
        });

        if(global.getColorBlind().equals("normal")){
            greenText=getResources().getColor(R.color.greenTextNormal);
            redText=getResources().getColor(R.color.redTextNormal);
        }else if(global.getColorBlind().equals("prot")){
            greenText=getResources().getColor(R.color.greenTextProt);
            redText=getResources().getColor(R.color.redTextProt);
        }else if(global.getColorBlind().equals("deut")){
            greenText=getResources().getColor(R.color.greenTextDeut);
            redText=getResources().getColor(R.color.redTextDeut);
        }

        return view;
    }

    private void updateDatabaseOrders(){
        databaseOrders.setValue(orders);
    }

    public class ManagerDeliveryOrderAdapter extends RecyclerView.Adapter<ManagerDeliveryOrderAdapter.MyViewHolder>{
        Context context;
        ManagerDeliveryOrderAdapter(Context context){
            this.context=context;
        }

        @NonNull
        @Override
        public ManagerDeliveryOrderAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.manager_orders_layout,parent,false);
            return new ManagerDeliveryOrderAdapter.MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ManagerDeliveryOrderAdapter.MyViewHolder holder, int position) {
            DeliveryOrder o=orders.get(position);
            holder.orderID.setText(o.getOrderID().equals("") ? "NONE" : "Order ID: "+o.getOrderID());
            holder.destination.setText("Destination: "+o.getDestinationName());
            holder.phoneNumber.setText("Customer Phone Number: "+o.getPhoneNumber());
            holder.eta.setText("ETA: "+o.getEta());
            holder.inProgress.setText(o.isInProgress() ? "In Progress" : "Not Accepted");
            holder.inProgress.setTextColor(o.isInProgress() ? greenText : redText);
            holder.cancelOrder.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Order")
                        .setMessage("Are you sure you want to delete this order? "+(o.isInProgress()?"A driver is currently delivering this order. ":"")+"This is irreversible.")
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            orders.remove(position);
                            updateDatabaseOrders();
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            TextView orderID,destination,phoneNumber,inProgress,eta;
            Button cancelOrder;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);
                orderID=itemView.findViewById(R.id.adapter_id_orderID);
                destination=itemView.findViewById(R.id.adapter_id_destination);
                phoneNumber=itemView.findViewById(R.id.adapter_id_phoneNumber);
                inProgress=itemView.findViewById(R.id.adapter_id_orderInProgress);
                eta=itemView.findViewById(R.id.adapter_id_eta);
                cancelOrder=itemView.findViewById(R.id.adapter_id_cancelOrder);
            }
        }
    }
}
