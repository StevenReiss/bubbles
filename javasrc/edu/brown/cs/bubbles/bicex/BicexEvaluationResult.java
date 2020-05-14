/********************************************************************************/
/*										*/
/*		BicexEvaluationResult.java					*/
/*										*/
/*	Hold the results of a BICEX evaluation					*/
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



package edu.brown.cs.bubbles.bicex;


import edu.brown.cs.bubbles.bicex.BicexConstants.BicexResult;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.ivy.xml.IvyXml;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;

class BicexEvaluationResult implements BicexConstants, BicexResult
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BicexExecution		for_execution;
private ExitType		exit_type;
private BicexEvaluationContext	root_context;
private BicexBaseValue		exit_value;
private Map<String,BicexBaseValue> previous_values;
private String			exit_message;
private String			thread_id;
private Map<String,BicexValue>	static_values;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexEvaluationResult(BicexExecution be)
{
   for_execution = be;
   exit_type = ExitType.PENDING;
   exit_value = null;
   exit_message = null;
   root_context = null;
   previous_values = null;
   static_values = null;
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void update(Element xml)
{
   if (xml == null) return;

   Element rxml = IvyXml.getChild(xml,"RUNNER");

   Map<String,BicexBaseValue> knownvalues = new HashMap<>();

   exit_value = null;

   thread_id = IvyXml.getAttrString(rxml,"THREAD");
   Element ret = IvyXml.getChild(rxml,"RETURN");
   exit_type = IvyXml.getAttrEnum(ret,"REASON",ExitType.NONE);
   exit_message = IvyXml.getTextElement(ret,"MESSAGE");
   Element val = IvyXml.getChild(ret,"VALUE");
   if (val != null) exit_value = BicexValue.createBaseValue(val,knownvalues,previous_values);

   Element cctx = IvyXml.getChild(rxml,"CONTEXT");
   // might want to update the context here rather than creating it from scratch
   root_context = new BicexEvaluationContext(null,cctx,knownvalues,previous_values);
   if (exit_value != null) {
      if (exit_type == ExitType.RETURN)
	 root_context.addValue("*RETURN*",exit_value);
      else if (exit_type == ExitType.EXCEPTION)
	 root_context.addValue("*THROWN*",exit_value);
    }

   static_values = new HashMap<>();
   Element sxml = IvyXml.getChild(xml,"STATICS");
   for (Element sval : IvyXml.children(sxml,"STATIC")) {
      String nm = IvyXml.getAttrString(sval,"NAME");
      BicexValue cv = BicexRefValue.createRefValue(sval,knownvalues,previous_values);
      static_values.put(nm,cv);
    }

   if (exit_type == ExitType.ERROR) {
      String stk = IvyXml.getTextElement(ret,"STACK");
      if (stk != null) {
	 String msg = exit_message;
	 if (msg == null || msg.length() == 0) msg = stk;
	 else msg = msg + " :: " + stk;
	 BoardLog.logX("BICEX","SEEDE Problem: " + msg);
       }
    }

   if (exit_type == ExitType.COMPILER_ERROR && previous_values != null)
      previous_values.putAll(knownvalues);
   else
      previous_values = knownvalues;
}




void reset()
{
   exit_type = ExitType.PENDING;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BicexExecution getExecution()			{ return for_execution; }

ExitType getExitType()				{ return exit_type; }

String getExitMessage() 			{ return exit_message; }

BicexEvaluationContext getRootContext() 	{ return root_context; }

String getThreadForContext(BicexEvaluationContext ctx)
{
   return thread_id;
}





}	// end of class BicexEvaluationResult




/* end of BicexEvaluationResult.java */

