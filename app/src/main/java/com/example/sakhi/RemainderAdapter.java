package com.example.sakhi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import java.util.List;

public class RemainderAdapter extends RecyclerView.Adapter<RemainderAdapter.ViewHolder> {

    private List<RemainderModel> remainderList;

    public RemainderAdapter(List<RemainderModel> remainderList) {
        this.remainderList = remainderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remainder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RemainderModel item = remainderList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(item.title);
        holder.tvTime.setText(item.repeat + " | " + item.time);

        // 1. Toggle Logic
        holder.switchToggle.setOnCheckedChangeListener(null);
        holder.switchToggle.setChecked(item.isActive);

        holder.switchToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isActive = isChecked;
            if (isChecked) {
                ReminderHelper.setAlarm(context, item);
                Toast.makeText(context, item.title + " Reminder ON", Toast.LENGTH_SHORT).show();
            } else {
                ReminderHelper.cancelAlarm(context, item.id);
                Toast.makeText(context, item.title + " Reminder OFF", Toast.LENGTH_SHORT).show();
            }
            saveListToPrefs(context);
        });

        // 2. Delete Logic
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                // Cancel the system alarm first
                ReminderHelper.cancelAlarm(context, item.id);

                // Remove from the list
                remainderList.remove(currentPosition);

                // Update the UI with animation
                notifyItemRemoved(currentPosition);
                notifyItemRangeChanged(currentPosition, remainderList.size());

                // Persist the change to SharedPreferences
                saveListToPrefs(context);

                Toast.makeText(context, "Reminder Deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveListToPrefs(Context context) {
        String json = new Gson().toJson(remainderList);
        context.getSharedPreferences("SakhiData", Context.MODE_PRIVATE)
                .edit()
                .putString("reminders", json)
                .apply();
    }

    @Override
    public int getItemCount() {
        return remainderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        SwitchCompat switchToggle;
        Button btnDelete; // Added Delete Button

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.itemTitle);
            tvTime = itemView.findViewById(R.id.itemTimeTag);
            switchToggle = itemView.findViewById(R.id.reminderToggle);
            btnDelete = itemView.findViewById(R.id.btnDelete); // ID from your updated XML
        }
    }
}