package com.example.test1pn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
/**/
/**
 * V. posible refference for simple input text dialog prompt
 * */
public class CgetStrDiag extends DialogFragment {
	private String gotSeq = "";//V.getSequence
	String title = "default title", msg = "default message";
	/* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface CgetStrDiagListener {
        public void onDialogPositiveClick(CgetStrDiag dialog);
        public void onDialogNegativeClick(CgetStrDiag dialog);
    }
    // Use this instance of the interface to deliver action events
    CgetStrDiagListener mListener;
    // Override the Fragment.onAttach() method to instantiate the CgetStrDiagListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the CgetStrDiagListener so we can send events to the host
            mListener = (CgetStrDiagListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement CgetStrDiagListener");
        }
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(msg);
		// Set up the input
		final EditText input = new EditText(getActivity());
		// Specify the type of input expected;
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(input);
		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
                // Send the positive button event back to the host activity
				gotSeq = input.getText().toString();
                mListener.onDialogPositiveClick(CgetStrDiag.this);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
                // Send the negative button event back to the host activity
                mListener.onDialogNegativeClick(CgetStrDiag.this);
			}
		});
		// Create the AlertDialog object and return it
		return builder.create();
	}
	
	public String getUserInput() {
		return gotSeq;
	}
}