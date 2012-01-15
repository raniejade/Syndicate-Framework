package org.q2.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class MD5 {
    public static String md5(byte[] data) {
	try {
	    MessageDigest digest = MessageDigest.getInstance("MD5");
	
	    digest.update(data);
	    byte[] results = digest.digest();
	
	    StringBuffer buffer = new StringBuffer();
	    for(byte t : results) {
		String hex = Integer.toHexString(0xFF & t);
		if(hex.length()==1) {
		    buffer.append('0');
		}
		buffer.append(hex);
	    }

	    return buffer.toString();
	} catch(NoSuchAlgorithmException e) {
	    e.printStackTrace();
	}
	return null;
    }
}