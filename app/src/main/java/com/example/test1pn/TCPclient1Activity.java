package com.example.test1pn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
//last allocated tag:Vladi17
public class TCPclient1Activity extends ActionBarActivity implements CgetStrDiag.CgetStrDiagListener {
    EditText et1; //Vl.editTextTCPlogger1
    private SocketChannel client = null;
    public RecvThread rt = null;
    SharedPreferences sharedPref;
    CgetStrDiag newF;//V.newFragment Dialog based on alert dialog
    Toast t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcpclient1);
        t = Toast.makeText(getBaseContext(), "", Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        et1 = (EditText) findViewById(R.id.editTextTCPlogger1);
        MainActivity.ev1.append("Vladi1.Test from TCPclient1Activity == TCPc1A\n");
        getMenuInflater().inflate(R.menu.menu1_tcpclient1activity, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent i;
        switch (id) {
            case R.id.settings1:
                i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, 0);
                return true;
            case R.id.connect:
                final String serverIpAddr, serverTcpPort;
                serverIpAddr = sharedPref.getString("server_address", "");
                serverTcpPort = sharedPref.getString("server_port", "");
                et1.append("Connect to: " + serverIpAddr + "." + serverTcpPort + " ....");
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            client = SocketChannel.open();
                        } catch (final IOException e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    et1.append("\nVladi2. got SocketChannel.open() IOException\n" + e.toString());
                                    et1.append("\nVladi3. receive thread not launched\n");
                                }
                            });
                            return;
                        }
                        InetSocketAddress isa = new InetSocketAddress(serverIpAddr, Integer.parseInt(serverTcpPort));
                        try {
                            client.connect(isa);
                        } catch (final IOException e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    et1.append("\nVladi4. got client.connect(isa) IOException, " + e.getMessage());
                                    et1.append("\nVladi5. receive thread not launched\n");
                                }
                            });
                            return;
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    et1.append("\nVladi6. got in client.connect(isa): " + e.toString());
                                    et1.append("\nVladi7. receive thread not launched\n");
                                }
                            });
                            return;
                        }
                        Vsupport1.log(et1, "done\n");
                        if (client.isConnected()) {
                            Vsupport1.log(et1, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .format(new java.util.Date()) + "\n");
                            receiveMessage(); //Vl.receive thread.
                        }
                    }
                }.start();
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
    // V. CgetStrDiag.CgetStrDiagListener interface implementation:
    @Override
    public void onDialogPositiveClick(CgetStrDiag dialog) {
        String msg = null;
        ByteBuffer bytebuf = ByteBuffer.allocate(1024);
        msg = dialog.getUserInput() + "\n";
        bytebuf = ByteBuffer.wrap(msg.getBytes());
        if (null != client) {
            final ByteBuffer finalBytebuf = bytebuf;
            new Thread() {
                @Override
                public void run() {
                    try {
                        client.write(finalBytebuf);
                    } catch (IOException e) {
                        t.setText("Vladi8.., we got an IOException in sendMessage(), leave..."); t.show();
                        Vsupport1.log(MainActivity.ev1, "Vladi8/TCPc1A.., we got an IOException in sendMessage(), closing SocketChannel..: " +
                                e.toString() + "\n");
                        try {
                            client.close();
                        } catch (IOException e1) {
//                            t.setText("Vladi9.., client.close() IOException"); t.show();
                            MainActivity.ev1.append("Vladi9/TCPc1A.., client.close() IOException\n");
                            Vsupport1.log(MainActivity.ev1, "Vladi9/TCPc1A.., SocketChannel.close() IOException..: +" +
                                    e1.toString() + "\n");
                            return;
                        }
                        //        System.exit(0);
                        t.setText("Vladi10.., normally closed TCPclient1Activity"); t.show();
                        Vsupport1.log(MainActivity.ev1, "Vladi10/TCPc1A.., normally closed TCPclient1Activity\n");
                        finish();
                    } catch (Exception e) {
                        t.setText("Vladi11. caught: " + e.toString()); t.show();
                        Vsupport1.log(MainActivity.ev1, "Vladi11/TCPc1A. caught: " + e.toString() + "\n");
                    }
                }
            }.start();
        } else {
            et1.append("\nVladi12. The SocketChannel (the client) not created (is null)." +
                    " Are you connected?\n");
        }
    }

    @Override
    public void onDialogNegativeClick(CgetStrDiag dialog) {
        MainActivity.ev1.append("Vladi15.Test2 from TCPc1A\n");
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
                    MainActivity.ev1.append("Vladi15.Test_3 from a TCPc1A thread != main activity thread\n");
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

    private void receiveMessage() {
        rt = new RecvThread("Receive THread", client);
        rt.start();
    }
    private class RecvThread extends Thread {

        private SocketChannel sc = null;
        private boolean val = true;

        private RecvThread(String str, SocketChannel client) {
            super(str);
            sc = client;
        }
        public void run() {
            byte[] sendArr = {'r','e','s','p','o','n','s','e',0,0,0,0,'d','e','l','a','y',0};
            int seq = 327, rcvDelay;
            System.out.println("Inside receivemsg");
            int nBytes, i = 0;
            ByteBuffer buf = ByteBuffer.allocate(1024);
            ByteBuffer bufref1 = ByteBuffer.wrap("keepalive".getBytes());
            ByteBuffer bufref1ro = bufref1.asReadOnlyBuffer();
            ByteBuffer sendBuf = ByteBuffer.wrap(sendArr);
            Charset charset = Charset.forName("us-ascii");
            java.nio.charset.CharsetDecoder decoder = charset.newDecoder();
            try {
                while (val) {
                    while ((nBytes = client.read(buf)) > 5) {
                        if (client.isBlocking()) {
                            System.out.println("V.The client SocketChannel is in blocking mode.");
                        } else {
                            System.out.println("V.The client SocketChannel is NOT in blocking mode !!!!");
                        }
                        i = 0;
                        buf.flip();
                        int bl = buf.limit();
                        if (bl > bufref1ro.capacity()) buf.limit(bufref1ro.capacity());
                        if (0 == bufref1ro.compareTo(buf)) {
                            buf.limit(buf.capacity());
                            seq = buf.getInt(bufref1ro.capacity());
                            rcvDelay = buf.get(bl - 1);
                            Vsupport1.log(et1, "ka" + seq + "d" + rcvDelay);
                            sendBuf.position(8);
                            sendBuf.putInt(seq);
                            sendBuf.put(sendBuf.capacity() - 1, (byte) (Integer.parseInt(sharedPref.getString("server_ka_interval","")) / 10));
                            sendBuf.clear();
                            nBytes = client.write(sendBuf);
                            Vsupport1.log(et1, "; ack\n");
                        } else {
                            buf.limit(bl);
                            java.nio.CharBuffer charBuffer = decoder.decode(buf);
                            String result = charBuffer.toString();
                            Vsupport1.log(et1, result + "\n");
                        }
                        buf.clear();
                    }
                    Vsupport1.log(et1, String.format("Incomplete read %d, nBytes=%d; buf: pos=%d, limit=%d, remaining=%d\n",
                            ++i, nBytes, buf.position(), buf.limit(), buf.remaining()));
                    if (nBytes == -1) throw new Exception("Vladi16 _Exception, read -1 Bytes => the channel has reached end-of-stream");
                    if (i > 3) return;
                }
            } catch (Exception e) {
                Vsupport1.log(et1, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date()) + "\n");
                Vsupport1.log(et1, "\nVladi13.., we got an Exception in RecvThread: " + e.toString() + "\n");
                StackTraceElement[] arrSTE = e.getStackTrace();
                String textSTEs = "";
                for (StackTraceElement ste : arrSTE) textSTEs += (ste.toString() + "\n");
                Vsupport1.log(et1, textSTEs);
            } finally {
                Vsupport1.log(et1, "Vladi16, closing socketChannel..\n");
                try {
                    client.close();
                } catch (IOException e) {
                    Vsupport1.log(et1, "\nVladi17.., socketChannel.close() IOException\n");
                }
            }
            Vsupport1.log(et1, "Vladi14.., socketChannel = " + client + ", leave RecvThread.\n");
        }
    }
}
