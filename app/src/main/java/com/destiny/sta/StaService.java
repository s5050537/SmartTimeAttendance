package com.destiny.sta;

import com.destiny.sta.model.AttendanceResponse;
import com.destiny.sta.model.ChangePasswordResponse;
import com.destiny.sta.model.LoginResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by Bobo on 8/2/2017.
 */

public interface StaService {

    @Multipart
    @POST("attend.php")
    Call<AttendanceResponse> recordAttendance(@Part("username") RequestBody username, @Part("latitude") RequestBody latitude,
                                              @Part("longitude") RequestBody longitude, @Part MultipartBody.Part file);

    @GET("login.php")
    Call<LoginResponse> login();

    @FormUrlEncoded
    @POST("change-password.php")
    Call<ChangePasswordResponse> changePassword(@Field("username") String username, @Field("password") String password);

}
