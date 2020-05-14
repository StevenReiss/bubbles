/********************************************************************************/
/*										*/
/*		PybaseDebugVariable.java					*/
/*										*/
/*	Handle debugging variable/value for Bubbles from Python 		*/
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



public class PybaseDebugVariable implements PybaseDebugConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String var_name;
private String var_type;
private String var_value;
private PybaseDebugTarget debug_target;
private boolean is_modified;
private String var_locator;

//Only create one instance of an empty array to be returned
private static final PybaseDebugVariable[] EMPTY_IVARIABLE_ARRAY = new PybaseDebugVariable[0];




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseDebugVariable(PybaseDebugTarget target,String name,String type,String value,String locator)
{
   var_value = value;
   var_name = name;
   var_type = type;
   debug_target = target;
   var_locator = locator;
   is_modified = false;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getLocation()			{ return var_locator + "\t" + var_name; }
public String getDetailText()			{ return getValueString(); }


public String getValueString()
{
   if (var_value == null) return "";
   if ("StringType".equals(var_type) || "UnicodeType".equals(var_type)) {
      // quote the strings
      return "\"" + var_value + "\"";
    }

   return var_value;
}


public String getName() 			{ return var_name; }
public PybaseDebugTarget getDebugTarget()	{ return debug_target; }

public boolean supportsValueModification()	{ return var_locator != null; }
public boolean hasValueChanged()		{ return is_modified; }
public void setModified(boolean mod)		{ is_modified = mod; }

public void setValue(String exp)
{
   PybaseDebugCommand.ChangeVariable changeVariableCommand = getChangeVariableCommand(debug_target, exp);
   debug_target.postCommand(changeVariableCommand);
   var_value = exp;
   // debug_target.fireEvent(new DebugEvent(this, DebugEvent.CONTENT|DebugEvent.CHANGE));
}


public PybaseDebugVariable[] getVariables()	{ return EMPTY_IVARIABLE_ARRAY; }

public boolean hasVariables()			{ return false; }


public PybaseDebugCommand.ChangeVariable getChangeVariableCommand(PybaseDebugTarget dbg, String expression)
{
   return new PybaseDebugCommand.ChangeVariable(dbg, var_locator, expression);
}




}	// end of class PybaseDebugVariable




/* end of PybaseDebugVariable.java */
