package com.example.aiassistantcoder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;

import com.example.aiassistantcoder.ui.ProfileKt;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function6;

public class ProfileRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_register, container, false);

        mAuth = FirebaseAuth.getInstance();

        ComposeView composeView = view.findViewById(R.id.compose_profile_register);

        ProfileKt.bindRegisterContent(
                composeView,
                // onRegister(firstName, lastName, email, confirmEmail, password, confirmPassword)
                new Function6<String, String, String, String, String, String, Unit>() {
                    @Override
                    public Unit invoke(String firstName, String lastName,
                                       String email, String confirmEmail,
                                       String password, String confirmPassword) {
                        registerWithEmail(firstName, lastName, email, confirmEmail, password, confirmPassword);
                        return Unit.INSTANCE;
                    }
                },
                // onBackToSignIn()
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                        return Unit.INSTANCE;
                    }
                }
        );

        return view;
    }

    // moved here from ProfileFragment
    private void registerWithEmail(
            String firstName,
            String lastName,
            String email,
            String confirmEmail,
            String password,
            String confirmPassword
    ) {
        final String fName = firstName == null ? "" : firstName.trim();
        final String lName = lastName == null ? "" : lastName.trim();
        final String e     = email == null ? "" : email.trim();
        final String ce    = confirmEmail == null ? "" : confirmEmail.trim();
        final String p     = password == null ? "" : password.trim();
        final String cp    = confirmPassword == null ? "" : confirmPassword.trim();

        if (fName.isEmpty() || lName.isEmpty()) {
            Toast.makeText(getContext(), "Name and surname required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (e.isEmpty() || ce.isEmpty()) {
            Toast.makeText(getContext(), "Email required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!e.equals(ce)) {
            Toast.makeText(getContext(), "Emails do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p.isEmpty() || cp.isEmpty()) {
            Toast.makeText(getContext(), "Password required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!p.equals(cp)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p.length() < 6) {
            Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(e, p)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) return;

                    String fullName = fName + " " + lName;
                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                    user.updateProfile(profileUpdates);

                    user.sendEmailVerification()
                            .addOnCompleteListener(v -> {
                                if (v.isSuccessful()) {
                                    Toast.makeText(getContext(),
                                            "Verification email sent to " + e,
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                    // Go back to ProfileFragment (now signed in)
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
    }
}
