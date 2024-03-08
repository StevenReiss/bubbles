/********************************************************************************/
/*										*/
/*		BtedFactory.java						*/
/*										*/
/*	Bubble Environment text editor facility factory 			*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubblePosition;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.burp.BurpHistory;
import edu.brown.cs.ivy.swing.SwingColorSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.PlainDocument;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.SyntaxStyle;
import jsyntaxpane.SyntaxStyles;
import jsyntaxpane.TokenType;
import jsyntaxpane.util.Configuration;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;


public class BtedFactory implements BtedConstants, BumpConstants,
	 BudaConstants.ButtonListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private HashMap<String, Document>      active_documents;
private HashMap<Document, Integer>     document_count;

private static HashMap<String, String> file_extensions = new HashMap<String,String>();

private static BtedFactory	     the_factory;
private static BoardProperties	 bted_props = BoardProperties.getProperties("Bted");

private static File		last_directory = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Return the singleton instance of the breakpoint factory.
 **/
public static BtedFactory getFactory()
{
   return the_factory;
}


private BtedFactory()
{
   active_documents = new HashMap<String, Document>();
   document_count = new HashMap<Document, Integer>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Setup the module.  This should be called from the initialization module
 *	(i.e. listed in the initialization setup).  It will register any buttons and
 *	bubble configurators that are needed.
 **/

public static void setup()
{
   BumpClient.getBump();
   the_factory = new BtedFactory();
   BudaRoot.addBubbleConfigurator("BTED", new BtedConfigurator());
   BudaRoot.registerMenuButton(NEW_FILE_BUTTON, the_factory);
   BudaRoot.registerMenuButton(LOAD_FILE_BUTTON, the_factory);
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BudaRoot.registerMenuButton(REMOTE_FILE_BUTTON,the_factory);
    }
   BtedBubble.registerDefaultKeys();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

static File getLastDirectory()
{
   return last_directory;
}


static void setLastDirectory(File d)
{
   last_directory = d;
}



/********************************************************************************/
/*										*/
/*	Document methods							*/
/*										*/
/********************************************************************************/

/**
 * Extracts the file extensions from the string that contains the file extensions
 *
 * @param String containing file extensions seperated by spaces, commas, or semicolons
 * @return a vector containing each file extension as a string
 */
private Vector<String> getExtensions(String str)
{
   str = str.toLowerCase();
   Vector<String> rslt = new Vector<String>();
   boolean inExt = false;
   int start = 0;
   for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch == '.') {
	 inExt = true;
	 start = i;
      }
      else if ((ch == ' ' || ch == ',' || ch == ';' || ch == '\n' || ch == '\r' || ch == Character.MIN_VALUE)
	       && inExt) {
	 inExt = false;
	 rslt.add(str.substring(start, i));
      }
      else if (i == str.length() - 1 && inExt) {
	 rslt.add(str.substring(start));
      }
   }
   return rslt;
}



