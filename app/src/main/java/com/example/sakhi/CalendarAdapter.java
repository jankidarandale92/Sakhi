package com.example.sakhi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.VH> {

    private final List<CalendarDay> days;
    private final SimpleDateFormat df = new SimpleDateFormat("d", Locale.US);

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CalendarDay d = days.get(pos);

        // 1. Handle Empty Slots (Padding for start of month)
        if (d.date == null) {
            h.tv.setText("");
            h.tv.setBackground(null); // Remove background for empty cells
            return;
        }

        // 2. Set the Date Text
        h.tv.setText(df.format(d.date));

        // 3. Apply Specific Backgrounds (Circles)
        if (d.isPeriod) {
            h.tv.setBackgroundResource(R.drawable.bg_day_period);
            h.tv.setTextColor(0xFFFFFFFF); // White text for better contrast
        } else if (d.isOvulation) {
            h.tv.setBackgroundResource(R.drawable.bg_day_ovulation);
            h.tv.setTextColor(0xFFFFFFFF);
        } else if (d.isFertile) {
            h.tv.setBackgroundResource(R.drawable.bg_day_fertile);
            h.tv.setTextColor(0xFF222222);
        } else if (d.isToday) {
            h.tv.setBackgroundResource(R.drawable.bg_day_today);
            h.tv.setTextColor(0xFFD81B60); // Pink text for today
        } else {
            // Default Circle
            h.tv.setBackgroundResource(R.drawable.bg_day_normal);
            h.tv.setTextColor(0xFF222222);
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(View v) {
            super(v);
            tv = v.findViewById(R.id.tvDay);
        }
    }
}