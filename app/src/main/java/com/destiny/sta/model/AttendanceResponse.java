package com.destiny.sta.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Bobo on 8/15/2017.
 */

public class AttendanceResponse implements Parcelable {

    private String username;
    private String date;
    private String time;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


    protected AttendanceResponse(Parcel in) {
        username = in.readString();
        date = in.readString();
        time = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(username);
        dest.writeString(date);
        dest.writeString(time);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<AttendanceResponse> CREATOR = new Parcelable.Creator<AttendanceResponse>() {
        @Override
        public AttendanceResponse createFromParcel(Parcel in) {
            return new AttendanceResponse(in);
        }

        @Override
        public AttendanceResponse[] newArray(int size) {
            return new AttendanceResponse[size];
        }
    };
}
