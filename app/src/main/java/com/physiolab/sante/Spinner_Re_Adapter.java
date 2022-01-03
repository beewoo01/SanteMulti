package com.physiolab.sante;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.physiolab.sante.santemulti.R;

import java.util.ArrayList;

public class Spinner_Re_Adapter  extends RecyclerView.Adapter<Spinner_Re_Adapter.ViewHolder> {

    private ArrayList<String> arrayList;

    public Spinner_Re_Adapter(ArrayList<String> arrayList) {
        this.arrayList = arrayList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addTime(String time){
        arrayList.add(time);
        notifyDataSetChanged();
        //notifyItemChanged(arrayList.size());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void removeAllItem() {
        arrayList.clear();
        notifyDataSetChanged();
    }

    public ArrayList<String> getItems(){
        return arrayList;
    }

    @NonNull
    @Override
    public Spinner_Re_Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_recyclerview_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(arrayList.get(position));
        if (arrayList.size()-1 == position){
            holder.view.setVisibility(View.INVISIBLE);
        }else holder.view.setVisibility(View.VISIBLE);
    }


    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        View view;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.record_txv);
            view = itemView.findViewById(R.id.record_line);
        }
    }
}

