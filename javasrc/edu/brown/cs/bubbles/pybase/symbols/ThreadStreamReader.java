/********************************************************************************/
/*										*/
/*		CompiledModule.java						*/
/*										*/
/*	Python Bubbles Base stream reader in a thread				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/08/2005
 */

package edu.brown.cs.bubbles.pybase.symbols;

import java.io.InputStream;
import java.io.InputStreamReader;



class ThreadStreamReader extends Thread {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

/**
 * Input stream read.
 */
private final InputStream input_stream;

/**
 * Buffer with the contents gotten.
 */
private final StringBuilder contents;

/**
 * Access to the buffer should be synchronized.
 */
private final Object lock = new Object();

/**
 * Whether the read should be synchronized.
 */
private final boolean is_synchronized;

/**
 * Keeps the next unique identifier.
 */
private static int next=0;

/**
 * Get a unique identifier for this thread.
 */

private static synchronized int next(){
   next ++;
   return next;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ThreadStreamReader(InputStream isr)
{
   this(isr, true); //default is synchronize.
}


public ThreadStreamReader(InputStream isr, boolean synchronize)
{
   this.setName("ThreadStreamReader: "+next());
   this.setDaemon(true);
   contents = new StringBuilder();
   this.input_stream = isr;
   this.is_synchronized = synchronize;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override public void run()
{
   try {
      InputStreamReader in = new InputStreamReader(input_stream);
      int c;
      if(is_synchronized){
	 while ((c = in.read()) != -1) {
	    synchronized(lock){
	       contents.append((char) c);
	     }
	  }
       }
      else{
	 while ((c = in.read()) != -1) {
	    contents.append((char) c);
	  }
       }
    }
   catch (Exception e) {
      //that's ok
    }
}



/**
 * @return the contents that were obtained from this instance since it was started or since
 * the last call to this method.
 */
public String getAndClearContents()
{
   synchronized(lock){
      String string = contents.toString();
      contents.setLength(0);
      return string;
    }
}



public String getContents()
{
   synchronized(lock){
      return contents.toString();
    }
}


}	// end of class ThreadStreamReader




/* end of ThreadStreamReader.java */
