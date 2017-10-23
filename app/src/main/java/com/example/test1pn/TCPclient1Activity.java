package com.example.test1pn;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
/**/
//last allocated tag:Vladi28
public class TCPclient1Activity extends ActionBarActivity implements CgetStrDiag.CgetStrDiagListener {
    static final String TAG1 = "TCPc1A";
    static EditText et1; //Vl.editTextTCPlogger1
    CgetStrDiag newF;//V.newFragment Dialog based on alert dialog
    Toast t;
    java.text.SimpleDateFormat sdfHMsS; //Vl.just HourMinuteSecondMilliseconds
    java.text.SimpleDateFormat sdf; //Vl.YearMonthDayHourMinuteSecondMilliseconds
//    java.util.concurrent.ArrayBlockingQueue<EVENT> eventArrayBlockingQueue;
    private static Intent tcpClient1ServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);// call the super class onCreate to complete the creation of activity like the view hierarchy
        t = Toast.makeText(getBaseContext(), "", Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        setContentView(R.layout.activity_tcpclient1);
        et1 = (EditText) findViewById(R.id.editTextTCPlogger1);
        sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdfHMsS = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
        Vsupport1.log(et1, "\n" + sdf.format(System.currentTimeMillis()) + TAG1 + ".onCreate()" +
                ", tcpClient1ServiceIntent=" + tcpClient1ServiceIntent);
    }
    @Override
    protected void onStart() {
        super.onStart(); //Derived classes must call through to the super class's implementation of this method.(Android doc)
        Vsupport1.log(et1, "\n" + TAG1 + ".onStart()");
    }
    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first(Android doc)(https://developer.android.com/guide/components/activities/activity-lifecycle.html)
        Vsupport1.log(et1, "\n" + TAG1 + ".onResume()" + sdf.format(System.currentTimeMillis()));
    }
    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first (Android doc)
        Vsupport1.log(et1, "\nV.from onPause()");
    }
    @Override
    protected void onStop() {
        super.onStop();// call the superclass method first (Android doc)
        Vsupport1.log(et1, "\nV.from OnStop()");
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Vsupport1.log(et1, "\n" + sdf.format(System.currentTimeMillis()) + ", Vl./TCPc1A., from onRestoreInstanceState(bundle)");
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Vsupport1.log(et1, "\n" + sdf.format(System.currentTimeMillis()) + ", Vl./TCPc1A., from onSaveInstanceState(bundle)");
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onDestroy() {
//        Vsupport1.log(MainActivity.ev1, "\nVl./TCPc1A., from onDestroy()");
        Vsupport1.textFileLog("\n" + sdf.format(System.currentTimeMillis()) +
                "\nVl./TCPc1A., from onDestroy()", App.bufW1);
//        try {
//            App.bufW1.close();
//        } catch (IOException e) {
////            Vsupport1.log(MainActivity.ev1, "\nVl./TCPc1A., at log file close.. " + e.toString());
//        }
        super.onDestroy();
    }
//    @Override
//    public void onBackPressed() {
//        final ro.vladi.utils1lib.ChooseDialog cd = new ro.vladi.utils1lib.ChooseDialog(this);
//        cd.set("Reconfirm", "Leave Activity?", "Yes", "No");
//        new Thread() {
//            @Override
//            public void run() {
//                if (cd.get()) {
//                    TCPclient1Activity.this.finish();
//                }
//            }
//        }.start();
////        ChooseDialog cd = new ChooseDialog();
//    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        MainActivity.ev1.append("\nVladi1.Test from TCPclient1Activity == TCPc1A");
        getMenuInflater().inflate(R.menu.menu1_tcpclient1activity, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem mi = menu.findItem(R.id.connect);
        if (TCPclient1Service.instance != null)
            if (TCPclient1Service.instance.tcpState == TCP_STATE.CONNECTED) mi.setEnabled(false);
            else mi.setEnabled(true);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent i;
        switch (id) {
            case R.id.settings1:
                i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.connect:
                tcpClient1ServiceIntent = new Intent(this, TCPclient1Service.class);
                startService(tcpClient1ServiceIntent);
                break;
            case  R.id.mess1: //Vl.sending one line message
                if (newF == null) newF = new CgetStrDiag();
                newF.title = "Sending Message";
                newF.msg = "One line message towards connected TCPserver:";
                newF.show(getFragmentManager(), "getStrDiag1");
                break;
            case R.id.stop_c1_service:
                final ro.vladi.utils1lib.ChooseDialog cd = new ro.vladi.utils1lib.ChooseDialog(this);
                cd.set("Reconfirm", "Destroy TCPclient1Service?!!", "Yes", "No");
                new Thread() {
                    @Override
                    public void run() {
                        if (cd.get()) {
                            if (tcpClient1ServiceIntent != null) {
                                if (!stopService(tcpClient1ServiceIntent)) Vsupport1.log(et1, "\nVl28.stopService() returned false..");
                                else  tcpClient1ServiceIntent = null;
                            }
                        }
                    }
                }.start();
        }
        return super.onOptionsItemSelected(item);
    }
    // V. CgetStrDiag.CgetStrDiagListener interface implementation:
    @Override
    public void onDialogPositiveClick(CgetStrDiag dialog) {
        String msg;
        msg = dialog.getUserInput() + "\n";
        if (TCPclient1Service.instance != null) TCPclient1Service.instance.writeOverTCP(msg);
    }

    @Override
    public void onDialogNegativeClick(CgetStrDiag dialog) {
        MainActivity.ev1.append("\nVladi15.Test2 from TCPc1A");
        String msg = dialog.getUserInput();
        if (msg.contains("Vladi15")) {
            Thread th1 = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        System.out.println("Vladi15 th1 thread, " + e.toString());//Vl.redirected to LogCat as Log.i()
                        MainActivity.ev1.append("Vladi15 th1 thread, \n" + e.toString() + "\n");
                    }
                    MainActivity.ev1.append("\nVladi15.Test_3 from a TCPc1A thread != main activity thread");
                }
            };
            th1.start();
            if (msg.contentEquals("Vladi157")) {
                try {Thread.sleep(500);} catch (InterruptedException e1) {}
                th1.interrupt();
            }
        }
//        switch (uir) {
//            case SHA256HMACenc:
//                this.strVar1 = "";
//                break;
//            default:
//                break;
//        }
    }
}
