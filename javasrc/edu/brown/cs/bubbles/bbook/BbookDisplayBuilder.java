/********************************************************************************/
/*										*/
/*		BbookDisplayBuilder.java					*/
/*										*/
/*	Programmers log book display generator					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bbook;

import edu.brown.cs.bubbles.bnote.BnoteFactory;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;



class BbookDisplayBuilder implements BbookConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String			for_project;
private BnoteTask		for_task;
private String			for_user;
private String			for_class;
private String			for_method;

private Orderings		order_by;

private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yy @ HH:mm");

private static Map<String,String> mime_info;

static {
   mime_info = new HashMap<>();

   mime_info.put("bmp","image");
   mime_info.put("gif","image");
   mime_info.put("ief","image");
   mime_info.put("jpe","image");
   mime_info.put("jpeg","image");
   mime_info.put("jpg","image");
   mime_info.put("jpg","image");
   mime_info.put("jfif","image");
   mime_info.put("svg","image");
   mime_info.put("tif","image");
   mime_info.put("tiff","image");
   mime_info.put("ras","image");
   mime_info.put("cmx","image");
   mime_info.put("ico","image");
   mime_info.put("pnm","image");
   mime_info.put("pbm","image");
   mime_info.put("pgm","image");
   mime_info.put("ppm","image");
   mime_info.put("rgb","image");
   mime_info.put("xbm","image");
   mime_info.put("xpm","image");
   mime_info.put("xwd","image");
   mime_info.put("png","image");

   mime_info.put("mp2","video");
   mime_info.put("mpa","video");
   mime_info.put("mpe","video");
   mime_info.put("mpeg","video");
   mime_info.put("mpg","video");
   mime_info.put("mpv2","video");
   mime_info.put("mp4","video");
   mime_info.put("qt","video");
   mime_info.put("lsf","video");
   mime_info.put("lsx","video");
   mime_info.put("asf","video");
   mime_info.put("asr","video");
   mime_info.put("asx","video");
   mime_info.put("avi","video");
   mime_info.put("movie","video");

   mime_info.put("au","audio");
   mime_info.put("snd","audio");
   mime_info.put("mid","audio");
   mime_info.put("rmi","audio");
   mime_info.put("mp3","audio");
   mime_info.put("aif","audio");
   mime_info.put("aifc","audio");
   mime_info.put("aiff","audio");
   mime_info.put("m3u","audio");
   mime_info.put("ra","audio");
   mime_info.put("ram","audio");
   mime_info.put("wav","audio");

   mime_info.put("css","text");
   mime_info.put("323","text");
   mime_info.put("htm","text");
   mime_info.put("html","text");
   mime_info.put("stm","text");
   mime_info.put("uls","text");
   mime_info.put("bas","text");
   mime_info.put("c","text");
   mime_info.put("h","text");
   mime_info.put("txt","text");
   mime_info.put("rtx","text");
   mime_info.put("sct","text");
   mime_info.put("tsv","text");
   mime_info.put("htt","text");
   mime_info.put("htc","text");
   mime_info.put("etx","text");
   mime_info.put("vcf","text");
   mime_info.put("java","text");
   mime_info.put("js","text");
   mime_info.put("py","text");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookDisplayBuilder(Map<String,Object> props)
{
   for_project = (String) props.get("PROJECT");
   for_task = (BnoteTask) props.get("TASK");
   for_user = (String) props.get("USER");
   for_class = (String) props.get("CLASS");
   for_method = (String) props.get("METHOD");

   order_by = (Orderings) props.get("ORDERBY");
   if (order_by == null) order_by = Orderings.TIME;
}



/********************************************************************************/
/*										*/
/*	Generation methods							*/
/*										*/
/********************************************************************************/

String generateHtml()
{
   String bkg = BoardSetup.getSetup().getResourcePath("bbookbkg.gif");

   StringBuffer html = new StringBuffer();

   html.append("<html><body");
   if (bkg != null) html.append(" background='file://" + bkg + "'");
   html.append(" style='margin-left: 24px'>");
   html.append("<h1 align='center' style='font-family: Arial, Helvetica, sans-serif; " +
		  "font-size: 24px'>Programmer's Log Book</h1>");

   GenContext ctx = new GenContext();

   generateHtmlOverview(ctx,html);

   generateContents(ctx,html);

   html.append("</body></html>");

   return html.toString();
}



