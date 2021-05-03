/********************************************************************************/
/*										*/
/*		BoardUpload.java						*/
/*										*/
/*	Bubbles log and working set uploader					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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

package edu.brown.cs.bubbles.board;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;



/**
 *	This class provides support to upload a file to the current Bubbles
 *	web space.
 **/

public class BoardUpload implements BoardConstants {



/********************************************************************************/
/*										*/
/* Private storage								*/
/*										*/
/********************************************************************************/

private URLConnection url_connection;
private OutputStream output_stream;
private String file_url;
private String boundary_string;
private int	time_out;


private static Random static_random = new Random();
private static boolean has_failed = false;



/********************************************************************************/
/*										*/
/* Constructors 								*/
/*										*/
/********************************************************************************/

/**
 * Upload a general file to a server for others to access
 *
 * @param file A File object of the file to upload
 * @throws IOException
 */

public BoardUpload(File file) throws IOException
{
   time_out = METRICS_UPLOAD_TIMEOUT;

   String url = getSaveAddress();
   if (url == null) return;

   setup(url);
   setParameter("file", file);
   setParameter("set", "1");
   postAndExit(url);
}




/**
 * Upload a metrics file to a server with associated information.
 *
 * @param file A File object of the file to upload
 * @param user UserID
 * @param runid RunID
 * @throws IOException
 */

public BoardUpload(File file, String user, String runid) throws IOException
{
   time_out = METRICS_UPLOAD_TIMEOUT;

   String url = getSaveAddress();
   if (url == null) return;

   runid = runid.replaceAll("\\s","_");

   setup(url);
   setParameter("file", file);
   setParameter("user", user);
   setParameter("runid", runid);
   setParameter("set", "0");
   postAndExit(url);
}



/********************************************************************************/
/*										*/
/*	Public method for retrieving the url of the posted file 		*/
/*										*/
/********************************************************************************/

/**
 *
 * @return The full url of the uploaded file (i.e. cs.brown.edu/codebubbles/uploadedfile.xml)
 */

public String getFileURL()
{
   return file_url;
}



/********************************************************************************/
/*										*/
/*	Methods for handling URLs						*/
/*										*/
/********************************************************************************/

private static String getSaveAddress()
{
   if (has_failed) return null;
   
   BoardProperties bp = BoardProperties.getProperties("Board");
   String rslt = bp.getProperty(BOARD_SAVE_ADDR_PROP);
   if (rslt == null) return null;
   
   rslt = rslt.replace("conifer.cs.brown.edu","conifer2.cs.brown.edu");

   return rslt;
}


public static String getUploadUrl()
{
   BoardProperties bp = BoardProperties.getProperties("Board");
   String rslt = bp.getProperty(BOARD_UPLOAD_URL);
   if (rslt == null) return null;
   
   rslt = rslt.replace("conifer.cs.brown.edu","conifer2.cs.brown.edu");
   
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods for shared code in constructors 				*/
/*										*/
/********************************************************************************/

private void setup(String url) throws IOException
{
   boundary_string = "---------------------------" + randomString() + randomString() + randomString();
   output_stream = null;
   file_url = null;

   try {
      url_connection = new URL(url).openConnection();
    }
   catch (MalformedURLException e) {
      BoardLog.logE("BOARD",
		       "Failed to connect to server while uploading", e);
      throw new IOException("Failed to connect to upload server", e);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD",
		       "Failed to connect to server while uploading", e);
      throw new IOException("Failed to connect to upload server", e);
    }

   url_connection.setConnectTimeout(time_out);
   url_connection.setReadTimeout(time_out);
   url_connection.setDoOutput(true);
   url_connection.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + boundary_string);
}



private void buildURL(String url)
{
   String[] t = url.split("/");

   String r = "";
   for (int i = 0; i < t.length - 1; i++) {
      r += t[i];
      r += "/";
    }

   file_url = r + file_url;
}



private void postAndExit(String url) throws IOException
{
   try {
      post();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD", "Error while posting to server", e);
      throw new IOException("Error while posting to server", e);
    }
   buildURL(url);
}




/********************************************************************************/
/*										*/
/*	Writing methods 							*/
/*										*/
/********************************************************************************/

private void writeName(String name) throws IOException
{
   newline();
   write("Content-Disposition: form-data; name=\"");
   write(name);
   write('"');
}



private static int pipe(InputStream in, OutputStream out) throws IOException
{
   byte[] buf = new byte[500000];
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



private void connect() throws IOException
{
   if (output_stream == null)
      output_stream = url_connection.getOutputStream();
}



private void write(char c) throws IOException
{
   connect();
   output_stream.write(c);
}



private void write(String s) throws IOException
{
   connect();
   output_stream.write(s.getBytes());
}



private void newline() throws IOException
{
   connect();
   write("\r\n");
}



private void writeln(String s) throws IOException
{
   connect();
   write(s);
   newline();
}



private static String randomString()
{
   return Long.toString(static_random.nextLong(), 36);
}



private void boundary() throws IOException
{
   write("--");
   write(boundary_string);
}



/********************************************************************************/
/*										*/
/*	Information setters							*/
/*										*/
/********************************************************************************/

private void setParameter(String name, String filename, InputStream is)
	throws IOException
{
   boundary();
   writeName(name);
   write("; filename=\"");
   write(filename);
   write('"');
   newline();
   write("Content-Type: ");
   String type = URLConnection.guessContentTypeFromName(filename);
   if (type == null)
      type = "application/octet-stream";
   writeln(type);
   newline();
   pipe(is, output_stream);
   newline();
}



private void setParameter(String name, String value) throws IOException
{
   boundary();
   writeName(name);
   newline();
   newline();
   if (value == null) writeln("null");
   else writeln(value);
}



private void setParameter(String name, File file) throws IOException
{
   setParameter(name, file.getPath(), new FileInputStream(file));
}


private void post() throws IOException
{
   boundary();
   writeln("--");
   output_stream.close();

   try {
      InputStream instream = url_connection.getInputStream();
      BufferedReader in = new BufferedReader(new InputStreamReader(instream));
      file_url = in.readLine();
      in.close();
    }
   catch (SocketTimeoutException e) {
      BoardLog.logE("BOARD","Upload failed due to timeout: " + e);
      has_failed = true;
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Upload failed due to I/O error: " + e,e);
    }
}



}	// end of class BoardUpload




/* end of BoardUpload.java */
