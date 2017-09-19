package com.destiny.sta.ui.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.destiny.sta.R;
import com.destiny.sta.ServiceGenerator;
import com.destiny.sta.StaService;
import com.destiny.sta.model.Attendance;
import com.destiny.sta.model.AttendanceResponse;
import com.destiny.sta.ui.fragment.CameraFragment;
import com.destiny.sta.ui.fragment.LoadingDialogFragment;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Destiny on 7/13/2017.
 */

public class AttendanceActivity extends AppCompatActivity implements CameraFragment.ImageCallback {

    public static final String TAG = AttendanceActivity.class.getSimpleName();

    public static final int REQUEST_LOCATION = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 2;

    private StaService staService;

    private String user;

    private LinearLayout addressLayout;
    private ImageView imageView;
    private Button submitButton;
    private CheckBox locationCheckBox;
    private CheckBox pictureCheckBox;

    private String imagePath = null;
    private Location currentLocation = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        getSupportActionBar().show();

        user = getIntent().getStringExtra("user");
        getSupportActionBar().setTitle(user);

        addressLayout = (LinearLayout) findViewById(R.id.addressLayout);
        imageView = (ImageView) findViewById(R.id.imageView);
        locationCheckBox = (CheckBox) findViewById(R.id.locationCheckBox);
        pictureCheckBox = (CheckBox) findViewById(R.id.pictureCheckBox);

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();

                Attendance attendance = new Attendance();
                attendance.setEmployeeCode(user);
                attendance.setLatitude(String.valueOf(currentLocation.getLatitude()));
                attendance.setLongitude(String.valueOf(currentLocation.getLongitude()));

                sendAttendance(attendance);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendAttendance(Attendance attendance) {
        File image = new File(imagePath);

        RequestBody requestImage = RequestBody.create(MediaType.parse("image/*"), image);
        RequestBody username = RequestBody.create(MultipartBody.FORM, attendance.getEmployeeCode());
        RequestBody latitude = RequestBody.create(MultipartBody.FORM, attendance.getLatitude());
        RequestBody longitude = RequestBody.create(MultipartBody.FORM, attendance.getLongitude());
        MultipartBody.Part body = MultipartBody.Part.createFormData("fileToUpload", image.getName(), requestImage);

        staService = ServiceGenerator.createService(StaService.class);

        Call<AttendanceResponse> call = staService.recordAttendance(username, latitude, longitude, body);
        call.enqueue(new Callback<AttendanceResponse>() {
            @Override
            public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {
                dismissLoadingDialog();

                if (response.code() == 200 && response.body() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AttendanceActivity.this);
                    builder.setMessage("[" + response.body().getDate() +
                            " " + response.body().getTime() + "]")
                            .setTitle(R.string.save_success)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AttendanceActivity.this);
                    builder.setMessage(R.string.save_error)
                            .setTitle(R.string.error)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }

            @Override
            public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                dismissLoadingDialog();

                AlertDialog.Builder builder = new AlertDialog.Builder(AttendanceActivity.this);
                builder.setMessage(R.string.network_error)
                        .setTitle(R.string.error)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                //
            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                onImageCaptured(data.getStringExtra("image_path"));
            }
        }
    }

    @Override
    public void onImageCaptured(String imagePath) {
        imageView.setVisibility(View.VISIBLE);
        pictureCheckBox.setChecked(true);

        this.imagePath = imagePath;

        File image = new File(imagePath);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap imageBitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
        //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, parent.getWidth(), parent.getHeight(), true);

        imageView.setImageBitmap(imageBitmap);

        onDataReceived();
    }

    private void onDataReceived() {
        if (currentLocation != null && imagePath != null) {
            submitButton.setText(R.string.submit);
            submitButton.setEnabled(true);
        } else {
            submitButton.setText(R.string.submit_step_1);
        }
    }

    private void showLoadingDialog() {
        DialogFragment loadingDialog = (DialogFragment) getSupportFragmentManager()
                .findFragmentByTag(LoadingDialogFragment.TAG);

        if (loadingDialog == null) {
            loadingDialog = new LoadingDialogFragment();
            loadingDialog.setCancelable(false);
            loadingDialog.show(this.getSupportFragmentManager(), LoadingDialogFragment.TAG);
        }
    }

    private void dismissLoadingDialog() {
        DialogFragment loadingDialog = (DialogFragment) getSupportFragmentManager()
                .findFragmentByTag(LoadingDialogFragment.TAG);

        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        logout();
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.logout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
