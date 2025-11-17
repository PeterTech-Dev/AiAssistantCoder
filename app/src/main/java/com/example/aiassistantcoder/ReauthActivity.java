package com.example.aiassistantcoder;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;

import com.example.aiassistantcoder.ui.ProfileKt;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ReauthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ComposeView composeView;  // we'll reuse this for both steps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reauth);

        mAuth = FirebaseAuth.getInstance();
        composeView = findViewById(R.id.compose_reauth);

        showReauthStep();
    }

    /** Step 1: ask user for their current password and re-authenticate them. */
    private void showReauthStep() {
        ProfileKt.bindReauthPasswordContent(
                composeView,
                new Function1<String, Unit>() {
                    @Override
                    public Unit invoke(String password) {
                        String pwd = password == null ? "" : password.trim();
                        if (pwd.isEmpty()) {
                            Toast.makeText(ReauthActivity.this,
                                    "Please enter your password",
                                    Toast.LENGTH_SHORT).show();
                            return Unit.INSTANCE;
                        }

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null || user.getEmail() == null) {
                            Toast.makeText(ReauthActivity.this,
                                    "No logged-in user.",
                                    Toast.LENGTH_SHORT).show();
                            return Unit.INSTANCE;
                        }

                        AuthCredential credential =
                                EmailAuthProvider.getCredential(user.getEmail(), pwd);

                        user.reauthenticate(credential)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Go to step 2 (new password screen)
                                        showNewPasswordStep();
                                    } else {
                                        Toast.makeText(ReauthActivity.this,
                                                "Re-authentication failed.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                        return Unit.INSTANCE;
                    }
                }
        );
    }

    /** Step 2: ask for new password (Compose validates length & confirm) and update in Firebase. */
    private void showNewPasswordStep() {
        ProfileKt.bindNewPasswordContent(
                composeView,
                new Function1<String, Unit>() {
                    @Override
                    public Unit invoke(String newPassword) {
                        String np = newPassword == null ? "" : newPassword.trim();

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(ReauthActivity.this,
                                    "No logged-in user.",
                                    Toast.LENGTH_SHORT).show();
                            return Unit.INSTANCE;
                        }

                        user.updatePassword(np)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ReauthActivity.this,
                                                "Password updated. Please log in again.",
                                                Toast.LENGTH_LONG).show();
                                        mAuth.signOut();
                                        finish();
                                    } else {
                                        Toast.makeText(ReauthActivity.this,
                                                "Failed to update password.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                        return Unit.INSTANCE;
                    }
                }
        );
    }
}
