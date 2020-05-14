/********************************************************************************/
/*										*/
/*		CompiledToken.java						*/
/*										*/
/*	Python Bubbles Base token in precompiled code representation		*/
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
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;


/**
 * @author Fabio Zadrozny
 */
public class CompiledToken extends AbstractToken {


public CompiledToken(String rep,String doc,String args,String parentPackage,TokenType type)
{
   super(rep,doc,args,parentPackage,type);
}



} // end of class CompiledToken


/* end of CompiledToken.java */
