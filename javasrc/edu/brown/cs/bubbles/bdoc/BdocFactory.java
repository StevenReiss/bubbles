/********************************************************************************/
/*										*/
/*		BdocFactory.java						*/
/*										*/
/*	Bubbles Environment Documentation factory and setup class		*/
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


package edu.brown.cs.bubbles.bdoc;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;



/**
 *	This class is used to create documentation bubbles.  It provides the
 *	entries for others to do so and handles initialization of the documentation
 *	pacakge at startup.
 *
 *	The source for the documentation provided by bubbles is defined in the
 *	Bdoc.props resource file.  This is stored in the directory ~/.bubbles
 *	where ~ indicates the user's home directory as Java sees it.  Each entry
 *	in this file with the key 'Bdoc.javadoc.#' defines a URL of a java doc
 *	hierarchy.  Here # is any number.  All entries 0-9 will be considered.
 *	Beyond that, only consecutive entries (i.e. 10,11,12,...) will be
 *	considered.
 *
 *	This file can be edited by the user.  We really should provide an
 *	interactive interface for changing this file at some point.
 *
 **/

public final class BdocFactory implements BdocConstants, BudaConstants.DocBoxCreator
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BdocFactory	the_factory = null;
private static BdocRepository	bdoc_repository = null;



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is called automatically from startup and does the necessary
 *	package initialization.
 **/

public static synchronized void setup()
{
   if (bdoc_repository != null) return;

   bdoc_repository = new BdocRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_DOC,bdoc_repository);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,bdoc_repository);
   BudaRoot.registerDocumentationCreator(getFactory());
   BudaRoot.addBubbleConfigurator("BDOC",new BdocConfigurator());
}



/**
 *	Return the singleton factory object for documentation bubbles.
 **/

public static synchronized BdocFactory getFactory()
{
   if (the_factory == null) the_factory = new BdocFactory();
   return the_factory;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BdocFactory()			{ }




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

/**
 *	Create a documenation bubble for the given item.  The bubble is displayed
 *	as new.
 **/

@Override public BudaBubble createDocBubble(String name)
{
   try {
      BdocReference ref = bdoc_repository.findReferenceFromName(name);
      if (ref != null) {
	 BudaBubble bb = new BdocBubble(ref);
	 return bb;
       }
    }
   catch (BdocException e) { 
      BoardLog.logE("BDOC","Problem loading documentation ",e);
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Wait for ready								*/
/*										*/
/********************************************************************************/

/**
 *	Initialization causes the javadoc specified in the Bdoc.props resource
 *	file to start loading.	This routine can be called to wait until this
 *	loading has finished.
 **/

public void waitForReady()
{
   bdoc_repository.waitForReady();
}




}	// end of class BdocFactory



/* end of BdocFactory.java */