/**
* Resets the file extension map.  Useful if the user changes the file extension
* properties.
*/
private void getExtensionsFromProperties()
{
   file_extensions.clear();
   for (String str : getExtensions(bted_props.getString(BASH_EXTENSION))) {
      file_extensions.put(str, "text/bash");
    }
   for (String str : getExtensions(bted_props.getString(C_EXTENSION))) {
      file_extensions.put(str, "text/c");
    }
   for (String str : getExtensions(bted_props.getString(CLOJURE_EXTENSION))) {
      file_extensions.put(str, "text/clojure");
    }
   for (String str : getExtensions(bted_props.getString(CPP_EXTENSION))) {
      file_extensions.put(str, "text/cpp");
    }
   for (String str : getExtensions(bted_props.getString(DOSBATCH_EXTENSION))) {
      file_extensions.put(str, "text/dosbatch");
    }
   for (String str : getExtensions(bted_props.getString(GROOVY_EXTENSION))) {
      file_extensions.put(str, "text/groovy");
    }
   for (String str : getExtensions(bted_props.getString(JAVA_EXTENSION))) {
      file_extensions.put(str, "text/java");
    }
   for (String str : getExtensions(bted_props.getString(JAVASCRIPT_EXTENSION))) {
      file_extensions.put(str, "text/javascript");
    }
   for (String str : getExtensions(bted_props.getString(JFLEX_EXTENSION))) {
      file_extensions.put(str, "text/jflex");
    }
   for (String str : getExtensions(bted_props.getString(LUA_EXTENSION))) {
      file_extensions.put(str, "text/lua");
    }
   for (String str : getExtensions(bted_props.getString(PROPERTIES_EXTENSION))) {
      file_extensions.put(str, "text/properties");
    }
   for (String str : getExtensions(bted_props.getString(PYTHON_EXTENSION))) {
      file_extensions.put(str, "text/python");
    }
   for (String str : getExtensions(bted_props.getString(RUBY_EXTENSION))) {
      file_extensions.put(str, "text/ruby");
    }
   for (String str : getExtensions(bted_props.getString(SCALA_EXTENSION))) {
      file_extensions.put(str, "text/scala");
    }
   for (String str : getExtensions(bted_props.getString(SQL_EXTENSION))) {
      file_extensions.put(str, "text/sql");
    }
   for (String str : getExtensions(bted_props.getString(TAL_EXTENSION))) {
      file_extensions.put(str, "text/tal");
    }
   for (String str : getExtensions(bted_props.getString(XHTML_EXTENSION))) {
      file_extensions.put(str, "text/xhtml");
    }
   for (String str : getExtensions(bted_props.getString(XML_EXTENSION))) {
      file_extensions.put(str, "text/xml");
    }
   for (String str : getExtensions(bted_props.getString(XPATH_EXTENSION))) {
      file_extensions.put(str, "text/xpath");
    }
}



/**
 * Loads a file into the editor.  Keeps track of which files are open.	If a
 * file is opened more than once, the duplicate editor is pointed at the same
 * Document as the original.
 *
 * @param file - the file to load into the editor
 * @param editor - the editor to load the file into
 */

void loadFileIntoEditor(File file,JEditorPane editor,UndoableEditListener listener)
{
   this.getExtensionsFromProperties();

   String path = file.getPath();
   int index = path.lastIndexOf(".");
   Document od = editor.getDocument();
   Object tabp = od.getProperty(PlainDocument.tabSizeAttribute);
   String contenttype = "text/plain";
   if (index >= 0) {
      String extension = path.substring(index).toLowerCase();
      if (file_extensions.containsKey(extension)) {
	 contenttype = file_extensions.get(extension);
      }
   }

   if (active_documents.containsKey(file.getPath())) {
      editor.setDocument(active_documents.get(file.getPath()));
      this.increaseCount(file);
   }
   else {
      try {
	 FileInputStream ins = new FileInputStream(file);
         try {
            editor.setContentType(contenttype);
          }
         catch (Throwable e) {
            BoardLog.logE("BTED","Bad content type " + contenttype + " " + file,e);
            editor.setContentType("text/plain");
          }
	 editor.read(ins,null);
	 // String cnts = IvyFile.loadFile(file);
	 // editor.setText(cnts);
	 active_documents.put(file.getPath(), editor.getDocument());
	 document_count.put(editor.getDocument(), Integer.valueOf(1));
	 ins.close();
       }
      catch (Throwable e) {
	 BoardLog.logE("BTED","Problem loading file for " + contenttype + " " + path,e);
      }
   }

   editor.getDocument().putProperty(PlainDocument.tabSizeAttribute,tabp);

   BurpHistory.getHistory().addEditor(editor);
   editor.getDocument().addUndoableEditListener(listener);

   if (BoardColors.isInverted()) {
      SyntaxStyle dfltsty = SyntaxStyles.getInstance().getStyle(TokenType.DEFAULT);
      Color topc = BoardColors.getColor(TOP_COLOR);
      BoardColors.setColors(editor,topc);
      Color c = editor.getForeground();
      dfltsty.setColorString(BoardColors.toColorString(c));
      invertColors(editor);
    }
}




/**
 * Reopens the old bubble. Necessary to keep track of the number of identical
 * Documents that are open.  It is called when a new file is opened in an old
 * bubble.
 *
 * @param path
 * @param oldBubble
 */
