package com.example.nameless.autoupdating.fragments.auth;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.activities.Authentification;
import com.example.nameless.autoupdating.common.AuthActions;

public class SignInFragment extends Fragment {

    private Button btnSignIn;
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
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);
        btnSignIn = view.findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(v -> {
            authActions.onAuthenticateAction(Authentification.RC_SIGN_IN, null);
        });
        return view;
    }
}
