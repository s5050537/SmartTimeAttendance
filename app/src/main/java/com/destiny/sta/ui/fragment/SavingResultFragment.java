package com.destiny.sta.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.destiny.sta.R;
import com.destiny.sta.model.AttendanceResponse;
import com.destiny.sta.model.LoginResponse;

/**
 * Created by Bobo on 8/30/2017.
 */

public class SavingResultFragment extends Fragment {

    private LoginFragment.LoginCallback loginCallback;

    private AttendanceResponse attendanceResponse;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof LoginFragment.LoginCallback) {
            loginCallback = (LoginFragment.LoginCallback) context;
        } else {
            throw new IllegalArgumentException("Activity must implement LoginCallback!");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.save_success);

        Bundle bundle = getArguments();
        attendanceResponse = bundle.getParcelable("attendance_response");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saving_result, container, false);

        TextView userTextView = view.findViewById(R.id.userTextView);
        TextView fullNameTextView = view.findViewById(R.id.fullNameEditText);
        TextView datetimeTextView = view.findViewById(R.id.datetimeTextView);
        TextView branchTextView = view.findViewById(R.id.branchTextView);
        Button okButton = view.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new LoginFragment())
                        .commit();
            }
        });

        LoginResponse user = loginCallback.getUser();
        userTextView.setText(user.getUsername());
        fullNameTextView.setText(user.getFullName());
        datetimeTextView.setText(attendanceResponse.getDate() + " " + attendanceResponse.getTime());
        branchTextView.setText(user.getBranch().getName());

        return view;
    }
}
