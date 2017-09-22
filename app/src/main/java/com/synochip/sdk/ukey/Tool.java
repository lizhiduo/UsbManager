package com.synochip.sdk.ukey;

public class Tool {
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		if (hexString.length() % 2 != 0) {
			hexString = hexString.substring(0, hexString.length() - 1)
					+ "0"
					+ hexString.substring(hexString.length() - 1,
							hexString.length());
		}

		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}
	public static String byte2HexStr(byte[] b, int len) {
		String stmp = "";
		StringBuffer sb = new StringBuffer();;
		for (int n = 0; n < len; n++) {
			stmp = Integer.toHexString(b[n] & 0xFF);
			sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
			sb.append(" ");
		}
		return sb.toString().toUpperCase().trim();
	}
	public static String short2HexStr(int i) {
		
		byte []b = new byte[2];
		b[0] = (byte)((i>>8) & 0xFF);
		b[1] = (byte)(i & 0xFF);
		return byte2HexStr( b, 2);
	}
	public static String int2HexStr(int i) {
		
		byte []b = new byte[4];
		b[0] = (byte)((i>>24) & 0xFF);
		b[1] = (byte)((i>>16) & 0xFF);
		b[2] = (byte)((i>>8) & 0xFF);
		b[3] = (byte)(i & 0xFF);
		return byte2HexStr( b, 4);
	}
	public static byte[] int2Hex(int i) {
		
		byte []b = new byte[4];
		b[3] = (byte)((i>>24) & 0xFF);
		b[2] = (byte)((i>>16) & 0xFF);
		b[1] = (byte)((i>>8) & 0xFF);
		b[0] = (byte)(i & 0xFF);
		return b;
	}
   public static byte[] hand2Hex(int i) {
		
		byte []b = new byte[2];
		b[1] = (byte)((i>>8) & 0xFF);
		b[0] = (byte)(i & 0xFF);
		return b; 
	}
	
	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}
}
