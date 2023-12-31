package com.example.newapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newapp.DataModel.Data;
import com.example.newapp.DataModel.NotificationSender;
import com.example.newapp.R;
import com.example.newapp.services.APIService;
import com.example.newapp.utils.Client;
import com.example.newapp.utils.DataUpdateServiceSettingsUtil;
import com.example.newapp.utils.MyResponse;
import com.example.newapp.utils.ServiceSettingsUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.widget.RatingBar;

import com.example.newapp.DataModel.Review;
import com.example.newapp.DataModel.SpaceShip;
import com.example.newapp.DataModel.Transaction;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserTransactionDetailsActivity extends AppCompatActivity implements PaymentResultListener {

    DecimalFormat decimalFormat = new DecimalFormat("#.##");
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat datePatternFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a");
    private Bitmap scaledBitmap;
    private TextView reviews_et;
    private TextView reviews_tv;
    private TextView submitReview_tv;
    private RatingBar ratingBar;
    private TextView companyNameTextView;
    private TextView spaceShipNameTextView;
    private TextView transactionIdTextView;
    private TextView fromTextView;
    private TextView toTextView;
    private TextView distanceTextView;
    private TextView totalCostTextView;
    private TextView isTransactionComplete_tv;
    private TextView completeJourneyTextView;
    private TextView endRecurringRideTextView;
    private TextView invoiceTextView;
    private TextView shareRideTextView;
    private Transaction currentTransaction;
    private SpaceShip transactionSpaceShip;
    private String chosenSeatConfig;
    private String currentSeatConfiguration;
    private ArrayList<Transaction> transactionArrayList;
    private Float rating;
    private String invoiceUrl;
    private ScrollView scrollView;

    private ScrollView scrollView2;
    private TextView rating_and_review_tv;
    private TextView status_tv;
    private TextView invoiceInfo;
    private BottomSheetBehavior bottomSheetBehavior;
    private TextView rating_tv2;
    private APIService apiService;
    private String token;
    private int countSpaceShipsOnSamePath;
    private float basicPay, serviceCharge, spaceTax , dynamicTrafficCost, totalAmount;
    private ProgressBar progressBar;
    private Bitmap bmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_transaction_details);

        getSupportActionBar().hide();

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);


        companyNameTextView = findViewById(R.id.companyName_transaction_details);
        spaceShipNameTextView = findViewById(R.id.spaceShipName_transaction_details);
        fromTextView = findViewById(R.id.from_transaction_details);
        totalCostTextView = findViewById(R.id.price_transaction_details);
        toTextView = findViewById(R.id.to_transaction_details);
        distanceTextView = findViewById(R.id.distance_transaction_details);
        transactionIdTextView = findViewById(R.id.transactionId_transaction_details);
        isTransactionComplete_tv = findViewById(R.id.isOngoing_transaction_details);
        completeJourneyTextView = findViewById(R.id.complete_transaction_details);
        reviews_et = findViewById(R.id.user_review_et);
        reviews_tv = findViewById(R.id.user_review_tv);
        ratingBar = findViewById(R.id.ratingBar);
        submitReview_tv = findViewById(R.id.submit_review_tv);
        rating_and_review_tv = findViewById(R.id.rating_and_review_tv);
        scrollView = findViewById(R.id.scrollView3);
        scrollView2 = findViewById(R.id.scrollView2);
        status_tv = findViewById(R.id.status_tv);
        invoiceInfo = findViewById(R.id.textView19);
        endRecurringRideTextView = findViewById(R.id.recurring_ride_end_tv);
        invoiceTextView = findViewById(R.id.invoice_tv);
        shareRideTextView = findViewById(R.id.share_ride_tv);

        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        scaledBitmap = Bitmap.createScaledBitmap(bmp, 250, 60, false);

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        rating_tv2 = findViewById(R.id.rating_and_review_tv2);
        progressBar = findViewById(R.id.progressBar_transaction);

        rating_and_review_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        rating_tv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });


        Intent intent = getIntent();
        currentTransaction = (Transaction) intent.getSerializableExtra("transaction");
        chosenSeatConfig = currentTransaction.getChosenSeatConfiguration();
        transactionArrayList = new ArrayList<>();

        setDataViews();
        spaceShipDensityCount();
        attachSeatsListener();

        if (!currentTransaction.isTransactionRecurring()) {
            endRecurringRideTextView.setVisibility(View.GONE);
        }

        if (currentTransaction.isTransactionComplete()) {
            completeJourneyTextView.setVisibility(View.GONE);
            status_tv.setVisibility(View.GONE);
            invoiceTextView.setVisibility(View.VISIBLE);
            invoiceInfo.setVisibility(View.VISIBLE);

            if (currentTransaction.getReview().getTime() == 0) {
                reviews_tv.setVisibility(View.GONE);
            } else {
                scrollView.setVisibility(View.VISIBLE);
                scrollView2.setVisibility(View.GONE);
                reviews_et.setVisibility(View.GONE);
                submitReview_tv.setVisibility(View.GONE);
                ratingBar.setFocusable(false);
                ratingBar.setIsIndicator(true);
            }
        } else {
            reviews_et.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
            submitReview_tv.setVisibility(View.GONE);
            reviews_tv.setVisibility(View.GONE);
            invoiceTextView.setVisibility(View.GONE);
            rating_and_review_tv.setVisibility(View.GONE);
        }

        completeJourneyTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                onPaymentSuccess(UUID.randomUUID().toString());
                // Disable the button after the first click
                completeJourneyTextView.setEnabled(false);
            }
        });


        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float chosenRating, boolean fromUser) {
                rating = chosenRating;
            }
        });

        submitReview_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateReviews();
                submitReview_tv.setEnabled(false);
            }
        });

        endRecurringRideTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endRecurringRide();
            }
        });

        invoiceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(currentTransaction.getInvoiceUrl()), "application/pdf");
                if (!currentTransaction.getInvoiceUrl().isEmpty()) {
                    startActivity(intent);
                } else {
                    Toast.makeText(UserTransactionDetailsActivity.this, "No invoice available",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        shareRideTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = "Guess what? I'm embarking on a cosmic ride through the galaxy, " +
                        "and I want to share the details of this interstellar journey with you.\n" +
                        "\n" +
                        "\uD83C\uDF0D Journey: From " + currentTransaction.getDeparture()
                        + " to " + currentTransaction.getDestination() + "\n" +
                        "\uD83D\uDD52 Departure Time: " + getDateFromTime(currentTransaction.getTransactionTime()) + "\n"
                        + "\uD83D\uDE80 Spaceship Model: " + currentTransaction.getSpaceShipName() +
                        "\n\n Emergency Contact:\n" +
                        "\uD83D\uDCDE Spaceship Hotline: 9056810273\n" +
                        " Let's make this ride a space-tacular adventure together! \uD83D\uDEF8\uD83D\uDCAB" +
                        " Safe travels across the cosmos!\n" + "\n" +
                        "#SpaceRide #GalaxyGlider #OutOfThisWorld.";

                // Creating a share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                // shareIntent.setPackage("com.whatsapp");
                startActivity(Intent.createChooser(shareIntent, "Share using"));
            }
        });

    }


    private void startPayment() {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_YEx4Fc8oJfPIUu");
        checkout.setImage(R.drawable.checkout_logo);

        final Activity activity = this;

        try {
            JSONObject options = new JSONObject();
            String amount = "6";
            amount = getIntent().getStringExtra("amt");
            options.put("name", "Galaxy Glider");
            options.put("description", "description");
            options.put("theme.color", "#000000");
            options.put("currency", "INR");
            options.put("amount", amount);
            options.put("prefill.email", "as.nishu18@gmail.com");
//            options.put("prefill.contact","8707279750");
            JSONObject retryObj = new JSONObject();
            checkout.open(activity, options);
        } catch (Exception e) {
            Log.e("TAG", "Error in starting Razorpay Checkout", e);
        }
    }


    @Override
    public void onPaymentSuccess(String s) {
//        Checkout.clearUserData(this);
        updateSeats();
        Log.d("onSUCCESS", "onPaymentSuccess: " + s);
    }


    // if payment fails redirect user to current activity.
    @Override
    public void onPaymentError(int i, String s) {
        Log.d("onERROR", "onPaymentError: " + s);
    }


    private float calculateFair(int countOfGliders) {
        countOfGliders++; // glider itself is a source of traffic.
        float journeyDistance = Float.parseFloat(currentTransaction.getDistance());
        float basePay = 1000;
        float pricePerLY = transactionSpaceShip.getPrice();
        basicPay = (pricePerLY * journeyDistance);
        serviceCharge = 0.18f * basicPay;
        dynamicTrafficCost = (countOfGliders / 100.0f) * basePay;
        float netCost = (dynamicTrafficCost) + basicPay + (serviceCharge);
        spaceTax = .02f * netCost;
        totalAmount = netCost + spaceTax;
        return totalAmount;
    }


    private void setDataViews() {

        spaceShipNameTextView.setText(currentTransaction.getSpaceShipName());
        companyNameTextView.setText(currentTransaction.getCompanyName());
        fromTextView.setText(currentTransaction.getDeparture());
        toTextView.setText(currentTransaction.getDestination());
        distanceTextView.setText(currentTransaction.getDistance() + " ly");
        totalCostTextView.setText("$" + String.valueOf(currentTransaction.getTotalFare()));
        transactionIdTextView.setText(currentTransaction.getTransactionId());
        String transaction_complete = String.valueOf(currentTransaction.isTransactionComplete());
        if (transaction_complete.equals("false")) {
            isTransactionComplete_tv.setText("Ongoing");
        } else {
            isTransactionComplete_tv.setText("");
        }

        if (currentTransaction.getReview().getTime() > 0) {
            ratingBar.setRating(Float.parseFloat(currentTransaction.getReview().getRating()));
            reviews_tv.setText(currentTransaction.getReview().getReview());
        }

        FirebaseDatabase.getInstance().getReference("Tokens/V7pthYVNJ7c24rUfNumgT2W6JDs2/token")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        token = snapshot.getValue().toString();
                        Log.e("token", token );
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }


    // vacate the seats and update it on database.
    private void updateSeats() {

        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("company")
                .child(currentTransaction.getCompanyId()).child("spaceShips");

        // Fetch the existing spaceShips
        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<SpaceShip> spaceShipArrayList = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot spaceShipSnapshot : dataSnapshot.getChildren()) {
                        SpaceShip spaceShip = spaceShipSnapshot.getValue(SpaceShip.class);
                        if (spaceShip != null) {
                            if (spaceShip.getSpaceShipId().equals(currentTransaction.getSpaceShipId())) {
                                // set updatedSeatConfiguration after seats have been vacated.
                                transactionSpaceShip = spaceShip;
                                setSeatsVacated();
                                spaceShipArrayList.add(transactionSpaceShip);
                            } else {
                                spaceShipArrayList.add(spaceShip);
                            }
                        }
                    }
                }

                // Set the updated spaceShips back to the company reference
                companyRef.setValue(spaceShipArrayList).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateTransactionStatus();
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void updateTransactionStatus() {

        try {

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("transactions")
                    .child(currentTransaction.getTransactionId());


            databaseReference.child("totalFare").setValue(calculateFair(countSpaceShipsOnSamePath))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });

            databaseReference.child("transactionComplete").setValue(true)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            completeJourneyTextView.setVisibility(View.GONE);
                            printPdf();
                            ServiceSettingsUtil.stopRideService(getApplicationContext());
                            Intent intent1 = new Intent(UserTransactionDetailsActivity.this, AllTransactionsList.class);
                            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent1);
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Slow Internet Connection", Toast.LENGTH_SHORT).show();
        }

    }


    private void updateReviews() {

        try {
            String reviewString = "";
            if (reviews_et != null) {
                reviewString = reviews_et.getText().toString();
            }

            Review newReview = new Review(reviewString, String.valueOf(rating), currentTransaction.getUserName(), currentTransaction.getUserEmail(),
                    System.currentTimeMillis());

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("transactions")
                    .child(currentTransaction.getTransactionId());

            databaseReference.child("review").setValue(newReview)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            updateSpaceShipRating();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Slow Internet Connection", Toast.LENGTH_SHORT).show();
        }

    }


    // get the changes seat configuration by vacating seat chosen seats
    private String getChangedSeatConfig() {
        String updatedSeatsConfiguration = currentSeatConfiguration;
        for (int position = 0; position < 12; position++) {
            if (chosenSeatConfig.charAt(position) == '1') {
                updatedSeatsConfiguration = setCharAt(updatedSeatsConfiguration, position, '1');
            } else {
                char character = currentSeatConfiguration.charAt(position);
                updatedSeatsConfiguration = setCharAt(updatedSeatsConfiguration, position, character);
            }
        }
        return updatedSeatsConfiguration;
    }


    // set character at given index in the string
    private String setCharAt(String str, int i, char ch) {
        char[] charArray = str.toCharArray();
        charArray[i] = ch;
        return new String(charArray);
    }


    // fetch the updates in seat configuration in realtime.
    private void attachSeatsListener() {

        try {
            DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("company")
                    .child(currentTransaction.getCompanyId()).child("spaceShips");

            companyRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot spaceShipSnapshot : dataSnapshot.getChildren()) {
                            SpaceShip spaceShip = spaceShipSnapshot.getValue(SpaceShip.class);
                            if (spaceShip != null && currentTransaction.getSpaceShipId().equals(spaceShip.getSpaceShipId())) {
                                transactionSpaceShip = spaceShip;
                                getSlotConfiguration();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Slow Internet Connection",
                    Toast.LENGTH_SHORT).show();
        }

    }


    private void getSlotConfiguration() {

        if (currentTransaction.getSlotNo().equals("0")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot1();
        } else if (currentTransaction.getSlotNo().equals("1")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot2();
        } else if (currentTransaction.getSlotNo().equals("2")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot3();
        } else if (currentTransaction.getSlotNo().equals("3")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot4();
        } else if (currentTransaction.getSlotNo().equals("4")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot5();
        } else if (currentTransaction.getSlotNo().equals("5")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot6();
        } else if (currentTransaction.getSlotNo().equals("6")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot7();
        } else if (currentTransaction.getSlotNo().equals("7")) {
            currentSeatConfiguration = transactionSpaceShip.getSlot8();
        }

    }


    private void setSeatsVacated() {

        if (currentTransaction.getSlotNo().equals("0")) {
            transactionSpaceShip.setSlot1(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("1")) {
            transactionSpaceShip.setSlot2(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("2")) {
            transactionSpaceShip.setSlot3(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("3")) {
            transactionSpaceShip.setSlot4(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("4")) {
            transactionSpaceShip.setSlot5(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("5")) {
            transactionSpaceShip.setSlot6(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("6")) {
            transactionSpaceShip.setSlot7(getChangedSeatConfig());
        } else if (currentTransaction.getSlotNo().equals("7")) {
            transactionSpaceShip.setSlot8(getChangedSeatConfig());
        }

    }


    private float updatedCompanyRating(SpaceShip currentSpaceShip) {
        float reviewCount = 0;
        if (currentSpaceShip.getTransactionIds() != null) {
            reviewCount = currentSpaceShip.getTransactionIds().size();
        }
        float currentRating = Float.parseFloat(currentSpaceShip.getSpaceShipRating());
        return ((currentRating * (reviewCount-1)) + rating) / (reviewCount);
    }


    private void updateSpaceShipRating() {


        FirebaseDatabase.getInstance().getReference("company/" + currentTransaction.getCompanyId() + "/spaceShips")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<SpaceShip> spaceShipArrayList = new ArrayList<>();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            SpaceShip spaceShip = dataSnapshot.getValue(SpaceShip.class);
                            if (spaceShip != null && spaceShip.getSpaceShipId().equals(currentTransaction.getSpaceShipId())) {
                                float newRating = updatedCompanyRating(spaceShip);
                                spaceShip.setSpaceShipRating(String.valueOf(newRating));
                                Log.e("updated comp rating", String.valueOf(newRating));
                                if(newRating <= 2.0){
                                    sendNotifications(token, currentTransaction.getCompanyName(),currentTransaction.getSpaceShipName());
                                }
                            }
                            spaceShipArrayList.add(spaceShip);
                        }

                        FirebaseDatabase.getInstance().getReference("company/" + currentTransaction.getCompanyId()
                                        + "/spaceShips")
                                .setValue(spaceShipArrayList).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Intent intent1 = new Intent(UserTransactionDetailsActivity.this, AllTransactionsList.class);
                                        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent1);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void printPdf() {
        int y;
        PdfDocument myPdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint linePaint = new Paint();
        linePaint.setColor(Color.rgb(0, 0, 0));
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(250, 350, 1).create();
        PdfDocument.Page myPage = myPdfDocument.startPage(myPageInfo);
        Canvas canvas = myPage.getCanvas();

        canvas.drawBitmap(scaledBitmap, 0, 0, paint);

        paint.setTextSize(18f);
        paint.setTypeface(Typeface.SERIF);
        paint.setColor(Color.rgb(227, 240, 237));
        canvas.drawText("Welcome to Galaxy Glider", 12, 35, paint);

        paint.setTextSize(8.5f);
        linePaint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(0, 0, 0));

        //  Details
        y = 75;
        canvas.drawText("Invoice No :  ", 20, y, paint);
        canvas.drawText("Date & Time :  ", 20, y + 12, paint);
        canvas.drawText("Status :  ", 20, y + 24, paint);
        y += 36;
        canvas.drawText("Customer Name :  ", 20, y, paint);
        canvas.drawText("Email :  ", 20, y + 12, paint);
        canvas.drawText("From :  ", 20, y + 24, paint);
        canvas.drawText("To :  ", 20, y + 36, paint);
        canvas.drawText("Total Distance :  ", 20, y + 48, paint);
        canvas.drawText("Company :  ", 20, y + 60, paint);
        canvas.drawText("SpaceShip :  ", 20, y + 72, paint);


        // get details from database/ intent
        String name = currentTransaction.getUserName();
        String mail = currentTransaction.getUserEmail();
        String from = currentTransaction.getDeparture();
        String to = currentTransaction.getDestination();
        long dis = Long.parseLong(currentTransaction.getDistance());
        String company = currentTransaction.getCompanyName();
        String spaceShip = currentTransaction.getSpaceShipName();


        y = 75;
        canvas.drawText(String.valueOf(currentTransaction.getTransactionId() + 1), 120, y, paint);
        canvas.drawText(datePatternFormat.format(new Date().getTime()), 120, y + 12, paint);
        canvas.drawText("Paid", 120, y + 24, paint);
        y += 36;
        canvas.drawText(name, 120, y, paint);
        canvas.drawText(mail, 120, y + 12, paint);
        canvas.drawText(from, 120, y + 24, paint);
        canvas.drawText(to, 120, y + 36, paint);
        canvas.drawText(String.valueOf(dis) + " LightYears", 120, y + 48, paint);
        canvas.drawText(company, 120, y + 60, paint);
        canvas.drawText(spaceShip, 120, y + 72, paint);

        y += 82;
        canvas.drawLine(10, y, 240, y, linePaint);
        paint.setTextSize(13f);
        canvas.drawText("Fair Calculation :", 20, y + 20, paint);
        paint.setTextSize(8.5f);
        y += 40;

        // amount calculation ---- Dynamic Fair

        canvas.drawText("Basic Pay ", 20, y, paint);
        canvas.drawText(transactionSpaceShip.getPrice() + " $/LightYear", 120, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.valueOf(basicPay), 230, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.drawText("Service Charges", 20, y + 12, paint);
        canvas.drawText("Tax 18%", 120, y + 12, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(decimalFormat.format(serviceCharge), 230, y + 12, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.drawText("Additional Charges", 20, y + 24, paint);
        canvas.drawText("Space Tax 2%", 120, y + 24, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(decimalFormat.format(spaceTax), 230, y + 24, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.drawText("Traffic Cost", 20, y + 36, paint);
        canvas.drawText("Variable", 120, y + 36, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(decimalFormat.format(dynamicTrafficCost), 230, y + 36, paint);
        paint.setTextAlign(Paint.Align.LEFT);


        canvas.drawText("Total", 20, y + 53, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(decimalFormat.format(totalAmount), 230, y + 53, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawLine(10, y + 63, 240, y + 63, linePaint);

        //TOTAL
        y += 63;
        paint.setTextSize(12f);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Amount Paid :  " + decimalFormat.format(totalAmount), 230, y + 15, paint);
        paint.setTextAlign(Paint.Align.LEFT);


        // Creating file
        myPdfDocument.finishPage(myPage);
        String pdfName = name + currentTransaction.getTransactionId() + ".pdf";

        File file = new File(getExternalFilesDir("/"), pdfName);
        try {
            myPdfDocument.writeTo(new FileOutputStream(file));

            // Uploading the file to Firebase Storage
            uploadPdfToFirebaseStorage(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        myPdfDocument.close();

    }

    private void uploadPdfToFirebaseStorage(File file) {
        // Get Firebase Storage reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        // Create a reference to 'pdfs/pdfName'
        StorageReference pdfRef = storageRef.child("invoice_pdfs/" + file.getName());

        // Create upload task
        UploadTask uploadTask = pdfRef.putFile(Uri.fromFile(file));

        // Register observers to listen for when the upload is done or if it fails
        uploadTask.addOnFailureListener(exception -> {
            // Handle unsuccessful uploads
            exception.printStackTrace();
        }).addOnSuccessListener(taskSnapshot -> {
            // Handle successful uploads
            // You can get the download URL of the uploaded file
            pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                invoiceUrl = uri.toString();
                Log.e("invoiceURL", invoiceUrl);
                uploadInvoiceToDatabase();

            });
        });
    }


    private void uploadInvoiceToDatabase() {

        FirebaseDatabase.getInstance().getReference("transactions/" + currentTransaction.getTransactionId() +
                "/invoiceUrl").setValue(invoiceUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                invoiceTextView.setVisibility(View.VISIBLE);
                Toast.makeText(UserTransactionDetailsActivity.this, "invoice downloaded...",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }


    private void endRecurringRide() {


        FirebaseDatabase.getInstance().getReference("company/" + currentTransaction.getCompanyId() + "/spaceShips")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<SpaceShip> spaceShipArrayList = new ArrayList<>();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            SpaceShip spaceShip = dataSnapshot.getValue(SpaceShip.class);
                            if (spaceShip != null && spaceShip.getSpaceShipId().equals(currentTransaction.getSpaceShipId())) {
                                spaceShip.setNextSeatConfigurations(endingRecurringRide(spaceShip));
                            }
                            spaceShipArrayList.add(spaceShip);
                        }

                        FirebaseDatabase.getInstance().getReference("company/" + currentTransaction.getCompanyId()
                                        + "/spaceShips")
                                .setValue(spaceShipArrayList).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        updateRecurringStatus();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private ArrayList<String> endingRecurringRide(SpaceShip spaceShip) {
        int slotNo = Integer.parseInt(currentTransaction.getSlotNo());
        ArrayList<String> nextSeatConfig = spaceShip.getNextSeatConfigurations();
        String seats = nextSeatConfig.get(slotNo);
        for (int position = 0; position < 12; position++) {
            if (currentTransaction.getChosenSeatConfiguration().charAt(position) == '1') {
                seats = setCharAt(seats, position, '1');
            }
        }
        nextSeatConfig.set(slotNo, seats);
        return nextSeatConfig;
    }


    private void updateRecurringStatus() {

        try {

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("transactions")
                    .child(currentTransaction.getTransactionId());

            databaseReference.child("transactionRecurring").setValue(false)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            endRecurringRideTextView.setVisibility(View.GONE);
                            DataUpdateServiceSettingsUtil.stopRecurringRideService(getApplicationContext());
                            Toast.makeText(UserTransactionDetailsActivity.this, "Recurring ride " +
                                    "seats vacated.", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Slow Internet Connection", Toast.LENGTH_SHORT).show();
        }

    }


    //This Method Sends the notifications combining all class of
    //SendNotificationPack Package work together
    public void sendNotifications(String usertoken, String title, String message) {
        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(data, usertoken);
        apiService.sendNotifcation(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call,
                                   Response<MyResponse> response) {
                if (response.code() == 200) {
                    assert response.body() != null;
                    if (response.body().success != 1) {
                        Log.e("UserTransactionDetailsActivity", "Sorry admin could not be informed. Please try again later.");
                    } else {
                        Log.e("UserTransactionDetailsActivity", "Admin has been informed.");
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {
            }
        });
    }

    private String getDateFromTime(long currentTimeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeInMillis);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekStr = getDayOfWeekString(dayOfWeek);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        String monthStr = getMonthString(month);

        int year = calendar.get(Calendar.YEAR);

        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);

        String amPm = (calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";

        return dayOfWeekStr + ", " + monthStr + " " + day + ", " + String.format("%02d:%02d", hour, minute) + " " + amPm;
    }

    private String getDayOfWeekString(int dayOfWeek) {
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        return daysOfWeek[dayOfWeek - 1];
    }

    private String getMonthString(int month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[month];
    }

    private void spaceShipDensityCount() {

        String from = currentTransaction.getDeparture();
        String to = currentTransaction.getDestination();

        FirebaseDatabase.getInstance().getReference("transactions/")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Transaction transaction = dataSnapshot.getValue(Transaction.class);
                            if(transaction.getDeparture().equals(from) && transaction.getDestination().equals(to)){
                                countSpaceShipsOnSamePath++;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}