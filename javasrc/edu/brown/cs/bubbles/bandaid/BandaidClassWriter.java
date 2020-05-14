/********************************************************************************/
/*										*/
/*		BandaidClassWriter.java 					*/
/*										*/
/*	Class writer to handle class lookup problems while tracing		*/
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



package edu.brown.cs.bubbles.bandaid;

import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.ClassReader;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.ClassWriter;


public class BandaidClassWriter extends ClassWriter
{




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidClassWriter(String nm)
{
   super(COMPUTE_MAXS|COMPUTE_FRAMES);
}



BandaidClassWriter(String nm,ClassReader r)
{
   super(r,COMPUTE_MAXS|COMPUTE_FRAMES);
}




/********************************************************************************/
/*										*/
/*	Handle common supertype finding 					*/
/*										*/
/********************************************************************************/

@Override protected String getCommonSuperClass(String type1,String type2)
{
   try {
      return super.getCommonSuperClass(type1,type2);
    }
   catch (Throwable t) { }

   // System.err.println("BANDAID: COMMON CLASS " + for_name + " " + type1 + " " + type2);

   return "java/lang/Object";
}




}	// end of BandaidClassWriter




/* end of BandaidClassWriter.java */
