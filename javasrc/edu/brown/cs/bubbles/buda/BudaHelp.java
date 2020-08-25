/********************************************************************************/
/*										*/
/*		BudaHelp.java							*/
/*										*/
/*	BUblles Display Area help mechanism					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*	Copyright 2009 Brown University -- Izaak Baker			      */
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




/**
 *	This class provides a help bubble.
 **/

public class BudaHelp extends BudaHover implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Component		for_component;
private BudaHelpClient		help_client;
private HelpArea		help_area;
private JScrollPane		scroll_area;
private boolean 		mouse_inside;
private Element 		help_xml;
private File			help_file;
private long			help_time;
private boolean 		force_help;

private static final String	HELP_WIDTH = "Buda.help.width";
private static final String	HELP_HEIGHT = "Buda.help.height";
private static final String	HELP_FONT_PROP = "Buda.help.font";
private static final Font	HELP_FONT = BoardFont.getFont(Font.SERIF,Font.PLAIN,12);
private static final String	HELP_COLOR_PROP = "Buda.help.color";
private static final String	HELP_BORDER_PROP = "Buda.help.border.color";
private static final String	HELP_RESOURCE = "helpdoc.xml";


private static final Map<String,String> HTML_COLOR_MAP;

static {
   HTML_COLOR_MAP = new HashMap<>();
   HTML_COLOR_MAP.put("TEXT","Buda.help.text.color");
   HTML_COLOR_MAP.put("LINK","Buda.help.link.color");
   HTML_COLOR_MAP.put("BACK","Buda.help.back.color");
   HTML_COLOR_MAP.put("ERROR","Buda.help.error.color");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaHelp(Component c,BudaHelpClient helper)
{
   super(c);

   for_component = c;
   help_client = helper;

   setHoverTime(2000);
   help_area = null;
   scroll_area = null;
   mouse_inside = false;
   force_help = false;

   String fn = BoardSetup.getSetup().getLibraryPath(HELP_RESOURCE);
   if (fn == null) {
      help_file = null;
      help_xml = null;
      help_time = 0;
    }
   else {
      help_file = new File(fn);
      help_xml = IvyXml.loadXmlFromFile(help_file);
      help_time = help_file.lastModified();
    }
}



/********************************************************************************/
/*										*/
/*	Hover callback methods							*/
/*										*/
/********************************************************************************/


@Override void simulateHover(MouseEvent e)
{
   force_help = true;
   super.simulateHover(e);
   force_help = false;
   mouse_inside = true;
}



@Override public void handleHover(MouseEvent e)
{
   if (!force_help && !BudaRoot.showHelpTips()) return;

   String txt = getHelpText(e);
   if (txt == null) return;

   if (help_area == null) {
      Mouser mm = new Mouser();
      help_area = new HelpArea(txt);
      help_area.addMouseListener(mm);
      help_area.addKeyListener(mm);
      scroll_area = new JScrollPane(help_area);
      scroll_area.addMouseListener(mm);
      scroll_area.addKeyListener(mm);
   }
   else {
      help_area.setText(txt);
   }

   int dx = BUDA_PROPERTIES.getInt("Buda.help.delta.x");
   int dy = BUDA_PROPERTIES.getInt("Buda.help.delta.y");

   BudaRoot root = BudaRoot.findBudaRoot(for_component);
   if (root == null) return;
   Container rootpanel = root.getLayeredPane();
   Point pt = root.convertPoint((Component) e.getSource(),e.getPoint(),rootpanel);

   pt.x -= dx;
   pt.y -= dy;

   rootpanel.remove(scroll_area);
   rootpanel.add(scroll_area);
   int w = BUDA_PROPERTIES.getInt(HELP_WIDTH,300);
   int h = BUDA_PROPERTIES.getInt(HELP_HEIGHT,200);
   Dimension d = new Dimension(w,h);
   scroll_area.setPreferredSize(d);
   scroll_area.setSize(d);
   scroll_area.setLocation(pt.x,pt.y);
   scroll_area.setVisible(true);
   scroll_area.validate();
   help_area.setCaretPosition(0);
   mouse_inside = false;
}




@Override public void endHover(MouseEvent e)
{
   if (scroll_area == null) return;
   if (e != null && mouse_inside) return;
   if (e != null) {
      Point p0 = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),scroll_area);
      if (p0 == null) return;
      // System.err.println("POINT " + p0 + " " + scroll_area.getWidth() + " " + scroll_area.getHeight());
      if (p0.x >= 0 && p0.x < scroll_area.getWidth() && p0.y >= 0 && p0.y < scroll_area.getHeight()) return;
    }
   scroll_area.setVisible(false);
}



