package com.example.test1pn;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
/**/
enum TCP_STATE {NOT_CONNECTED, PRE_CONNECTING, CONNECTING, CONNECTED}
enum EVENT {STATE_OUT_OF_SERVICE, CALL_STATE_RINGING, CALL_STATE_OFFHOOK, DataConnectivity_UP, OptionsMenuItemCONNECT,
    ActiveNetworkConnectedConfirmation, NO_ActiveNetworkConnectedConfirmation, TCP_CONNECTING_FAILED, TCP_CONNECTED,
    TCP_CONNECT_NO_ACTIVE_NETWORK_CONNECTED, SERVER_DISCONNECTED, TCP_HALF_OPEN,
    /*for GPRS class B devices*/DATA_SUSPENDED, DataConnectivity_DOWN}
//last allocated tag:Vladi26
public class TCPclient1Activity extends ActionBarActivity implements CgetStrDiag.CgetStrDiagListener {
    EditText et1; //Vl.editTextTCPlogger1
    private SocketChannel sc = null;
    private InetSocketAddress isa = null;
    public RecvThread rt = null;
    SharedPreferences sharedPref;
    CgetStrDiag newF;//V.newFragment Dialog based on alert dialog
    Toast t;
    java.text.SimpleDateFormat sdfHMsS; //Vl.just HourMinuteSecondMilliseconds
    java.text.SimpleDateFormat sdf; //Vl.YearMonthDayHourMinuteSecondMilliseconds
    TCP_STATE tcpState;
    //Vl.enum EVENT related
    boolean fTCP_AUTO_CONNECT = false; //Vl. f=flag
    final int TCP_FAIL_MAX_AUTO_RECONNECT = 3; //TODO: this should be SharedPreferences
    int cntReconnect;
    private boolean fTCP_FAIL_ONLY;//Vl.suspicious redundant, we can test cntReconnect > 0
    boolean wasCellularCall = false;
    //media
    android.net.Uri alarm1URI = null;
    android.media.Ringtone rt1;
//Vl. server ka receiving thread vars
    private byte[] sendArr = {'r','e','s','p','o','n','s','e',0,0,0,0,'d','e','l','a','y',0};
    private int seq = 327;
    private ByteBuffer buf = ByteBuffer.allocate(32);
    private ByteBuffer bufref1 = ByteBuffer.wrap("keepalive".getBytes());
    private ByteBuffer bufref1ro = bufref1.asReadOnlyBuffer();
    private ByteBuffer sendBuf = ByteBuffer.wrap(sendArr);
    private Charset charset = Charset.forName("us-ascii");
    private java.nio.charset.CharsetDecoder decoder = charset.newDecoder();
    //telephony related
    private android.telephony.TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    //(data) Connectivity related
    ConnectivityEventsReceiver connectivityEventsReceiver;
    android.net.NetworkInfo activeNetwork; //Requires the ACCESS_NETWORK_STATE permission.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);// call the super class onCreate to complete the creation of activity like the view hierarchy
        t = Toast.makeText(getBaseContext(), "", Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        telephonyManager = (android.telephony.TelephonyManager) getSystemService(android.content.Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener();
        sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdfHMsS = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
        tcpState = TCP_STATE.NOT_CONNECTED;
        connectivityEventsReceiver = new ConnectivityEventsReceiver();
        setContentView(R.layout.activity_tcpclient1);
        et1 = (EditText) findViewById(R.id.editTextTCPlogger1);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE
                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
        this.registerReceiver(connectivityEventsReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }
    @Override
    protected void onStart() {
        super.onStart(); //Derived classes must call through to the super class's implementation of this method.(Android doc)
        Vsupport1.log(et1, "\nV.from onStart()");
    }
    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first(Android doc)(https://developer.android.com/guide/components/activities/activity-lifecycle.html)
        Vsupport1.log(et1, "\nV.from onResume()");
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
    protected void onDestroy() {
        Vsupport1.log(MainActivity.ev1, "\nVl./TCPc1A., from onDestroy()");
        this.unregisterReceiver(connectivityEventsReceiver);
        try {
            App.bufW1.close();
        } catch (IOException e) {
            Vsupport1.log(MainActivity.ev1, "\nVl./TCPc1A., at log file close.. " + e.toString());
        }
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        final ro.vladi.utils1lib.ChooseDialog cd = new ro.vladi.utils1lib.ChooseDialog(this);
        cd.set("Reconfirm", "Leave Activity?", "Yes", "No");
        new Thread() {
            @Override
            public void run() {
                if (cd.get()) {
                    fTCP_AUTO_CONNECT = false;
                    if (sc != null) try {
                        sc.close();
                    } catch (IOException e) {
                        Vsupport1.log(MainActivity.ev1, "\nVladi20/TCPc1A.., SocketChannel.close() IOException on backButton: +" +
                                e.toString());
                    }
                    TCPclient1Activity.this.finish();
                }
            }
        }.start();
//        ChooseDialog cd = new ChooseDialog();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MainActivity.ev1.append("\nVladi1.Test from TCPclient1Activity == TCPc1A");
        getMenuInflater().inflate(R.menu.menu1_tcpclient1activity, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem mi = menu.findItem(R.id.connect);
        if (fTCP_AUTO_CONNECT) mi.setEnabled(false);
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
                fTCP_AUTO_CONNECT = true;
                cntReconnect = 0;
                tcpFSM(EVENT.OptionsMenuItemCONNECT);
                break;
            case  R.id.mess1: //Vl.sending one line message
                if (newF == null) newF = new CgetStrDiag();
                newF.title = "Sending Message";
                newF.msg = "One line message towards connected TCPserver:";
                newF.show(getFragmentManager(), "getStrDiag1");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    final boolean SUCCEED = true, FAIL = false;
    ConnectivityManager cm;
    TestDataChannelBR testDataChannelBR;
    private class TestDataChannelBR extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);
            if (activeNetworkConnected()) tcpFSM(EVENT.ActiveNetworkConnectedConfirmation);
            else tcpFSM(EVENT.NO_ActiveNetworkConnectedConfirmation);
        }
    }

    private boolean activeNetworkConnected() {
        if (cm == null) cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) return true; //"short-circuiting" behavior
            //Vl.layer 3 isConnected not layer 4
        return false;
    }

    android.app.AlarmManager alarmMgr;
    Intent testDataChannelIntent = null;
    android.app.PendingIntent testDataChannelPendingIntent = null;

    private void tcpFSM(EVENT event) {
        Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": " + tcpState + "#" + event);
        switch (tcpState) {
            case NOT_CONNECTED:
                switch (event) {
                    case OptionsMenuItemCONNECT:
                        tcpState = TCP_STATE.CONNECTING;
                        tcpConnect();
                        break;
                    case DataConnectivity_UP:
                        if (wasCellularCall) {
                            wasCellularCall = false;
                            tcpState = TCP_STATE.CONNECTING;
                            tcpConnect();
                        } else {
                            if (!fTCP_AUTO_CONNECT) return;
                            if (alarmMgr == null)
                                alarmMgr = (android.app.AlarmManager) this.getSystemService(android.content.Context.ALARM_SERVICE);
                            if (testDataChannelIntent == null)
                                testDataChannelIntent = new Intent("com.example.test1pn.testDataChannelAction");
                            if (testDataChannelPendingIntent == null) testDataChannelPendingIntent =
                                    android.app.PendingIntent.getBroadcast(this, 0, testDataChannelIntent, 0);
                            if (testDataChannelBR == null)
                                testDataChannelBR = new TestDataChannelBR();
                            this.registerReceiver(testDataChannelBR, new IntentFilter("com.example.test1pn.testDataChannelAction"));
                            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, android.os.SystemClock.elapsedRealtime() + 60000,
                                    testDataChannelPendingIntent);
                            tcpState = TCP_STATE.PRE_CONNECTING;
                        }
                        break;
                }
                break;
            case PRE_CONNECTING: //TODO: treat CALL_STATE_RINGING also
                switch (event) {
                    case STATE_OUT_OF_SERVICE:
                        alarmMgr.cancel(testDataChannelPendingIntent);//Vl.we know is not null since we always come from NOT_CONNECTED
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case NO_ActiveNetworkConnectedConfirmation:
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case ActiveNetworkConnectedConfirmation:
                        tcpState = TCP_STATE.CONNECTING;
                        tcpConnect();
                        break;
                }
                break;
            case CONNECTING:
                switch (event) {
                    case TCP_CONNECT_NO_ACTIVE_NETWORK_CONNECTED: //Vl.now fTCP_FAIL_ONLY flag suspected as redundant
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case TCP_CONNECTED:
                        fTCP_FAIL_ONLY = false;
                        cntReconnect = 0;
                        tcpState = TCP_STATE.CONNECTED;
                        break;
                    case TCP_CONNECTING_FAILED:
                        if (fTCP_FAIL_ONLY)
                            if (++cntReconnect <= TCP_FAIL_MAX_AUTO_RECONNECT) tcpConnect(); //Vl.stay CONNECTING
                            else {
                                fTCP_AUTO_CONNECT = false;
                                tcpState = TCP_STATE.NOT_CONNECTED;
                            }
                        else tcpState = TCP_STATE.NOT_CONNECTED; //Vl.for STATE_OUT_OF_SERVICE
                        break;
                }
                break;
            case CONNECTED:
                switch (event) {
                    case DATA_SUSPENDED: /*for GPRS class B devices, see GPRS Wikipedia*/
                        if (true) {//Vl.here we should check if we are in GPRS mode
                            try {
                                if (sc.isConnected()) sc.close(); //Vl.hopefully we have enough time to send a valid TCP SYN flag to tcpServer_1
                            } catch (Exception ex) {                //RecvThread will die anyway
                                Vsupport1.log(et1, "\nVladi23.., SocketChannel.close() (IO)Exception when CONNECTED/CALL_STATE_RINGING: +" +
                                        ex.toString());
                            }
                            tcpState = TCP_STATE.NOT_CONNECTED;
                        }
                        break;
                    case STATE_OUT_OF_SERVICE:
                    case DataConnectivity_DOWN://Vl.hopefully far after DATA_SUSPENDED
                        if (fTCP_AUTO_CONNECT) {
                            closeSocketChannel(); //Vl.also unleash alarm
                            tcpState = TCP_STATE.NOT_CONNECTED;
                        }
                        break;
                    case TCP_HALF_OPEN:
                        if (fTCP_AUTO_CONNECT) {
                            closeSocketChannel(); //Vl.also unleash alarm
                            fTCP_FAIL_ONLY = true;
                            tcpState = TCP_STATE.CONNECTING;
                            tcpConnect();
                        }
                }
        }
    }

    private void closeSocketChannel() {
        try {
            if (sc.isConnected()) sc.close();
            //Vl.hopefully we have enough time to send a valid TCP SYN flag to tcpServer_1 when STATE_OUT_OF_SERVICE
        } catch (Exception ex) {//RecvThread will die anyway
            Vsupport1.log(et1, "\nVladi23.., SocketChannel.close() (IO)Exception when CONNECTED/STATE_OUT_OF_SERVICE: +" +
                    ex.toString());
        }
        if (alarm1URI == null) alarm1URI = android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_beep_03_1);
        if (rt1 == null) rt1 = android.media.RingtoneManager.getRingtone(this, alarm1URI);
        rt1.play();
    }

    private void tcpConnect() {
        final String serverIpAddr, serverTcpPort;
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Vsupport1.log(et1, "\nVl26. " + e.toString());
        }
        if (!activeNetworkConnected()) {
            tcpFSM(EVENT.TCP_CONNECT_NO_ACTIVE_NETWORK_CONNECTED);
            return;
        }
        Vsupport1.log(et1, "\nActiveNetworkType is: " + activeNetwork.getType() + ", "
                + activeNetwork.getTypeName());
        serverIpAddr = sharedPref.getString("server_address", "");
        serverTcpPort = sharedPref.getString("server_port", "");
        Vsupport1.log(et1, "\nConnecting to: " + serverIpAddr + "." + serverTcpPort + " ....");
        new Thread() {
            @Override
            public void run() {
                try {
                    if (sc == null || !sc.isOpen()) sc = SocketChannel.open();
                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            et1.append("\nVladi2. got SocketChannel.open() IOException\n" + e.toString());
                            et1.append("\nVladi3. receive thread not launched\n");
                        }
                    });
                    tcpFSM(EVENT.TCP_CONNECTING_FAILED);
                    return;
                }
                if (isa == null) isa = new InetSocketAddress(serverIpAddr, Integer.parseInt(serverTcpPort));
                try {
                    sc.connect(isa);
                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            et1.append("\nVladi4. got sc.connect(isa) IOException, " + e.toString());
//                                    + ", Vl4.Throwable detail message: " + e.getMessage());
                            et1.append("\nVladi5. receive thread not launched\n");
                        }
                    });
                    tcpFSM(EVENT.TCP_CONNECTING_FAILED);
                    return;
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            et1.append("\nVladi6. got in sc.connect(isa): " + e.toString());
                            et1.append("\nVladi7. receive thread not launched\n");
                        }
                    });
                    tcpFSM(EVENT.TCP_CONNECTING_FAILED);
                    return;
                }
                Vsupport1.log(et1, "done\n");
                if (sc.isConnected()) {
                    Vsupport1.log(et1, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date()) + "\n");
                    kaLoop(); //Vl.keepalive receive thread starting
                    tcpFSM(EVENT.TCP_CONNECTED);
                }
            }
        }.start();
    }

    // V. CgetStrDiag.CgetStrDiagListener interface implementation:
    @Override
    public void onDialogPositiveClick(CgetStrDiag dialog) {
        String msg = null;
        ByteBuffer bytebuf = ByteBuffer.allocate(1024);
        msg = dialog.getUserInput() + "\n";
        bytebuf = ByteBuffer.wrap(msg.getBytes());
        if (null != sc) {
            final ByteBuffer finalBytebuf = bytebuf;
            final String finalMsg = msg;
            new Thread() {
                @Override
                public void run() {
                    try {
                        sc.write(finalBytebuf);
                    } catch (IOException e) {
                        t.setText("Vladi8.., we got an IOException in sendMessage(), leave..."); t.show();
                        Vsupport1.log(et1, "\nVladi8.., we got an IOException in sendMessage(), closing SocketChannel..: " +
                                e.toString());
                        try {
                            sc.close();
                        } catch (IOException e1) {
//                            t.setText("Vladi9.., sc.close() IOException"); t.show();
                            Vsupport1.log(et1, "\nVladi9.., SocketChannel.close() IOException..: +" +
                                    e1.toString());
                            return;
                        }
                        //        System.exit(0);
                    } catch (Exception e) {
                        t.setText("Vladi11. caught: " + e.toString()); t.show();
                        Vsupport1.log(MainActivity.ev1, "\nVladi11 caught: " + e.toString());
                        return;
                    }
                    Vsupport1.log(et1, "\nVladi19. sent: " + finalBytebuf.array().toString());
                    if (finalMsg.contains("down")) fTCP_AUTO_CONNECT = false;
                }
            }.start();
        } else {
            et1.append("\nVladi12. The SocketChannel (the sc) not created (is null)." +
                    " Are you connected?\n");
        }
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

    private void kaLoop() {//Vl.KAs are sending by the sever
        rt = new RecvThread("Receive THread");
        rt.start();
    }
    private class RecvThread extends Thread {

//        private SocketChannel sc = null;
        private boolean val = true;

        private RecvThread(String str) {
            super(str);
//            sc = client;
        }
        public void run() {
            System.out.println("Inside receivemsg");
            int nBytes, i = 0;
            int previousKAi = 20000, kAinterval = 20000; //must be correlated with Erlang tcpServer_1 loop(Socket,start) delay
            Vsupport1.log(et1, "\n");
            try {
                while (val) {
                    if (kAinterval != previousKAi) {
                        sc.socket().setSoTimeout(kAinterval + kAinterval / 2);
                        previousKAi = kAinterval;
                    }
                    while ((nBytes = TCPclient1Activity.this.sc.read(buf)) > 5) {
                        if (TCPclient1Activity.this.sc.isBlocking()) {
                            System.out.println("V.The sc SocketChannel is in blocking mode.");
                        } else {
                            System.out.println("V.The sc SocketChannel is NOT in blocking mode !!!!");
                        }
                        i = 0;
                        buf.flip();
                        int bl = buf.limit();
                        if (bl > bufref1ro.capacity()) buf.limit(bufref1ro.capacity());
                        if (0 == bufref1ro.compareTo(buf)) {
                            buf.limit(buf.capacity());
                            seq = buf.getInt(bufref1ro.capacity());
                            int rcvDelay = buf.get(bl - 1);
                            Vsupport1.log(et1, "ka" + seq + "d" + rcvDelay);
                            sendBuf.position(8);
                            sendBuf.putInt(seq);
                            kAinterval = Integer.parseInt(sharedPref.getString("server_ka_interval", "20"));
                            sendBuf.put(sendBuf.capacity() - 1, (byte) (kAinterval / 10));
                            sendBuf.clear();
                            nBytes = TCPclient1Activity.this.sc.write(sendBuf);
                            Vsupport1.log(et1, "ack ");
                        } else {
                            buf.limit(bl);
                            java.nio.CharBuffer charBuffer = decoder.decode(buf);
                            String result = charBuffer.toString();
                            Vsupport1.log(et1, result + "\n");
                            //Vl.here was a bug.. we need to send  again in order to maintain ka/response handshake with Erlang tcpServer_1 echo_server rel.11
//                            sendBuf.clear();
//                            nBytes = TCPclient1Activity.this.sc.write(sendBuf);
                        }
                        buf.clear();
                    }
                    Vsupport1.log(et1, String.format("\nIncomplete read %d, nBytes=%d; buf: pos=%d, limit=%d, remaining=%d",
                            ++i, nBytes, buf.position(), buf.limit(), buf.remaining()));
                    if (nBytes == -1)
                        throw new Exception("Vladi16 _Exception, read -1 Bytes => the channel has reached end-of-stream");
                    if (i > 3) return;
                }
            } catch (java.net.SocketTimeoutException ex) {
                Vsupport1.log(et1, "\n" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(new java.util.Date()));
                Vsupport1.log(et1, "\nVl24.., got SocketTimeoutException in RecvThread: " + ex.toString());
                Vsupport1.log(et1, "\nVl24.SocketChannel isConnected() returns " + TCPclient1Activity.this.sc.isConnected());
                tcpFSM(EVENT.TCP_HALF_OPEN);
            } catch (Exception e) {
                Vsupport1.log(et1, "\nVl13." + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(new java.util.Date()));
                Vsupport1.log(et1, "\nVl13.., we got an Exception in RecvThread: " + e.toString()
                        + ", Vl13.Throwable detail message: " + e.getMessage());
//                StackTraceElement[] arrSTE = e.getStackTrace();
//                String textSTEs = "";
//                for (StackTraceElement ste : arrSTE) textSTEs += (ste.toString() + "\n");
//                Vsupport1.log(et1, textSTEs);
                Vsupport1.log(et1, "\nVl13.SocketChannel isConnected() returns " + TCPclient1Activity.this.sc.isConnected());
//                tcpFSM(EVENT.SERVER_DISCONNECTED);
            }
//            finally {
//                Vsupport1.log(et1, "Vladi16, closing socketChannel..\n");
//                try {
//                    sc.close();
//                } catch (IOException e) {
//                    Vsupport1.log(et1, "\nVladi17.., socketChannel.close() IOException\n");
//                }
//            }
//            Vsupport1.log(et1, "Vl14.., InetSocketAddress isa = " + TCPclient1Activity.this.isa.toString() + "\n");
//            Vsupport1.log(et1, "Vl14.., socketChannel sc = " + TCPclient1Activity.this.sc.toString() + ", leave RecvThread.\n");
        }
    }

    private class PhoneStateListener extends android.telephony.PhoneStateListener {
        @Override
        final public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, CALL_STATE_IDLE"); break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
//                    wasCellularCall = true;
//                    tcpFSM(EVENT.CALL_STATE_OFFHOOK);
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, CALL_STATE_OFFHOOK"); break;
                case TelephonyManager.CALL_STATE_RINGING:
//                    wasCellularCall = true;
//                    tcpFSM(EVENT.CALL_STATE_RINGING);
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, CALL_STATE_RINGING"); break;
                default:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, CALL_STATE_UNKNOWN");
            }
        }
        @Override
        final public void onDataConnectionStateChanged(int state) {
            switch (state) {
                case TelephonyManager.DATA_DISCONNECTED:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl18, DATA_DISCONNECTED"); break;
                case TelephonyManager.DATA_CONNECTING:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, DATA_CONNECTING"); break;
                case TelephonyManager.DATA_CONNECTED:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, DATA_CONNECTED"); break;
                case TelephonyManager.DATA_SUSPENDED:
                    wasCellularCall = true;
                    tcpFSM(EVENT.DATA_SUSPENDED);
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vladi18, DATA_SUSPENDED"); break;
                default:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl18, DATA_STATE_UNKNOWN");
            }
        }
        @Override
        final public void onServiceStateChanged(ServiceState ss) {
            switch (ss.getState()) {
                case ServiceState.STATE_EMERGENCY_ONLY:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl22, STATE_EMERGENCY_ONLY"); break;
                case ServiceState.STATE_IN_SERVICE:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl22, STATE_IN_SERVICE"); break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl22, STATE_OUT_OF_SERVICE"); break;
                case ServiceState.STATE_POWER_OFF:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl22, STATE_POWER_OFF"); break;
                default:
                    Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()) + ": Vl22, STATE_UNKNOWN");
            }
        }
    }
    private class ConnectivityEventsReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Vsupport1.log(et1, "Vl21, got: " + intent.getAction());
//            android.net.NetworkInfo networkInfo = intent.getParcelableExtra(android.net.ConnectivityManager.EXTRA_NETWORK_INFO);
            boolean noDataConnectivity = intent.getBooleanExtra(android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Vsupport1.log(et1, "\n" + sdfHMsS.format(System.currentTimeMillis()));
            if (noDataConnectivity) {
                Vsupport1.log(et1, ": Vl21, DataConnectivity_DOWN");
                tcpFSM(EVENT.DataConnectivity_DOWN);
            }
            else {
                Vsupport1.log(et1, ": Vl21, DataConnectivity_UP");
                tcpFSM(EVENT.DataConnectivity_UP);
            }
        }
    }
}
