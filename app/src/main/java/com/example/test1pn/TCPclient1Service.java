package com.example.test1pn;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

enum TCP_STATE {NOT_CONNECTED, PRE_CONNECTING, CONNECTING, CONNECTED}
enum EVENT {STATE_OUT_OF_SERVICE, CALL_STATE_RINGING, CALL_STATE_OFFHOOK, DataConnectivity_UP, OptionsMenuItemCONNECT,
    ActiveNetworkConnectedConfirmation, NO_ActiveNetworkConnectedConfirmation, TCP_CONNECTING_FAILED, TCP_CONNECTED,
    TCP_CONNECT_NO_ACTIVE_NETWORK_CONNECTED, SERVER_DISCONNECTED, TCP_HALF_OPEN,
    /*for GPRS class B devices*/DATA_SUSPENDED, DataConnectivity_DOWN}
//last allocated tag:Vladi32

public class TCPclient1Service extends Service {
    static final String TAG1 = "TCPc1Service";
    static TCPclient1Service instance;
    java.util.Date startTimeStamp;
    int totalSeqAcks = 0;
    static android.widget.EditText et1; //Vl.editTextTCPlogger1 of TCPclient1Activity
    java.text.SimpleDateFormat sdfHMsS; //Vl.just HourMinuteSecondMilliseconds
    java.text.SimpleDateFormat sdf; //Vl.YearMonthDayHourMinuteSecondMilliseconds
    private SocketChannel sc = null;
    private InetSocketAddress isa = null;
    public RecvThread rt = null;
    SharedPreferences sharedPref;
    TCP_STATE tcpState;
    //Vl.enum EVENT related
    boolean fTCP_AUTO_CONNECT = false; //Vl. f=flag
    final int TCP_FAIL_MAX_AUTO_RECONNECT = 3; //TODO: this should be SharedPreferences
    int cntReconnect;
    private boolean fTCP_FAIL_ONLY;//Vl.suspicious redundant, we can test cntReconnect > 0
    boolean wasCellularCall = false;
    //media
    android.net.Uri alarm1URI = null;
    android.net.Uri connected1URI;
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
    public void onCreate() {
        et1 = TCPclient1Activity.et1;
        sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdfHMsS = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
        sharedPref = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        telephonyManager = (android.telephony.TelephonyManager) getSystemService(android.content.Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener();
        tcpState = TCP_STATE.NOT_CONNECTED;
        connectivityEventsReceiver = new ConnectivityEventsReceiver();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE
                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
        this.registerReceiver(connectivityEventsReceiver,
                new android.content.IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        Vsupport1.log(et1, "\n" + sdf.format(System.currentTimeMillis()) + TAG1 + ".onCreate()");
        instance = this;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Vsupport1.log(et1, "\n" + sdf.format(System.currentTimeMillis()) + TAG1 + ".onStartCommand(..)");
//        Notification notification = new Notification(
//                R.drawable.android1,
//                getText(R.string.app_name), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, TCPclient1Activity.class);
//        notificationIntent.setAction(Intent.ACTION_MAIN);
//        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
//        notification.setLatestEventInfo(this, getText(R.string.app_name),
//                getText(R.string.app_running), pendingIntent);
        Notification notification =
                new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.notification1_title))
                .setContentText(getText(R.string.notification1_message))
                .setSmallIcon(R.drawable.android1)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.ticker1_text))
                .build();
        cntReconnect = 0;
        fTCP_AUTO_CONNECT = true;
        tcpFSM(EVENT.OptionsMenuItemCONNECT);
        startForeground(1, notification);
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
//        Vsupport1.log(MainActivity.ev1, "\nVl./TCPc1A., from onDestroy()");
        Vsupport1.textFileLog("\n" + sdf.format(System.currentTimeMillis()) +
                "\nVl./TCPc1Service., from onDestroy()", App.bufW1);
        this.unregisterReceiver(connectivityEventsReceiver);
        fTCP_AUTO_CONNECT = false;
        if (sc != null) try {
            sc.close();//Vl.here also RecvThread should die.., but et1??
        } catch (java.io.IOException e) {
            Vsupport1.log(MainActivity.ev1, "\nVladi20/TCPc1Service.., SocketChannel.close() IOException on backButton: +" +
                    e.toString());
        }
        try {
            App.bufW1.close();
        } catch (java.io.IOException e) {
//            Vsupport1.log(MainActivity.ev1, "\nVl./TCPc1A., at log file close.. " + e.toString());
        }
        instance = null;
        super.onDestroy();
    }
    void writeOverTCP(String msg) {
        ByteBuffer bytebuf = ByteBuffer.allocate(1024);
        bytebuf = ByteBuffer.wrap(msg.getBytes());
        if (null != sc) {
            final ByteBuffer finalBytebuf = bytebuf;
            final String finalMsg = msg;
            new Thread() {
                @Override
                public void run() {
                    Vsupport1.log(et1, "\nVladi19. sending: " + finalBytebuf.array().toString() + ", " +
                            Arrays.toString(finalBytebuf.array()));
                    if (finalMsg.contains("down")) fTCP_AUTO_CONNECT = false;
                    try {
                        sc.write(finalBytebuf);
                    } catch (java.io.IOException e) {
                        Vsupport1.log(et1, "\nVladi8.., we got an IOException in sendMessage(), closing SocketChannel..: " +
                                e.toString());
                        try {
                            sc.close();
                        } catch (java.io.IOException e1) {
                            Vsupport1.log(et1, "\nVladi9.., SocketChannel.close() IOException..: +" +
                                    e1.toString());
                        }
                        //        System.exit(0);
                    } catch (Exception e) {
                        Vsupport1.log(MainActivity.ev1, "\nVladi11 caught: " + e.toString());
                    }
                }
            }.start();
        } else {
            et1.append("\nVladi12. The SocketChannel (the sc) not created (is null)." +
                    " Are you connected?\n");
        }
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
            int previousKAi = 20000; //must be correlated with Erlang tcpServer_1 loop(Socket,start) delay
            int kAinterval = Integer.parseInt(sharedPref.getString("server_ka_interval", "20")) * 1000;
            Vsupport1.log(et1, "\n");
            if (sc.isBlocking()) {
                Vsupport1.log(et1, "\nV.The sc SocketChannel is in blocking mode.");
            } else {
                Vsupport1.log(et1, "\nV.The sc SocketChannel is NOT in blocking mode !!!!");
            }
            try {
                sc.socket().setSoTimeout(kAinterval / 5);//Vl.kAinterval always 5 multiple, otherwise will crash..
                while (val) {
                    while ((nBytes = sc.read(buf)) > 5) {
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
                            kAinterval = Integer.parseInt(sharedPref.getString("server_ka_interval", "20")) * 1000;
                            sendBuf.put(sendBuf.capacity() - 1, (byte) (kAinterval / 10000));
                            sendBuf.clear();
                            nBytes = sc.write(sendBuf);
                            Vsupport1.log(et1, "ack ");
                            totalSeqAcks++;
                            if (1 == seq) {
                                if (connected1URI == null) connected1URI = android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.echo_beep_1);
                                rt1 = android.media.RingtoneManager.getRingtone(TCPclient1Service.this, connected1URI);
                                rt1.play();
                            }
                        } else {
                            buf.limit(bl);
                            java.nio.CharBuffer charBuffer = decoder.decode(buf);
                            String result = charBuffer.toString();
                            Vsupport1.log(et1, "\nVl.30 msgReceived: " + result + "\n");
                        }
                        if (kAinterval != previousKAi) {
                            final int kato = kAinterval + kAinterval / 2; //ka time out
                            sc.socket().setSoTimeout(kato);
                            previousKAi = kAinterval;
                            Vsupport1.log(et1, "\nVl27. setSoTimeout to " + kato / 1000 + " seconds.");
                        }
                        buf.clear();
                    }
                    Vsupport1.log(et1, String.format("\nIncomplete read %d, nBytes=%d; buf: pos=%d, limit=%d, remaining=%d",
                            ++i, nBytes, buf.position(), buf.limit(), buf.remaining()));
                    if (nBytes == -1)
                        throw new Exception("Vladi16 _Exception, read -1 Bytes => the channel has reached end-of-stream");
                    if (i > 7)
                        throw new Exception(String.format("Vl32. _Exception, read %d Bytes", nBytes));
                }
            } catch (java.net.SocketTimeoutException ex) {
                Vsupport1.log(et1, "\n" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(new java.util.Date()));
                Vsupport1.log(et1, "\nVl24.., got SocketTimeoutException in RecvThread: " + ex.toString());
                Vsupport1.log(et1, "\nVl24.SocketChannel isConnected() returns " + sc.isConnected());
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
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {}
                Vsupport1.log(et1, "\nVl13.SocketChannel isConnected() returns " + sc.isConnected() +
                        ".., sent 2s delayed EVENT.SERVER_DISCONNECTED");
                tcpFSM(EVENT.SERVER_DISCONNECTED);
            }
            Vsupport1.log(et1, "\n" + sdf.format(System.currentTimeMillis()) + TAG1 + " leaved RecvThread.");
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
    final boolean SUCCEED = true, FAIL = false;
    android.net.ConnectivityManager cm;
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
        if (cm == null) cm = (android.net.ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) return true; //"short-circuiting" behavior
        //Vl.layer 3 isConnected not layer 4
        return false;
    }

    android.app.AlarmManager alarmMgr;
    Intent testDataChannelIntent = null;
    android.app.PendingIntent testDataChannelPendingIntent = null;

    private void closeSocketChannel() {
        try {
            if (sc.isConnected()) sc.close();
            //Vl.hopefully we have enough time to send a valid TCP SYN flag to tcpServer_1 when STATE_OUT_OF_SERVICE
        } catch (Exception ex) {//RecvThread will die anyway
            Vsupport1.log(et1, "\nVladi23.., SocketChannel.close() (IO)Exception when CONNECTED/STATE_OUT_OF_SERVICE: +" +
                    ex.toString());
        }
        if (alarm1URI == null) alarm1URI = android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_beep_03_1);
        rt1 = android.media.RingtoneManager.getRingtone(this, alarm1URI);
        rt1.play();
    }

    private void tcpConnect() {
        final String serverIpAddr, serverTcpPort;
        serverIpAddr = sharedPref.getString("server_address", "");
        serverTcpPort = sharedPref.getString("server_port", "");
        new Thread() {
            @Override
            public void run() {
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
                Vsupport1.log(et1, "\nConnecting to: " + serverIpAddr + "." + serverTcpPort + " ....");
                try {
                    if (sc == null || !sc.isOpen()) sc = SocketChannel.open();
                } catch (final java.io.IOException e) {
                    et1.post(new Runnable() {
                        public void run() {
                            et1.append("\nVladi2. got SocketChannel.open() IOException\n" + e.toString());
                            et1.append("\nVladi3. receive thread not launched\n");
                        }
                    });
                    tcpFSM(EVENT.TCP_CONNECTING_FAILED);
                    return;
                }
                if (isa == null)
                    isa = new InetSocketAddress(serverIpAddr, Integer.parseInt(serverTcpPort));
                try {
                    sc.connect(isa);
                } catch (final java.io.IOException e) {
                    et1.post(new Runnable() {
                        public void run() {
                            et1.append("\nVladi4. got sc.connect(isa) IOException, " + e.toString());
//                                    + ", Vl4.Throwable detail message: " + e.getMessage());
                            et1.append("\nVladi5. receive thread not launched\n");
                        }
                    });
                    tcpFSM(EVENT.TCP_CONNECTING_FAILED);
                    return;
                } catch (final Exception e) {
                    et1.post(new Runnable() {
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
                    java.util.Date timeStamp = new java.util.Date();
                    if (null == startTimeStamp) startTimeStamp = timeStamp;
                    Vsupport1.log(et1, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(timeStamp) + "\n");
                    kaLoop(); //Vl.keepalive receive thread starting
                    tcpFSM(EVENT.TCP_CONNECTED);
                }
            }
        }.start();
    }
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
                            this.registerReceiver(testDataChannelBR, new android.content.IntentFilter("com.example.test1pn.testDataChannelAction"));
                            alarmMgr.set(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, android.os.SystemClock.elapsedRealtime() + 60000,
                                    testDataChannelPendingIntent);
                            tcpState = TCP_STATE.PRE_CONNECTING;
                            Vsupport1.log(et1, "\nVl.switched to " + tcpState);
                        }
                        break;
                    default:
                        Vsupport1.log(et1, " .. not treated.");
                }
                break;
            case PRE_CONNECTING:
                switch (event) {
                    case STATE_OUT_OF_SERVICE:
                        alarmMgr.cancel(testDataChannelPendingIntent);//Vl.we know is not null since we always come from NOT_CONNECTED
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case DATA_SUSPENDED:
                    case NO_ActiveNetworkConnectedConfirmation:
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case ActiveNetworkConnectedConfirmation:
                        tcpState = TCP_STATE.CONNECTING;
                        tcpConnect();
                        break;
                    default:
                        Vsupport1.log(et1, " .. not treated.");
                }
                break;
            case CONNECTING:
                switch (event) {
                    case TCP_CONNECT_NO_ACTIVE_NETWORK_CONNECTED: //Vl.now fTCP_FAIL_ONLY flag suspected as redundant
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case TCP_CONNECTED:
                        cntReconnect = 0;
                        tcpState = TCP_STATE.CONNECTED;
                        break;
                    case TCP_CONNECTING_FAILED:
                        if (fTCP_FAIL_ONLY)
                            if (++cntReconnect <= TCP_FAIL_MAX_AUTO_RECONNECT) tcpConnect(); //Vl.stay CONNECTING
                            else {
                                fTCP_AUTO_CONNECT = false;
                                tcpState = TCP_STATE.NOT_CONNECTED;
                                Vsupport1.textFileLog("\n" + sdf.format(System.currentTimeMillis()) +
                                        "\nVl29./TCPc1Service., reach TCP_FAIL_MAX_AUTO_RECONNECT", App.bufW1);
                                stopSelf();
                            }
                        else tcpState = TCP_STATE.NOT_CONNECTED; //Vl.for STATE_OUT_OF_SERVICE
                        break;
                    default:
                        Vsupport1.log(et1, " .. not treated.");
                }
                break;
            case CONNECTED:
                switch (event) {
                    case DATA_SUSPENDED: /*for GPRS class B devices, see GPRS Wikipedia*/
                        try {
                            if (sc.isConnected())
                                sc.close(); //Vl.hopefully we have enough time to send a valid TCP SYN flag to tcpServer_1
                        } catch (Exception ex) {                //RecvThread will die anyway
                            Vsupport1.log(et1, "\nVladi23.., SocketChannel.close() (IO)Exception when CONNECTED/CALL_STATE_RINGING: +" +
                                    ex.toString());
                        }
                        fTCP_FAIL_ONLY = false;
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case STATE_OUT_OF_SERVICE:
                    case DataConnectivity_DOWN://Vl.hopefully far after DATA_SUSPENDED above
                        closeSocketChannel(); //Vl.also unleash alarm
                        fTCP_FAIL_ONLY = false;
                        tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    case SERVER_DISCONNECTED:
                    case TCP_HALF_OPEN:
                        if (fTCP_AUTO_CONNECT) {
                            closeSocketChannel(); //Vl.also unleash alarm
                            fTCP_FAIL_ONLY = true;
                            tcpState = TCP_STATE.CONNECTING;
                            tcpConnect();
                        } else tcpState = TCP_STATE.NOT_CONNECTED;
                        break;
                    default:
                        Vsupport1.log(et1, " .. not treated.");
                }
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
        final public void onServiceStateChanged(android.telephony.ServiceState ss) {
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
        public void onReceive(android.content.Context context, Intent intent) {
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