private String getHelpText(MouseEvent e)
{
   String txt = "";

   String lbl = help_client.getHelpLabel(e);
   if (lbl != null) {
      if (help_file != null && help_file.exists() && help_file.lastModified() > help_time) {
	 help_time = help_file.lastModified();
	 help_xml = IvyXml.loadXmlFromFile(help_file);
       }
      else if (help_xml == null) {
	 help_xml = IvyXml.loadXmlFromStream(BoardProperties.getLibraryFile(HELP_RESOURCE));
       }

      //Style the window
      for (Element xml : IvyXml.children(help_xml,"STYLE")) {
	 txt += IvyXml.getTextElement(xml,"TEXT");
       }

      for (Element xml : IvyXml.children(help_xml,"HELP")) {
	 String key = IvyXml.getAttrString(xml,"KEY");
	 if (key.equals(lbl)) {
	    txt += IvyXml.getTextElement(xml,"TEXT");
	    if (txt != null && txt.length() > 0) {
	       break;
	    }
	  }
       }
    }

   if (txt == null || txt.length() == 0) {
      txt = help_client.getHelpText(e);
    }

   if (txt != null && txt.length() > 0) {
      txt = txt.trim();
      if (!txt.startsWith("<html>")) txt = "<html>" + txt;
    }

   Map<String,String> subs = new HashMap<>();
   for (Map.Entry<String,String> ent : HTML_COLOR_MAP.entrySet()) {
      String key = ent.getKey();
      String cv = BoardColors.toHtmlColor(ent.getValue());
      subs.put(key,cv);
    }
   txt = IvyFile.expandText(txt,subs);
   
   return txt;
}




/********************************************************************************/
/*										*/
/*	Editor component for help						*/
/*										*/
/********************************************************************************/

private class HelpArea extends JEditorPane {

   private static final long serialVersionUID = 1;

   HelpArea(String cnts) {
      super("text/html",cnts);
      initialize();
      setText(cnts);
    }

   private void initialize() {
      setEditable(false);
   
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setFont(BUDA_PROPERTIES.getFont(HELP_FONT_PROP,HELP_FONT));
      addHyperlinkListener(new HyperListener());
      BoardColors.setColors(this,HELP_COLOR_PROP);
      Color bc = BoardColors.getColor(HELP_BORDER_PROP);
      setBorder(new LineBorder(bc));
    }

}	// end of inner class HelpArea




/********************************************************************************/
/*										*/
/*	Hyper link listener							*/
/*										*/
/********************************************************************************/

private class HyperListener implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	 URL u = e.getURL();
	 if (u == null) {
	    endHover(null);
	    String d = e.getDescription();
	    int idx = d.indexOf(":");
	    if (idx < 0) return;
	    String proto = d.substring(0,idx);
	    HyperlinkListener hl = BudaRoot.getListenerForProtocol(proto);
	    if (hl != null) {
	       hl.hyperlinkUpdate(e);
	    }
	   return;
	 }

	 try {
	    Desktop.getDesktop().browse(u.toURI());
	 }
	 catch (IOException ex) { }
	 catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class HyperListener


private class Mouser extends MouseAdapter implements KeyListener {

   @Override public void mouseEntered(MouseEvent e) {
      // System.err.println("MOUSE ENTERED " + (e.getSource() == scroll_area) + " " + (e.getSource() == help_area) + " " + e);
      mouse_inside = true;
   }

   @Override public void mouseExited(MouseEvent e) {
      // Point p0 = e.getPoint();
      Point p1 = scroll_area.getMousePosition();
      // System.err.println("POINT " + p1 + " " + p0 + " " + scroll_area.getWidth() + " " + scroll_area.getHeight());
      if (p1 != null) return;
      mouse_inside = false;
      // System.err.println("MOUSE EXITED");
      scroll_area.setVisible(false);
   }

