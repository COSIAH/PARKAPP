package com.dominicsilveira.parkingsystem.NormalUser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.dominicsilveira.parkingsystem.R;
import com.dominicsilveira.parkingsystem.classes.BookedSlots;
import com.dominicsilveira.parkingsystem.classes.NumberPlate;
import com.dominicsilveira.parkingsystem.classes.ParkingArea;
import com.dominicsilveira.parkingsystem.classes.User;
import com.dominicsilveira.parkingsystem.utils.AppConstants;
import com.dominicsilveira.parkingsystem.utils.pdf.InvoiceGenerator;
import com.dominicsilveira.parkingsystem.utils.notifications.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BookParkingAreaActivity extends AppCompatActivity {
    Spinner numberPlateSpinner;
    TextView placeText,wheelerText,amountText,endDateText,endTimeText;
    FloatingActionButton bookBtn;
    LinearLayout endDate,endTime;

    FirebaseAuth auth;
    FirebaseDatabase db;

    SupportMapFragment supportMapFragment;
    GoogleMap gMap;
    LatLng globalLatLng=null;
    MarkerOptions options;

    List<Integer> numberPlateWheeler = new ArrayList<Integer>();
    List<String> numberPlateNumber = new ArrayList<String>();

    final int UPI_PAYMENT = 0;
    Calendar calendar;

    BookedSlots bookingSlot=new BookedSlots();
    ParkingArea parkingArea;
    User userObj;
    NotificationHelper mNotificationHelper;
    AppConstants globalClass;

    String UUID;

    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_parking_area);

        initComponents();
        attachListeners();
        addItemsOnSpinner();
        addListenerOnSpinnerItemSelection();
        askCameraFilePermission();
    }

    private void initComponents() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        globalClass=(AppConstants)getApplicationContext();
        userObj=globalClass.getUserObj();

        placeText = findViewById(R.id.placeText);
        numberPlateSpinner = findViewById(R.id.vehicleSelect);
        endDate = findViewById(R.id.endDate);
        endTime = findViewById(R.id.endTime);
        endDateText = findViewById(R.id.endDateText);
        endTimeText = findViewById(R.id.endTimeText);
//        endDateText.setInputType(InputType.TYPE_NULL);
//        endTimeText.setInputType(InputType.TYPE_NULL);
        bookBtn = findViewById(R.id.bookBtn);
        wheelerText = findViewById(R.id.wheelerText);
        amountText = findViewById(R.id.amountText);
        mNotificationHelper=new NotificationHelper(this);

        calendar=new GregorianCalendar();
        bookingSlot.startTime=bookingSlot.endTime=calendar.getTime();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
        endTimeText.setText(simpleDateFormat.format(bookingSlot.endTime));
        simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
        endDateText.setText(simpleDateFormat.format(bookingSlot.endTime));
        bookingSlot.readNotification=0;
        bookingSlot.hasPaid=1;
        bookingSlot.userID=auth.getCurrentUser().getUid();

        Bundle bundle = getIntent().getExtras();
        bookingSlot.placeID=bundle.getString("UUID");
        final ParkingArea parkingArea = (ParkingArea) getIntent().getSerializableExtra("ParkingArea");
        Log.e("BookParkingAreaActivity",parkingArea.name+" "+bookingSlot.placeID);

        placeText.setText(parkingArea.name);
        globalLatLng=new LatLng(parkingArea.latitude,parkingArea.longitude);
        options=new MarkerOptions().position(globalLatLng)
                .title(parkingArea.name);
        supportMapFragment=(SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap=googleMap;
                gMap.clear();
                gMap.addMarker(options);
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(globalLatLng,30));
            }
        });
    }

    private void attachListeners() {
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(endDateText);
            }
        });
        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(endTimeText);
            }
        });
        bookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String amount = "1";
