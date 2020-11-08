package com.enroute.design;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ManagerUI extends Fragment implements OnMapReadyCallback{

    ArrayList<Driver> driverList=new ArrayList<>();
    RecyclerView drivers;
    private View view;
    private MapView mapView;
    private ImageView openList,driverInfoImage;
    private LinearLayout driverInfo;
    private TextView driverInfoName,driverInfoOnDuty;
    private Button driverInfoExit;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.manager,container,false);

        for(int x=0;x<16;x++)
            driverList.add(new Driver("Driver "+(x+1),(BitmapDrawable)getResources().getDrawable(R.drawable.ic_default_pfp),x<8));

        drivers=view.findViewById(R.id.id_driverList);
        drivers.setAdapter(new CustomAdapter(getContext()));
        drivers.setLayoutManager(new GridLayoutManager(getContext(),4));
        drivers.addItemDecoration(new SpacesItemDecoration(10));

        mapView=view.findViewById(R.id.id_managerMap);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        openList=view.findViewById(R.id.id_openDriverList);
        LinearLayout layout=view.findViewById(R.id.id_bottomSheet);
        final BottomSheetBehavior sheetBehavior=BottomSheetBehavior.from(layout);
        openList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sheetBehavior.getState()==BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    openList.setImageDrawable(getResources().getDrawable(R.drawable.ic_down_arrow));
                }else{
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    openList.setImageDrawable(getResources().getDrawable(R.drawable.ic_up_arrow));
                }

            }
        });

        driverInfo=view.findViewById(R.id.id_driverInfo);
        driverInfoName=view.findViewById(R.id.id_driverInfoName);
        driverInfoExit=view.findViewById(R.id.id_driverInfoExit);
        driverInfoImage=view.findViewById(R.id.id_driverInfoImage);
        driverInfoOnDuty=view.findViewById(R.id.id_driverInfoOnDuty);
        driverInfoExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driverInfo.setVisibility(View.GONE);
                drivers.setVisibility(View.VISIBLE);
            }
        });


        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }


    class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.DriverViewHolder>{

        Context context;

        CustomAdapter(Context context){
            this.context=context;
        }

        @NonNull
        @Override
        public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=LayoutInflater.from(context).inflate(R.layout.driver_adapter_layout,parent,false);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    driverInfoName.setText(driverList.get(drivers.getChildAdapterPosition(v)).getName());
                    driverInfoImage.setImageDrawable(driverList.get(drivers.getChildAdapterPosition(v)).getImage());
                    driverInfoOnDuty.setText(driverList.get(drivers.getChildAdapterPosition(v)).onDuty()?"On Duty":"Off Duty");
                    driverInfoOnDuty.setTextColor(driverList.get(drivers.getChildAdapterPosition(v)).onDuty()?Color.parseColor("#00FF00"):Color.BLACK);
                    driverInfo.setVisibility(View.VISIBLE);
                    drivers.setVisibility(View.GONE);
                }
            });
            return new DriverViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
            holder.name.setText(driverList.get(position).getName());
            holder.image.setImageDrawable(driverList.get(position).getImage());
            holder.card.setStrokeColor(driverList.get(position).onDuty()? Color.parseColor("#00FF00"):Color.GRAY);
        }

        @Override
        public int getItemCount() {
            return driverList.size();
        }

        class DriverViewHolder extends RecyclerView.ViewHolder{

            MaterialCardView card;
            TextView name;
            ImageView image;

            DriverViewHolder(@NonNull View itemView) {
                super(itemView);
                name=itemView.findViewById(R.id.id_driverName);
                image=itemView.findViewById(R.id.id_driverImage);
                card=itemView.findViewById(R.id.id_driverCard);
            }
        }
    }

    class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
        }
    }
}

