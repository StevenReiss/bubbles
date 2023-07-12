/********************************************************************************/
/*										*/
/*		BumpBreakSet.java						*/
/*										*/
/*	BUblles Mint Partnership breakpoint set maintainer			*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



class BumpBreakSet implements BumpConstants, BumpConstants.BumpBreakModel {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BumpBreakImpl>	current_breakpoints;
private Map<BumpBreakpointHandler,File> break_handlers;

private BumpClient		bump_client;
private BumpBreakMode		break_mode;
private BumpExceptionMode	exception_mode;

private File                    dummy_file = new File("");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpBreakSet(BumpClient bc)
{
   current_breakpoints = new HashMap<>();
   break_handlers = new ConcurrentHashMap<>();
   break_mode = BumpBreakMode.SUSPEND_THREAD;
   exception_mode = BumpExceptionMode.ALL;
   bump_client = bc;
}



/********************************************************************************/
/*										*/
/*	Breakpoint creation/deletion/editing methods				*/
/*										*/
/********************************************************************************/

/**
 *	Add a breakpoint at the give line of the given file.  The class is optional.
 **/

@Override public boolean addLineBreakpoint(String proj,File file,String cls,int line,
					   BumpBreakMode mode)
{
   if (mode == BumpBreakMode.DEFAULT) mode = break_mode;

   return bump_client.addLineBreakpoint(proj,file,cls,line,mode.isSuspendVm(),mode.isTrace());
}



/**
 *	Add an exception breakpoint for the given class which should be a subclass
 *	of Throwable.  The emode parameters indicates when the breakpoint should be
 *	triggered (i.e. when the caught or uncaught exceptions).
 **/

@Override public void addExceptionBreakpoint(String proj,String cls,BumpExceptionMode emode,
						BumpBreakMode mode,boolean subclass)
{
   if (mode == BumpBreakMode.DEFAULT) mode = break_mode;
   if (emode == BumpExceptionMode.DEFAULT) emode = exception_mode;

   bump_client.addExceptionBreakpoint(proj,cls,emode.isCaught(),emode.isUncaught(),
					 mode.isSuspendVm(),subclass);
}


/**
 *	Clear all breakpoints at the given line of the given file.  The cls parameter is
 *	optional.
 **/

@Override public void clearLineBreakpoint(String proj,File file,String cls,int line)
{
   bump_client.clearLineBreakpoint(proj,file,cls,line);
}



/**
 *	Toggle a line breakpoint for the given file.  If a line breakpoint already
 *	exists at this line it is removed; otherwise a new breakpoint is created with
 *	the current default settings.
 **/

@Override public void toggleBreakpoint(String proj,File file,int line,BumpBreakMode mode)
{
   if (line == 0 || file == null) return;

   String cls = null;

   boolean fnd = false;
   for (BumpBreakpoint bb : bump_client.getBreakpoints(file)) {
      int lno = bb.getIntProperty("LINE");
      if (lno == line) {
	  cls = bb.getProperty("CLASS");
	  fnd = true;
       }
    }

   if (fnd) clearLineBreakpoint(proj,file,cls,line);
   else addLineBreakpoint(proj,file,cls,line,mode);
}



/**
 * Enables a line breakpoint
 * @param file
 * @param line
 */

@Override public void enableBreakpoint(File file, int line)
{
   BumpBreakpoint bp = findBreakpoint(file, line);
   if (bp != null) {
      bump_client.editBreakpoint(bp.getBreakId(), "ENABLE", "TRUE");
    }
}



/**
 * Disables a line breakpoint
 * @param file
 * @param line
 */

@Override public void disableBreakpoint(File file, int line)
{
   BumpBreakpoint bp = findBreakpoint(file, line);
   if (bp != null) {
      bump_client.editBreakpoint(bp.getBreakId(), "ENABLE", "FALSE");
    }
}



/**
 * Finds the breakpoint at the given file and line
 * @param file
 * @param line
 * @return the breakpoint
 */

@Override public BumpBreakpoint findBreakpoint(File file, int line)
{
   for (BumpBreakpoint bb : bump_client.getBreakpoints(file)) {
      int lno = bb.getIntProperty("LINE");
      if (lno == line) {
	 return bb;
       }
    }

   return null;
}


@Override public BumpBreakpoint findBreakpoint(Element xml)
{
   String id = xml.getAttribute("ID");
   BumpBreakpoint bp = current_breakpoints.get(id);
   if (bp != null) return bp;
   BumpBreakImpl nbp = new BumpBreakImpl(xml);
   for (BumpBreakpoint obp : current_breakpoints.values()) {
       if (nbp.isEquivalentTo(obp)) return obp;
    }
   return null;
}



/**
 *	Remove breakpoint
 **/

