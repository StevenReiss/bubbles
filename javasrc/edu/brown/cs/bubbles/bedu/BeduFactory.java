/********************************************************************************/
/*										*/
/*		BeduFactory.java						*/
/*										*/
/*	Bubbles for Education -- setup for courses				*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven Reiss 			*/
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

package edu.brown.cs.bubbles.bedu;


import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaErrorBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BeduFactory implements BeduConstants, BoardConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BeduCourseData			course_data;
private boolean 			is_setup;

private static BeduFactory		the_factory = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static synchronized BeduFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BeduFactory();
    }
   return the_factory;
}


private BeduFactory()
{
   course_data = null;
   is_setup = false;

   File cdir = getCourseDirectory();

   if (cdir != null) setupProperties(cdir);
   File cfil = new File(cdir,"course.xml");
   Element cxml = IvyXml.loadXmlFromFile(cfil);
   if (cxml != null) course_data = new BeduCourseData(cxml);

   setupWorkspace();
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   getFactory();
}



public static void initialize(BudaRoot br)
{
   the_factory.setupCourseButtons();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public List<Assignment> getAllAssignments()
{
   if (course_data == null) return new ArrayList<Assignment>();
   return course_data.getAllAssignments();
}



public List<Assignment> getCreatableAssignments()
{
   List<Assignment> rslt = new ArrayList<Assignment>();
   for (Assignment a : getAllAssignments()) {
      if (a.isPast() || a.isCurrent()) {
	 if (!a.isInUserWorkspace() && a.getName() != null) rslt.add(a);
       }
    }
   return rslt;
}



public boolean useCourseChat()
{
   if (course_data == null) return false;
   return course_data.doChat();
}



/********************************************************************************/
/*										*/
/*	Button setup and handling						*/
/*										*/
/********************************************************************************/

private void setupCourseButtons()
{
   if (is_setup) return;
   is_setup = true;

   if (course_data != null) {
      String cnm = course_data.getName();
      for (Assignment asg : getAllAssignments()) {
	 if (asg.canSubmit()) {
	    String nm = "Bubble." + cnm + ".Submit " + asg.getName();
	    BudaRoot.registerMenuButton(nm,new Submitter(asg));
	  }
	 if (asg.isPast() || asg.isCurrent()) {
	    String doc = asg.getDocumentationUrl();
	    if (doc != null) {
	       String nm = "Bubble." + cnm + ".Assignment " + asg.getName();
	       BudaRoot.registerMenuButton(nm,new Documenter(asg));
	     }
	  }
       }
      String hlp = course_data.getHelpUrl();
      if (hlp != null) {
	 String nm = "Bubble.Help.Help for " + course_data.getName();
	 BudaRoot.registerMenuButton(nm,new Helper());
       }
    }
}



private void showSurvey(Assignment a)
{
   String survey = a.getSurveyUrl();
   if (survey == null) survey = course_data.getSurveyUrl();
   if (survey == null) return;

   String pnm = "Bedu.survey." + course_data.getName() + "." + a.getName() + ".done";
   BoardProperties props = BoardProperties.getProperties("Bedu");
   if (props.getBoolean(pnm)) return;
   props.setProperty(pnm,true);
   try {
      props.save();
    }
   catch (IOException e) { }

   showPage(survey);
}



private void showPage(String url)
{
   if (url == null) return;

   try {
      URI u = new URI(url);
      Desktop.getDesktop().browse(u);
    }
   catch (Throwable e) { }
}




private class Submitter implements Runnable, BudaConstants.ButtonListener {

   private Assignment for_assignment;
   private BudaBubbleArea bubble_area;
   private Point at_point;

   Submitter(Assignment a) {
      for_assignment = a;
    }

   @Override public void run() {
      String rslt = for_assignment.handin();
      BudaErrorBubble bbl = new BudaErrorBubble(rslt,Color.BLACK);
      bubble_area.addBubble(bbl,null,at_point,BudaConstants.PLACEMENT_MOVETO);
      showSurvey(for_assignment);
    }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      bubble_area = bba;
      at_point = pt;
      BoardThreadPool.start(this);
    }

}	// end of inner class Submitter



