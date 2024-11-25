/********************************************************************************/
/*										*/
/*		BumpDebugServer.java						*/
/*										*/
/*	Bubble Environment debugger process message server			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss			*/
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




package edu.brown.cs.bubbles.bump;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConnect;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public final class BumpDebugServer implements BumpConstants, MintConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BumpDebugServer bds = new BumpDebugServer(args);
   bds.process();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		mint_name;
private String		our_name;
private int		port_number;
private SocketThread	socket_thread;
private MintControl	mint_control;
private Set<Connection> active_connections;
private Map<String,Connection> named_connections;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BumpDebugServer(String [] args)
{
   mint_name = null;
   our_name = null;
   port_number = 0;
   socket_thread = null;
   mint_control = null;
   active_connections = new HashSet<Connection>();
   named_connections = new HashMap<String,Connection>();

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-M") && i+1 < args.length) {           // -M <mint name>
	    mint_name = args[++i];
	  }
	 else badArgs();
       }
      else badArgs();
    }

   if (mint_name == null) badArgs();
}



private void badArgs()
{
   System.err.println("BUMPDEBUG: bumpdebug -M <mintname>");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (mint_name == null) return;
   if (our_name == null) our_name = mint_name + "_BDDT";

   try {
      ServerSocket ss = new ServerSocket(port_number,5);
      if (!MintConnect.registerSocket(our_name,ss)) return;

      mint_control = MintControl.create(mint_name,MintSyncMode.ONLY_REPLIES);
      mint_control.register("<BANDAID CMD='_VAR_0' ID='_VAR_1'/>",new BandaidHandler());
      mint_control.register("<BDDT CMD='_VAR_0' />",new BumpHandler());
      mint_control.register("<BUBBLES DO='EXIT' />",new ExitHandler());

      socket_thread = new SocketThread(ss);
      socket_thread.start();
    }
   catch (IOException e) { }
}




/********************************************************************************/
/*										*/
/*	Connection processing methods						*/
/*										*/
/********************************************************************************/

private void handleConnection(Socket s)
{
   Connection c = new Connection(s);
   active_connections.add(c);
   c.start();
}



private void removeConnection(Connection c)
{
   active_connections.remove(c);

   if (active_connections.size() == 0) {
      System.exit(0);
    }
}


/********************************************************************************/
/*										*/
/*	Thread for accepting connections from debug clients			*/
/*										*/
/********************************************************************************/

private class SocketThread extends Thread {

   private ServerSocket server_socket = null;

   SocketThread(ServerSocket ss) {
      super("BumpDebugServerSocketListener");
      server_socket = ss;
    }

   String getLocation() {
      String h = server_socket.getInetAddress().getHostAddress();
      if (h.equals("0.0.0.0")) {
	try {
	    h = InetAddress.getLocalHost().getHostAddress();
	  }
	 catch (UnknownHostException e) { }
       }
      return "<SOCKET HOST='" + h + "' PORT='" + server_socket.getLocalPort() + "' />";
    }

   @Override public void run() {
      Socket s;
      while (server_socket != null) {
	 try {
	    s = server_socket.accept();
	    handleConnection(s);
	  }
	 catch (IOException e) { }
       }
    }

}	// end of inner class SocketThread




/********************************************************************************/
/*										*/
/*	Class representing a debugger connection				*/
/*										*/
/********************************************************************************/

private class Connection extends Thread {

   private Socket client_socket;
   private String client_id;
   private PrintWriter output_writer;

   Connection(Socket s) {
      super("BumpDebugConnection_" + s.getRemoteSocketAddress());
      try {
	 s.setSoTimeout(60000);
       }
      catch (SocketException e) {
	 System.err.println("BUMPDEBUG: Problem with socket timeout: " + e);
       }
      client_socket = s;
      output_writer = null;
      client_id = null;
    }

   @Override public void run() {
      System.err.println("BUMP: start connect");
   
      try {
         InputStream ins = client_socket.getInputStream();
         try (BufferedReader lnr = new BufferedReader(new InputStreamReader(ins))) {
            boolean done = false;
            while (!done) {
               StringBuffer body = null;
               for ( ; ; ) {
                  String s = lnr.readLine();
                  // System.err.println("READ " + s);
                  if (s == null) {
                     done = true;
                     break;
                   }
                  if (s.equals(BUMP_BANDAID_TRAILER)) break;
                  if (body == null) body = new StringBuffer(s);
                  else {
                     body.append("\n");
                     body.append(s);
                   }
                }
               if (body != null) {
                  String btxt = body.toString();
                  if (client_id == null && btxt.startsWith("CONNECT")) {
                     client_id = btxt.substring(8).trim();
                     named_connections.put(client_id,this);
                   }
                  else {
                     mint_control.send(btxt);
                   }
                }
             }
          }
       }
      catch (IOException e) {
         System.err.println("BUMP: Problem with socket: " + e);
         e.printStackTrace();
       }
      catch (Throwable t) {
         System.err.println("BUMP: Problem with command: " + t);
         t.printStackTrace();
       }
   
      System.err.println("BUMP CONNECTION EXITED");
   
   
      try {
         client_socket.close();
         if (output_writer != null) output_writer.close();
       }
      catch (IOException e) { }
   
      removeConnection(this);
    }

   synchronized void sendCommand(String msg) {
      if (output_writer == null) {
         try {
            output_writer = new PrintWriter(client_socket.getOutputStream());
          }
         catch (IOException e) {
            return;
         }
       }
      output_writer.println(msg);
      output_writer.flush();
    }

}	// end of inner class Connection





/********************************************************************************/
/*										*/
/*	Message handling							*/
/*										*/
/********************************************************************************/

private final class BandaidHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String id = args.getArgument(1);
      Connection c = named_connections.get(id);
      if (c != null) c.sendCommand(cmd);
      msg.replyTo();
    }

}	// end of inner class BandaidHandler



private final class BumpHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String rply = null;
      if (cmd == null) ;
      else if (cmd.equals("PORT")) {
         rply = socket_thread.getLocation();
       }
   
      msg.replyTo(rply);
    }

}	// end of inner class BumpHandler



private final class ExitHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      System.exit(0);
    }

}	// end of inner class ExitHandler






}	// end of class BumpDebugServer




/* end of BumpDebugServer.java */
