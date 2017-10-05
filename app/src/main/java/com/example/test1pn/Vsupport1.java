package com.example.test1pn;

public class Vsupport1 {
	/**
	 * Append in an EditText whos parent is an Activity. The Activity is inferred from EditText
	 * param. This method can be called from a background thread:
	 * <p>
	 * {@code ((android.app.Activity)et.getContext()).runOnUiThread(new Runnable()...}
	 * </p>
	 * @param et
	 *            the EditText instance to append into
	 * @param msg
	 *            the message to log
	 * @author Vladi
	 * 
	 */
	public static void log(final android.widget.EditText et, final String msg) {
//		android.util.Log.i("Vsupport1", "V.from log(..)");
		((android.app.Activity)et.getContext()).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				et.append(msg);
			}
		});
	}
	public static enum XLINK {
		PAYLOAD(102), ACK(103), PAYLOAD_AND_ACK(104),
				PMP_PAYLOAD(105), PTP_DATA(106), PTP_DATA_ACK(107),
				PTP_IC(108), PTP_IC_ACK(109), UNKNOWN(110);
		private final int numVal;//V.numericValue
		private XLINK(int numVal) {
			this.numVal = numVal;
		}
		public static String getName(int numVal) {
			for (XLINK x : XLINK.values())
				if (x.numVal == numVal) return x.name();
			return XLINK.UNKNOWN.name();
		}
		public int getNumericValue() {
			return numVal;
		}
	}
	// http://stackoverflow.com/a/6209739/3180812
	public static void textFileLog(String text, java.io.BufferedWriter buf) {
		try {// BufferedWriter for performance
			buf.append(text);
			buf.newLine();
//			buf.close();
		} catch (java.io.IOException e) {
			android.util.Log.i("textFileLog", "Vl." + e.toString());
		}
	}
}
