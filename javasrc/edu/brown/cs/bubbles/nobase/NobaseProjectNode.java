/********************************************************************************/
/*										*/
/*		NobaseProjectNode.java						*/
/*										*/
/*	Node.JS javascript project definition					*/
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

import java.io.File;


class NobaseProjectNode extends NobaseProject
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseProjectNode(NobaseMain pm,String name,File base)
{
   super(pm,name,base);
}



/********************************************************************************/
/*										*/
/*	Setup defaults for a node.js project					*/
/*										*/
/********************************************************************************/

@Override void setupDefaults()
{
   super.setupDefaults();
}



@Override protected File findInterpreter(String name)
{
   if (name == null) {
      File f = findInterpreter("node");
      if (f != null) return f;
    }

   return super.findInterpreter(name);
}


}	// end of class NobaseProjectNode




/* end of NobaseProjectNode.java */

