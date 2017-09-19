package com.destiny.sta.ui.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.destiny.sta.R;
import com.destiny.sta.ServiceGenerator;
import com.destiny.sta.StaService;
import com.destiny.sta.model.LoginResponse;
import com.destiny.sta.ui.FragmentBase;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Destiny on 7/13/2017.
 */

public class LoginFragment extends FragmentBase {

    public final String TAG = LoginFragment.class.getSimpleName();

    public interface LoginCallback {
        void loginSuccess(LoginResponse user);

        LoginResponse getUser();
    }

    private LoginCallback loginCallback;

    private EditText userEditText;
    private EditText passEditText;

    private StaService staService;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LoginCallback) {
            loginCallback = (LoginCallback) context;
        } else {
            throw new IllegalArgumentException("Activity must implement LoginCallback!");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.login);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        userEditText = view.findViewById(R.id.userEditText);
        passEditText = view.findViewById(R.id.passEditText);

        userEditText.requestFocus();

        Button exitButton = view.findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        Button loginButton = view.findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                String user = userEditText.getText().toString();
                String pass = passEditText.getText().toString();

                user = user.trim();
                pass = pass.trim();

                if (user.isEmpty()) { //|| pass.isEmpty()
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.login_error_message)
                            //.setTitle(R.string.login_error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    showLoadingDialog();

                    staService = ServiceGenerator.createService(StaService.class, user, pass);
                    Call<LoginResponse> call = staService.login();
                    call.enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, final Response<LoginResponse> response) {
                            dismissLoadingDialog();

                            if (response.code() == 200 && response.body() != null) {
                                loginCallback.loginSuccess(response.body());

                                Fragment fragment;

                                String password = response.body().getPassword();
                                if (password != null) {
                                    fragment = new WelcomeFragment();
                                } else {
                                    fragment = new PasswordChangerFragment();
                                }

                                getActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.content_frame, fragment)
                                        .commit();
                            } else if (response.code() == 401) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(R.string.wrong_user_pass)
                                        //.setTitle(R.string.error)
                                        .setPositiveButton(android.R.string.ok, null);
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            dismissLoadingDialog();

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(R.string.network_error)
                                    //.setTitle(R.string.error)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow((null == getActivity().getCurrentFocus()) ? null : getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

}
