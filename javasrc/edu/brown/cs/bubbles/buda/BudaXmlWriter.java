/********************************************************************************/
/*										*/
/*		BudaXmlWriter.java						*/
/*										*/
/*	BUblles Display Area xml writer 					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;


/**
 *	This class is a minor extension of IvyXmlWriter that supports writing
 *	out graphical elements such as rectangles and colors.  It is passed to
 *	bubble components that implement BubbleOutputer.
 *
 **/


public class BudaXmlWriter extends IvyXmlWriter implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaXmlWriter(String file) throws IOException
{
   super(file);
}



BudaXmlWriter(File file) throws IOException
{
   super(file);
}


BudaXmlWriter()
{ }



/********************************************************************************/
/*										*/
/*	Helper methods for common structures					*/
/*										*/
/********************************************************************************/

/**
 *	Output an arbitrary element with the given name and string value.
 **/

public void element(String id,String s)
{
   textElement(id,s);
}



/**
 *	Output an element with a rectangular value
 **/

public void element(String id,Rectangle2D r)
{
   if (r == null) return;

   begin(id);
   field("X",r.getX());
   field("Y",r.getY());
   field("WIDTH",r.getWidth());
   field("HEIGHT",r.getHeight());
   end(id);
}



/**
 *	Output an element with a point value
 **/

public void element(String id,Point2D p)
{
   if (p == null) return;

   begin(id);
   field("X",p.getX());
   field("Y",p.getY());
   end(id);
}



/**
 *	Output an element with a color value
 **/

public void element(String id,Color c)
{
   if (c == null) return;

   textElement(id,"0x" + Integer.toHexString(c.getRGB()));
}




}	// end of class BudaXmlWriter




/* end of BudaXmlWriter.java */
