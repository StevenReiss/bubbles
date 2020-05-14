/********************************************************************************/
/*										*/
/*		BedrockBreakMover.java						*/
/*										*/
/*	Handle resetting breakpoints that disappear on a commit 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.limbo.LimboFactory;
import edu.brown.cs.ivy.limbo.LimboLine;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;

import java.util.ArrayList;
import java.util.List;


class BedrockBreakMover implements BedrockConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IBreakpointManager	break_manager;
private List<BreakPos>		saved_breaks;
private String			for_file;
private String			for_project;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockBreakMover(String proj,String file)
{
   break_manager = DebugPlugin.getDefault().getBreakpointManager();
   IBreakpoint [] bps = break_manager.getBreakpoints();
   BedrockPlugin.logD("START CHECKING BREAKPOINTS " + bps.length);
   for_file = file;

   saved_breaks = new ArrayList<>();
   for (IBreakpoint bp : bps) {
      if (bp instanceof IJavaLineBreakpoint) {
	 IJavaLineBreakpoint lbp = (IJavaLineBreakpoint) bp;
	 if (lbp.getMarker() == null) continue;
	 try {
	    String cls = lbp.getTypeName();
	    if (cls != null) {
	       String fnm = BedrockUtil.findFileForClass(cls);
	       if (fnm != null && fnm.equals(file)) {
		  saved_breaks.add(new BreakPos(lbp,for_file));
		}
	     }
	  }
	 catch (CoreException _ex) {
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Fixup methods								*/
/*										*/
/********************************************************************************/

void restoreBreakpoints()
{
   if (saved_breaks.isEmpty()) return;

   break_manager = DebugPlugin.getDefault().getBreakpointManager();
   IBreakpoint [] bps = break_manager.getBreakpoints();

   BedrockPlugin.logD("RECHECKING BREAKPOINTS " + bps.length);

   loop:
   for (BreakPos bp : saved_breaks) {
      IJavaLineBreakpoint lbp = bp.getBreakpoint();
      if (lbp.getMarker() != null && lbp.getMarker().exists()) {
	 for (IBreakpoint ibp : bps) {
	    if (ibp == lbp) continue loop;
	  }
       }
      BedrockPlugin.logD("NEED TO REINSERT " + lbp +  " " +
			    bp.getLineNumber() + " " + lbp.getMarker().exists());

      bp.update();
      int line = bp.getLineNumber();
      if (line <= 0) continue;

      try {
	 BedrockBreakpoint bpmgr = BedrockPlugin.getPlugin().getBreakManager();
	 bpmgr.setLineBreakpoint(for_project,null,for_file,null,bp.getLineNumber(),
				    bp.isSuspendVM(),bp.isTracepoint());
       }
      catch (BedrockException e) {
	 BedrockPlugin.logE("Problem reinserting breakpoint",e);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Represent a breakpoint position 					*/
/*										*/
/********************************************************************************/

private static class BreakPos {

   private IJavaLineBreakpoint break_point;
   private int line_number;
   private boolean is_trace;
   private boolean suspend_vm;
   private LimboLine limbo_line;

   BreakPos(IJavaLineBreakpoint bp,String file) {
      break_point = bp;
      IMarker mk = bp.getMarker();
      if (mk != null && mk.getAttribute("TRACEPOINT",false)) is_trace = true;
      else is_trace = false;
      line_number = 0;
      limbo_line = null;

      try {
	 int susp = bp.getSuspendPolicy();
	 suspend_vm = (susp == IJavaLineBreakpoint.SUSPEND_VM);
	 line_number = bp.getLineNumber();
	 limbo_line = LimboFactory.createLine(file,line_number);
	 BedrockPlugin.logD("SAVE BREAKPOINT " + bp + " " + line_number + " " +
			       bp.getCharStart() + " " + bp.getCharEnd());
       }
      catch (CoreException e) {
	 BedrockPlugin.logI("Problem getting break pos: " + e);
       }

    }

   void update() {
      if (limbo_line != null) {
	 limbo_line.revalidate();
	 int newline = limbo_line.getLine();
	 BedrockPlugin.logD("Move BREAKPOINT " + line_number + " " + newline);
	 line_number = newline;
       }
    }

   IJavaLineBreakpoint getBreakpoint()		{ return break_point; }
   int getLineNumber()				{ return line_number; }
   boolean isTracepoint()			{ return is_trace; }
   boolean isSuspendVM()			{ return suspend_vm; }

}	// end of inner class BreakPos



}	// end of class BedrockBreakMover




/* end of BedrockBreakMover.java */
