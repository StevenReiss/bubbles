/********************************************************************************/
/*										*/
/*		BaleIndenterLsp.java						*/
/*										*/
/*	Indenter that uses back end (LSPBASE) to compute indents		*/
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



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;


class BaleIndenterLsp extends BaleIndenter implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int indent_space;
private int tab_size;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleIndenterLsp(BaleDocument bd)
{
   super(bd);
   loadProperties();
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override int getUnindentSize()
{
   return indent_space;
}


@Override public int getTabSize()
{
   return tab_size;
}



@Override int getSplitIndentationDelta(int offset)
{
   return getIndent(offset,false);
}



@Override int getDesiredIndentation(int offset)
{
   return getIndent(offset,true);
}


private int getIndent(int offset,boolean split)
{
   int eoff = bale_document.mapOffsetToEclipse(offset);

   BumpClient bc = BumpClient.getBump();
   Element rslt = bc.computeIndent(bale_document.getProjectName(),
	 bale_document.getFile(),
	 bale_document.getEditCounter(),
	 eoff,split);

   return IvyXml.getAttrInt(rslt,"TARGET");
}



/********************************************************************************/
/*										*/
/*	Property methods							*/
/*										*/
/********************************************************************************/

private void loadProperties()
{
   indent_space = BALE_PROPERTIES.getInt("Bale.python.indent",4);
   tab_size = BALE_PROPERTIES.getInt("Bale.python.tabsize",8);
}




}	// end of class BaleIndenterLsp




/* end of BaleIndenterLsp.java */