/********************************************************************************/
/*										*/
/*	Overview table								*/
/*										*/
/********************************************************************************/

private void generateHtmlOverview(GenContext ctx,StringBuffer buf)
{
   Date start = null;
   Date end = null;

   List<Date> dts = BnoteFactory.getFactory().getDatesForTask(for_project,for_task);
   if (dts != null) {
      for (Date d1 : dts) {
	 if (start == null || start.compareTo(d1) > 0) start = d1;
	 if (end == null || end.compareTo(d1) < 0) end = d1;
       }
    }

   buf.append("<table>");
   if (for_task != null) {
      buf.append("<tr><td>Task:</td><td>");
      buf.append(fixText(for_task.getName()));
      buf.append("</td></tr>");
    }
   if (for_user != null) {
      buf.append("<tr><td>User:</td><td>");
      buf.append(fixText(for_user));
      buf.append("</td></tr>");
    }
   else {
      List<String> usrs = BnoteFactory.getFactory().getUsersForTask(for_project,for_task);
      if (usrs.size() == 1) {
	 buf.append("<tr><td>Author:</td><td>");
	 buf.append(fixText(usrs.get(0)));
	 buf.append("</td></tr>");
	 ctx.setLastUser(usrs.get(0));
       }
      else if (usrs.size() > 1) {
	 buf.append("<tr><td>Authors:</td><td>");
	 for (int i = 0; i < usrs.size(); ++i) {
	    if (i > 0) buf.append(", ");
	    buf.append(fixText(usrs.get(i)));
	  }
	 buf.append("</td></tr>");
       }
    }

   if (for_class != null || for_method != null) {
      buf.append("<tr><td>Code:</td><td>");
      if (for_class == null) buf.append("*.");
      else if (for_class.endsWith(".")) buf.append(for_class + "*.");
      else buf.append(fixText(for_class));
      if (for_method == null) buf.append("*");
      else buf.append(fixText(for_method));
      buf.append("</td></tr>");
    }

   if (start != null) {
      buf.append("<tr><td>Start:</td><td>");
      buf.append(start.toString());
      buf.append("</td></tr>");
    }
   if (end != null) {
      buf.append("<tr><td>Finish:</td><td>");
      buf.append(end.toString());
      buf.append("</td></tr>");
    }

   buf.append("</table>");
   buf.append("<hr />");
}



/********************************************************************************/
/*										*/
/*	Entry generation							*/
/*										*/
/********************************************************************************/

private void generateContents(GenContext ctx,StringBuffer buf)
{
   List<BnoteEntry> ents = BnoteFactory.getFactory().getEntriesForTask(for_project,for_task);

   for (Iterator<BnoteEntry> it = ents.iterator(); it.hasNext(); ) {
      BnoteEntry ent = it.next();
      if (!isRelevant(ent)) it.remove();
    }

   switch (order_by) {
      case TIME :
	 break;
      case TASK :
	 ents = orderByTask(ents);
	 break;
      case METHOD :
	 ents = orderByMethod(ents);
	 break;
    }

   for (BnoteEntry be : ents) {
      BnoteTask bt = be.getTask();
      if (ctx.getTask() != bt) {
	 outputTaskHeader(bt,ctx,buf);
       }
      outputEntryData(be,ctx,buf);
    }
}



private boolean isRelevant(BnoteEntry ent)
{
   // project and task already accounted for

   if (for_user != null) {
      String u = ent.getUser();
      if (!for_user.equals(u)) return false;
    }

   if (for_class != null || for_method != null) {
      String enm = ent.getProperty("NAME");
      if (enm == null) return false;
      if (for_class != null) {
	 if (!enm.startsWith(for_class)) return false;
       }
      if (for_method != null) {
	 int idx1 = enm.indexOf("(");
	 int idx;
	 if (idx1 < 0) idx = enm.lastIndexOf(".");
	 else idx = enm.lastIndexOf(".",idx1);
	 String mnm = enm.substring(idx+1);
	 if (!for_method.equals(mnm)) return false;
       }
    }

   return true;
}


