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
import com.destiny.sta.model.LoginResponse;

/**
 * Created by Bobo on 8/29/2017.
 */

public class WelcomeFragment extends Fragment {

    private LoginFragment.LoginCallback loginCallback;

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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.welcome);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        TextView userTextView = view.findViewById(R.id.userTextView);
        TextView datetimeTextView = view.findViewById(R.id.datetimeTextView);

        final LoginResponse user = loginCallback.getUser();

        userTextView.setText(user.getFullName());
        datetimeTextView.setText(user.getDate() + " " + user.getTime());

        Button okButton = view.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, new MapFragment())
                        .commit();
            }
        });

        return view;
    }
}
