package com.example.nameless.autoupdating.fragments.auth;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Authentification;
import com.example.nameless.autoupdating.generalModules.AuthActions;
import com.example.nameless.autoupdating.models.User;

public class SignUpFragment extends Fragment {

    private EditText etLogin;
    private Button btnSignUp;
    private AuthActions authActions;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        authActions = (AuthActions) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);
        etLogin = view.findViewById(R.id.etLogin);
        btnSignUp = view.findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(v -> {
            User user = new User();
            user.setLogin(String.valueOf(etLogin.getText()));
            authActions.onAuthenticateAction(Authentification.RC_SIGN_UP, user);
        });
        return view;
    }
}
