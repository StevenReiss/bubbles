/********************************************************************************/
/*                                                                              */
/*              BhelpWebServer.java                                             */
/*                                                                              */
/*      Web server to handle showme requests                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.board.BoardLog;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


class BhelpWebServer implements BhelpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ServerSocket    web_socket;

private final static byte [] EOL_BYTES = "\r\n".getBytes();
private final static String CRLF = "\r\n";
private final static String LF = "\n";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BhelpWebServer() throws IOException
{
   web_socket = new ServerSocket(HELP_SHOWME_PORT);
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process()
{
   Acceptor ac = new Acceptor();
   ac.start();
}



/********************************************************************************/
/*                                                                              */
/*      Accept connections                                                      */
/*                                                                              */
/********************************************************************************/

private class Acceptor extends Thread {
   
   Acceptor() { 
      super("BhelpWebAcceptor");
    }
   
   @Override public void run() {
      try {
         for ( ; ; ) {
            Socket s = web_socket.accept();
            if (s != null) {
               Client c = new Client(s);
               c.start();
             }
          }
       }
      catch (IOException e) {
         try {
            web_socket.close();
            web_socket = null;
          }
         catch (IOException ex) { }
       }
    }
   
}       // end of inner class Acceptor




/********************************************************************************/
/*                                                                              */
/*      Web client                                                              */
/*                                                                              */
/********************************************************************************/

private static class Client extends Thread {
   
   private Socket client_socket;
   private InputStream input_stream;
   private OutputStream output_stream;
   
   Client(Socket s) {
      super("BhelpWebClient_" + s);
      client_socket = s;
      try {
         input_stream = s.getInputStream();
         output_stream = s.getOutputStream();
       }
      catch (IOException e) { }
    }
   
   @Override public void run() {
      try {
         for ( ; ; ) {
            if (input_stream == null) break;
            String hdr = readHeader();
            if (hdr == null) break;
            String loc = parseHeader(hdr);
            File floc = new File(loc);
            String what = floc.getName();
            if (what == null) continue;
            BhelpFactory bf = BhelpFactory.getFactory();
            bf.startDemonstration(null,what);
            sendReply();
          }
       }
      catch (IOException e) {
         BoardLog.logE("BHELP","I/O Problem with bhelp web client",e);
       }
      finally {
         try {
            input_stream.close();
            input_stream = null;
          }
         catch (IOException e) { }
         try {
            output_stream.close();
            output_stream = null;
          }
         catch (IOException e) { }
         try {
            client_socket.close();
            client_socket = null;
          }
         catch (IOException e) { }
       }
    }   
   
   private String readHeader() throws IOException {
      byte [] bytes = new byte[20480];
      int idx = 0;
      byte [] onebyte = new byte[1];
      
      while (input_stream != null && input_stream.read(onebyte) != -1) {
         if (idx + 1 > bytes.length) {
            byte [] t = bytes;
            bytes = new byte[t.length*2];
            for (int i = 0; i < idx; ++i) bytes[i] = t[i];
          }
         bytes[idx++] = onebyte[0];
         if ((idx >= 2 && bytes[idx-1] == '\n' && bytes[idx-2] == '\n') ||
               (idx >= 4 && bytes[idx-1] == '\n' && bytes[idx-2] == '\r' &&
                     bytes[idx-3] == '\n' && bytes[idx-4] == '\r')) {
            String hdr = new String(bytes,0,idx,"8859_1");
            return hdr;
          }
       }
      if (idx == 0) return null;
      
      throw new IOException("Incomplete HTTP Header (" + idx + ")" +
            new String(bytes,0,idx,"8859_1"));
    }
   
   private String parseHeader(String hdr) throws IOException {
      if (hdr == null) return null;
      String delim = (hdr.endsWith(CRLF) ? CRLF : LF);
      int pos = hdr.indexOf(delim);
      String line = hdr.substring(0,pos);
      int idx2 = line.indexOf(" HTTP/");
      if (idx2 < 0) return null;
      int idx1 = line.indexOf(" ");
      if (idx1 < 0) return null;
      String loc = line.substring(idx1+1,idx2);
      return loc;
    }
   
   private void sendReply() throws IOException {
      if (output_stream == null) return;
      writeHeader("HTTP/1.1 200 OK");
      writeHeader("Content-Length: 0");
      writeHeader("");
      output_stream.flush();
    }
   
   private void writeHeader(String s) throws IOException {
      byte [] buf = s.getBytes();
      output_stream.write(buf);
      output_stream.write(EOL_BYTES);
    }
            
}       // end of inner class Client
   



}       // end of class BhelpWebServer




/* end of BhelpWebServer.java */

