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
                final String ipAddr, tcpPort;
                ipAddr = sharedPref.getString("server_address", "");
                tcpPort = sharedPref.getString("server_port", "");
                et1.append("Connect to: " + ipAddr + "." + tcpPort + " ....");
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            client = SocketChannel.open();
                        } catch (final IOException e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    et1.append("\nVladi. got SocketChannel.open() IOException\n" + e.toString());
                                    et1.append("\nVladi. receive thread not launched\n");
                                }
                            });
                            return;
                        }
                        InetSocketAddress isa = new InetSocketAddress(ipAddr, Integer.parseInt(tcpPort));
                        try {
                            client.connect(isa);
                        } catch (final IOException e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    et1.append("\nVladi. got client.connect(isa) IOException, " + e.getMessage());
                                    et1.append("\nVladi. receive thread not launched\n");
                                }
                            });
                            return;
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    et1.append("\nVladi. got in client.connect(isa): " + e.toString());
                                    et1.append("\nVladi. receive thread not launched\n");
                                }
                            });
                            return;
                        }
                        Vsupport1.log(et1, "done\n");
                        if (client.isConnected()) receiveMessage(); //Vl.receive thread.
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
        try {
            if (null != client) client.write(bytebuf);
            else {
                et1.append("\nVladi. The SocketChannel (the client) not created (is null)." +
                        " Are you connected?\n");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            t.setText("Vladi.., we got an IOException in sendMessage(), leave...");	t.show();
            try {
                client.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                et1.append("\nVladi.., client.close() IOException\n");
                return;
            }
        } catch (Exception e) {
            et1.append("\nVladi. caught: " + e.toString());
            return;
        }
//        System.exit(0);
        t.setText("Vladi.., normally closed TCPclient1Activity");	t.show();
        finish();
    }

    @Override
    public void onDialogNegativeClick(CgetStrDiag dialog) {
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
                            Vsupport1.log(et1, "keepalive" + seq + "delay" + rcvDelay + "\n");
                            sendBuf.position(8);
                            sendBuf.putInt(seq);
                            sendBuf.put(sendBuf.capacity() - 1, (byte) (Integer.parseInt(sharedPref.getString("server_ka_interval","")) / 10));
                            sendBuf.clear();
                            nBytes = client.write(sendBuf);
                        } else {
                            buf.limit(bl);
                            java.nio.CharBuffer charBuffer = decoder.decode(buf);
                            String result = charBuffer.toString();
                            System.out.println(result);
                        }
                        buf.clear();
                    }
                    Vsupport1.log(et1, String.format("Incomplete read %d, nBytes=%d; buf: pos=%d, limit=%d, remaining=%d",
                            ++i, nBytes, buf.position(), buf.limit(), buf.remaining()));
                    if (i > 10) return;
                }
            } catch (IOException e) {
                Vsupport1.log(et1, "\nVladi.., we got an IOException in RecvThread.\n");
                e.printStackTrace();
            }
            Vsupport1.log(et1, "Vladi.., leave RecvThread.\n");
        }
    }
}
