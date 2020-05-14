/********************************************************************************/
/*										*/
/*		CompletionParticipantsHandler.java				*/
/*										*/
/*	Python Bubbles Base completion code helper class			*/
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

package edu.brown.cs.bubbles.pybase.symbols;

import org.python.pydev.core.FullRepIterable;

import java.util.ArrayList;
import java.util.Collection;


public class CompletionParticipantsHelper {

/**
 * Get the completions based on the arguments received
 *
 * @param state this is the state used for the completion
 * @param localScope this is the scope we're currently on (may be null)
 */
public static Collection<AbstractToken> getCompletionsForTokenWithUndefinedType(CompletionState state,
										   LocalScope localScope)
{
   AbstractToken[] localTokens = localScope.getLocalTokens(-1, -1, false); // only to get the
   // args
   String activationToken = state.getActivationToken();
   String firstPart = FullRepIterable.getFirstPart(activationToken);
   for (AbstractToken token : localTokens) {
      if (token.getRepresentation().equals(firstPart)) {
	 Collection<AbstractToken> interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
	 Collection<AbstractToken> argsCompletionFromParticipants = getCompletionsForTokenWithUndefinedTypeFromParticipants(
	    state, localScope, interfaceForLocal);
	 return argsCompletionFromParticipants;
       }
    }
   return getCompletionsForTokenWithUndefinedTypeFromParticipants(state, localScope, null);
}

/**
 * If we were unable to find its type, pass that over to other completion participants.
 */
public static Collection<AbstractToken> getCompletionsForTokenWithUndefinedTypeFromParticipants(CompletionState state,LocalScope localScope,
												   Collection<AbstractToken> interfaceForLocal)
{
   ArrayList<AbstractToken> ret = new ArrayList<AbstractToken>();

   //	List<?> participants = ExtensionHelper
   //	       .getParticipants(ExtensionHelper.PYDEV_COMPLETION);
   //	for (Iterator<?> iter = participants.iterator(); iter.hasNext();) {
   //	   IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
   //	   ret.addAll(participant.getCompletionsForTokenWithUndefinedType(state, localScope,
   //		  interfaceForLocal));
   //	}
   return ret;
}


/**
 * Get the completions based on the arguments received
 *
 * @param state this is the state used for the completion
 * @param localScope this is the scope we're currently on (may be null)
 */
public static Collection<AbstractToken> getCompletionsForMethodParameter(CompletionState state,
									    LocalScope localScope)
{
   AbstractToken[] args = localScope.getLocalTokens(-1, -1, true); // only to get the args
   String activationToken = state.getActivationToken();
   String firstPart = FullRepIterable.getFirstPart(activationToken);
   for (AbstractToken token : args) {
      if (token.getRepresentation().equals(firstPart)) {
	 Collection<AbstractToken> interfaceForLocal = localScope.getInterfaceForLocal(state
											  .getActivationToken());
	 Collection<AbstractToken> argsCompletionFromParticipants = getCompletionsForMethodParameterFromParticipants(
	    state, localScope, interfaceForLocal);
	 for (AbstractToken t : interfaceForLocal) {
	    if (!t.getRepresentation().equals(state.getQualifier())) {
	       argsCompletionFromParticipants.add(t);
	     }
	  }
	 return argsCompletionFromParticipants;
       }
    }
   return new ArrayList<AbstractToken>();
}

/**
 * If we were able to find it as a method parameter, this method is called so that clients can extend those completions.
 */
public static Collection<AbstractToken> getCompletionsForMethodParameterFromParticipants(CompletionState state,
											    LocalScope localScope,
											    Collection<AbstractToken> interfaceForLocal)
{
   ArrayList<AbstractToken> ret = new ArrayList<AbstractToken>();

   //	List<?> participants = ExtensionHelper
   //	       .getParticipants(ExtensionHelper.PYDEV_COMPLETION);
   //	for (Iterator<?> iter = participants.iterator(); iter.hasNext();) {
   //	   IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
   //	   ret.addAll(participant.getCompletionsForMethodParameter(state, localScope,
   //		  interfaceForLocal));
   //	}
   return ret;
}



}	// end of class CompletionParticipantsHelper



/* end of CompletionParticipantsHelper.java */