private List<BnoteEntry> orderByTask(List<BnoteEntry> ents)
{
   List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();

   while (ents.size() > 0) {
      BnoteTask task = null;
      for (Iterator<BnoteEntry> it = ents.iterator(); it.hasNext(); ) {
	 BnoteEntry ent = it.next();
	 if (task == null) task = ent.getTask();
	 else if (ent.getTask() != task) continue;
	 it.remove();
	 rslt.add(ent);
       }
    }

   return rslt;
}



private List<BnoteEntry> orderByMethod(List<BnoteEntry> ents)
{
   List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();

   while (ents.size() > 0) {
      String method = null;
      for (Iterator<BnoteEntry> it = ents.iterator(); it.hasNext(); ) {
	 BnoteEntry ent = it.next();
	 String mnm = ent.getProperty("NAME");
	 if (method == null) method = mnm;
	 else if (!method.equals(mnm)) continue;
	 it.remove();
	 rslt.add(ent);
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Specific output methods 						*/
/*										*/
/********************************************************************************/

private void outputTaskHeader(BnoteTask bt,GenContext ctx,StringBuffer buf)
{
   setState(ctx,ContextState.TASK,buf);

   boolean full = ctx.setTask(bt);
   buf.append("<h2>Task: " + fixText(bt.getName()) + "</h2>");
   if (full && bt.getDescription() != null) {
      buf.append("<blockquote>");
      buf.append(fixTextBlock(bt.getDescription()));
      buf.append("</blockquote>");
    }
}




private void outputEntryData(BnoteEntry ent,GenContext ctx,StringBuffer buf)
{
   long when = ent.getTime().getTime();
   long ld = ctx.getLastTime();
   if (ld > 0 && when - ld > 1000*60*30) {
      setState(ctx,ContextState.INFO,buf);
      buf.append("<h3>Date: " + DATE_FORMAT.format(ent.getTime()) + "</h3>");
    }
   ctx.setLastTime(when);

   String usr = ent.getUser();
   String lusr = ctx.getLastUser();
   if (usr != null && !usr.equals(lusr)) {
      setState(ctx,ContextState.INFO,buf);
      buf.append("<h3>User: " + usr + "</h3>");
      ctx.setLastUser(usr);
    }

   switch (ent.getType()) {
      case NONE :
      case TASK :
      case NEW_TASK :
	 return;
      case OPEN :
      case CLOSE :
      case EDIT :
      case SAVE :
	 String nm = ent.getProperty("NAME");
	 if (nm == null) return;
	 setState(ctx,ContextState.WORK,buf);
	 buf.append("<li>");
	 buf.append(ent.getType().toString());
	 buf.append(" ");
	 buf.append(fixText(nm));
	 buf.append("</li>");
	 break;
      case NOTE :
	 setState(ctx,ContextState.NOTE,buf);
	 String nt = ent.getProperty("NOTE");
	 buf.append("<blockquote>");
	 buf.append(fixTextBlock(nt));
	 buf.append("</blockquote>");
	 break;
      case COPY :
	 nm = ent.getProperty("NAME");
	 String src = ent.getProperty("SOURCE");
	 setState(ctx,ContextState.WORK,buf);
	 buf.append("<li>");
	 buf.append(ent.getType().toString());
	 buf.append(" ");
	 buf.append(" FROM ");
	 buf.append(fixText(src));
	 buf.append(" TO ");
	 buf.append(fixText(nm));
	 buf.append("</li>");
	 break;
      case ATTACHMENT :
	 String fnm = ent.getProperty("SOURCE");
	 String aid = ent.getProperty("ATTACHID");
	 String ptyp = null;
	 int idx = fnm.lastIndexOf(".");
	 if (idx > 0) {
	    String ext = fnm.substring(idx+1).toLowerCase();
	    ptyp = mime_info.get(ext);
	  }

	 if (ptyp == null) break;
	 setState(ctx,ContextState.ATTACH,buf);
	 if (ptyp.equals("text")) {
	    String txt = BnoteFactory.getFactory().getAttachmentAsString(aid);
	    buf.append("<blockquote>");
	    buf.append(fixTextBlock(txt));
	    buf.append("</blockquote>");
	  }
	 else if (ptyp.equals("audio")) {
	    File fn = BnoteFactory.getFactory().getAttachment(aid);
	    if (fn != null) {
	       buf.append("<A HREF='file://" + fn.getPath() + "'>");
	       buf.append("Play Audio");
	       buf.append("</A>");
	     }
	  }
	 else if (ptyp.equals("image")) {
	    File fn = BnoteFactory.getFactory().getAttachment(aid);
	    if (fn != null) {
	       int w = 64;
	       int h = 64;
	       try {
		  BufferedImage bi = ImageIO.read(fn);
		  int w1 = bi.getWidth();
		  int h1 = bi.getHeight();
		  double scl = 1.0;
		  if (h1 > h) scl = ((double) h) / h1;
		  h = (int) (h1 * scl);						
		  w = (int) (w1 * scl);
		}
	       catch (Exception e) { }
	       buf.append("<A HREF='file://" + fn.getPath() + "'>");
	       buf.append("<IMG src='file://" + fn.getPath() + "'");
	       buf.append("alt='inserted image' width='" + w + "' height='" + h + "' />");
	       buf.append("</A>");
	     }
	  }
	 else if (ptyp.equals("video")) {
	    File fn = BnoteFactory.getFactory().getAttachment(aid);
	    if (fn != null) {
	       buf.append("<A HREF='file://" + fn.getPath() + "'>");
	       buf.append("Play Video");
	       buf.append("</A>");
	     }
	  }
	 break;
    }
}



private void setState(GenContext ctx,ContextState st,StringBuffer buf)
{
   if (st == ctx.getState()) return;

   // handle closing current items within a task
   switch (ctx.getState()) {
      case WORK :
	 buf.append("</ul>");
	 ctx.setState(ContextState.TASK);
	 break;
      case NOTE :
	 ctx.setState(ContextState.TASK);
	 break;
      case ATTACH :
	 buf.append("<br>");
	 break;
      default:
	 break;
    }

   switch (st) {
      case NONE :
	 buf.append("<hr />");
	 break;
      case TASK :
	 break;
      case WORK :
	 buf.append("<h3>Work on Task:</h3>");
	 buf.append("<ul>");
	 break;
      case NOTE :
	 buf.append("<h3>Notes:</h3>");
	 break;
      case ATTACH :
	 buf.append("<h3>Attachments:</h3>");
	 break;
      case INFO :
	 break;
    }

   ctx.setState(st);
}




/********************************************************************************/
/*										*/
/*	String management							*/
/*										*/
/********************************************************************************/

private String fixText(String s)
{
   if (s == null) return null;

   return IvyXml.htmlSanitize(s);
}


private String fixTextBlock(String s)
{
   if (s == null) return null;

   String mdt = IvyFile.parseMarkdown(s);
   
   if (mdt != null) return mdt;
  
   return IvyXml.htmlSanitize(s);
}



/********************************************************************************/
/*										*/
/*	Output context management						*/
/*										*/
/********************************************************************************/

enum ContextState {
   NONE,
   TASK,
   WORK,
   INFO,
   ATTACH,
   NOTE
}



private static class GenContext {

   private BnoteTask	current_task;
   private Set<BnoteTask> done_tasks;
   private ContextState current_state;
   private long 	last_time;
   private String	last_user;

   GenContext() {
      current_task = null;
      done_tasks = new HashSet<BnoteTask>();
      current_state = ContextState.NONE;
      last_time = 0;
      last_user = null;
    }

   BnoteTask getTask()				{ return current_task; }
   boolean setTask(BnoteTask t) {
      current_task = t;
      return done_tasks.add(t);
    }

   ContextState getState()			{ return current_state; }
   void setState(ContextState st)		{ current_state = st; }

   long getLastTime()				{ return last_time; }
   void setLastTime(long t)			{ last_time = t; }

   String getLastUser() 			{ return last_user; }
   void setLastUser(String u)			{ last_user = u; }

}	// end of inner class GenContext




}	// end of class BbookDisplayBuilder




/* end of BbookDisplayBuilder.java */

