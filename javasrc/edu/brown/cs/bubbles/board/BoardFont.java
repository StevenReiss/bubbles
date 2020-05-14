/********************************************************************************/
/*										*/
/*		BoardFont.java							*/
/*										*/
/*	Bubbles default font handling						*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.board;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;



/**
 * Provide ways for changing the default font families globally
 **/

public class BoardFont implements BoardConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BoardProperties		font_properties = BoardProperties.getProperties("Board");

private static Map<String,String>	family_map = new HashMap<String,String>();




/********************************************************************************/
/*										*/
/*	Static access methods						       */
/*										*/
/********************************************************************************/

/**
 *	Return the default font for the given family
 **/

public static Font getFont(String fam,int style,int sz)
{
   return getMappedFont("Board.font." + fam,fam,style,sz);
}



private static Font getMappedFont(String prop,String dfam,int style,int sz)
{
   String fam = font_properties.getString(prop);
   if (fam == null || fam.length() == 0) fam = dfam;
   String xfam = family_map.get(fam);
   if (xfam != null) fam = xfam;

   Font ft = new Font(fam,style,sz);
   if (!ft.getFamily().equals(fam)) {
      // font doesn't exist
      family_map.put(fam,ft.getFamily());
      ft = new Font(dfam,style,sz);
    }

   return ft;
}



}	// end of class BoardFont



/* end of BoardFont.java */