//                String note ="Payment for ".concat(bookingSlot.placeID).concat(" and number ").concat(bookingSlot.numberPlate);
//                String name = "Michael Silveira";
//                String upiId = "micsilveira111@oksbi";
//                payUsingUpi(amount, upiId, name, note);
                saveData();
            }
        });

        db.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        parkingArea.setData(snapshot.getKey(),snapshot.getValue(int.class));
                        Log.e("CalledTwice", String.valueOf(snapshot.getKey())+snapshot.getValue(int.class));
                    }
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        db.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ParkingArea parkingArea = snapshot.getValue(ParkingArea.class);
                        setAddValues(parkingArea);
                        Log.e("CalledTwice", String.valueOf(snapshot.getKey()));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    private void setAddValues(ParkingArea parkingArea) {
        this.parkingArea=parkingArea;
        placeText.setText(parkingArea.name);
    }

    private void saveData() {
        bookingSlot.notificationID=Math.abs((int)Calendar.getInstance().getTimeInMillis());
        final String key=db.getReference("BookedSlots").push().getKey();
        if(parkingArea.availableSlots>0){
            parkingArea.availableSlots-=1;
            parkingArea.occupiedSlots+=1;
            db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        db.getReference("BookedSlots").child(key).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(BookParkingAreaActivity.this,"Success",Toast.LENGTH_SHORT).show();
                                    File file = new File(BookParkingAreaActivity.this.getExternalCacheDir(), File.separator + "invoice.pdf");
                                    InvoiceGenerator invoiceGenerator=new InvoiceGenerator(bookingSlot,parkingArea,key,userObj,file);
                                    invoiceGenerator.create();
                                    invoiceGenerator.uploadFile(BookParkingAreaActivity.this);
                                    Intent intent = new Intent(BookParkingAreaActivity.this, MainNormalActivity.class);
                                    intent.putExtra("FRAGMENT_NO", 0);
                                    startActivity(intent);
                                    finish();
                                }else{
                                    Toast.makeText(BookParkingAreaActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                                    parkingArea.availableSlots+=1;
                                    parkingArea.occupiedSlots-=1;
                                    db.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                                }
                            }
                        });
                    }
                }
            });
        }else {
            Toast.makeText(BookParkingAreaActivity.this,"Failed! Slots are full.",Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker(final TextView button) {
        DatePickerDialog.OnDateSetListener dateSetListener=new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, final int date) {
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month);
                calendar.set(Calendar.DAY_OF_MONTH,date);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
                button.setText(simpleDateFormat.format(calendar.getTime()));
                bookingSlot.endTime = calendar.getTime();
                calcRefreshAmount();
            }
        };
        DatePickerDialog datePickerDialog=new DatePickerDialog(BookParkingAreaActivity.this,dateSetListener,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(final TextView button) {
        TimePickerDialog.OnTimeSetListener timeSetListener= new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY,hour);
                calendar.set(Calendar.MINUTE,minute);
                calendar.set(Calendar.SECOND, 0);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
                bookingSlot.endTime = calendar.getTime();
                if(bookingSlot.endTime.after(bookingSlot.startTime)){
                    button.setText(simpleDateFormat.format(calendar.getTime()));
                    bookingSlot.endTime = calendar.getTime();
                    calcRefreshAmount();
                }else{
                    bookingSlot.endTime = bookingSlot.startTime;
                    Toast.makeText(BookParkingAreaActivity.this,
                            "Please select a time after Present time!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        TimePickerDialog timePickerDialog=new TimePickerDialog(BookParkingAreaActivity.this,timeSetListener,calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false);
        timePickerDialog.show();
    }

    public void addItemsOnSpinner() {
        numberPlateWheeler.add(0);
        numberPlateNumber.add("Select a vehicle");
        db.getReference().child("NumberPlates").orderByChild("userID").equalTo(auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            NumberPlate numberPlate = dataSnapshot.getValue(NumberPlate.class);
                            if(numberPlate.isDeleted==0){
                                numberPlateWheeler.add(numberPlate.wheelerType);
                                numberPlateNumber.add(numberPlate.numberPlate);
                            }
                        }
                        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(BookParkingAreaActivity.this,
                                android.R.layout.simple_spinner_item, numberPlateNumber);
                        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        numberPlateSpinner.setAdapter(dataAdapter);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void addListenerOnSpinnerItemSelection() {
        numberPlateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if(position!=0){
                    bookingSlot.numberPlate= numberPlateNumber.get(position);
                    bookingSlot.wheelerType= numberPlateWheeler.get(position);
                    calcRefreshAmount();
                    String wheelerTypeStr=String.valueOf(bookingSlot.wheelerType);
                    wheelerText.setText(wheelerTypeStr);
                    Toast.makeText(BookParkingAreaActivity.this, String.valueOf(numberPlateSpinner.getSelectedItem())+String.valueOf(position), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void calcRefreshAmount() {
        bookingSlot.calcAmount(parkingArea);
        String amountStr=String.valueOf(bookingSlot.amount);
        amountText.setText(amountStr);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void askCameraFilePermission() {
        if (!hasPermissions(BookParkingAreaActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(BookParkingAreaActivity.this, PERMISSIONS, AppConstants.SCAN_PERMISSION_ALL);
        }else{
//            openCamera();
        }
    }

    void payUsingUpi(String amount, String upiId, String name, String note) {
        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build();
        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);
        // will always show a dialog to user to choose an app
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");
        // check if intent resolves
        if(null != chooser.resolveActivity(getPackageManager()))
            startActivityForResult(chooser, UPI_PAYMENT);
        else
            Toast.makeText(BookParkingAreaActivity.this,"No UPI app found, please install one to continue",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case UPI_PAYMENT:
                if ((RESULT_OK == resultCode) || (resultCode == 11)) {
                    if (data != null) {
                        String trxt = data.getStringExtra("response");
                        Log.d("UPI", "onActivityResult: " + trxt);
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add(trxt);
                        upiPaymentDataOperation(dataList);
                    } else {
                        Log.d("UPI", "onActivityResult: " + "Return data is null");
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add("nothing");
                        upiPaymentDataOperation(dataList);
                    }
                } else {
                    Log.d("UPI", "onActivityResult: " + "Return data is null"); //when user simply back without payment
                    ArrayList<String> dataList = new ArrayList<>();
                    dataList.add("nothing");
                    upiPaymentDataOperation(dataList);
                }
                break;
        }
    }

    private void upiPaymentDataOperation(ArrayList<String> data) {
        if (isConnectionAvailable(BookParkingAreaActivity.this)) {
            String str = data.get(0);
            Log.d("UPIPAY", "upiPaymentDataOperation: "+str);
            String paymentCancel = "";
            if(str == null) str = "discard";
            String status = "";
            String approvalRefNo = "";
            String response[] = str.split("&");
            for (int i = 0; i < response.length; i++) {
                String equalStr[] = response[i].split("=");
                if(equalStr.length >= 2) {
                    if (equalStr[0].toLowerCase().equals("Status".toLowerCase()))
                        status = equalStr[1].toLowerCase();
                    else if (equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase()) || equalStr[0].toLowerCase().equals("txnRef".toLowerCase()))
                        approvalRefNo = equalStr[1];
                }
                else {
                    paymentCancel = "Payment cancelled by user-1.";
                }
            }
            if (status.equals("success")) {
                //Code to handle successful transaction here.
                Toast.makeText(BookParkingAreaActivity.this, "Transaction successful.", Toast.LENGTH_SHORT).show();
                saveData();
                Log.d("UPIPay", "responseStr: "+approvalRefNo);
            } else if("Payment cancelled by user.".equals(paymentCancel))
                Toast.makeText(BookParkingAreaActivity.this, "Payment cancelled by user-2.", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(BookParkingAreaActivity.this, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(BookParkingAreaActivity.this, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()
                    && netInfo.isConnectedOrConnecting()
                    && netInfo.isAvailable()) {
                return true;
            }
        }
        return false;
    }
}