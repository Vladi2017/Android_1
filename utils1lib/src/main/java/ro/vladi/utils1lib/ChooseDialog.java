package ro.vladi.utils1lib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
//import android.support.v7.app.AlertDialog;

/**
 * Created by Vladi on 9/25/17 10:13PM based on https://stackoverflow.com/a/2478662/3180812
 * The bible is here: https://developer.android.com/studio/projects/android-library.html
 */

/**
 * Simple ChooseDialog box for convenience simple reuse purposes.. Returns boolean.. Intended usage:
 *
 * <p>{@code ro.vladi.utils1lib.ChooseDialog cd = new ro.vladi.utils1lib.ChooseDialog(this);}</p>
 * <p>{@code cd.set("Title", "message", "Yes", "No"); //Vl. or: cd.setDefault();}</p>
 * <p>{@code if (cd.get()) ..
 *     else ..
 *     }</p>
 *
 * @author Vladi, based on https://stackoverflow.com/a/2478662/3180812
 *
 */
public class ChooseDialog {
    private final String title, message, posButtText, negButtText;
    private boolean result;
    private AlertDialog.Builder builder;
//    private ChooseDialog cdContext;
    private Activity callerContext;
    private final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    result = true;
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    result = false;
                    break;
            }
            synchronized (this) {
                notify();
            }
        }
    };
    public ChooseDialog(Activity context) {
        builder = new AlertDialog.Builder(context);
        title = "Chose Dialog";
        message = "Please select Yes/true No/false";
        negButtText = "No";
        posButtText = "Yes";
//        cdContext = this;
        callerContext = context;
    }
    public void set(String title, String message, String posButtText, String negButtText) {
        builder.setTitle(title).setMessage(message).setPositiveButton(posButtText, dialogClickListener)
                .setNegativeButton(negButtText, dialogClickListener);
    }
    public void setDefault() {
        builder.setTitle(title).setMessage(message).setPositiveButton(posButtText, dialogClickListener)
                .setNegativeButton(negButtText, dialogClickListener);
    }
    public boolean get() {
        callerContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.show();
            }
        });
        synchronized (dialogClickListener) {
            try {
                dialogClickListener.wait();
            } catch (InterruptedException e) {}
        }
        return result;
    }
}

