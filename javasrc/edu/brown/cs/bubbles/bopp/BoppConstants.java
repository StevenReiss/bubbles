/********************************************************************************/
/*										*/
/*		BoppConstants.java						*/
/*										*/
/*	Conststand for the option panel 					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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


package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Insets;
import java.util.Collection;
import java.util.regex.Pattern;


/**
 * Interface defining constants for Bopp
 *
 *
 * @author ahills
 *
 */

public interface BoppConstants {



/********************************************************************************/
/*										*/
/*	Enumerations for different preferent types				*/
/*										*/
/********************************************************************************/

/**
 * Enumeration for different types of preferences that can be set
 */
enum OptionType {
   NONE,
   COLOR,
   INTEGER,
   DOUBLE,
   STRING,
   BOOLEAN,
   FONT,
   DIVIDER,
   COMBO,
   RADIO,
   DIMENSION
}




/********************************************************************************/
/*										*/
/*	Strings for different settings						*/
/*										*/
/********************************************************************************/

/**
 * Name of preferences xml
 */
String	  PREFERENCES_XML_FILENAME = "preferences.xml";



/********************************************************************************/
/*										*/
/*	Constants for option button on the main panel				*/
/*										*/
/********************************************************************************/

Insets	  BOPP_BUTTON_INSETS = new Insets(0,8,0,8);



/********************************************************************************/
/*										*/
/*	Basic option								*/
/*										*/
/********************************************************************************/

interface BoppOptionNew {

   String getOptionName();
   OptionType getOptionType();
   Collection<String> getOptionTabs();

   void addButton(SwingGridPanel pnl);

   boolean search(Pattern [] pat);

}	// end of inner interface BoppOptionNew



}	// end of interface BoppConstants



/* end of BoppConstants.java */
