/********************************************************************************/
/*										*/
/*		NobaseName.java 						*/
/*										*/
/*	Information about a name in Javascript (for nobbles)			*/
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



package edu.brown.cs.bubbles.nobase;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

class NobaseName implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseProject	for_project;
private NobaseFile	for_file;
private ASTNode 	def_node;
private NameType	def_type;
private String		def_name;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseName(NobaseProject p,NobaseFile ifd,ASTNode n,String nm,NameType typ)
{
   for_project = p;
   for_file = ifd;
   def_node = n;
   def_name = nm;
   def_type = typ;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

NobaseProject getProject()		{ return for_project; }

NobaseFile getFileData()		{ return for_file; }

ASTNode getAstNode()			{ return def_node; }

NameType getNameType()			{ return def_type; }

String getName()			{ return def_name; }


}	// end of class NobaseName




/* end of NobaseName.java */

