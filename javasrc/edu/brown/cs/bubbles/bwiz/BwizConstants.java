/********************************************************************************/
/*										*/
/*		BwizConstants.java						*/
/*										*/
/*	Constants for bubble wizards package					*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 UCF -- Jared Bott				      */
/*	Copyright 2013 Brown University -- Annalia Sunderland		      */
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.bwiz;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.EventListener;
import java.util.List;



public interface BwizConstants
{


/********************************************************************************/
/*										*/
/*	Interface for accessibility						*/
/*										*/
/********************************************************************************/

interface IAccessibilityUpdatable {

   void updateAccessibility(ActionEvent e);

}	// end of inner interface IAccessibilityUpdatable



/********************************************************************************/
/*										*/
/*	Interface for signature update						*/
/*										*/
/********************************************************************************/

interface ISignatureUpdate {

    void updateSignature();

}	// end of inner interface ISignatureUpdate



/********************************************************************************/
/*										*/
/*	Interface for input string verification 				*/
/*										*/
/********************************************************************************/

interface IVerifier {

    boolean verify(String test);
    List<String> results(String test);


}	// end of inner interface IVerifier


/********************************************************************************/
/*										*/
/*	Listener interfaces							*/
/*										*/
/********************************************************************************/

interface ItemChangeListener extends EventListener {

   void itemAdded(String item);
   void itemRemoved(String item);

}	// end of inner interface ItemChangeListener




interface VerificationListener extends EventListener
{

    void verificationEvent(Object sender, boolean success);

}	// end of inner interface VerificationListener




/********************************************************************************/
/*										*/
/*	Accessibility information						*/
/*										*/
/********************************************************************************/

enum Accessibility
{
   PUBLIC("public"),
   PRIVATE("private"),
   PROTECTED("protected"),
   DEFAULT("default");

   private final String accessibility_label;

   Accessibility(String s) {
      accessibility_label = s;
    }

   public boolean equalsAccessibility(String otheraccess) {
      if (otheraccess == null) return false;
      return accessibility_label.equals(otheraccess);
    }

   @Override public String toString() {
      return accessibility_label;
    }

   public static Accessibility fromString(String text) {
      if (text != null) {
         for (Accessibility b : Accessibility.values()) {
            if (text.equalsIgnoreCase(b.accessibility_label))
               return b;
          }
       }
   
      throw new IllegalArgumentException("No constant with text " + text + " found");
    }

}	// end of enum Accessibility




/********************************************************************************/
/*										*/
/*	Create Type information 						*/
/*										*/
/********************************************************************************/

enum CreateType {
   CLASS("class"),
   ENUM("enum"),
   INTERFACE("interface"),
   METHOD("method");

   private final String type_label;

   CreateType(String s) {
      type_label = s;
    }

   public boolean equalsType(String othertype) {
      if (othertype == null) return false;
      return type_label.equals(othertype);
    }

   @Override public String toString() {
      return type_label;
    }

   public static CreateType fromString(String text) {
      if (text != null) {
	 for (CreateType b : CreateType.values()) {
	    if (text.equalsIgnoreCase(b.type_label))
	       return b;
	  }
       }

      throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}	// end of enum CreateType




/********************************************************************************/
/*										*/
/*	Font definitions							*/
/*										*/
/********************************************************************************/

Font BWIZ_FONT_SIZE_MAIN = new Font("Sans Serif", Font.BOLD,16);

Font BWIZ_FONT_BUTTON = BWIZ_FONT_SIZE_MAIN.deriveFont(1,10);
Font BWIZ_FONT_OPTION = BWIZ_FONT_SIZE_MAIN.deriveFont((float) 12);



/********************************************************************************/
/*										*/
/*	bedu String text definitions						*/
/*										*/
/********************************************************************************/

String PAST_ASSIGNMENTS_TEXT = "Past Assignments";
String CURRENT_ASSIGNMENTS_TEXT = "Current Assignments";

String CREATE_WORKSPACE_TEXT = "Create New Workspace";
String OPEN_WORKSPACE_TEXT = "Open Workspace";

String CREATE_CLASS_TEXT = "New Class";
String CREATE_PROJECT_TEXT = "Create New Project";
String OPEN_ASSIGNMENT_TEXT = "Start Assignment";
String OPEN_PROJECT_TEXT = "Open Existing Project";
String CREATE_INTERFACE_TEXT = "New Interface";
String CREATE_ENUM_TEXT = "New Enum";



/********************************************************************************/
/*										*/
/*	Overhead Panel definitions						*/
/*										*/
/********************************************************************************/

String PANEL_OVERVIEW_COLOR_PROP = "Bwiz.PanelOverviewColor";



/********************************************************************************/
/*										*/
/*	Accessibility flags							*/
/*										*/
/********************************************************************************/

int SHOW_PRIVATE = 0x1;
int SHOW_PROTECTED = 0x2;
int SHOW_ABSTRACT = 0x4;
int SHOW_FINAL = 0x8;
int SHOW_OVERRIDES = 0x10;
int SHOW_CLASS = 0x20;
int SHOW_INTERFACE = 0x40;




}	// end of interface BwizConstants



/* end of BwizConstants.java */