   @Override public void mouseClicked(MouseEvent e) { }
   @Override public void mousePressed(MouseEvent e) { }
   @Override public void mouseReleased(MouseEvent e) { }

   @Override public void keyPressed(KeyEvent e) 		{ scroll_area.setVisible(false); }
   @Override public void keyTyped(KeyEvent e)			{ scroll_area.setVisible(false); }
   @Override public void keyReleased(KeyEvent e)		{ }

}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Main program to generate a HTML To-Do page from the help file		*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   Parser2 parser = new Parser2(BoardSetup.getSetup().getLibraryPath(HELP_RESOURCE),"helptext.html");
   parser.parse();
}




/********************************************************************************/
/*										*/
/*	Parser for help information						*/
/*										*/
/********************************************************************************/

private static class Parser2 {

   private String input_filename;
   private String output_filename;
   private List<String> when_block;
   private List<String> demo_block;
   private Map<String, String> demo_map;
   private Map<String, List<String>> when_map;

   //Constructor
   public Parser2(String f, String o) {
      input_filename = f;
      output_filename = o;
      when_block = new ArrayList<String>();
      demo_block = new ArrayList<String>();
      demo_map = new HashMap<String,String>();
      when_map = new HashMap<String,List<String>>();
    }

   public void parse() {
      sort();
      build();
      output();
    }

   //Determines whether each help element is a node (when) element or leaf (demo) element
   private void sort() {
      Element file_as_element = IvyXml.loadXmlFromFile(input_filename);
      List<Element> demo_elements = new ArrayList<Element>();
      List<Element> when_elements = new ArrayList<Element>();
   
      for(Element help_element : IvyXml.children(file_as_element, "HELP")) {
         String key = IvyXml.getAttrString(help_element,"KEY");
         if (key.startsWith("spec_")) {
            demo_elements.add(help_element);
          }
         else {
            when_elements.add(help_element);
          }
       }
   
      for(Element when_element : when_elements) {
         makeWhenMap(when_element);
       }
   
      for(Element demo_element : demo_elements) {
         makeDemoMap(demo_element);
       }
    }

   //Creates a mapping from when elements to their constitutent demo elements
   private void makeWhenMap(Element when_element) {
      //Retrieve necessary information
      String key = IvyXml.getAttrString(when_element, "KEY");
      String when = IvyXml.getAttrString(when_element, "WHEN");
      String body = IvyXml.getTextElement(when_element, "TEXT");

      //Construct the key and regex
      String map_key = key + "|" + when;
      Pattern p = Pattern.compile(".*<a href='gotodemo:(.*);backto:.*'>(.*)</a>.*");

      //Locate demos that happen during this when, and map to them
      for(String line : body.split("\n")) {
	 Matcher m = p.matcher(line);
	 if(m.matches()) {
	    if(!when_map.containsKey(map_key)) {
	       when_map.put(map_key, new ArrayList<String>());
	     }
	    when_map.get(map_key).add(m.group(1) + "|" + m.group(2));
	  }
       }
    }

   //Creates a mapping from demo elements to their body of text
   private void makeDemoMap(Element demo_element) {
      String key = IvyXml.getAttrString(demo_element, "KEY");
      String body = IvyXml.getTextElement(demo_element, "TEXT");
      demo_map.put(key, body);
    }

