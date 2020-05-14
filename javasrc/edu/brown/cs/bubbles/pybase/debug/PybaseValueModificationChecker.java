/********************************************************************************/
/*										*/
/*		PybaseValueModifiationChecker.java				*/
/*										*/
/*	Check for value modifications in the stacks				*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class should check for value modifications in the stacks while debugging.
 * Its public interface is completely synchronized.
 *
 * @author Fabio
 */
class PybaseValueModificationChecker {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// Thread id --> stack id --> stack
private Map<String,Map<String, PybaseDebugStackFrame>> cache = new HashMap<>();

private Object lock = new Object();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseValueModificationChecker()			{ }





/********************************************************************************/
/*										*/
/*	Verify modified values							*/
/*										*/
/********************************************************************************/

public synchronized void verifyModified(PybaseDebugStackFrame frame, PybaseDebugVariable[] newFrameVariables)
{
   synchronized(lock) {
      Map<String, PybaseDebugStackFrame> threadIdCache = cache.get(frame.getThreadRemoteId());
      if(threadIdCache == null){
	 threadIdCache = new HashMap<String, PybaseDebugStackFrame>();
	 cache.put(frame.getThreadRemoteId(), threadIdCache);
       }

      PybaseDebugStackFrame cacheFrame = threadIdCache.get(frame.getId());
      if(cacheFrame == null){
	 threadIdCache.put(frame.getId(), frame);
	 return;
       }
      //not null
      if(cacheFrame == frame){ //if is same, it has already been checked.
	 return;
       }

      //if it is not the same, we have to check it and mark it as the new frame.
      verifyVariablesModified(newFrameVariables, cacheFrame);
      threadIdCache.put(frame.getId(), frame);
    }
}




/********************************************************************************/
/*										*/
/*	Method to remove from cache any threads that no longer exist		*/
/*										*/
/********************************************************************************/

synchronized void onlyLeaveThreads(Collection<PybaseDebugThread> threads)
{
   synchronized(lock){
      HashSet<String> ids = new HashSet<String>();
      for (PybaseDebugThread thread : threads) {
	 ids.add(thread.getRemoteId());
       }
      //Must iterate in a copy.
      Set<String> keySet = new HashSet<String>(cache.keySet());
      for (String id: keySet) {
	 if (!ids.contains(id)) {
	    cache.remove(id);
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Compare stack frames to find and mark modified variables		*/
/*										*/
/********************************************************************************/

private void verifyVariablesModified(PybaseDebugVariable[] newFrameVariables, PybaseDebugStackFrame oldFrame )
{
   PybaseDebugVariable newVariable = null;

   Map<String, PybaseDebugVariable> variablesAsMap = oldFrame.getVariablesAsMap();

   //we have to check for each new variable
   for( int i=0; i<newFrameVariables.length; i++ ) {
      newVariable = newFrameVariables[i];

      PybaseDebugVariable oldVariable = variablesAsMap.get(newVariable.getName());

      if( oldVariable != null) {
	 boolean equals = newVariable.getValueString().equals( oldVariable.getValueString() );

	 //if it is not equal, it was modified
	 newVariable.setModified( !equals );

       }
      else{ //it didn't exist before...
	 newVariable.setModified( true );
       }
    }


}




}	// end of class PybaseValueModificationChecker





/* end of PybaseValueModificationChecker.java */
