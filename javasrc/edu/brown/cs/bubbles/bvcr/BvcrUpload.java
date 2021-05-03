/********************************************************************************/
/*										*/
/*		BvcrUpload.java 						*/
/*										*/
/*	Bubble Version Collaboration Repository code to upload change sets	*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* SVN: $Id$ */


package edu.brown.cs.bubbles.bvcr;


import edu.brown.cs.bubbles.board.BoardProperties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEParameterSpec;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;


class BvcrUpload implements BvcrConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private URLConnection	url_connection;
private OutputStream	output_stream;
private String		boundary_string;
private int		time_out;


private static Random static_random = new Random();




/********************************************************************************/
/*										*/
/*	Static entries								*/
/*										*/
/********************************************************************************/

static boolean upload(String data,String user,String rid,SecretKey key)
{
   if (user == null || rid == null) return false;

   try {
      BvcrUpload bu = new BvcrUpload();
      bu.processUpload(data,user,rid,key);
    }
   catch (IOException e) {
      System.err.println("BVCR: Upload status: " + e);
      e.printStackTrace();
      return false;
    }

   return true;
}



static InputStream download(String user,String rid,long dlm)
{
   InputStream ins = null;

   try {
      BvcrUpload bu = new BvcrUpload();
      ins = bu.processDownload(user,rid,dlm);
    }
   catch (IOException e) {
      return null;
    }

   return ins;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BvcrUpload()
{
   time_out = 30*1000;
   url_connection = null;
   output_stream = null;
   boundary_string = "---------------------------" + randomString() + randomString() + randomString();
}




/********************************************************************************/
/*										*/
/*	Upload methods								*/
/*										*/
/********************************************************************************/

@SuppressWarnings("resource")
private void processUpload(String data,String uid,String rid,SecretKey key) throws IOException
{
   String url = getRepoUrl() + "savebvcr.php";
   setupConnection(url);
   output_stream = url_connection.getOutputStream();
   byte [] bdata;

   if (KEY_COMPRESS) {
      // have to use DeflatorOutput to match InflatorInput
      // DeflatorInput and InflatorInput don't seem to be compatible
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DeflaterOutputStream dos = new DeflaterOutputStream(bos);
      for (byte b : data.getBytes()) {
	 dos.write(b);
       }
      dos.close();
      bdata = bos.toByteArray();
    }
   else bdata = data.getBytes();

   InputStream is = new ByteArrayInputStream(bdata);

   if (key != null) {
      try {
	 Cipher c = Cipher.getInstance(key.getAlgorithm());
	 AlgorithmParameterSpec ps = new PBEParameterSpec(KEY_SALT,KEY_COUNT);
	 c.init(Cipher.ENCRYPT_MODE,key,ps);
	 is = new CipherInputStream(is,c);
       }
      catch (Exception e) {
	 System.err.println("BVCR: Unable to create cipher: " + e);
       }
    }

   setParameter("U",uid);
   setParameter("R",rid);
   setParameter("file","/tmp/" + uid + "_" + rid + ".bvcr",is);
   is.close();
   InputStream ins = post();

   BufferedReader in = new BufferedReader(new InputStreamReader(ins));
   for ( ; ; ) {
      String ln = in.readLine();
      if (ln == null) break;
      System.err.println("BVCR: UPLOAD RESULT: " + ln);
    }
   System.err.println("UPLOAD COMPLETE");
   in.close();
}




/********************************************************************************/
/*										*/
/*	Download methods							*/
/*										*/
/********************************************************************************/

private InputStream processDownload(String uid,String rid,long dlm) throws IOException
{
   String url = getRepoUrl() + "loadbvcr.php";

   setupConnection(url);
   output_stream = url_connection.getOutputStream();

   setParameter("U",uid);
   setParameter("R",rid);
   setParameter("D",Long.toString(dlm));

   System.err.println("BVCR: URL = " + url + "?U=" + uid + "&R=" + rid + "&D=" + Long.toString(dlm));

   InputStream ins = post();

   return ins;
}



/********************************************************************************/
/*										*/
/*	Methods for handling URLs						*/
/*										*/
/********************************************************************************/

static String getRepoUrl()
{
   BoardProperties bp = BoardProperties.getProperties("Bvcr");
   String rslt = bp.getProperty("Bvcr.repo.url");

   if (rslt == null || rslt.length() == 0 || rslt.startsWith("*")) return null;
   rslt = rslt.replace("conifer.cs.brown.edu","conifer2.cs.brown.edu");

   if (!rslt.endsWith("/")) rslt += "/";

   return rslt;
}




/********************************************************************************/
/*										*/
/*	URL management								*/
/*										*/
/********************************************************************************/

private void setupConnection(String url) throws IOException
{
   try {
      url_connection = new URL(url).openConnection();
    }
   catch (MalformedURLException e) {
      System.err.println("BVCR: Failed to connect to server while uploading: " + e);
      throw new IOException("Failed to connect to upload server",e);
    }
   catch (IOException e) {
      System.err.println("BVCR: Failed to connect to server while uploading: " + e);
      throw new IOException("Failed to connect to upload server",e);
    }

   url_connection.setConnectTimeout(time_out);
   url_connection.setReadTimeout(time_out);
   url_connection.setDoOutput(true);
   url_connection.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + boundary_string);
}



private static String randomString()
{
   return Long.toString(static_random.nextLong(), 36);
}




/********************************************************************************/
/*										*/
/*	Parameter field methods 						*/
/*										*/
/********************************************************************************/

private void setParameter(String name,String value) throws IOException
{
   boundary();
   writeName(name);
   newline();
   newline();
   if (value == null) writeln("null");
   else writeln(value);
}



private void setParameter(String name,String filename,InputStream is) throws IOException
{
   boundary();
   writeName(name);
   write("; filename=\"");
   write(filename);
   write('"');
   newline();
   writeln("Content-Type: ");
   writeln("application/octet-stream");
   newline();
   pipe(is, output_stream);
   newline();
}




private void boundary() throws IOException
{
   write("--");
   write(boundary_string);
}



private void writeName(String name) throws IOException
{
   newline();
   write("Content-Disposition: form-data; name=\"");
   write(name);
   write('"');
}



private void newline() throws IOException
{
   write("\r\n");
}



private void write(char c) throws IOException
{
   output_stream.write(c);
}



private void write(String s) throws IOException
{
   output_stream.write(s.getBytes());
}



private void writeln(String s) throws IOException
{
   write(s);
   newline();
}




private static int pipe(InputStream in,OutputStream out) throws IOException
{
   byte[] buf = new byte[10240];
   int nread;
   int total = 0;
   synchronized (in) {
      while ((nread = in.read(buf, 0, buf.length)) >= 0) {
	 out.write(buf, 0, nread);
	 total += nread;
       }
    }
   out.flush();
   buf = null;

   return total;
}




/********************************************************************************/
/*										*/
/*	Sending methods 							*/
/*										*/
/********************************************************************************/

private InputStream post() throws IOException
{
   boundary();
   writeln("--");
   output_stream.close();

   return url_connection.getInputStream();
}





}	// end of class BvcrUpload




/* end of BvcrUpload.java */
