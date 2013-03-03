/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.storage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class SRPAuthentication 
{
	private static MessageDigest digest;
	private static SecureRandom random;
	private static Mac hmac;
	protected static BigInteger N, g, k;
	
	static {
		try {
			digest = MessageDigest.getInstance("SHA-1");
			hmac = Mac.getInstance("HmacSHA1");
			random = new SecureRandom();
			N = new BigInteger("EEAF0AB9ADB38DD69C33F80AFA8FC5E86072618775FF3C0B9EA2314C9C256576D674DF7496EA81D3383B4813D692C6E0E0D5D8E250B98BE48E495C1D6089DAD15DC7D7B46154D6B6CE8EF4AD69B15D4982559B297BCF1885C529F566660E57EC68EDBC3C05726CC02FD4CBF4976EAA9AFD5138FE8376435B9FC61D2FC0EB06E3", 16);
			g = BigInteger.valueOf(2);
			k = H(N,g);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] getBigBytes(BigInteger value) 
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
			digest.update(getBigBytes(b));
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
	protected SecretKey key;
	
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

	
	/**
	 * Get signature for message based on its content and HTTP headers.
	 * Add the appropriate header to the request for authentication.
	 * H(method, path, content-type, content-length, content)
	 */
	class ScorekeeperSigner implements HttpRequestInterceptor
	{
		@Override
		public void process(HttpRequest request, HttpContext context) throws IOException
		{
			//hmac.init(key);

			digest.update(request.getRequestLine().getMethod().getBytes());
			digest.update(request.getRequestLine().getUri().getBytes());
			digest.update(request.getFirstHeader("Content-Type").getValue().getBytes());
			digest.update(request.getFirstHeader("Content-Length").getValue().getBytes());

			if (request instanceof HttpEntityEnclosingRequest)
				digest.update(EntityUtils.toByteArray(((HttpEntityEnclosingRequest)request).getEntity()));
			
			request.setHeader("X-Scorekeeper",  Base64.encodeBase64String(digest.digest()));
			request.setHeader("Authorization", "some authorization stuff here");
		}
	}
	
	class DialogCreds extends BasicCredentialsProvider
	{
	    public Credentials getCredentials(final AuthScope authscope) 
	    {
	    	Credentials creds = super.getCredentials(authscope);
	    	if (creds == null)
	    	{
	    		// Input Dialog
	    	}
	    	return creds;
	    }
		
	}
	
	public void start() throws IOException, URISyntaxException
	{			
		DefaultHttpClient httpclient = new DefaultHttpClient();
		//httpclient.addRequestInterceptor(new ScorekeeperSigner());
		httpclient.setCredentialsProvider(new DialogCreds());
		
        try {
        	/*
            List<NameValuePair> parm = new ArrayList<NameValuePair>();
            parm.add(new BasicNameValuePair("username", username));
            parm.add(new BasicNameValuePair("A", Base64.encodeBase64String(getBigBytes(A))));
			*/
            HttpPost httppost = new HttpPost(new URI("http", "scorekeeper.wwscc.org", "/authtest.txt", null)); 
            //httppost.setEntity(new UrlEncodedFormEntity(parm));
			
        	
            HttpResponse response = httpclient.execute(httppost);
            try {
                HttpEntity resEntity = response.getEntity();
                System.out.println(response.getStatusLine());
                if (response.getStatusLine().getStatusCode() == 401)
                	System.out.println(response.getFirstHeader("WWW-Authenticate"));
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                    System.out.println("Chunked?: " + resEntity.isChunked());
                }
                
                System.out.println(EntityUtils.toString(resEntity));
            } finally {
                //response.close();
            }
        } finally {
            //httpclient.close();
        }
	}
	
	public static void main(String args[]) throws IOException, URISyntaxException
	{
		new SRPAuthentication("127.0.0.1", "ww2013:series", "ww13").start();
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
		
		key = new SecretKeySpec(K.toByteArray(), "HmacSHA1");
		// authenticated and shared key ready
	}
	
	private String computeSignature(String tosign, byte[] keyData) throws GeneralSecurityException, UnsupportedEncodingException 
	{
	    SecretKey secretKey = new SecretKeySpec(keyData, "HmacSHA1");
	    Mac mac = Mac.getInstance("HmacSHA1");
	    mac.init(secretKey);
	    return "";
	    //return Base64.encodeBase64String(mac.doFinal(tosign.getBytes()), true).trim();
	}
}