private class Documenter implements BudaConstants.ButtonListener {

   private Assignment for_assignment;

   Documenter(Assignment a) {
      for_assignment = a;
    }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      showPage(for_assignment.getDocumentationUrl());
    }
}



private class Helper implements BudaConstants.ButtonListener {

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      showPage(course_data.getHelpUrl());
    }

}	// end of innter class Helper



/********************************************************************************/
/*										*/
/*	Property methods							*/
/*										*/
/********************************************************************************/

private void setupProperties(File dir)
{
   String [] fils = dir.list(new PropsFilter());
   if (fils == null) return;

   for (String nm : fils) {
      int idx0 = nm.indexOf(".");
      String what = nm.substring(0,idx0);
      boolean force = nm.endsWith(".force.props");
      BoardProperties bp = BoardProperties.getProperties(what);
      Properties np = new Properties();
      File f1 = new File(dir,nm);
      boolean upd = false;
      try {
	 FileInputStream ins = new FileInputStream(f1);
	 np.loadFromXML(ins);
	 for (String xnm : np.stringPropertyNames()) {
	    Object v = bp.getProperty(xnm);
	    if (v == null || force) {
	       bp.setProperty(xnm,np.getProperty(xnm));
	       upd = true;
	     }
	  }
	 if (upd) bp.save();
       }
      catch (IOException e) {
	 BoardLog.logE("BEDU","Can't load property file",e);
       }
    }
}



private static class PropsFilter implements FilenameFilter {

@Override public boolean accept(File dir,String nm) {
   return nm.endsWith(".props");
 }
}




/********************************************************************************/
/*										*/
/*	Workspace setup methods 						*/
/*										*/
/********************************************************************************/

private void setupWorkspace()
{
   BoardProperties sysprop = BoardProperties.getProperties("System");
   if (sysprop.getProperty("edu.brown.cs.bubbles.workspace") != null) return;
   if (course_data == null) return;
   try {
      course_data.setupWorkspace();
    }
   catch (IOException e) {
      BoardLog.logE("BEDU","Problem setting up initial workspace",e);
    }
}




/********************************************************************************/
/*										*/
/*	Find initial course directory						*/
/*										*/
/********************************************************************************/

static File getCourseDirectory()
{
   File f = BoardSetup.getSetup().getCourseDirectory();
   if (f != null) return f;

   String cnm = BoardSetup.getSetup().getCourseName();
   if (cnm == null) return null;

   f = getInstallDirectory();

   File f1 = f.getParentFile();
   File f2 = new File(f1,cnm);
   if (!f2.exists() || !f2.isDirectory()) f2 = new File(f,cnm);
   if (!f2.exists() || !f2.isDirectory()) return null;

   return f2;
}


static File getInstallDirectory()
{
   File f = null;

   String suds = System.getProperty("edu.brown.cs.bubbles.suds");
   if (suds == null) suds = System.getenv("BUBBLES_SUDS");
   if (suds != null) {
      f = new File(suds);
      if (!f.exists() || !f.isDirectory()) f = null;
   }

   if (f == null) {
      URL url = BeduFactory.class.getClassLoader().getResource(BOARD_RESOURCE_PLUGIN_ECLIPSE);
      if (url != null) {
	 String file = url.toString();
	 if (file.startsWith("jar:file:/")) file = file.substring(9);
	 if (file.length() >= 3 && file.charAt(0) == '/' &&
		  Character.isLetter(file.charAt(1)) && file.charAt(2) == ':' &&
		  File.separatorChar == '\\')
	    file = file.substring(1);
	 int idx = file.lastIndexOf('!');
	 if (idx >= 0) file = file.substring(0,idx);
	 if (File.separatorChar != '/') file = file.replace('/',File.separatorChar);
	 file = file.replace("%20"," ");
	 File f1 = new File(file);
	 f = f1.getParentFile();
      }
   }

   if (f == null) return null;

   try {
      f = f.getCanonicalFile();
   }
   catch (IOException e) { }

   return f;
}




}	// end of class BeduFactory




/* end of BeduFactory.java */
