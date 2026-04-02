package com.drc.aidbridge.ui.common;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.R;

import java.util.List;

/**
 * Reusable controller for 6-digit OTP style input boxes.
 */
public class OtpInputController {

    public interface BoxStateListener {
        void onChanged(@NonNull EditText box, boolean filled);
    }

    public interface OtpCompleteListener {
        void onCompleted(@NonNull String otp);
    }

    private final List<EditText> boxes;
    private final BoxStateListener boxStateListener;
    private final OtpCompleteListener otpCompleteListener;

    public OtpInputController(@NonNull List<EditText> boxes,
                              @Nullable BoxStateListener boxStateListener) {
        this(boxes, boxStateListener, null);
    }

    public OtpInputController(@NonNull List<EditText> boxes,
                              @Nullable BoxStateListener boxStateListener,
                              @Nullable OtpCompleteListener otpCompleteListener) {
        this.boxes = boxes;
        this.boxStateListener = boxStateListener;
        this.otpCompleteListener = otpCompleteListener;
    }

    public void bind() {
        for (int i = 0; i < boxes.size(); i++) {
            final int index = i;
            final EditText box = boxes.get(i);

            box.setOnFocusChangeListener((v, hasFocus) -> applyBoxVisualState(box));

            box.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    applyBoxVisualState(box);
                    if (boxStateListener != null) {
                        boxStateListener.onChanged(box, s.length() > 0);
                    }
                    if (s.length() == 1 && index < boxes.size() - 1) {
                        boxes.get(index + 1).requestFocus();
                    }

                    if (otpCompleteListener != null && isComplete()) {
                        otpCompleteListener.onCompleted(collectOtp());
                    }
                }
            });

            box.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DEL
                        && box.getText() != null
                        && box.getText().length() == 0
                        && index > 0) {
                    EditText previousBox = boxes.get(index - 1);
                    previousBox.requestFocus();
                    previousBox.setText("");
                    return true;
                }
                return false;
            });

            applyBoxVisualState(box);
        }
        focusFirst();
    }

    public void focusFirst() {
        if (!boxes.isEmpty()) {
            boxes.get(0).requestFocus();
        }
    }

    @NonNull
    public String collectOtp() {
        StringBuilder builder = new StringBuilder();
        for (EditText box : boxes) {
            builder.append(box.getText() != null ? box.getText().toString() : "");
        }
        return builder.toString();
    }

    public void clear() {
        for (EditText box : boxes) {
            box.setText("");
            if (boxStateListener != null) {
                boxStateListener.onChanged(box, false);
            }
        }
        focusFirst();
    }

    private boolean isComplete() {
        for (EditText box : boxes) {
            if (box.getText() == null || box.getText().length() != 1) {
                return false;
            }
        }
        return !boxes.isEmpty();
    }

    private void applyBoxVisualState(@NonNull EditText box) {
        boolean highlighted = box.hasFocus();
        box.setBackgroundResource(highlighted ? R.drawable.bg_otp_box_active : R.drawable.bg_otp_box);
    }
}
