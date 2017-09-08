package com.example.test1pn;

/*V. pn=ProjectName
 * this is intended as a template project for some tests:
 * 1. cryptographically strong random number generator (RNG): https://developer.android.com/reference/java/security/SecureRandom.html
 * 2. compute SHA256 digest.
 * 3. HMAC tag compute.
 * 4. Some Datagrams tests..
 * 5. test_ScheduledThreadPoolExecutor and logging static method from background thread.. 
 * 6. Some enum type tests.
 * 7. java.util.Timer class test.
 * 8. android.os.PowerManager stuff (WAKE_LOCK).
 * 9. resume this app from notification.
 * 10.TCPclient1.
 * 11.AlarmManager1. https://developer.android.com/training/scheduling/alarms.html#boot
 * */
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.example.test1pn.Vsupport1.XLINK;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements CgetStrDiag.CgetStrDiagListener {
	private static final String TAG1 = "MainActivity"; 
	SecureRandom random = null;// V.default protected I think..
	byte bytes[];
	EditText ev1;
	MessageDigest digest = null;
	MainActivity maContext;
	CgetStrDiag newF;//V.newFragment Dialog based on alert dialog
	enum UserInputRequest {RNG, SHA256, SHA256HMACenc, enumTest}
	UserInputRequest uir;
	Mac sha256_HMAC;
	String strVar1 = "";// V.various usage
	int n1 = 0;
	Toast t;
	//runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
	static java.util.concurrent.ScheduledThreadPoolExecutor stpe;//V.a pool of threads
	java.util.concurrent.ScheduledFuture<?> stpeTimer1;
	java.util.Timer timer;
	private TimerElapsedTask timer_ElapsedTask;
	final long timerElapsedTaskTimeout = 5000;
	android.content.res.Resources res;
	android.os.PowerManager pm;
	android.os.PowerManager.WakeLock wl;
	private android.app.AlarmManager alarmMgr;
	private android.app.PendingIntent alarmIntent;

	private boolean ftimer_ElapsedTask_canceled = false;//V.f=flag
    Runnable timer1Runnable = new Runnable() {
        @Override
        public void run() {
        	n1 += 1;
			if (n1 % 10 == 0) {
				t.setText("Seconds elapsed: " + n1);
				t.setDuration(Toast.LENGTH_SHORT);
				t.show();
			}
            timerHandler.postDelayed(this, 1000);
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		t = Toast.makeText(getBaseContext(), "", Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		timerHandler.postDelayed(timer1Runnable, 0);
	}
	@Override
	protected void onStop() {
		android.util.Log.i(TAG1, "V.from OnStop()");
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		android.util.Log.i(TAG1, "V.from onDestroy()");
		timerHandler.removeCallbacks(timer1Runnable);
		if (stpe != null) {
			stpe.shutdown();
		}
		if ((pm != null) && wl.isHeld()) wl.release();
		if (timer_ElapsedTask != null) {
			timer_ElapsedTask.cancel();
			timer.cancel();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		bytes = new byte[16];
		ev1 = (EditText) findViewById(R.id.editText1);
		maContext = this;
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		TextView tw1 = (TextView) findViewById(R.id.textView1);
		if (tw1 != null)
			((ViewGroup) tw1.getParent()).removeView(tw1);
		if (res == null) res = getResources();
		int id = item.getItemId();
        if (id == R.id.timer1) {
            if (item.getTitle().toString().contains("ON")) {
                item.setTitle(res.getString(R.string.timer1OFF));
                timerHandler.postDelayed(timer1Runnable, 0);
            } else {
                item.setTitle(res.getString(R.string.timer1ON));
                timerHandler.removeCallbacks(timer1Runnable);
            }
        }
		if (id == R.id.alarmManager1) {
			t.setText("Wait for 30 seconds"); t.setDuration(Toast.LENGTH_SHORT); t.show();
			alarmMgr = (android.app.AlarmManager)maContext.getSystemService(android.content.Context.ALARM_SERVICE);
			Intent intent = new Intent(maContext, SettingsActivity.class);
			alarmIntent = android.app.PendingIntent.getBroadcast(maContext, 0, intent, 0);
			alarmMgr.set(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
					android.os.SystemClock.elapsedRealtime() + 30 * 1000, alarmIntent);
		}
		if (id == R.id.PowerManager) {
			if (pm == null) {
				pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
				wl = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "V_WakeLockTag");
				item.setTitle(res.getString(R.string.WakeLockOFF));
				wl.setReferenceCounted(false);
				wl.acquire();
			} else if (wl.isHeld()) {
				wl.release();
				item.setTitle(res.getString(R.string.WakeLockON));
			} else {
				wl.acquire();
				item.setTitle(res.getString(R.string.WakeLockOFF));
			}
		}
		if (id == R.id.rng1) {
			if (random == null)
				random = new SecureRandom();
			random.nextBytes(bytes);
			ev1.append(Arrays.toString(bytes) + "\n");
			return true;
		}
		if (id == R.id.sha256) {
			if (digest == null)
				try {
					digest = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			uir = UserInputRequest.SHA256;
			if (newF == null) {
				newF = new CgetStrDiag();
				t.setText("newDialogFragment");	t.setDuration(Toast.LENGTH_LONG); t.show();
			}
			newF.title = "SHA256 input";
			newF.msg = "Insert sequence to get SHA256 digest:";
			newF.show(getFragmentManager(), "getStrDiag1");
		}
		if (id == R.id.sha256HmacEncode) {
			if (newF == null) {
				newF = new CgetStrDiag();
				t.setText("newDialogFragment");	t.setDuration(Toast.LENGTH_LONG); t.show();
			}
			uir = UserInputRequest.SHA256HMACenc;
			newF.title = "SHA256HMACencode step_1";
			newF.msg = "Insert key sequence:";
			newF.show(getFragmentManager(), "getStrDiag1");
		}
		if (id == R.id.datagrams) {
			testDatagrams();
		}
		if (id == R.id.ScheduledThreadPoolExecutor) {
			if (stpe == null) {
				stpe = new java.util.concurrent.ScheduledThreadPoolExecutor(7);
				stpeTimer1 = stpe.scheduleAtFixedRate(runAtStpeTimer1, 0, 3, TimeUnit.SECONDS);
				stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
				ev1.append("ev1.getParent(): "
						+ ev1.getParent().getClass().getSimpleName() + "\n"
						+ "ev1.getParent().getParent(): "
						+ ev1.getParent().getParent().getClass().getSimpleName()
						+ "\n" + "this: " + this.getClass().getSimpleName() + "\n");
			} else if (!stpeTimer1.isCancelled()) {
				boolean done = stpeTimer1.cancel(false);
				if (done == true)
					ev1.append("\nthe stpeTimer1 ScheduledFuture object was canceled. OK!");
				else
					ev1.append("\nstpeTimer1.cancel() returned false.. This typically because it has already completed normally.");
			} else {
				stpeTimer1 = stpe.scheduleAtFixedRate(runAtStpeTimer1, 0, 7, TimeUnit.SECONDS);
				android.util.Log.i(TAG1, "V.stpe.getTaskCount()=" + stpe.getTaskCount());
				android.util.Log.i(TAG1, "V.stpe.getCorePoolSize()=" + stpe.getCorePoolSize());
				android.util.Log.i(TAG1, "V.stpe.getActiveCount()=" + stpe.getActiveCount());
				android.util.Log.i(TAG1, "V.stpe.getPoolSize()=" + stpe.getPoolSize());
			}
		}
		if (id == R.id.TimerTest) {
			if (timer == null) {
				timer = new java.util.Timer();
				timer_ElapsedTask = new TimerElapsedTask();
				timerElapsedTaskStart();
			} else if (!ftimer_ElapsedTask_canceled) timerElapsedTaskStop();
			else timerElapsedTaskStart();
		}
		if (id == R.id.enumTest) {
			if (newF == null) newF = new CgetStrDiag();
			newF.title = "Test XLINK enum:";
			newF.msg = "XLINK signalings are: [102 - 109].";
			newF.show(getFragmentManager(), "getStrDiag1");
			uir = UserInputRequest.enumTest;
		}
		if (id == R.id.notify1) {//V.http://stackoverflow.com/q/5605207/3180812
			String ns = android.content.Context.NOTIFICATION_SERVICE;
			android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(ns);
			// 2.Instantiate the Notification
			int icon = R.drawable.android1;
			CharSequence tickerText = res.getString(R.string.app_name); // temp msg on status line
			long when = System.currentTimeMillis();
			android.app.Notification notification = new android.app.Notification(icon, tickerText, when);
			// 3.Define the Notification's expanded message and Intent:

			android.content.Context context = getApplicationContext();
			CharSequence contentTitle = "Notification";
			CharSequence contentText = "Vladi's app!.. " + tickerText; // message to user
			android.content.Intent notificationIntent = new android.content.Intent(this, MainActivity.class);
			notificationIntent.setAction(Intent.ACTION_MAIN);
			notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			android.app.PendingIntent contentIntent = android.app.PendingIntent.getActivity(this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			// 4.Pass the Notification to the NotificationManager:
			final int NOTIFICATION_ICON_ID = 1;
			mNotificationManager.notify(NOTIFICATION_ICON_ID, notification);
		}
		if (id == R.id.tcpClient1) {
			Intent tcpClient1Intent = new Intent(this, TCPclient1Activity.class);
			startActivity(tcpClient1Intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	Runnable runAtStpeTimer1 = new Runnable() {
		private int i = 0;
		private final String TAG1 = "runAtStpeTimer1";
		public void run() {
			android.util.Log.i(TAG1, "V.before Vsupport1.log(..)");
			Vsupport1.log(ev1, Integer.toString(i++) + " ");
			android.util.Log.i(TAG1, "V.after Vsupport1.log(..)");
		}};

	class TimerElapsedTask extends java.util.TimerTask {
		private int i = 0;
		@Override
		public void run() {
			Vsupport1.log(ev1, Integer.toString(i++) + " ");
		}
	}

	void timerElapsedTaskStart() {//V. launch as a.start() in Xlink class
		if (ftimer_ElapsedTask_canceled) timer_ElapsedTask = new TimerElapsedTask();
		ftimer_ElapsedTask_canceled = false;
		timer.schedule(timer_ElapsedTask, timerElapsedTaskTimeout, timerElapsedTaskTimeout);
	}
	void timerElapsedTaskStop() {//V. launch as a.stop() in Xlink class
		if (!timer_ElapsedTask.cancel()) Vsupport1.log(ev1, "Ack.stop()" + ", WARNING: returns false!!");
		ftimer_ElapsedTask_canceled = true;
		ev1.append("timer.purge() - the number of canceled tasks that were removed from the task queue = " +  timer.purge() + "\n");
	}
	public static String bin2hex(byte[] data) {// V.http://stackoverflow.com/q/7166129/3180812
		return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}
	
	// The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the CgetStrDiag.CgetStrDiagListener interface
	// V. CgetStrDiag.CgetStrDiagListener interface implementation:
    @Override
    public void onDialogPositiveClick(CgetStrDiag dialog) {
    	String gotSeq;
    	if ("" == dialog.getUserInput()) return;
    	gotSeq = dialog.getUserInput();
    	switch (uir) {
		case SHA256:
			ev1.append("SHA256 input: " + gotSeq + "\nSHA256 digest: ");
			digest.reset();
			ev1.append(bin2hex(digest.digest(gotSeq.getBytes())) + "\n");
			break;
		case SHA256HMACenc:
			if (strVar1.equals("")) {
				this.strVar1 = gotSeq;// V.key
				CgetStrDiag newF;//V.newFragment local obj
				newF = new CgetStrDiag();
				newF.title = "SHA256HMACencode step_2";
				newF.msg = "Insert data sequence:";
				newF.show(getFragmentManager(), "getStrDiag1");
				return; //V.dismiss global newF, I think dismiss!=destroy
			}
			if (sha256_HMAC == null)
				try {
					sha256_HMAC = Mac.getInstance("HmacSHA256");
				} catch (NoSuchAlgorithmException e) {
					ev1.append(e.getMessage() + "V.when tried Mac.getInstance(\"HmacSHA256\");\n");
					return;
				}
			String key = strVar1;// V.for clarity
			try {// V.SecretKeySpec class specifies a secret key in a provider-independent fashion.(see git commit)
				SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
				sha256_HMAC.init(secret_key);
			} catch (Exception e) {
				ev1.append(e.getMessage() + "V.when tried sha256_HMAC.init(secret_key);\n");
				return;
			}
			String data = gotSeq;
			ev1.append("SHA256HMACkey: " + this.strVar1 + "\ndata:" + data + "\n");
			byte[] hmac = null;
			try {
				hmac = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				ev1.append(e.getMessage() + "V.when tried sha256_HMAC.doFinal(data..);\n");
				return;
			}
			ev1.append("SHA256HMACencodedVector: " + bin2hex(hmac) + "\n");
			this.strVar1 = "";
			break;
		case enumTest:
			ev1.append("got (numeric) value: " + gotSeq + "\nXLINK signalling: ");
			int sigNumVal = Integer.parseInt(gotSeq);//V.signallingNumericValue
			Vsupport1.XLINK xl1 = XLINK.UNKNOWN;
			ev1.append(XLINK.getName(sigNumVal) + "\n");
			xl1 = XLINK.valueOf(XLINK.getName(sigNumVal));
			ev1.append("XLINK signalling (numeric) value: " + xl1.getNumericValue() + "\n");
			ev1.append("XLINK.PTP_DATA (numeric) value: " + XLINK.PTP_DATA.getNumericValue());
			ev1.append("\nxl1.toString(): " + xl1.toString());
			ev1.append("\nxl1.getDeclaringClass().getCanonicalName(): " + xl1.getDeclaringClass().getCanonicalName());
			ev1.append("\nxl1.getDeclaringClass().getName(): " + xl1.getDeclaringClass().getName());
			ev1.append("\nxl1.getDeclaringClass().getSimpleName(): " + xl1.getDeclaringClass().getSimpleName() + "\n");
		default:
			break;
		}
    }
    @Override
    public void onDialogNegativeClick(CgetStrDiag dialog) {
    	switch (uir) {
		case SHA256HMACenc:
			this.strVar1 = "";
			break;
		default:
			break;
		}
    }
    
	void testDatagrams() {// V.http://stackoverflow.com/questions/869033/how-do-i-copy-an-object-in-java
		class BufferableDatagramPacketTry1 {
			private DatagramPacket dgp;
			public BufferableDatagramPacketTry1(DatagramPacket _dgp) {//V.copy constructor
				this.dgp = _dgp;
			}
			public DatagramPacket getBufferableDatagramPacket() {
				return dgp;
			}
		}
//		class BufferableDatagramPacketTry2 extends DatagramPacket {
		//V.The type BufferableDatagramPacketTry2 cannot subclass the final class DatagramPacket
//		}
		class BufferableDatagramPacketContainer {
			final int pkDataBufSize = 7;
			byte[] pkDataBuf = new byte[pkDataBufSize];
			private DatagramPacket dgp;
			public BufferableDatagramPacketContainer(DatagramPacket _dgp) {
				dgp = new DatagramPacket(pkDataBuf, pkDataBuf.length);
				java.lang.System.arraycopy(_dgp.getData(), 0, pkDataBuf, 0, _dgp.getLength());
				dgp.setLength(_dgp.getLength());
				dgp.setSocketAddress(_dgp.getSocketAddress());//V.SocketAddress is an immutable object
			}
			public DatagramPacket getBufferableDatagramPacket() {
				return dgp;
			}
		}
		java.net.Inet4Address net4addr = null;
		java.net.InetSocketAddress sockAddr;
		final int pkDataBufSize = 7;
		byte[] pkDataBuf1 = { 0, 1, 2, 3, 4, 5, 6 };
		byte[] pkDataBuf2 = new byte[pkDataBufSize];
		java.net.DatagramPacket dgp1, dgp2;
		dgp1 = new DatagramPacket(pkDataBuf1, pkDataBuf1.length);
		dgp2 = new DatagramPacket(pkDataBuf2, pkDataBuf2.length);
		ev1.append("initial_dgp1Data: " + bin2hex(dgp1.getData()) + "\n");
		ev1.append("initial_dgp2Data: " + bin2hex(dgp2.getData()) + "\n");
		ev1.append("dgp = DatagramPacket for receiving packets.\n");
		dgp2 = dgp1;
		ev1.append("V.made dgp2 = dgp1;\n");
		try {
			net4addr = (Inet4Address) java.net.InetAddress.getByName("8.8.8.8");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sockAddr = new InetSocketAddress(net4addr, 1234);
		dgp1.setSocketAddress(sockAddr);
		ev1.append("dgp1Data: " + bin2hex(dgp1.getData()) + "; dgp1.getLength(): "
				+ dgp1.getLength() + "; dgp1.getHostAddress(): " + dgp1.getAddress().getHostAddress()
				+ "; dgp1.getPort(): " + dgp1.getPort() + "; dgp2 looks the same!\n");
		BufferableDatagramPacketTry1 dgpBufferedTry1 = new BufferableDatagramPacketTry1(dgp1);
		BufferableDatagramPacketContainer dgpBufferedTry3 = new BufferableDatagramPacketContainer(dgp1);
		ev1.append("V.constructed BufferableDatagramPacketContainer(s) dgpBufferedTry1 and dgpBufferedTry3 based on dgp1.\n");
		dgp1.setLength(3);	pkDataBuf1[4] = 0x77; pkDataBuf1[5] = 0x77;//V.emulate a new received datagram
		try {
			net4addr = (Inet4Address) java.net.InetAddress.getByName("192.168.0.105");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sockAddr = new InetSocketAddress(net4addr, 7777);
		dgp1.setSocketAddress(sockAddr);
		ev1.append("Touched dgp1: DataBuf1, length, SocketAddress.\n");
	// ev1.append("dgp1: " + bin2hex((byte[])dgp1) + "\n");Cannot cast from DatagramPacket to byte[]
		ev1.append("dgp1Data: " + bin2hex(dgp1.getData()) + "; dgp1.getLength(): "
				+ dgp1.getLength() + "; dgp1.getHostAddress(): " + dgp1.getAddress().getHostAddress()
				+ "; dgp1.getPort(): " + dgp1.getPort() + "\n");
		ev1.append("dgp2Data: " + bin2hex(dgp2.getData()) + "; dgp2.getLength(): "
				+ dgp2.getLength() + "; dgp2.getHostAddress(): " + dgp2.getAddress().getHostAddress()
				+ "; dgp2.getPort(): " + dgp2.getPort() + "\n");
		ev1.append("dgpBufferedTry1: "
				+ bin2hex(dgpBufferedTry1.getBufferableDatagramPacket().getData())
				+ "; dgpBufferedTry1.getLength(): "
				+ dgpBufferedTry1.getBufferableDatagramPacket().getLength()
				+ "; dgpBufferedTry1.getHostAddress(): "
				+ dgpBufferedTry1.getBufferableDatagramPacket().getAddress().getHostAddress()
				+ "; dgpBufferedTry1.getPort(): "
				+ dgpBufferedTry1.getBufferableDatagramPacket().getPort() + "\n");
		ev1.append("dgpBufferedTry3: "
				+ bin2hex(dgpBufferedTry3.getBufferableDatagramPacket().getData())
				+ "; dgpBufferedTry3.getLength(): "
				+ dgpBufferedTry3.getBufferableDatagramPacket().getLength()
				+ "; dgpBufferedTry3.getHostAddress(): "
				+ dgpBufferedTry3.getBufferableDatagramPacket().getAddress().getHostAddress()
				+ "; dgpBufferedTry3.getPort(): "
				+ dgpBufferedTry3.getBufferableDatagramPacket().getPort() + "\n");
		ev1.append("\nV.A new approach.\n");
		ArrayBlockingQueue<DatagramPacket> netRcvBuffer = new ArrayBlockingQueue<DatagramPacket>(
				10, true);// V.raw layer 4 receiving buffer (including Daragram header)
		for (int i = 0; i < 4; i++) {
			pkDataBuf1 = new byte[] { 6, 5, 4, 3, 2, 1, (byte) i };
			try {
				net4addr = (Inet4Address) java.net.InetAddress.getByName("192.168.0."
						+ Integer.toString(100 + i));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dgp1 = new DatagramPacket(pkDataBuf1, 2 + i);
			sockAddr = new InetSocketAddress(net4addr, 7770 + i);// V.InetSocketAddress class has no
																	// set methods
			dgp1.setSocketAddress(sockAddr);
			try {
				netRcvBuffer.put(dgp1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		java.net.DatagramPacket dgp = null;
		for (int i = 0; i < 4; i++) {
			try {
				dgp = netRcvBuffer.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ev1.append("Queued_dgp .getData(): " + bin2hex(dgp.getData()) + "; .getLength(): "
					+ dgp.getLength() + "; .getHostAddress(): " + dgp.getAddress().getHostAddress()
					+ "; .getPort(): " + dgp.getPort() + "\n");
		}
	}
}
