/********************************************************************************/
/*										*/
/*		BussFactory.java						*/
/*										*/
/*	BUbble Stack Strategies bubble stack factory class			*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buss;

import edu.brown.cs.bubbles.buda.BudaRoot;

import java.util.Collection;


/**
 *	This class provides the entries to create stack bubbles
 **/

public class BussFactory implements BussConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BussFactory	the_factory = null;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Return the singleton instance of the stack bubble factory.
 **/

public static synchronized BussFactory getFactory()
{
   if (the_factory == null) the_factory = new BussFactory();

   return the_factory;
}



private BussFactory()				{ }




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Initialize stack bubbles. This is called automatically at system initialization.
 **/

public static void setup()
{
   BudaRoot.addBubbleConfigurator("BUSS",new BussConfigurator());
}


/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

/**
 *	Create a bubble stack for a list of entities.  The content width passed in
 *	is the desired width of the content area of the bubble stack.
 **/

public BussBubble createBubbleStack(Collection<BussEntry> ents,int contentwidth)
{
   return new BussBubble(ents,contentwidth);
}



}	// end of class BussFactory




/* end of BussFactory.java */

