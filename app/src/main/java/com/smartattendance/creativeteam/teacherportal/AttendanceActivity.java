package com.smartattendance.creativeteam.teacherportal;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;
import es.dmoral.toasty.Toasty;

public class AttendanceActivity extends AppCompatActivity {

    private String mDate;
    private String mReference;
    private String mTitle;
    private String mSection;
    private String mSemester;
    private String mCode;
    private Boolean mState;
    private int absent = 0;
    private int attend = 0;
    public Boolean instance = false;
    private EditText mDialogName;
    private EditText mDialogId;
    private TextView mInfoView;
    private TextView mClassInfo;
    private TextView mAttendView;
    private TextView mAbsentView;
    private TextView mBtnStatus;
    private ImageView mBtnIcon;
    private ProgressBar mProgressbar;
    private FirebaseAuth mAuth;
    private String mCurrentUID;
    private ConnectionCheck mNet;
    private ProgressDialog mLoadingDialog;
    private LinearLayout mNoItemView;
    private LinearLayout mStudentListBtn;
    private LinearLayout mStartBtn;
    private FirebaseUser mCurrentUser;
    public RecyclerView mRecycleView;
    public EasyLocationProvider easyLocationProvider;
    public int mButtonType = 0;

    private FloatingActionButton mAddStudent;
    private DatabaseReference mRootReference;
    private DatabaseReference mSateReference;
    private DatabaseReference mTotalDataReference;
    private ShimmerFrameLayout mShimmer;
    private ValueEventListener mSateListener = null;
    private ValueEventListener mTotalDataListener = null;

    public FirebaseRecyclerOptions<StudentModel> mOptions;
    public FirebaseRecyclerAdapter<StudentModel,StudentViewHolder> mAdapter;

    public WifiManager mWifiManager;
    public WifiP2pManager mManager;
    public WifiP2pManager.Channel mChannel;
    public BroadcastReceiver mReceiver;


