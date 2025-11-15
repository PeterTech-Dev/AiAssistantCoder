package com.example.aiassistantcoder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;

import com.example.aiassistantcoder.ui.ProfileKt;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.concurrent.TimeUnit;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authListener;

    private LinearLayout loggedInView, loggedOutView;
    private ComposeView loggedInCompose, loggedOutCompose;

    private GoogleSignInClient mGoogleSignInClient;

    // phone auth
    private String phoneVerificationId;
    private PhoneAuthProvider.ForceResendingToken phoneResendToken;

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks phoneCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Instant or auto-retrieval
                    signInWithPhoneCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(getContext(),
                            "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    phoneVerificationId = verificationId;
                    phoneResendToken = token;
                    Toast.makeText(getContext(), "Code sent", Toast.LENGTH_SHORT).show();
                }
            };

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    Toast.makeText(getContext(), "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(getContext(), "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        authListener = firebaseAuth -> updateUI(firebaseAuth.getCurrentUser());

        loggedInView     = view.findViewById(R.id.logged_in_view);
        loggedOutView    = view.findViewById(R.id.logged_out_view);
        loggedInCompose  = view.findViewById(R.id.logged_in_compose);
        loggedOutCompose = view.findViewById(R.id.logged_out_compose);

        setupGoogleSignIn();

        // ---------- SIGN-IN COMPOSE UI ----------
        ProfileKt.bindSignInContent(
                loggedOutCompose,

                // onEmailSignIn(email, password)
                new Function2<String, String, Unit>() {
                    @Override
                    public Unit invoke(String email, String password) {
                        email = email.trim();
                        password = password.trim();
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(getContext(), "Email and password required", Toast.LENGTH_SHORT).show();
                            return Unit.INSTANCE;
                        }
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                                    }
                                    // authListener will update UI
                                });
                        return Unit.INSTANCE;
                    }
                },

                // onGoogleClick()
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        googleSignInLauncher.launch(signInIntent);
                        return Unit.INSTANCE;
                    }
                },

                // onPhoneSendCode(phone)
                new Function1<String, Unit>() {
                    @Override
                    public Unit invoke(String phone) {
                        sendPhoneVerificationCode(phone.trim());
                        return Unit.INSTANCE;
                    }
                },

                // onPhoneVerifyCode(code)
                new Function1<String, Unit>() {
                    @Override
                    public Unit invoke(String code) {
                        verifyPhoneCode(code.trim());
                        return Unit.INSTANCE;
                    }
                },

                // onForgotPassword(email)
                new Function1<String, Unit>() {
                    @Override
                    public Unit invoke(String email) {
                        email = email.trim();
                        if (email.isEmpty()) {
                            Toast.makeText(getContext(), "Enter your email first", Toast.LENGTH_SHORT).show();
                            return Unit.INSTANCE;
                        }
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Reset link sent", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show();
                                    }
                                });
                        return Unit.INSTANCE;
                    }
                },

                // onRegisterClick() -> open RegisterActivity
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        Intent intent = new Intent(requireActivity(), RegisterActivity.class);
                        startActivity(intent);
                        requireActivity().overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                        );
                        return Unit.INSTANCE;
                    }
                }
        );

        return view;
    }

    // ---------- PHONE HELPERS ----------

    private void sendPhoneVerificationCode(String phone) {
        if (phone.isEmpty()) {
            Toast.makeText(getContext(), "Enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phone) // must be in +E.164 format (+27...)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(phoneCallbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneCode(String code) {
        if (phoneVerificationId == null || code.isEmpty()) {
            Toast.makeText(getContext(), "Request a code first", Toast.LENGTH_SHORT).show();
            return;
        }
        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(phoneVerificationId, code);
        signInWithPhoneCredential(credential);
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Signed in with phone", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Phone sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (authListener != null) mAuth.addAuthStateListener(authListener);
        updateUI(mAuth.getCurrentUser());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) mAuth.removeAuthStateListener(authListener);
    }

    // =================== GOOGLE HELPERS ===================
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(), "Google auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =================== UI ===================
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            loggedInView.setVisibility(View.VISIBLE);
            loggedOutView.setVisibility(View.GONE);

            String welcome;
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                welcome = "Welcome, " + user.getDisplayName();
            } else if (user.getEmail() != null) {
                welcome = "Welcome, " + user.getEmail();
            } else if (user.getPhoneNumber() != null) {
                welcome = "Welcome, " + user.getPhoneNumber();
            } else {
                welcome = "Welcome!";
            }

            ProfileKt.bindLoggedInContent(
                    loggedInCompose,
                    welcome,
                    new Function1<String, Unit>() {
                        @Override
                        public Unit invoke(String newName) {
                            newName = newName.trim();
                            if (newName.isEmpty()) return Unit.INSTANCE;
                            FirebaseUser u = mAuth.getCurrentUser();
                            if (u != null) {
                                UserProfileChangeRequest req =
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(newName)
                                                .build();
                                u.updateProfile(req).addOnCompleteListener(t -> {
                                    if (t.isSuccessful()) {
                                        Toast.makeText(getContext(), "Display name updated.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            return Unit.INSTANCE;
                        }
                    },
                    // onResetPassword()
                    new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            startActivity(new Intent(getActivity(), ReauthActivity.class));
                            return Unit.INSTANCE;
                        }
                    },
                    // onSignOut()
                    new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            mAuth.signOut();
                            return Unit.INSTANCE;
                        }
                    }
            );

        } else {
            loggedInView.setVisibility(View.GONE);
            loggedOutView.setVisibility(View.VISIBLE);
        }
    }
}
