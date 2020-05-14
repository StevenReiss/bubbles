/********************************************************************************/
/*										*/
/*		BhelpFactory.java						*/
/*										*/
/*	Factory for Bubbles help demonstrations 				*/
/*										*/
/********************************************************************************/
/*	Copyright 2012 Brown University -- Steven P. Reiss		      */
/*	Copyright 2013 Brown University -- Izaak Baker			      */
/*********************************************************************************
 *  Copyright 2012, Brown University, Providence, RI.				 *
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



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import java.awt.Component;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;



public class BhelpFactory implements BhelpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BhelpDemo>	demo_map;
private BudaRoot		buda_root;
private BhelpWebServer		web_server;

private static BhelpFactory	the_factory = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BhelpFactory getFactory()
{
   return the_factory;
}



private BhelpFactory(BudaRoot br)
{
   buda_root = br;
   demo_map = new HashMap<String,BhelpDemo>();
   InputStream ins = BoardProperties.getLibraryFile(HELP_RESOURCE);
   if (ins != null) {
      Element xml = IvyXml.loadXmlFromStream(ins);
      for (Element ex : IvyXml.children(xml,"DEMO")) {  // for each element in demo file
	 BhelpDemo bd = new BhelpDemo(ex);		// create new demo w/ instructions
	 demo_map.put(bd.getName(),bd); 		// save that name for bhelp
       }
    }
   BudaRoot.addHyperlinkListener("showme",new Hyperlinker("showme"));
   BudaRoot.addHyperlinkListener("gotodemo",new Hyperlinker("gotodemo"));

   try {
      web_server = new BhelpWebServer();
      web_server.process();
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*										*/
/*	Setup methods (called by BEMA)						*/
/*										*/
/********************************************************************************/

public static void setup()
{
   // work is done by the static initializer
}



public static void initialize(BudaRoot br)
{
   the_factory = new BhelpFactory(br);

   br.registerKeyAction(new TestAction(),"Test Help Sequence",
	 KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,
	       InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK));
}




/********************************************************************************/
/*										*/
/*	Help demonstration start						*/
/*										*/
/********************************************************************************/

void startDemonstration(Component comp,String name)
{
   if (comp == null) {
      comp = buda_root.getCurrentBubbleArea();
    }

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(comp);
   if (bba == null) {
      BudaRoot br = BudaRoot.findBudaRoot(comp);
      if (br == null) return;
      bba = br.getCurrentBubbleArea();
      if (bba == null) return;
    }

   String[] sides = name.split("_");
   if (sides.length > 1 && sides[1].equals("silent")) {
      BhelpDemo demo = demo_map.get(sides[0]);
      if (demo == null) return;
      demo.executeDemo(bba,true);
    }
   else {
      BhelpDemo demo = demo_map.get(name);
      if (demo == null) return;
      demo.executeDemo(bba,false);
    }
}



/********************************************************************************/
/*										*/
/*	Demonstraction routines 						*/
/*										*/
/********************************************************************************/

//This will display a specific demo's "show or tell" screen

void displayDemoScreen(Component comp,String name,String backto)
{
   if(comp == null) {
      comp = buda_root.getCurrentBubbleArea();
    }

   if(comp instanceof JEditorPane) {
      String text = "";

      //Bring stuff back to being visible
      JEditorPane jcomp = (JEditorPane) comp;
      jcomp.getParent().getParent().setVisible(true);

      //Retrieve the proper demo
      InputStream ins = BoardProperties.getLibraryFile(HELP_DOCUMENT);
      if (ins != null) {
	 Element xml = IvyXml.loadXmlFromStream(ins);
	 text += "<html>";
	 if(!backto.equals("NULL")) {
	    text += getTextOfHelp(xml, "instructions");
	  }
	 text += getTextOfHelp(xml, name);
	 if(!backto.equals("NULL")) {
	    text += "[<a class='tbutton' id='back' href='gotodemo:" + backto + ";backto:NULL'> BACK </a>]</p>";
	  }
	 text += "</html>";
       }

      jcomp.setText(text);
    }
}



String getTextOfHelp(Element xml, String name)
{
   for(Element ex : IvyXml.children(xml, "HELP")) {
      String key = IvyXml.getAttrString(ex, "KEY");
      if(name.equals(key)) {
	 return IvyXml.getTextElement(ex, "TEXT");
       }
    }
   return "<p><span class='error'>No available demo for this topic</span></p>";
}



/********************************************************************************/
/*										*/
/*	Hyperlink actions							*/
/*										*/
/********************************************************************************/

private class Hyperlinker implements HyperlinkListener {

   private String link_type;

   Hyperlinker(String t) {
      link_type = t;
    }

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      String description = e.getDescription();
      switch (link_type) {
	 case "showme" :
	    int index = description.indexOf(":");
	    if (index < 0) return;
	    String what = description.substring(index+1);
	    startDemonstration((Component) e.getSource(),what);
	    break;
	 case "gotodemo" :
	    description = description.substring(9);	// warning hardcoded
	    String [] broken = description.split(";backto:");
	    if (broken.length != 2) return;
	    displayDemoScreen((Component) e.getSource(),broken[0],broken[1]);
	    break;
       }
   }

}	// end of inner class Hyperlinker



/********************************************************************************/
/*										*/
/*	Test actions								*/
/*										*/
/********************************************************************************/

private static class TestAction extends AbstractAction {

   @Override public void actionPerformed(ActionEvent e) {
      BhelpFactory bf = BhelpFactory.getFactory();
      Component c = (Component) e.getSource();
      bf.startDemonstration(c,"testaction");
    }

}	// end of inner class TestAction


}	// end of class BhelpFactory




/* end of BhelpFactory.java */

