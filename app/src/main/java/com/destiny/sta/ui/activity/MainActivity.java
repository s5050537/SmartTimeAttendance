package com.destiny.sta.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.destiny.sta.R;
import com.destiny.sta.ServiceGenerator;
import com.destiny.sta.StaService;
import com.destiny.sta.model.Attendance;
import com.destiny.sta.model.AttendanceResponse;
import com.destiny.sta.model.LoginResponse;
import com.destiny.sta.ui.fragment.SavingResultFragment;
import com.destiny.sta.ui.fragment.LoadingDialogFragment;
import com.destiny.sta.ui.fragment.LoginFragment;
import com.destiny.sta.ui.fragment.MapFragment;
import com.destiny.sta.ui.fragment.PictureFragment;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.destiny.sta.ui.fragment.PictureFragment.REQUEST_IMAGE_CAPTURE;

/**
 * Created by Destiny on 7/11/2017.
 */

public class MainActivity extends AppCompatActivity implements MapFragment.LocationCallback, PictureFragment.ImageCallback, LoginFragment.LoginCallback {

    private StaService staService;

    private LoginResponse user = null;
    private Location location = null;
    private String imagePath = null;

    private long pauseTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            redirectToLogin();
//            Intent intent = new Intent(this, CameraActivity.class);
//            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        pauseTime = System.currentTimeMillis();
    }

//    private void handleLeftUnfinished(Bundle savedInstanceState) {
//        long currentTime = System.currentTimeMillis();
//        if(savedInstanceState != null) {
//            pauseTime = savedInstanceState.getLong("time_millis");
//        }
//
//        if(pauseTime != 0) {
//            long durationInSeconds = (currentTime - pauseTime) / 1000;
//            if (durationInSeconds > 45) {
//                redirectToLogin();
//            }
//        }
//    }

    private void redirectToLogin() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new LoginFragment(), "login_fragment")
                .commit();
    }

    @Override
    public void onImageCaptured(String path) {
        imagePath = path;

        prepareAttendance();
    }

    @Override
    public void onLocationUpdated(Location location) {
        this.location = location;

        prepareAttendance();
    }

    private void showCompletion(AttendanceResponse attendanceResponse) {
        Fragment fragment = new SavingResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("attendance_response", attendanceResponse);
        fragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    private void prepareAttendance() {
        if (location != null && imagePath != null) {
            showLoadingDialog();

            Attendance attendance = new Attendance();
            attendance.setEmployeeCode(user.getUsername());
            attendance.setLatitude(String.valueOf(location.getLatitude()));
            attendance.setLongitude(String.valueOf(location.getLongitude()));
            sendAttendance(attendance);
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
                    showCompletion(response.body());

                    location = null;
                    imagePath = null;
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
    public void loginSuccess(LoginResponse user) {
        this.user = user;
    }

    @Override
    public LoginResponse getUser() {
        return user;
    }

    @Override
    public void onBackPressed() {
        logout();
    }

    private void logout() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("login_fragment");
        if (fragment != null) {
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder//.setTitle(R.string.logout)
                .setMessage(R.string.logout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        redirectToLogin();
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
