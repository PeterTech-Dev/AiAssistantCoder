package com.example.aiassistantcoder;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;

import com.example.aiassistantcoder.ui.ProfileKt;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function6;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);  // 🔹 uses your activity_register.xml

        mAuth = FirebaseAuth.getInstance();

        ComposeView composeView = findViewById(R.id.compose_register);

        // Hook up the Compose register UI
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
                        finish(); // just close this activity and go back to ProfileActivity
                        return Unit.INSTANCE;
                    }
                }
        );
    }

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
            Toast.makeText(this, "Name and surname required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (e.isEmpty() || ce.isEmpty()) {
            Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!e.equals(ce)) {
            Toast.makeText(this, "Emails do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p.isEmpty() || cp.isEmpty()) {
            Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!p.equals(cp)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(e, p)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
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
                                    Toast.makeText(this,
                                            "Verification email sent to " + e,
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                    // after successful registration, go back to sign-in/profile
                    finish();
                });
    }
}
