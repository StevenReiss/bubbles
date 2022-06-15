/********************************************************************************/
/*										*/
/*		BdocDocMethod.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles javadoc field detail		*/
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


package edu.brown.cs.bubbles.bdoc;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;


class BdocDocMethod extends BdocDocItem implements BdocConstants
{


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocDocMethod(URL u) throws IOException
{
   super(u);

   loadUrl(u);
}



/********************************************************************************/
/*                                                                              */
/*      JSoup extraction methods                                                */
/*                                                                              */
/********************************************************************************/

@Override void extractItem(Element e0)
{
   if (e0 == null) return;
   scanSubitems(e0);
   scanSignature(e0,".member-signature");
   scanBody(e0);
}



}	// end of class BdocDocMethod




/* end of BdocDocMethod.java */



