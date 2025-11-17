package com.example.aiassistantcoder;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class PhoneSignInDialog extends DialogFragment {

    // UI
    private CountryCodePicker ccp;
    private EditText phoneField, codeField;
    private Button sendCodeBtn, verifyBtn, resendBtn;
    private ImageButton closeBtn;
    private TextView countdownText;

    // Firebase phone auth
    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // Resend cooldown
    private CountDownTimer resendTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.bottom_sheet_phone_sign_in, container, false);

        mAuth = FirebaseAuth.getInstance();

        // ---- bind views ----
        ccp           = v.findViewById(R.id.ccp);
        phoneField    = v.findViewById(R.id.phone_field);
        codeField     = v.findViewById(R.id.code_field);
        sendCodeBtn   = v.findViewById(R.id.btn_send_code);
        verifyBtn     = v.findViewById(R.id.btn_verify_code);
        resendBtn     = v.findViewById(R.id.btn_resend_code);
        closeBtn      = v.findViewById(R.id.close_button);
        countdownText = v.findViewById(R.id.tv_countdown);

        // Link CCP to phone input so it formats +E.164 automatically
        ccp.registerCarrierNumberEditText(phoneField);
        ccp.setAutoDetectedCountry(true);

        // Close dialog
        closeBtn.setOnClickListener(view -> dismiss());

        // Send OTP
        sendCodeBtn.setOnClickListener(view -> {
            if (ccp.isValidFullNumber()) {
                String e164 = ccp.getFullNumberWithPlus();
                sendVerificationCode(e164, /*useResendToken*/ false);
            } else {
                Toast.makeText(getContext(), "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
        });

        // Resend OTP
        resendBtn.setOnClickListener(view -> {
            if (!ccp.isValidFullNumber()) {
                Toast.makeText(getContext(), "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            String e164 = ccp.getFullNumberWithPlus();
            sendVerificationCode(e164, /*useResendToken*/ true);
        });

        // Verify OTP
        verifyBtn.setOnClickListener(view -> {
            String code = codeField.getText().toString().trim();
            if (!TextUtils.isEmpty(code) && verificationId != null) {
                PhoneAuthCredential cred = PhoneAuthProvider.getCredential(verificationId, code);
                signInWithPhoneCredential(cred);
            } else {
                Toast.makeText(getContext(), "Enter the code", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    // Center the dialog & apply square background/dim
    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            d.getWindow().setBackgroundDrawableResource(R.drawable.dialog_square_background);
            d.getWindow().setGravity(Gravity.CENTER);
            d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setDimAmount(0.5f);
            d.setCanceledOnTouchOutside(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelResendTimer();
    }

    // --------------------------------------------------------
    // Phone auth helpers
    // --------------------------------------------------------

    private void sendVerificationCode(String e164, boolean useResendToken) {
        // Disable send/resend to prevent multiple taps
        sendCodeBtn.setEnabled(false);
        resendBtn.setEnabled(false);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(e164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(callbacks);

        if (useResendToken && resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
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

                    sendCodeBtn.setEnabled(true);
                    resendBtn.setEnabled(false);
                    cancelResendTimer();
                    if (countdownText != null) countdownText.setText(" ");
                }

                @Override
                public void onCodeSent(@NonNull String s,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = s;
                    resendToken = token;
                    Toast.makeText(getContext(), "OTP sent", Toast.LENGTH_SHORT).show();

                    sendCodeBtn.setEnabled(true);
                    startResendCooldown(30); // 30 seconds
                }
            };

    private void startResendCooldown(int seconds) {
        resendBtn.setEnabled(false);
        if (countdownText != null) {
            countdownText.setText("Resend in " + seconds + "s");
        }

        cancelResendTimer();

        resendTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int s = (int) (millisUntilFinished / 1000L);
                if (countdownText != null) {
                    countdownText.setText("Resend in " + s + "s");
                }
            }

            @Override
            public void onFinish() {
                if (countdownText != null) countdownText.setText(" ");
                resendBtn.setEnabled(true);
            }
        }.start();
    }

    private void cancelResendTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        verifyBtn.setEnabled(false);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    verifyBtn.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Signed in with phone", Toast.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        Toast.makeText(getContext(), "Phone sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
