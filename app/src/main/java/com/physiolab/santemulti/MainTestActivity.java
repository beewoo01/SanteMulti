package com.physiolab.santemulti;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.physiolab.santemulti.databinding.ActivityMainTestBinding;
import com.physiolab.santemulti.databinding.SearchDeviceItemBinding;

import java.util.ArrayList;

public class MainTestActivity extends AppCompatActivity {

    private ActivityMainTestBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();

    }


    private void initView(){
        ArrayList<Pair<String, String>> arrayList = new ArrayList<>();
        arrayList.add(new Pair<>("1", "일번"));
        arrayList.add(new Pair<>("2", "이번"));
        arrayList.add(new Pair<>("1", "일번"));
        arrayList.add(new Pair<>("1", "일번"));
        arrayList.add(new Pair<>("1", "일번"));

        binding.deviceRe.setLayoutManager(new LinearLayoutManager(this));
        binding.deviceRe.setAdapter(new SearchDeviceAdapter(arrayList));
    }


    private class SearchDeviceAdapter extends RecyclerView.Adapter<SearchDeviceAdapter.ViewHolder>{

        private final ArrayList<Pair<String, String>> pairs;


        public SearchDeviceAdapter(ArrayList<Pair<String, String>> pairs) {
            this.pairs = pairs;
        }

        public void addList(){

        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            SearchDeviceItemBinding binding = SearchDeviceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.holderBinding.txvAddress.setText(pairs.get(position).first);
            holder.holderBinding.txvName.setText(pairs.get(position).second);
        }

        @Override
        public int getItemCount() {
            return pairs.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            private SearchDeviceItemBinding holderBinding;
            public ViewHolder(@NonNull SearchDeviceItemBinding holderBinding) {
                super(holderBinding.getRoot());
                this.holderBinding = holderBinding;
            }
        }
    }
}
