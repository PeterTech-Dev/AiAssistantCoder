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
import com.example.aiassistantcoder.ui.SnackBarApp;
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
                    SnackBarApp.INSTANCE.show(
                            requireActivity().findViewById(android.R.id.content),
                            "Verification failed: " + e.getMessage(),
                            SnackBarApp.Type.ERROR
                    );

                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    phoneVerificationId = verificationId;
                    phoneResendToken = token;
                    SnackBarApp.INSTANCE.show(
                            requireActivity().findViewById(android.R.id.content),
                            "Code sent",
                            SnackBarApp.Type.SUCCESS
                    );

                }
            };

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    SnackBarApp.INSTANCE.show(
                            requireActivity().findViewById(android.R.id.content),
                            "Google sign-in cancelled",
                            SnackBarApp.Type.ERROR
                    );

                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    SnackBarApp.INSTANCE.show(
                            requireActivity().findViewById(android.R.id.content),
                            "Google sign-in failed",
                            SnackBarApp.Type.ERROR
                    );

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
                            SnackBarApp.INSTANCE.show(
                                    requireActivity().findViewById(android.R.id.content),
                                    "Email and Password required",
                                    SnackBarApp.Type.WARNING
                            );

                            return Unit.INSTANCE;
                        }
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        SnackBarApp.INSTANCE.show(
                                                requireActivity().findViewById(android.R.id.content),
                                                "Authentication failed",
                                                SnackBarApp.Type.ERROR
                                        );

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
                            SnackBarApp.INSTANCE.show(
                                    requireActivity().findViewById(android.R.id.content),
                                    "Enter your email first",
                                    SnackBarApp.Type.WARNING
                            );
                            return Unit.INSTANCE;
                        }
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        SnackBarApp.INSTANCE.show(
                                                requireActivity().findViewById(android.R.id.content),
                                                "Reset link sent",
                                                SnackBarApp.Type.SUCCESS
                                        );
                                    } else {
                                        SnackBarApp.INSTANCE.show(
                                                requireActivity().findViewById(android.R.id.content),
                                                "Failed to send reset link",
                                                SnackBarApp.Type.ERROR
                                        );
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
            SnackBarApp.INSTANCE.show(
                    requireActivity().findViewById(android.R.id.content),
                    "Enter phone number",
                    SnackBarApp.Type.WARNING
            );
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
            SnackBarApp.INSTANCE.show(
                    requireActivity().findViewById(android.R.id.content),
                    "Request a code first",
                    SnackBarApp.Type.WARNING
            );
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
                        SnackBarApp.INSTANCE.show(
                                requireActivity().findViewById(android.R.id.content),
                                "Signed in with phone",
                                SnackBarApp.Type.SUCCESS
                        );
                    } else {
                        SnackBarApp.INSTANCE.show(
                                requireActivity().findViewById(android.R.id.content),
                                "Phone sign-in failed",
                                SnackBarApp.Type.ERROR
                        );
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
                        SnackBarApp.INSTANCE.show(
                                requireActivity().findViewById(android.R.id.content),
                                "Google auth failed",
                                SnackBarApp.Type.ERROR
                        );
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
                                        SnackBarApp.INSTANCE.show(
                                                requireActivity().findViewById(android.R.id.content),
                                                "Display name updated",
                                                SnackBarApp.Type.SUCCESS
                                        );
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
