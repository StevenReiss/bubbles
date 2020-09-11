/********************************************************************************/
/*										*/
/*		BtedConstants.java						*/
/*										*/
/*	Bubble Environment text editor facility constant definitions		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook 			*/
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



package edu.brown.cs.bubbles.bted;

import java.awt.Font;
import java.awt.Insets;

import edu.brown.cs.bubbles.board.BoardFont;


public interface BtedConstants {


/********************************************************************************/
/*										*/
/*	File extensions 							*/
/*										*/
/********************************************************************************/

/**
 * Bash file extensions
 */
String BASH_EXTENSION	    = "Bted.extension.bash";

/**
 * C file extensions
 */
String C_EXTENSION	  = "Bted.extension.c";

/**
 * Clojure file extensions
 */
String CLOJURE_EXTENSION    = "Bted.extension.clojure";

/**
 * C++ file extensions
 */
String CPP_EXTENSION	= "Bted.extension.cpp";

/**
 * DosBatch file extensions
 */
String DOSBATCH_EXTENSION   = "Bted.extension.dosbatch";

/**
 * Groovy file extensions
 */
String GROOVY_EXTENSION     = "Bted.extension.groovy";

/**
 * Java file extensions
 */
String JAVA_EXTENSION	    = "Bted.extension.java";

/**
 * Javascript file extensions
 */
String JAVASCRIPT_EXTENSION = "Bted.extension.javascript";

/**
 * Jflex file extensions
 */
String JFLEX_EXTENSION	    = "Bted.extension.jflex";

/**
 * Lua file extensions
 */
String LUA_EXTENSION	= "Bted.extension.lua";

/**
 * Properties file extensions
 */
String PROPERTIES_EXTENSION = "Bted.extension.properties";

/**
 * Python file extensions
 */
String PYTHON_EXTENSION     = "Bted.extension.python";

/**
 * Ruby file extensions
 */
String RUBY_EXTENSION	    = "Bted.extension.ruby";

/**
 * Scala file extensions
 */
String SCALA_EXTENSION	    = "Bted.extension.scala";

/**
 * SQL file extensions
 */
String SQL_EXTENSION	= "Bted.extension.sql";

/**
 * Tal file extensions
 */
String TAL_EXTENSION	= "Bted.extension.tal";

/**
 * XHTML file extensions
 */
String XHTML_EXTENSION	    = "Bted.extension.xhtml";

/**
 * XML file extensions
 */
String XML_EXTENSION	= "Bted.extension.xml";

/**
 * XPATH file extensions
 */
String XPATH_EXTENSION	    = "Bted.extension.xpath";



/********************************************************************************/
/*										*/
/*	Text Editor Bubble Properties						*/
/*										*/
/********************************************************************************/

/**
 * Name of the button for loading a new file
 */
String NEW_FILE_BUTTON	    = "Bubbles.Create New File";

/**
 * Name of the button for loading an existing file
 */
String LOAD_FILE_BUTTON     = "Bubbles.Open File";

String REMOTE_FILE_BUTTON   = "Bubbles.Open Remote File";

/**
 * The default width of the text editor bubble
 */
String INITIAL_WIDTH	= "Bted.width";

/**
 * The default height of the
 */
String INITIAL_HEIGHT	    = "Bted.height";

/**
 * The color of the bubble in the overview panel
 */
String OVERVIEW_COLOR	    = "Bted.overview.color";

/**
 * The top color of the bubble
 */
String TOP_COLOR	    = "Bted.top.color";

/**
 * The bottom color of the bubble
 */
String BOTTOM_COLOR	 = "Bted.bottom.color";

/**
 * The color that a found item is highlighted with
 */
String HIGHLIGHT_COLOR	    = "Bted.highlight.color";

/**
 * The color that the text field is colored when a word cannot be found
 */
String NOT_FOUND_COLOR	    = "Bted.not.found.color";

/**
 * The margin of the buttons in the text editor
 */
Insets BUTTON_MARGIN	= new Insets(0,0,0,0);

/**
 * Line wrapping in the text editor
 */
String WRAPPING      = "Bted.line.wrapping";

Font EDITOR_FONT = BoardFont.getFont(Font.MONOSPACED,Font.PLAIN,12);
String EDITOR_FONT_PROP = "Bted.font";


/********************************************************************************/
/*										*/
/*	Search constants							*/
/*										*/
/********************************************************************************/

enum SearchMode {
   NEXT,
   PREVIOUS,
   FIRST
}


enum StartMode {
   NEW,
   NEW_REMOTE,
   LOCAL,
   REMOTE
}



}	// end of interface BtedConstants



/* end of BtedConstants.java */
