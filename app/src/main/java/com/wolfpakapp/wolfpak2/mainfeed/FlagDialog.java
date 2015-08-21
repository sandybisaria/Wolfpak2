package com.wolfpakapp.wolfpak2.mainfeed;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.wolfpakapp.wolfpak2.R;

import java.util.Random;

/**
 * Created by Syed Raziq Mohideen on 8/20/2015.
 */
public class FlagDialog extends DialogFragment {

    public interface FlagDialogListener {
        void onDialogPositiveClick();
        void onDialogNegativeClick();
        void onDialogCanceled();
    }

    // Use this instance of the interface to deliver action events
    private FlagDialogListener mListener;

    public void setFlagDialogListener(FlagDialogListener flagDialogListener)  {
        mListener = flagDialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_flag, null);
        Button reportButton = (Button) view.findViewById(R.id.dialog_flag_report_button);
        Button cancelButton = (Button) view.findViewById(R.id.dialog_flag_cancel_button);

        final String captchaString = generateRandomString();
        TextView captchaTextView = (TextView) view.findViewById(R.id.dialog_flag_captcha_text_view);
        captchaTextView.setText(captchaString);

        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText inputEditText = (EditText) view.findViewById(R.id.dialog_flag_input_edit_text);
                if (captchaString.equals(inputEditText.getText().toString())) {
                    mListener.onDialogPositiveClick();
                    getDialog().dismiss();
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDialogNegativeClick();
                getDialog().cancel();
            }
        });

        builder.setView(view);
        Dialog dialog = builder.create();

        // Required for the rounding of corners to be visible (else sharp corners will be visible).
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    /**
     * Generate a random String for the captcha.
     **/
    private String generateRandomString() {
        char[] charArray = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            char c = charArray[random.nextInt(charArray.length)];
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mListener.onDialogCanceled();
        super.onCancel(dialog);
    }
}