    public IntentFilter mIntentFilter;
    public List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    public String[] deviceNameArray;
    public WifiP2pDevice[] deviceArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        mAuth = FirebaseAuth.getInstance();
        mNet = new ConnectionCheck(this);
        mCurrentUser = mAuth.getCurrentUser();
        mCurrentUID = mCurrentUser.getUid();
        mAddStudent = (FloatingActionButton)findViewById(R.id.fab_add);
        mNoItemView = (LinearLayout)findViewById(R.id.noitem);
        mRecycleView = (RecyclerView)findViewById(R.id.recycleview);
        mInfoView = (TextView)findViewById(R.id.infoid);
        mStudentListBtn = (LinearLayout)findViewById(R.id.studentlist);
        mShimmer = (ShimmerFrameLayout)findViewById(R.id.shimmerid);
        mClassInfo = (TextView)findViewById(R.id.classinfo);
        mAttendView = (TextView)findViewById(R.id.attend);
        mAbsentView = (TextView)findViewById(R.id.absent);
        mStartBtn = (LinearLayout)findViewById(R.id.startid);
        mBtnStatus = (TextView)findViewById(R.id.btnstatus);
        mBtnIcon = (ImageView)findViewById(R.id.btnicon);
        mProgressbar = (ProgressBar)findViewById(R.id.progress);
        mRootReference = FirebaseDatabase.getInstance().getReference();
        mRootReference.keepSynced(true);
        mDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        mWifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.setHasFixedSize(true);
        mNoItemView.setVisibility(View.VISIBLE);
        mInfoView.setVisibility(View.INVISIBLE);

        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager,mChannel,this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        fabbtn();
        getExtra();
        getLocation();
        auto_hide();
        setCalender();
        startAttendance();
        setloadingdialog();
        showstudentlist();
        checkConnectionState();

    }

    private void startAttendance() {
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mButtonType==0)
                {
                    if(mWifiManager.isWifiEnabled())
                    {
                        waitingforwifi();
                    }
                    else
                    {
                        mWifiManager.setWifiEnabled(true);
                        waitingforwifi();
                    }
                }
                else
                {
                    mLoadingDialog.show();
                    stopPeerDiscovery();
                }

            }
        });
    }
    private void waitingforwifi() {
        mLoadingDialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mWifiManager.isWifiEnabled())
                {
                    changeDeviceName();
                }

            }
        }, 1000);
    }
    private void changeDeviceName() {
        int numberOfParams = 3;
        Class[] methodParameters = new Class[numberOfParams];
        methodParameters[0] = WifiP2pManager.Channel.class;
        methodParameters[1] = String.class;
        methodParameters[2] = WifiP2pManager.ActionListener.class;

        Object arglist[] = new Object[numberOfParams];
        arglist[0] = mChannel;
        arglist[1] = mReference;
        arglist[2] = new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                startdicover();
            }

            public void onFailure(int reason) {
                String resultString = "Fail reason: " + String.valueOf(reason);
                Toast.makeText(getApplicationContext(), resultString,Toast.LENGTH_LONG).show();
            }
        };

        ReflectionUtils.executePrivateMethod(mManager,WifiP2pManager.class,"setDeviceName",methodParameters,arglist);

    }
    private void startdicover() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mProgressbar.setVisibility(View.VISIBLE);
                mButtonType = 1;
                mBtnStatus.setText("Stop");
                mBtnIcon.setImageResource(R.drawable.ic_close);
                mLoadingDialog.dismiss();
                Toasty.success(AttendanceActivity.this,"Attendance start!",Toasty.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                mLoadingDialog.dismiss();
                Toasty.error(AttendanceActivity.this,"Attendance start failed!",Toasty.LENGTH_SHORT).show();
            }
        });
    }
    private void stopPeerDiscovery(){
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mProgressbar.setVisibility(View.INVISIBLE);
                mButtonType = 0;
                mBtnStatus.setText("Start");
                mBtnIcon.setImageResource(R.drawable.ic_done);
                mLoadingDialog.dismiss();
                Toasty.success(AttendanceActivity.this,"Attendance stop!",Toasty.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                mLoadingDialog.dismiss();
                Toasty.error(AttendanceActivity.this,"Attendance stop failed!",Toasty.LENGTH_SHORT).show();
            }
        });
    }
    private void auto_hide() {

        mRecycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    animatefloatAction(true);
                } else if (dy < 0) {
                    animatefloatAction(false);
                }
            }
        });
    }
    private void checkConnectionState() {
        mSateReference= FirebaseDatabase.getInstance().getReference(".info/connected");
        mSateListener = mSateReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    mState = true;
                } else {
                    mState = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void showstudentlist() {
        mStudentListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAbsentView.setText("0");
                mAttendView.setText("0");
                removeTotalDataListner();
                checkStudentDate();
                mRecycleView.setVisibility(View.INVISIBLE);
                mStudentListBtn.setEnabled(false);
                mShimmer.startShimmer();
                mNoItemView.setVisibility(View.VISIBLE);
                mInfoView.setVisibility(View.INVISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mStudentListBtn.setEnabled(true);
                        totalAttendAndAbsent();
                        checkStudentExistOrNot();
                    }
                }, 3000);
            }
        });
    }
    private void removeTotalDataListner() {
        if(mTotalDataListener!=null)
        {
            mTotalDataReference.removeEventListener(mTotalDataListener);
        }
    }
    private void totalAttendAndAbsent() {

        mTotalDataReference = mRootReference.child("teacherattendance").child(mCurrentUID).child(mReference).child(mDate);
        mTotalDataListener = mTotalDataReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    absent = 0;
                    attend = 0;
                    for(DataSnapshot dataSnap : dataSnapshot.getChildren())
                    {
                        Boolean value = (Boolean) dataSnap.child("value").getValue();
                        if(!value)
                        {
                            absent++;
                        }
                        else
                        {
                            attend++;
                        }
                    }
                    mAbsentView.setText(Integer.toString(absent));
                    mAttendView.setText(Integer.toString(attend));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void checkStudentDate() {
        DatabaseReference reference = mRootReference.child("students").child(mCurrentUID).child(mReference);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    for(DataSnapshot dataSnap: dataSnapshot.getChildren())
                    {
                        String id = dataSnap.getKey();
                        setDefaultAttendance(id);
                    }
                }
                else
                {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void setDefaultAttendance(final String id) {
        DatabaseReference reference = mRootReference.child("teacherattendance").child(mCurrentUID).child(mReference).child(mDate).child(id);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists())
                {
                    saveAttendance(id);
                }
                else
                {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void saveAttendance(String id) {

        HashMap<String,Object> studentvalue = new HashMap<>();
        studentvalue.put("value",false);
        studentvalue.put("date",mDate);

        HashMap<String,Object> teachervalue = new HashMap<>();
        teachervalue.put("value",false);

        Map<String, Object> map = new HashMap<>();
        map.put("/studentattendance/" + mReference + "/" + id + "/" + "/" + mDate + "/", studentvalue);
        map.put("/teacherattendance/" + "/" + mCurrentUID + "/" + mReference + "/" + mDate + "/" + "/" + id + "/", teachervalue);


        mRootReference.updateChildren(map);
    }
    private void checkStudentExistOrNot() {
        DatabaseReference reference = mRootReference.child("students").child(mCurrentUID).child(mReference);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    checkStudentDate();
                    totalAttendAndAbsent();
                    mShimmer.stopShimmer();
                    loadstudentdata();
                }
                else
                {
                    mShimmer.stopShimmer();
                    mNoItemView.setVisibility(View.VISIBLE);
                    mInfoView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void loadstudentdata() {

        mNoItemView.setVisibility(View.INVISIBLE);
        mInfoView.setVisibility(View.INVISIBLE);
        mRecycleView.setVisibility(View.VISIBLE);

        DatabaseReference reference = mRootReference.child("teacherattendance").child(mCurrentUID).child(mReference).child(mDate);
        reference.keepSynced(true);
        mOptions = new FirebaseRecyclerOptions.Builder<StudentModel>()
                .setQuery(reference,StudentModel.class).build();
        mAdapter = new FirebaseRecyclerAdapter<StudentModel, StudentViewHolder>(mOptions) {
            @Override
            protected void onBindViewHolder(@NonNull StudentViewHolder holder, final int position, @NonNull StudentModel model) {
                final String key = getRef(position).getKey();

                checkAttendance(holder,model.getValue());

                getTotalAttendDay(key,holder);
                getStudentInformation(key,holder);
                holder.mDeleteBtn.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        String key = getRef(position).getKey();
                        showDeleteDialog(key);
                        return false;
                    }
                });
                holder.mAttendenceBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String key = getRef(position).getKey();
                        changeAttendance(key);
                    }
                });
            }

            @NonNull
            @Override
            public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.student_layout,viewGroup,false);
                return new StudentViewHolder(view);
            }
        };
        mAdapter.startListening();
        mRecycleView.setAdapter(mAdapter);

        instance = true;
    }
    private void checkAttendance(StudentViewHolder holder, Boolean value) {
        if(!value)
        {
            holder.mAttendenceBtn.setImageResource(R.drawable.ic_close);
        }
        else
        {
            holder.mAttendenceBtn.setImageResource(R.drawable.ic_done);
        }
    }
    private void getStudentInformation(String key, final StudentViewHolder holder) {
        DatabaseReference reference = mRootReference.child("students").child(mCurrentUID).child(mReference).child(key);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String id = dataSnapshot.child("id").getValue().toString();
                    holder.mName.setText(name);
                    holder.mId.setText(id);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    private void changeAttendance(final String key) {
        DatabaseReference reference = mRootReference.child("teacherattendance").child(mCurrentUID).child(mReference).child(mDate).child(key);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    Boolean value = (Boolean) dataSnapshot.child("value").getValue();
                    if(!value)
                    {
                        changeSave(key,true);
                    }
                    else
                    {
                        changeSave(key,false);
                    }
                }
                else
                {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void changeSave(String key, boolean b) {
        HashMap<String,Object> studentvalue = new HashMap<>();
        studentvalue.put("value",b);
        studentvalue.put("date",mDate);

        HashMap<String,Object> teachervalue = new HashMap<>();
        teachervalue.put("value",b);

        Map<String, Object> map = new HashMap<>();
        map.put("/studentattendance/" + mReference + "/" + key+ "/" + "/" + mDate + "/", studentvalue);
        map.put("/teacherattendance/" + "/" + mCurrentUID + "/" + mReference + "/" + mDate + "/" + "/" + key + "/", teachervalue);


        mRootReference.updateChildren(map);
    }
    private void getTotalAttendDay(String key, final StudentViewHolder holder) {
        DatabaseReference reference = mRootReference.child("studentattendance").child(mReference).child(key);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    int count = 0;
                    for(DataSnapshot snapshot : dataSnapshot.getChildren())
                    {
                        Boolean value = (Boolean) snapshot.child("value").getValue();
                        if(!value)
                        {
                        }
                        else
                        {
                            count++;
                        }
                    }
                    holder.mTotal.setText(Integer.toString(count));
                }
                else
                {
                    holder.mTotal.setText("0");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    private void getExtra() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mReference = extras.getString("ref");
            mTitle = extras.getString("title");
            mSection = extras.getString("section");
            mSemester = extras.getString("semester");
            mCode = extras.getString("code");
            mClassInfo.setText(mSection+" "+mCode);
        }
    }
    private void fabbtn() {
        mAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                studentAddDialog();
            }
        });
    }
    private void studentAddDialog() {
        mDialogName = null;
        mDialogId = null;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_add_student);
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        mDialogName = (EditText)dialog.findViewById(R.id.name);
        mDialogId = (EditText)dialog.findViewById(R.id.id);
        final LinearLayout scan = (LinearLayout)dialog.findViewById(R.id.scanqr);
        ImageButton mCloseBtn = dialog.findViewById(R.id.bt_close);
        Button mSaveBtn = dialog.findViewById(R.id.bt_save);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanQrCode();
            }
        });

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState)
                {
                    String name = mDialogName.getText().toString();
                    String id = mDialogId.getText().toString().trim();
                    if(!name.equals("")&&!id.equals(""))
                    {
                        createStudent(name,id);
                        dialog.dismiss();
                        mLoadingDialog.show();

                    }
                    else
                    {
                        Toasty.warning(AttendanceActivity.this,"Please fill in required fields",Toasty.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toasty.error(AttendanceActivity.this,"No Internet",Toasty.LENGTH_SHORT).show();
                }

            }
        });

        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }
    private void createStudent(final String name, final String id) {
        DatabaseReference reference = mRootReference.child("students").child(mCurrentUID).child(mReference).child(id);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists())
                {
                    savedata(name,id);
                }
                else
                {
                    mLoadingDialog.dismiss();
                    Toasty.warning(AttendanceActivity.this,"Student already exits !",Toasty.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    private void savedata(String name, final String id) {

        DatabaseReference reference = mRootReference.child("students").child(mCurrentUID).child(mReference).child(id);
        HashMap<String,String> stuentvalue = new HashMap<>();
        stuentvalue.put("name",name);
        stuentvalue.put("id",id);

        HashMap<String,String> classvalue = new HashMap<>();
        classvalue.put("section",mSection);
        classvalue.put("code",mCode);
        classvalue.put("title",mTitle);
        classvalue.put("semester",mSemester);
        classvalue.put("date",mDate);

        Map<String, Object> map = new HashMap<>();
        map.put("/students/" + mCurrentUID + "/"+ mReference + "/" + id + "/", stuentvalue);
        map.put("/studentclasses/" + id  + "/" + mReference + "/" , classvalue);

        mRootReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    mLoadingDialog.dismiss();
                    Toasty.success(AttendanceActivity.this,"New student added !",Toasty.LENGTH_SHORT).show();
                }
                else
                {
                    mLoadingDialog.dismiss();
                    Toasty.error(AttendanceActivity.this,"Something went wrong !",Toasty.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void scanQrCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(Potrait.class);
        integrator.setOrientationLocked(true);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan Code");
        integrator.initiateScan();
    }
    private void setCalender() {
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.MONTH, -1);
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 1);
        HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(this, R.id.calendarView)
                .range(startDate,endDate)
                .datesNumberOnScreen(5)
                .build();
        horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Calendar calendar, int position) {
                //do something
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                mDate = format.format(calendar.getTime());
            }
        });
    }
    private void showDeleteDialog(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to Delete?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(mState)
                        {
                            mLoadingDialog.show();
                            deleteStudent(key);
                        }
                        else
                        {
                            Toasty.error(AttendanceActivity.this,"No Internet",Toasty.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
    private void deleteStudent(final String key) {
        Map<String, Object> map = new HashMap<>();
        map.put("/students/" + mCurrentUID + "/"+ mReference + "/" + key + "/", null);
        map.put("/studentattendance/" + mReference + "/" + key + "/" , null);
        map.put("/studentclasses/" + key  + "/" + mReference + "/" , null);
        map.put("/teacherattendance/" + "/" + mCurrentUID + "/" + mReference + "/" + mDate + "/" + "/" + key + "/", null);

        mRootReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    mLoadingDialog.dismiss();
                    Toasty.success(AttendanceActivity.this,"Student deleted !",Toasty.LENGTH_SHORT).show();
                    deleteteacherattendance(key);
                }
                else
                {
                    mLoadingDialog.dismiss();
                    Toasty.error(AttendanceActivity.this,"Something went wrong !",Toasty.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void deleteteacherattendance(final String key) {
        final DatabaseReference reference = mRootReference.child("teacherattendance").child(mCurrentUID).child(mReference);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    for(DataSnapshot dataSnap : dataSnapshot.getChildren())
                    {
                        if(dataSnap.hasChild(key))
                        {
                            reference.child(dataSnap.getKey()).child(key).removeValue();
                        }
                    }
                }
                else
                {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void setloadingdialog() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage("Please wait...");
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setCancelable(false);
    }
    private void getLocation() {

        easyLocationProvider = new EasyLocationProvider.Builder(AttendanceActivity.this)
                .setInterval(5000)
                .setFastestInterval(2000)
                //.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setListener(new EasyLocationProvider.EasyLocationCallback() {
                    @Override
                    public void onGoogleAPIClient(GoogleApiClient googleApiClient, String message) {

                    }

                    @Override
                    public void onLocationUpdated(double latitude, double longitude) {

                    }

                    @Override
                    public void onLocationUpdateRemoved() {

                    }
                }).build();

        getLifecycle().addObserver(easyLocationProvider);
    }
    boolean isFloatActionBtn = false;
    private void animatefloatAction(final boolean hide) {
        if (isFloatActionBtn && hide || !isFloatActionBtn && !hide) return;
        isFloatActionBtn = hide;
        int moveY = hide ? (2 * mAddStudent.getHeight()) : 0;
        mAddStudent.animate().translationY(moveY).setStartDelay(100).setDuration(300).start();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {

            } else {
                // Get String data from Intent

                String returnString = result.getContents();
                if(returnString!=null)
                {
                    if(returnString.contains("_"))
                    {
                        String[] separated = returnString.split("_");
                        String code = separated[0];
                        if(code.equals("Jam"))
                        {
                            String id = separated[1];
                            String name = separated[2];

                            mDialogName.setText(name);
                            mDialogId.setText(id);
                        }

                    }

                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    public void onStop() {
        if(mAdapter!=null)
        {
            mAdapter.stopListening();
        }
        super.onStop();
    }
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
        if(mAdapter!=null)
        {
            mAdapter.startListening();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        easyLocationProvider.removeUpdates();
        getLifecycle().removeObserver(easyLocationProvider);
        removeStateListner();
    }
    @Override
    protected void onPause() {
        super.onPause();
        removeStateListner();
        unregisterReceiver(mReceiver);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        removeTotalDataListner();
        removeStateListner();
    }
    private void removeStateListner() {
        if(mSateListener!=null)
        {
            mSateReference.removeEventListener(mSateListener);
        }
    }
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers))
            {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                //[Phone]GG

                for(WifiP2pDevice device : peerList.getDeviceList())
                {
                    if(device.deviceName.contains("]"))
                    {
                        String[] separated = device.deviceName.split("]");
                        String id = separated[1];
                        setattendance(id.trim());
                    }
                    else
                    {
                        setattendance(device.deviceName.trim());
                    }
                }

                if(peers.size()==0)
                {

                }

            }
        }
    };

    private void setattendance(final String id) {
        DatabaseReference reference = mRootReference.child("teacherattendance").child(mCurrentUID).child(mReference).child(mDate).child(id);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    changeSave(id,true);
                }
                else
                {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


}
