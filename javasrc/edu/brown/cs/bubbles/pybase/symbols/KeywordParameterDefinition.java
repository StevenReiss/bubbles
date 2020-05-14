/********************************************************************************/
/*										*/
/*		KeywordParameterDefinition					*/
/*										*/
/*	Python Bubbles Base definition for parameters				*/
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

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Call;


/**
 * A definition where we found things as a keyword parameter in a call.
 *
 * It contains the access to the keyword paramater as its ast and an additional call attribute (and attribute
 * if the call was inside an attribute)
 *
 * @author fabioz
 */
public class KeywordParameterDefinition extends Definition {

public Call param_call;

public KeywordParameterDefinition(int line,int col,String value,SimpleNode ast,
	 LocalScope scope,AbstractModule module,Call call)
{
   super(line,col,value,ast,scope,module,false);
   param_call = call;
}

} // end of class KeywordParameterDefinition


/* end of KeywordParameterDefinition.java */
