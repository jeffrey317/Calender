package com.example.calendar.ui.meals;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.calendar.R;
import java.util.Calendar;
import java.util.Date;

public class DateSearchDialog extends DialogFragment {
    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_date_search, null);
        DatePicker datePicker = view.findViewById(R.id.datePicker);

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("確定", (dialog, which) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    if (listener != null) {
                        listener.onDateSelected(calendar.getTime());
                    }
                })
                .setNegativeButton("取消", null)
                .create();
    }
} 