/********************************************************************************/
/*										*/
/*		BoardThreadPool.java						*/
/*										*/
/*	Bubbles attribute and property management worker thread pool support	*/
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


/* SVN: $Id$ */


package edu.brown.cs.bubbles.board;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



/**
 *	This class provides a thread pool to handle background tasks without
 *	the overhead of continually creating new threads.
 **/

public final class BoardThreadPool extends ThreadPoolExecutor
	implements BoardConstants, ThreadFactory
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BoardThreadPool	the_pool = new BoardThreadPool();
private static int		thread_counter = 0;




/********************************************************************************/
/*										*/
/*	Static entries								*/
/*										*/
/********************************************************************************/

/**
 *	Execute a background task using our thread pool.
 **/

public static void start(Runnable r)
{
   if (r != null) {
      for ( ; ; ) {
         try {
            the_pool.execute(r);
            break;
          }
         catch (RejectedExecutionException e) { }
         synchronized (the_pool) {
            try {
               the_pool.wait(100);
             }
            catch (InterruptedException e) { }
          }   
       }
    }
}




/**
 *	Remove a task if possible
 **/

public static void finish(Runnable r)
{
   if (r != null) the_pool.remove(r);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardThreadPool()
{
   super(BOARD_CORE_POOL_SIZE,BOARD_MAX_POOL_SIZE,
	    BOARD_POOL_KEEP_ALIVE_TIME,TimeUnit.MILLISECONDS,
	    new SynchronousQueue<Runnable>());

   setThreadFactory(this);
}




/********************************************************************************/
/*										*/
/*	Thread creation methods 						*/
/*										*/
/********************************************************************************/

@Override public Thread newThread(Runnable r)
{
   return new Thread(r,"BoardWorkerThread_" + (++thread_counter));
}



/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

@Override protected void beforeExecute(Thread t,Runnable r)
{
   super.beforeExecute(t,r);
}



@Override protected void afterExecute(Runnable r,Throwable t)
{
   super.afterExecute(r,t);

   if (t != null) {
      BoardLog.logE("BOARD","Problem with background task " + r.getClass().getName() + " " + r,t);
    }
}




}	// end of class BoardThreadPool




/* end of BoardThreadPool.java */