@Override public boolean removeBreakpoint(String id)
{
   return bump_client.editBreakpoint(id,"CLEAR");
}



/**
 * Enables all breakpoints in the given file
 * @param file
 *
 */

@Override public void enableAllBreakpoints(File file)
{
   Iterator<BumpBreakpoint> it = bump_client.getAllBreakpoints().iterator();
   while (it.hasNext()) {
      BumpBreakpoint bp = it.next();
      if (file == null ||(bp.getFile() != null && bp.getFile().equals(file))) enableBreakpoint(bp.getFile(), bp.getLineNumber());
   }
}



/**
 * Disables all breakpoints in the given file
 * @param file
 *
 */

@Override public void disableAllBreakpoints(File file)
{
   Iterator<BumpBreakpoint> it = bump_client.getAllBreakpoints().iterator();
   while (it.hasNext()) {
      BumpBreakpoint bp = it.next();
      if (file == null ||(bp.getFile() != null && bp.getFile().equals(file))) disableBreakpoint(bp.getFile(), bp.getLineNumber());
   }
}



/********************************************************************************/
/*										*/
/*	Parameter setting modes 						*/
/*										*/
/********************************************************************************/

/**
 *	Set the default breakpoint mode.  The parameter can not be DEFAULT.
 **/

@Override public void setDefaultBreakMode(BumpBreakMode md)
{
   if (md != BumpBreakMode.DEFAULT) break_mode = md;
}



/**
 *	Set the default exception mode.  The parameter can not be DEFAULT.
 **/

@Override public void setDefaultExceptionMode(BumpExceptionMode md)
{
   if (md != BumpExceptionMode.DEFAULT) exception_mode = md;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addBreakpointHandler(File f,BumpBreakpointHandler bh)
{
   synchronized (break_handlers) {
      if (f == null) f = dummy_file;
      break_handlers.put(bh,f);
    }
}



synchronized void removeBreakpointHandler(BumpBreakpointHandler bh)
{
   synchronized (break_handlers) {
      break_handlers.remove(bh);
    }
}



/********************************************************************************/
/*										*/
/*	Set access methods							*/
/*										*/
/********************************************************************************/

List<BumpBreakpoint> getBreakpoints(File f)
{
   List<BumpBreakpoint> rslt = new ArrayList<BumpBreakpoint>();

   synchronized (current_breakpoints) {
      for (BumpBreakImpl bp : current_breakpoints.values()) {
	 if (fileMatch(f,bp)) {
	    rslt.add(bp);
	  }
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Maintenance methods							*/
/*										*/
/********************************************************************************/

void handleUpdate(Element ep)
{
   String reason = IvyXml.getAttrString(ep,"REASON");
   if (reason == null) reason = "LIST";

   List<BumpBreakImpl> rem = new ArrayList<BumpBreakImpl>();
   List<BumpBreakImpl> add = new ArrayList<BumpBreakImpl>();
   List<BumpBreakImpl> upd = new ArrayList<BumpBreakImpl>();

   synchronized (current_breakpoints) {
      for (Element be : IvyXml.children(ep,"BREAKPOINT")) {
	 String id = IvyXml.getAttrString(be,"ID");
	 if (reason.equals("REMOVE")) {
	    BumpBreakImpl bbi = current_breakpoints.remove(id);
	    if (bbi != null) rem.add(bbi);
	  }
	 else {
	    BumpBreakImpl bbi = current_breakpoints.get(id);
	    if (bbi != null) {
	       if (bbi.update(be)) upd.add(bbi);
	     }
	    else {
	       bbi = new BumpBreakImpl(be);
	       current_breakpoints.put(id,bbi);
	       add.add(bbi);
	     }
	  }
       }
    }

   for (Map.Entry<BumpBreakpointHandler,File> ent : break_handlers.entrySet()) {
      BumpBreakpointHandler hdlr = ent.getKey();
      File f = ent.getValue();
      if (f == dummy_file) f = null;
      for (BumpBreakImpl bbi : rem) {
	 if (fileMatch(f,bbi)) hdlr.handleBreakpointRemoved(bbi);
       }
      for (BumpBreakImpl bbi : add) {
	 if (fileMatch(f,bbi)) hdlr.handleBreakpointAdded(bbi);
       }
      for (BumpBreakImpl bbi : upd) {
	 if (fileMatch(f,bbi)) hdlr.handleBreakpointChanged(bbi);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Break handling methods							*/
/*										*/
/********************************************************************************/

private boolean fileMatch(File forfile,BumpBreakImpl bp)
{
   if (forfile == null || forfile == dummy_file) return true;
   return forfile.equals(bp.getFile());
}



}	// end of class BumpBreakSet




/* end of BumpBreakSet.java */

