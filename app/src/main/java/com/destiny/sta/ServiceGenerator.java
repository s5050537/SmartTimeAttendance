package com.destiny.sta;

import android.text.TextUtils;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Bobo on 8/10/2017.
 */

public class ServiceGenerator {

    // Trailing slash is needed
    public static final String BASE_URL = "http://122.155.213.2/smart_tat/";

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null, null);
    }

    public static <S> S createService(Class<S> serviceClass, String username, String password) {
        String authToken = null;
        if (!TextUtils.isEmpty(username) && (!TextUtils.isEmpty(password)) || password != null) {
            authToken = Credentials.basic(username, password);
        }

        return createService(serviceClass, authToken);
    }

    public static <S> S createService(Class<S> serviceClass, String authToken) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());

        if (!TextUtils.isEmpty(authToken)) {
            AuthenticationInterceptor interceptor = new AuthenticationInterceptor(authToken);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(interceptor);

            builder.client(httpClient.build());
        }

        return builder.build().create(serviceClass);
    }

}