   private void build() {
      when_block.add("<div class='whenblock'>");
      demo_block.add("<div class='demoblock'>");

      for(String key : when_map.keySet()) {
	 //Retrieve necessary information
	 int indexOfBar = key.indexOf("|");
	 String id = key.substring(0, indexOfBar);
	 String long_id = key.substring(indexOfBar + 1);

	 //Put title for when section
	 when_block.add("<div class='whenitem'>");
	 when_block.add("<div class='whenitemtitle'>");
	 when_block.add("<a href='#" + id + "'>" + long_id + "</a>");
	 when_block.add("</div>");
	 when_block.add("<ul>");

	 //Put title for demo section
	 demo_block.add("<div class='whendemoblock'>");
	 demo_block.add("<a id='" + id + "'></a>");
	 demo_block.add("<div class='wdb-title'>");
	 demo_block.add("<h2>" + long_id + "</h2>");
	 demo_block.add("</div>");

	 //Put elements in the list under this block
	 for(String demo : when_map.get(key)) {
	    indexOfBar = demo.indexOf("|");
	    String name = demo.substring(0, indexOfBar);
	    String long_name = demo.substring(indexOfBar + 1);
	    when_block.add("<li> To <a href='#" + name + "'>" + long_name + "</a></li>");

	    demo_block.add("<div class='demoitem'>");
	    if(demo_map.containsKey(name)) {
	       demo_block.addAll(parseIndividualDemo(demo_map.get(name), long_name, name));
	     }
	    else {
	       //ERROR with names
	     }
	    demo_block.add("</div>");
	  }

	 when_block.add("</ul></div>");
	 demo_block.add("</div>");
       }

      when_block.add("</div>");
      demo_block.add("</div>");
    }

   private List<String> parseIndividualDemo(String demo, String long_name, String name) {
      //Remove buttons
      Pattern p = Pattern.compile("\\[.*\\]");
      Matcher m = p.matcher(demo);
      demo = m.replaceAll("");

      //Replace bold tag with di-emph span
      p = Pattern.compile("\\<b\\>");
      m = p.matcher(demo);
      demo = m.replaceAll("<span class='di-emph'>");

      //Replace closing bold tag
      p = Pattern.compile("\\</b\\>");
      m = p.matcher(demo);
      demo = m.replaceAll("</span>");

      //Construct list to return and return it
      List<String> return_value = new ArrayList<String>();
      return_value.add("<a id='" + name + "'></a>");
      return_value.add("<h3>&raquo; To " + long_name + "</h3>");
      return_value.add("<p class='explanation'>");
      return_value.add(demo);
      return_value.add("</p>");
      return_value.add("<p class='demolinks'>");
      return_value.add("<form>");
      return_value.add("<span class='tbutton' onclick='demo(\"" + name.substring(name.indexOf("_") + 1) + "\");'>Tell me</span>");
      return_value.add("<span class='tbutton' onclick='demo(\"" + name.substring(name.indexOf("_") + 1) + "_silent\");'>Show me</span>");
      return_value.add("</form>");
      return_value.add("</p>");

      return return_value;
    }

   private void output() {
      PrintWriter pw = null;
      try {
	 pw = new PrintWriter(new FileWriter(output_filename));

	 pw.println("<html><head>");
	 pw.println("<title>The CodeBubbles Help Page</title>");
	 pw.println("<script type='text/javascript'>");
	 pw.println("function demo(x) {");
	 pw.println("var xmlhttp = new XMLHttpRequest();");
	 pw.println("xmlhttp.open('GET', 'http://localhost:19888/' + x, false);");
	 pw.println("xmlhttp.send(null); }");
	 pw.println("</script>");
	 pw.println("<link href='http://fonts.googleapis.com/css?family=Gudea' rel='stylesheet' type='text/css'>");
	 pw.println("<link rel='stylesheet' type='text/css' href='style.css'>");
	 pw.println("<style>");
	 pw.println(".tbutton { ");
	 pw.println("  background-color: lightgray;");
	 pw.println("  border: black;");
	 pw.println("  border-style: outset;");
	 pw.println("  padding: 3px;");
	 pw.println("}");
	 pw.println("</style>");
	 pw.println("</head>");
	 pw.println("<body>");
	 pw.println("<!-- Note that this page is generated automatically -->");
	 pw.println("<div class='page'>");
	 pw.println("<div class='title'>");
	 pw.println("<h1>The <span id='codebubbles'>CodeBubbles</span> Help Page</h1>");
	 pw.println("</div>");

	 for(String line : when_block) {
	    pw.println(line);
	  }

	 for(String line : demo_block) {
	    pw.println(line);
	  }

	 pw.println("</div></body></html>");
	 pw.close();
       }
      catch (IOException e) {}
    }

}	// end of inner class Parser2












}	// end of class BudaHelp



/* end of BudaHelp.java */

