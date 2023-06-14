/********************************************************************************/
/*										*/
/*		BaleEditorKitLsp.java						*/
/*										*/
/*	Bubble Annotated Language Editor editor kit for LSP-based backends	*/
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


package edu.brown.cs.bubbles.bale;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.text.Keymap;


class BaleEditorKitLsp implements BaleConstants, BaleConstants.BaleLanguageKit
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/


private static final Action [] local_actions = {
};

private static final Map<String,String> content_map;
private static final Set<String> post_content;

static {
   content_map = new HashMap<>();
   content_map.put("{","}");
   content_map.put("(",")");
   content_map.put("[","]");
   content_map.put("'","'");
   content_map.put("\"","\"");
   post_content = new HashSet<>();
   for (String val : content_map.values()) {
      for (int i = 0; i < val.length(); ++i) {
	 String c = val.substring(i,i+1);
	 post_content.add(c);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKitLsp()
{
}



/********************************************************************************/
/*										*/
/*	Action Methods								*/
/*										*/
/********************************************************************************/

@Override public Action [] getActions()
{
   return local_actions;
}


@Override public Keymap getKeymap(Keymap base)
{
   return base;
}



@Override public String getPostContent(String content)
{
   if (!BALE_PROPERTIES.getBoolean(BALE_AUTO_INSERT_CLOSE)) return null;
   return content_map.get(content);
}

@Override public boolean checkContent(String content)
{
   if (!BALE_PROPERTIES.getBoolean(BALE_AUTO_INSERT_CLOSE)) return false;
   return post_content.contains(content);
}






}	// end of class BaleEditorKitLsp



/* end of BaleEditorKitLsp.java */

