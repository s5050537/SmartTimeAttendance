package com.destiny.sta;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by Destiny on 7/19/2017.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        Parse.initialize(new Parse.Configuration.Builder(this)
//                .applicationId("d0e63a6dcd6548cd1831ead8e5b3a27d1cd5d34a")
//                .server("http://ec2-54-169-232-185.ap-southeast-1.compute.amazonaws.com:80/parse")
//                .build()
//        );
    }
}
