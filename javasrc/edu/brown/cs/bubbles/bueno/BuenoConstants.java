/********************************************************************************/
/*										*/
/*		BuenoConstants.java						*/
/*										*/
/*	BUbbles Environment New Objects creator constants			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.JPanel;

import java.awt.Point;
import java.io.File;
import java.util.EventListener;
import java.util.List;
import java.util.Map;



public interface BuenoConstants {



/********************************************************************************/
/*										*/
/*	New object types							*/
/*										*/
/********************************************************************************/

enum BuenoType {
   NEW_PACKAGE,
   NEW_MODULE,			// python module
   NEW_FILE,                    // JS file/module
   
   NEW_CLASS,
   NEW_INTERFACE,
   NEW_ENUM,
   NEW_ANNOTATION,
   NEW_TYPE,			// any of the above
   
   NEW_INNER_CLASS,
   NEW_INNER_INTERFACE,
   NEW_INNER_ENUM,
   NEW_INNER_TYPE,		// any of the above
   
   NEW_CONSTRUCTOR,
   NEW_METHOD,
   NEW_GETTER,
   NEW_SETTER,
   NEW_GETTER_SETTER,
   
   NEW_FIELD,
   
   NEW_MARQUIS_COMMENT,
   NEW_BLOCK_COMMENT,
   NEW_JAVADOC_COMMENT,
}



/********************************************************************************/
/*										*/
/*	Property keys for object creation					*/
/*										*/
/********************************************************************************/

enum BuenoKey {
   KEY_NAME,			// String
   KEY_PARAMETERS,		// String, String [], List<String>
   KEY_RETURNS, 		// String (used for return type and field type)
   KEY_MODIFIERS,		// Integer
   KEY_ADD_COMMENT,		// Boolean (include preceding comment)
   KEY_ADD_JAVADOC,		// Boolean (include preceding javadoc)
   KEY_FIELD,			// String (field name for getter/setter)
   KEY_COMMENT, 		// String (text for comment)
   KEY_CONTENTS,		// String (method contents)
   KEY_PACKAGE, 		// String (package for class)
   KEY_IMPORTS, 		// String, String [], List<String>
   KEY_TYPE,			// String { class, interface, enum }
   KEY_INITIAL_VALUE,		// String (field initial value)
   KEY_INDENT,			// Integer or String
   KEY_INITIAL_INDENT,		// Integer or String
   KEY_SIGNATURE,		// String (full method/class signature)
   KEY_THROWS,			// String, String [], List<String>
   KEY_EXTENDS, 		// String
   KEY_IMPLEMENTS,		// String, String [], List<String>
   KEY_CLASS_NAME,              // String
   KEY_FIELD_NAME,              // String
   KEY_FIELD_TYPE,              // String;
   KEY_AUTHOR,			// String
   KEY_FILE,			// String
   KEY_FILETAIL,		// String
   KEY_PROJECT, 		// String
   KEY_RETURN_STMT,		// String
   KEY_ATTRIBUTES,		// String
   KEY_CREATE_INIT,		// Boolean (create __init__ module)
   KEY_REFORMAT,                // Boolean (reformat after insertion)
}


int MODIFIER_OVERRIDES = 0x1000000;



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

enum BuenoMethod {
   METHOD_ECLIPSE,		// use eclipse creation
   METHOD_SIMPLE,		// our own simple creator
   METHOD_TEMPLATE,		// using our templates
   METHOD_USER, 		// learn the users properties and use them
}



/********************************************************************************/
/*										*/
/*	Properties								*/
/*										*/
/********************************************************************************/

String BUENO_TEMPLATE_TAB_SIZE = "Bueno.template.tabsize";
String BUENO_CREATION_METHOD = "Bueno.creation.method";
String BUENO_PROPERTY_HEAD = "Bueno.property.";



/********************************************************************************/
/*										*/
/*	Interface to handle insertions						*/
/*										*/
/********************************************************************************/

interface BuenoInserter extends EventListener {
   boolean insertText(BuenoLocation loc,String text,boolean reformat);
}


interface BuenoBubbleCreator {
   void createBubble(String proj,String name,BudaBubbleArea bba,Point p);
}



/********************************************************************************/
/*										*/
/*	Interface for default creators						*/
/*										*/
/********************************************************************************/


interface BuenoMethodCreatorInstance {

   boolean showMethodDialogBubble(BudaBubble source,Point location,
        			     BuenoProperties known,
        			     BuenoLocation insert,
        			     String label,
        			     BuenoBubbleCreator newer);


}	// end of interface BuenoMethodCreatorInstance

interface BuenoClassCreatorInstance {

   boolean useSeparateTypeButtons();

   boolean showClassDialogBubble(BudaBubble source,Point location,BuenoType typ,
	 BuenoProperties known,BuenoLocation insert,String lbl,
	 BuenoBubbleCreator newer);

}	// end of interface BuenoClassCreatorInstance

interface BuenoPackageCreatorInstance {

   boolean showPackageDialogBubble(BudaBubble source,Point location,BuenoType typ,
         BuenoProperties known,BuenoLocation insert,String lbl,
         BuenoBubbleCreator newer);

}	// end of interface BuenoPackageCreatorInstance


interface BuenoClassMethodFinder {
    List<BumpLocation> findClassMethods(String name);
}	// end of interface BuenoClassMethodFinder



/********************************************************************************/
/*										*/
/*	Interface for project creators						*/
/*										*/
/********************************************************************************/

String PROJ_PROP_NAME = "ProjectName";
String PROJ_PROP_BASE = "ProjectBase";
String PROJ_PROP_DIRECTORY = "ProjectDirectory";
String PROJ_PROP_SOURCE = "ProjectSource";
String PROJ_PROP_LIBS = "ProjectLibraries";
String PROJ_PROP_LINKS = "ProjectLinks";
String PROJ_PROP_ANDROID = "ProjectAndroid";
String PROJ_PROP_ANDROID_PKG = "ProjectAndroidPackage";
String PROJ_PROP_JUNIT = "ProjectJunit";

interface BuenoProjectProps {

   Object get(String k);
   String getString(String k);
   File getFile(String k);
   Object put(String k,Object v);
   Object remove(String k);
   List<File> getLibraries();
   List<File> getSources();
   Map<String,File> getLinks();

}	// end of interface BuenoProjectProps


interface BuenoProjectMaker {

   String getLabel();
   boolean checkStatus(BuenoProjectProps props);
   JPanel createPanel(BuenoProjectCreationControl ctrl,BuenoProjectProps props);
   void resetPanel(BuenoProjectProps props);
   boolean setupProject(BuenoProjectCreationControl ctrl,BuenoProjectProps props);

}	// end of inner interface BuenoProjectMaker

interface BuenoProjectCreationControl {

   void checkStatus();
   BuenoProjectProps getProperties();

   boolean generateClassPathFile();
   boolean generateProjectFile();
   boolean generateSettingsFile();
   boolean generateOtherFiles();
   String getPackageName(File f);

}	// end of innter interface BuenoProjectCreationControl



/********************************************************************************/
/*										*/
/*	Handle signature validation						*/
/*										*/
/********************************************************************************/

interface BuenoValidatorCallback {

   void validationDone(BuenoValidator val,boolean pass);

}	// end of inner interface BuenoValidatorCallback


}	// end of interface BuenoConstants



/* end of BuenoConstants.java */
