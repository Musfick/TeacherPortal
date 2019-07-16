package com.smartattendance.creativeteam.teacherportal;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import android.Manifest;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import es.dmoral.toasty.Toasty;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ArrayAdapter<String> mSemesterAdapter;
    private ArrayAdapter<String> mCourseCodeAdapter;
    private ArrayAdapter<String> mCourseTitleAdapter;
    private String[] mSemesterList;
    private String[] mCourseCodeList;
    private String[] mCourseTitleList;
    private String[] mSectionList;
    private String mCurrentUID;
    private String mCurrentDate;
    private TextView mInfo;
    private TextView mProfileEmail;
    private TextView mProfileName;
    private RecyclerView mRecycleView;
    private LinearLayout mLogoutBtn;
    private LinearLayout mCreateClass;
    private LinearLayout mNoItemView;
    private ProgressDialog mLoadingDialog;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mRootReference;
    private CircularImageView mProfileImage;
    private SwipeRefreshLayout mSwipeRefresh;
    private ShimmerFrameLayout mShimmer;
    private ConnectionCheck mNet;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseRecyclerOptions<ClassModel> options;
    private FirebaseRecyclerAdapter<ClassModel, ClassViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mAuth = FirebaseAuth.getInstance();
        mNet = new ConnectionCheck(this);
        mCurrentUser =  mAuth.getCurrentUser();
        mCurrentUID = mCurrentUser.getUid();
        mInfo = (TextView)findViewById(R.id.infoid);
        mProfileEmail = (TextView)findViewById(R.id.profileid);
        mProfileName = (TextView)findViewById(R.id.profilename);
        mLogoutBtn = (LinearLayout)findViewById(R.id.logoutbtn);
        mCreateClass = (LinearLayout)findViewById(R.id.createclassid);
        mNoItemView = (LinearLayout)findViewById(R.id.noitem);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mShimmer = (ShimmerFrameLayout)findViewById(R.id.shimmerid);
        mSwipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipeid);
        mProfileImage = (CircularImageView)findViewById(R.id.profileimage);
        mRecycleView = (RecyclerView)findViewById(R.id.recycleview);

        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.setHasFixedSize(true);

        mSemesterList = getResources().getStringArray(R.array.semester_list);
        mCourseCodeList = getResources().getStringArray(R.array.coursecode_list);
        mCourseTitleList = getResources().getStringArray(R.array.coursetitle_list);
        mSectionList = getResources().getStringArray(R.array.section_list);

        mSemesterAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mSemesterList);
        mCourseCodeAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mCourseCodeList);
        mCourseTitleAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mCourseTitleList);

        mRootReference = FirebaseDatabase.getInstance().getReference();
        mCurrentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());



        logutBtn();
        createBtn();
        swiperefresh();
        checkAuthState();
        takepermission();
        setloadingdialog();
        setUserInformation();
        checkclassExistOrNot();
    }

    private void checkclassExistOrNot() {
        DatabaseReference reference = mRootReference.child("teacherclasses").child(mCurrentUID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {

                    mNoItemView.setVisibility(View.INVISIBLE);
                    mSwipeRefresh.setRefreshing(false);
                    loadRecycleviewData();
                }
                else
                {

                    noitemanimation();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void loadRecycleviewData() {
        mRecycleView.setVisibility(View.VISIBLE);
        DatabaseReference reference = mRootReference.child("teacherclasses").child(mCurrentUID);
        options = new FirebaseRecyclerOptions.Builder<ClassModel>().setQuery(reference,ClassModel.class).build();
        adapter = new FirebaseRecyclerAdapter<ClassModel, ClassViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ClassViewHolder holder, final int position, @NonNull final ClassModel model) {
                holder.mTitle.setText(model.getTitle());
                holder.mSection.setText(model.getSection());
                holder.mSemester.setText(model.getSemester());
                holder.mCode.setText(model.getCode());

                holder.mRipple.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String ref = getRef(position).getKey();
                        String title = model.getTitle();
                        String section = model.getSection();
                        String semester = model.getSemester();
                        String code = model.getCode();
                        attendanceActivity(ref,title,section,semester,code);
                    }
                });

                if (position%4 == 0){
                    holder.mCardView.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.random1));
                } else if (position%4 == 1){
                    holder.mCardView.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.random2));
                } else if (position%4 == 2){
                    holder.mCardView.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.random3));
                } else if (position%4 == 3){
                    holder.mCardView.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.random4));
                }
            }

            @NonNull
            @Override
            public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.class_layout,viewGroup,false);
                return new ClassViewHolder(view);
            }
        };
        adapter.startListening();
        mRecycleView.setAdapter(adapter);

    }

    private void attendanceActivity(String ref, String title, String section, String semester, String code) {
        Intent intent = new Intent(HomeActivity.this,AttendanceActivity.class);
        intent.putExtra("ref", ref);
        intent.putExtra("title", title);
        intent.putExtra("section", section);
        intent.putExtra("semester", semester);
        intent.putExtra("code", code);
        startActivity(intent);
    }

    private void noitemanimation() {
        mNoItemView.setVisibility(View.VISIBLE);
        mShimmer.startShimmer();
        mInfo.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mShimmer.stopShimmer();
                mInfo.setVisibility(View.VISIBLE);
                mSwipeRefresh.setRefreshing(false);
            }
        }, 3000);
    }
    private void createBtn() {
        mCreateClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateClassDialog();
            }
        });
    }
    private void showCreateClassDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_create_class);
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final TextView mSectionView = dialog.findViewById(R.id.sectionview);
        ImageButton mCloseBtn = dialog.findViewById(R.id.bt_close);
        ImageButton mSection = dialog.findViewById(R.id.section);
        Button mSaveBtn = dialog.findViewById(R.id.bt_save);
        final AutoCompleteTextView mSemester = dialog.findViewById(R.id.semester);
        final AutoCompleteTextView mCourseCode = dialog.findViewById(R.id.coursecode);
        final AutoCompleteTextView mCourseTitle = dialog.findViewById(R.id.coursetitle);

        mSemester.setAdapter(mSemesterAdapter);
        mCourseCode.setAdapter(mCourseCodeAdapter);
        mCourseTitle.setAdapter(mCourseTitleAdapter);

        mSemester.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSemester.onEditorAction(EditorInfo.IME_ACTION_DONE);
            }
        });
        mCourseCode.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCourseCode.onEditorAction(EditorInfo.IME_ACTION_DONE);
            }
        });
        mCourseTitle.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCourseTitle.onEditorAction(EditorInfo.IME_ACTION_DONE);
            }
        });

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNet.isConnected())
                {
                    String section = mSectionView.getText().toString().trim();
                    String semester = mSemester.getText().toString();
                    String code = mCourseCode.getText().toString();
                    String title = mCourseTitle.getText().toString();
                    if(!section.equals("Select section")&&!semester.equals("")&&!code.equals("")&&!title.equals(""))
                    {
                        createClass(section,code,title,semester);
                        dialog.cancel();
                        mLoadingDialog.show();
                    }
                    else
                    {
                        Toasty.warning(HomeActivity.this,"Please fill in required fields",Toasty.LENGTH_SHORT).show();
                    }

                }
                else
                {
                    Toasty.error(HomeActivity.this,"No Internet",Toasty.LENGTH_SHORT).show();
                }

            }
        });

        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        mSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(HomeActivity.this);
                builder.setCancelable(true);
                builder.setSingleChoiceItems(mSectionList, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mSectionView.setTextColor(Color.BLACK);
                        mSectionView.setText(mSectionList[which]);
                    }
                });
                builder.show();

            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }
    private void createClass(final String section, final String code, final String title, final String semester) {

        final String rootkey = section+code.trim();
        DatabaseReference reference = mRootReference.child("classes").child(mCurrentUID).child(rootkey);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists())
                {
                    saveclass(section,code,title,semester,rootkey);
                }
                else
                {
                    showwaringdialog();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    private void saveclass(String section, String code, String title, String semester, String rootkey) {

        DatabaseReference reference = mRootReference.child("teacherclasses").child(mCurrentUID).child(rootkey);
        HashMap<String,String> map = new HashMap<>();
        map.put("section",section);
        map.put("code",code);
        map.put("title",title);
        map.put("semester",semester);
        map.put("date",mCurrentDate);

        reference.setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    mLoadingDialog.dismiss();
                    showsuccessDialog();
                }
                else
                {
                    mLoadingDialog.dismiss();
                }
            }
        });

    }
    private void showsuccessDialog() {
        SweetAlertDialog mDialog;
        mDialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        mDialog.setTitle("Successfull");
        mDialog.setContentText("Class has been created!");
        mDialog.setCancelable(true);
        mDialog.show();

    }
    private void showwaringdialog() {
        mLoadingDialog.dismiss();
        SweetAlertDialog mDialog;
        mDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        mDialog.setTitleText("Oops!");
        mDialog.setContentText("Class already exist!");
        mDialog.setCancelable(true);
        mDialog.show();
    }
    private void setloadingdialog() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setTitle("Please wait");
        mLoadingDialog.setMessage("Creating new class...");
        mLoadingDialog.setIndeterminate(true);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }
    private void swiperefresh() {
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefresh.setRefreshing(true);
                checkclassExistOrNot();

            }
        });
    }

    private void setUserInformation() {
        if(mCurrentUser!=null)
        {
            String name = mCurrentUser.getDisplayName();
            String email = mCurrentUser.getEmail();
            Uri photoUrl = mCurrentUser.getPhotoUrl();
            mProfileName.setText(name);
            mProfileEmail.setText(email);
            Picasso.get().load(photoUrl).placeholder(R.drawable.avatar).into(mProfileImage);
        }
    }
    private void logutBtn() {
        mLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
    }
    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        signOut();
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
    private void signOut() {
        GoogleSignInClient mGoogleSignInClient ;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getBaseContext(), gso);
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {  //signout Google
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseAuth.getInstance().signOut(); //signout firebase
                    }
                });
    }
    private void checkAuthState() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if(firebaseAuth.getCurrentUser()==null)
                {
                    changeActivity();
                }
            }
        };
    }
    private void changeActivity() {
        Intent intent = new Intent(HomeActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    @AfterPermissionGranted(123)
    private void takepermission() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permisson Granted", Toast.LENGTH_SHORT).show();
        } else {
            EasyPermissions.requestPermissions(this, "We need permissions because this and that",
                    123, perms);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @Override
    public void onStop() {
        if(adapter!=null)
        {
            adapter.stopListening();
        }
        super.onStop();
    }
    @Override
    public void onResume() {
        super.onResume();
        if(adapter!=null)
        {
            adapter.startListening();
        }
    }

}