void reopenBubble(String path,BtedBubble oldBubble)
{
   BudaBubble bb = new BtedBubble(path,oldBubble.getMode());
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(oldBubble);
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaConstraint bc = new BudaConstraint(BudaBubblePosition.MOVABLE,oldBubble.getX(),
	    oldBubble.getY());
   if (bba != null) bba.removeBubble(oldBubble);
   if (br != null) br.add(bb, bc);
}



/**
 * Increases the count to the file.
 *
 * @param file
 */
void increaseCount(File file)
{
   Document doc = active_documents.get(file.getPath());
   int i = document_count.remove(doc);
   i = i + 1;
   document_count.put(doc, i);
}



/**
 * Decreases the count to the file and removes the corresponding document
 * from the map if the count becomes 0.
 *
 * @param file
 */
void decreaseCount(File file)
{
   Document doc = active_documents.get(file.getPath());

   if (doc == null || !document_count.containsKey(doc)) return;

   int i = document_count.remove(doc);
   i = i - 1;
   document_count.put(doc, i);
   if (i == 0) {
      Document removed = active_documents.remove(file.getPath());
      document_count.remove(removed);
   }
}



/**
 * Determines if the file is open
 * @param file
 * @return true if the file is open
 */
boolean isFileOpen(File file)
{
   return active_documents.containsKey(file.getPath());
}



/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{

   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;
   ScriptEngineManager sem = new ScriptEngineManager();
   ScriptEngine e = sem.getEngineByExtension("js");
   ScriptEngine e2 = sem.getEngineByMimeType("text/javascript");
   ScriptEngine e1 = sem.getEngineByName("nashorn");
   System.err.println("FOUND " + e + " " + e1 + " " + e2);

   try {
      if (id.equals(NEW_FILE_BUTTON)) {
	 bb = new BtedBubble(null,StartMode.NEW);
       }
      else if (id.equals(LOAD_FILE_BUTTON)) {
	 JFileChooser chooser = new JFileChooser(last_directory);
	 int returnval = chooser.showOpenDialog(bba);
	 if (returnval == JFileChooser.APPROVE_OPTION) {
	    File f = chooser.getSelectedFile();
	    setLastDirectory(f);
	    if (f.exists() && f.canRead()) {
	       bb = new BtedBubble(f.getPath(),StartMode.LOCAL);
	     }
	  }
       }
      else if (id.equals(REMOTE_FILE_BUTTON)) {
	 JFileChooser chooser = new JFileChooser(last_directory,
	       BoardFileSystemView.getFileSystemView());
	 int returnval = chooser.showOpenDialog(bba);
	 if (returnval == JFileChooser.APPROVE_OPTION) {
	    File f = chooser.getSelectedFile();
	    setLastDirectory(f);
	    if (f.exists() && f.canRead()) {
	       bb = new BtedBubble(f.getPath(),StartMode.LOCAL);
	     }
	  }
       }
    }
   catch (Throwable t) {
      String msg = "Problem creating text editor bubbles: " + t;
      BoardLog.logE("BTED",msg,t);
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb, bc);
      bb.grabFocus();
   }

}


/********************************************************************************/
/*										*/
/*	Handle inverted colors							*/
/*										*/
/********************************************************************************/

private void invertColors(JEditorPane editor)
{
   Pattern matchall = Pattern.compile("Style.*");

   EditorKit edkit = editor.getEditorKit();
   if (edkit != null && edkit instanceof DefaultSyntaxKit) {
      DefaultSyntaxKit dsk = (DefaultSyntaxKit) edkit;
      Configuration config = dsk.getConfig();
      Set<Configuration.StringKeyMatcher> ckeys = config.getKeys(matchall);
      String done = config.get("BTED_INVERTED");
      if (done != null) return;
      for (Configuration.StringKeyMatcher ckey : ckeys) {
	 String [] props = config.getPropertyList(ckey.key);
	 Color c1 = SwingColorSet.getColorByName(props[0]);
	 Color c2 = BoardColors.invertColor(c1);
	 String s2 = BoardColors.toColorString(c2);
	 String rslt = s2 + ", " + props[1];
	 config.put(ckey.key,rslt);
       }
      config.put("BTED_INVERTED","DONE");
      editor.setEditorKit(new DefaultEditorKit());
      editor.setEditorKit(edkit);
    }
}




}	// end of class BtedFactory




/* end of BtedFactory.java */
