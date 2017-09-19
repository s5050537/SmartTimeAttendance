package com.destiny.sta.ui.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.destiny.sta.R;
import com.destiny.sta.ServiceGenerator;
import com.destiny.sta.StaService;
import com.destiny.sta.model.ChangePasswordResponse;
import com.destiny.sta.model.LoginResponse;
import com.destiny.sta.ui.FragmentBase;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Bobo on 8/29/2017.
 */

public class PasswordChangerFragment extends FragmentBase {

    private LoginFragment.LoginCallback loginCallback;

    private StaService staService;

    private EditText passEditText;
    private EditText pass2EditText;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LoginFragment.LoginCallback) {
            loginCallback = (LoginFragment.LoginCallback) context;
        } else {
            throw new IllegalArgumentException("Activity must implement LoginCallback!");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.change_password);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_password_changer, container, false);

        TextView userTextView = view.findViewById(R.id.userTextView);
        passEditText = view.findViewById(R.id.passEditText);
        pass2EditText = view.findViewById(R.id.pass2EditText);
        final Button submitButton = view.findViewById(R.id.submitButton);

        final LoginResponse user = loginCallback.getUser();

        userTextView.setText(user.getUsername());

//        passEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                if (!editable.toString().isEmpty()) {
//                    if (!pass2EditText.isEnabled()) {
//                        pass2EditText.setEnabled(true);
//                    }
//                }
//
//            }
//        });
//
//        pass2EditText.setEnabled(false);
//        pass2EditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                if (editable.toString().equals(passEditText.getText().toString())) {
//                    if (!submitButton.isEnabled()) {
//                        submitButton.setEnabled(true);
//                    }
//                }
//            }
//        });
//
//        submitButton.setEnabled(false);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(passEditText.getText().toString().trim().equals("") || !passEditText.getText().toString().equals(pass2EditText.getText().toString())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.password_not_match)
                            //.setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    return;
                }

                showLoadingDialog();

                staService = ServiceGenerator.createService(StaService.class);
                Call<ChangePasswordResponse> call = staService.changePassword(user.getUsername(), passEditText.getText().toString());
                call.enqueue(new Callback<ChangePasswordResponse>() {
                    @Override
                    public void onResponse(Call<ChangePasswordResponse> call, Response<ChangePasswordResponse> response) {
                        dismissLoadingDialog();

                        if(response.code() == 200) {
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.content_frame, new WelcomeFragment())
                                    .commit();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(R.string.change_password_error)
                                    //.setTitle(R.string.error)
                                    .setPositiveButton(android.R.string.ok, null);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ChangePasswordResponse> call, Throwable t) {
                        dismissLoadingDialog();

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.network_error)
                                //.setTitle(R.string.error)
                                .setPositiveButton(android.R.string.ok, null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            }
        });

        return view;
    }


}
