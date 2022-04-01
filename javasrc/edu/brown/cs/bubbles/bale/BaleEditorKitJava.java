/********************************************************************************/
/*										*/
/*		BaleEditorKitJava.java						*/
/*										*/
/*	Bubble Annotated Language Editor editor kit for Java-specific cmds	*/
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


import javax.swing.Action;
import javax.swing.text.Keymap;




class BaleEditorKitJava implements BaleConstants, BaleConstants.BaleLanguageKit
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleJavaHinter java_hinter;

private static final Action [] local_actions = {
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKitJava()
{
   java_hinter = new BaleJavaHinter();
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


@Override public BaleHinter getHinter()
{
   return java_hinter;
}








}	// end of class BaleEditorKitJava



/* end of BaleEditorKitJava.java */
