package com.smartattendance.creativeteam.teacherportal;

import  android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;

public class StudentViewHolder extends RecyclerView.ViewHolder {

    public TextView mId;
    public TextView mName;
    public ImageButton mAttendenceBtn;
    public TextView mTotal;
    public MaterialRippleLayout mDeleteBtn;
    public StudentViewHolder(@NonNull View itemView) {
        super(itemView);
        mId = (TextView)itemView.findViewById(R.id.id);
        mName = (TextView)itemView.findViewById(R.id.name);
        mAttendenceBtn = (ImageButton) itemView.findViewById(R.id.attendbtn);
        mTotal = (TextView)itemView.findViewById(R.id.total);
        mDeleteBtn = (MaterialRippleLayout)itemView.findViewById(R.id.deletebtn);
    }
}
