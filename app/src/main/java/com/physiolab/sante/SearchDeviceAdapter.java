package com.physiolab.sante;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.physiolab.sante.santemulti.MainTestActivity;
import com.physiolab.sante.santemulti.databinding.SearchDeviceItemBinding;

import java.util.ArrayList;
import java.util.HashSet;

public class SearchDeviceAdapter  extends RecyclerView.Adapter<SearchDeviceAdapter.ViewHolder> {

    private ArrayList<BluetoothDevice> deviceList;

    public interface ItemClick {
        void onClick(BluetoothDevice bluetoothDevice);
    }

    private ItemClick itemClick;

    public SearchDeviceAdapter(ItemClick itemClick) {
        deviceList = new ArrayList<>();
        this.itemClick = itemClick;
    }

    public void addItem(BluetoothDevice bluetoothDevice) {
        HashSet<BluetoothDevice> hashSet = new HashSet<>(deviceList);
        hashSet.add(bluetoothDevice);
        deviceList = new ArrayList<>(hashSet);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SearchDeviceItemBinding binding = SearchDeviceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SearchDeviceAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchDeviceAdapter.ViewHolder holder, int position) {
        holder.holderBinding.txvAddress.setText(deviceList.get(position).getName());
        holder.holderBinding.txvName.setText(deviceList.get(position).getAddress());
        holder.holderBinding.deviceItemParent.setOnClickListener( v ->
                itemClick.onClick(deviceList.get(position))
        );
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final SearchDeviceItemBinding holderBinding;

        public ViewHolder(@NonNull SearchDeviceItemBinding holderBinding) {
            super(holderBinding.getRoot());
            this.holderBinding = holderBinding;
        }
    }
}
