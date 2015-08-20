package com.wolfpakapp.wolfpak2.camera.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.wolfpakapp.wolfpak2.R;

/**
 * Dialog for prompting user upload details before sending to server
 * @author Roland Fong
 */
public class UploadDialog extends DialogFragment {

    private String handle;
    private EditText title;
    private Switch nsfw;
    private Button post;
    private Button cancel;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface UploadDialogListener {
        void onDialogPositiveClick(UploadDialog dialog);
        void onDialogNegativeClick(UploadDialog dialog);
        void onDialogCanceled(UploadDialog dialog);
    }

    // Use this instance of the interface to deliver action events
    private UploadDialogListener mListener;

    public void setUploadDialogListener(UploadDialogListener uploadDialogListener)  {
        mListener = uploadDialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_upload, null);
        title = (EditText) view.findViewById(R.id.txt_title);
        nsfw = (Switch) view.findViewById(R.id.sw_nsfw);
        post = (Button) view.findViewById(R.id.btn_post);
        cancel = (Button) view.findViewById(R.id.btn_cancel);

        post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
                mListener.onDialogPositiveClick(UploadDialog.this);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                mListener.onDialogNegativeClick(UploadDialog.this);
            }
        });
        // Inflate and set the layout for the dialog
        builder.setView(view);
//                .setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//                        mListener.onDialogPositiveClick(UploadDialog.this);
//                    }
//                })
//                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        getDialog().cancel();
//                        mListener.onDialogNegativeClick(UploadDialog.this);
//                    }
//                });

        Dialog dialog = builder.create();
        // required for the rounding of corners to be visible (else sharp corners will be visible)
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    /**
     * @return isNsfw
     */
    public boolean isNsfw() {
        return nsfw.isChecked();
    }

    /**
     * @return the title
     */
    public String getHandle() {
        handle = title.getText().toString();
        return handle;
    }

    /**
     * Sets the title
     * @param handle the title of the upload
     */
    public void setHandle(String handle) {
        this.handle = handle;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mListener.onDialogCanceled(UploadDialog.this);
    }
    
}