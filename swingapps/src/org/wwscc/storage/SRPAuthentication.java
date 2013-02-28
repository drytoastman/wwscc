package org.wwscc.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.sasl.AuthenticationException;

import org.wwscc.util.Base64;
import org.wwscc.util.CancelException;


@SuppressWarnings("unused")
public class SRPAuthentication 
{
	private static MessageDigest digest;
	private static SecureRandom random;
	protected static BigInteger N, g, k;
	
	static {
		try {
			digest = MessageDigest.getInstance("SHA-1");
			random = new SecureRandom();
			N = new BigInteger("EEAF0AB9ADB38DD69C33F80AFA8FC5E86072618775FF3C0B9EA2314C9C256576D674DF7496EA81D3383B4813D692C6E0E0D5D8E250B98BE48E495C1D6089DAD15DC7D7B46154D6B6CE8EF4AD69B15D4982559B297BCF1885C529F566660E57EC68EDBC3C05726CC02FD4CBF4976EAA9AFD5138FE8376435B9FC61D2FC0EB06E3", 16);
			g = BigInteger.valueOf(2);
			k = H(N,g);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] getMyBytes(BigInteger value) 
	{	
		byte[] bytes = value.toByteArray();
       	if (bytes[0] == 0) 
       	{	
			byte[] tmp = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, tmp, 0, tmp.length);
			return tmp;
		}
		return bytes;
	}
	
	public static BigInteger H(BigInteger ... args)
	{
		for (BigInteger b : args)
		{
			byte[] todigest = getMyBytes(b);
			digest.update(todigest);
		}
		return new BigInteger(1, digest.digest());
	}
	
	public static BigInteger H(String ... args)
	{
		for (String s : args)
			digest.update(s.getBytes());
		return new BigInteger(1, digest.digest());
	}
	
	protected String hostname, username, password;
	protected BigInteger a, A, M1, K;
	
	public SRPAuthentication(String host, String user, String pass)
	{				
		hostname = host;
		username = user;
		password = pass;
		byte junk[] = new byte[256];
		random.nextBytes(junk);
		a = new BigInteger(1, junk);
		A = g.modPow(a, N);
	}
	
	public void start()
	{			
		// send username, A
	}
	
	public void server1(BigInteger salt, BigInteger B) throws AuthenticationException
	{
		if (B.mod(N).equals(BigInteger.ZERO))
			throw new AuthenticationException("BmodN is 0");
		
		BigInteger x = H( salt, H(username + ':' + password ));        
		BigInteger u = H(A,B);
		BigInteger S = B.subtract(k.multiply(g.modPow(x, N))).modPow(u.multiply(x).add(a), N);
		
		K = H(S);
		M1 = H(H(N).xor(H(g)), H(username), salt, A, B, K);
		
		// send M1
	}
	    		
	public void server2(BigInteger M2) throws AuthenticationException
	{
		if (!M2.equals(H(A, M1, K)))
			throw new AuthenticationException("Invalid server proof");
		
		// authenticated and shared key ready
	}
	
	private String computeSignature(String tosign, byte[] keyData) throws GeneralSecurityException, UnsupportedEncodingException 
	{
	    SecretKey secretKey = new SecretKeySpec(keyData, "HmacSHA1");
	    Mac mac = Mac.getInstance("HmacSHA1");
	    mac.init(secretKey);
	    return "";
	    //return Base64.encodeToString(mac.doFinal(tosign.getBytes()), true).trim();
	}
}




