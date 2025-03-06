package io.xconn.securehome.maincontroller;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.DeviceAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;


public class DevicesFragment extends Fragment {

    private List<Device> deviceList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        // Initialize RecyclerView
        RecyclerView deviceRecyclerView = view.findViewById(R.id.deviceRecyclerView);
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceAdapter = new DeviceAdapter(deviceList, requireContext());
        deviceRecyclerView.setAdapter(deviceAdapter);

        // Initialize FAB and set click listener
        FloatingActionButton addDeviceFab = view.findViewById(R.id.addDeviceFab);
        addDeviceFab.setOnClickListener(v -> addNewDevice());

        return view;
    }

    private void addNewDevice() {
        // Add a new device with default configurations
        Device newDevice = new Device("Device " + (deviceList.size() + 1), false);
        deviceList.add(newDevice);
        deviceAdapter.notifyItemInserted(deviceList.size() - 1);
    }
}