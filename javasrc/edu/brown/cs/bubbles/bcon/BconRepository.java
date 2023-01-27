/********************************************************************************/
/*										*/
/*		BconRepository.java						*/
/*										*/
/*	Bubbles Environment Context Viewer name repository for access		*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassConstants.BassUpdatableRepository;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class BconRepository implements BconConstants, BassConstants, BassUpdatableRepository,
			BassConstants.BassPopupHandler
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BconName> active_names;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconRepository()
{
   active_names = new HashMap<>();

   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case JAVA_IDEA :
      case REBUS :
	 BassRepository br = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_CODE);
	 BassUpdatingRepository bur = (BassUpdatingRepository) br;
	 if (bur == null) return;
	 bur.addUpdateRepository(this);

	 synchronized (active_names) {
	    for (BassName bn : br.getAllNames()) {
	       switch (bn.getNameType()) {
		  case CLASS :
		  case INTERFACE :
		  case ENUM :
		  case THROWABLE :
		  case MODULE :
		  case ANNOTATION :
		     break;
		  default :
		     continue;
		}

	       BumpLocation bl = bn.getLocation();
	       if (bl == null) continue;
	       String ky = bl.getKey();
	       if (active_names.containsKey(ky)) continue;
	       BconName bcn = new BconName(bl,bn.getSubProject());
	       active_names.put(ky,bcn);
	     }
	  }
	 break;
      default :
	 break;
    }

   BassFactory.getFactory().addPopupHandler(this);
   // BumpClient.getBump().addChangeHandler(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   synchronized (active_names) {
      return new ArrayList<BassName>(active_names.values());
    }
}


@Override public boolean isEmpty()
{
   return active_names.isEmpty();
}



@Override public boolean includesRepository(BassRepository br)	{ return br == this; }



@Override public void reloadRepository()
{
   reload();
}


/********************************************************************************/
/*										*/
/*	Definition of a name							*/
/*										*/
/********************************************************************************/

private static class BconName extends BassNameBase {

   private BumpLocation bump_location;
   private String	sub_project;

   BconName(BumpLocation bl,String subpro) {
      bump_location = bl;
      name_type = BassNameType.OTHER_CLASS;
      sub_project = subpro;
    }

   @Override public String getProject() 	{ return bump_location.getSymbolProject(); }
   @Override public String getSubProject()	{ return sub_project; }
   @Override public int getModifiers()		{ return bump_location.getModifiers(); }
   @Override protected String getKey()		{ return bump_location.getKey() + "<BCON>"; }
   @Override protected String getSymbolName()	{ return bump_location.getSymbolName(); }
   @Override protected String getParameters()	{ return bump_location.getParameters(); }

   @Override public String getLocalName() {
      return "< OVERVIEW >";
    }

   @Override public String getNameHead() {
      String nm = getUserSymbolName();
      return stripTemplates(nm);
    }

   @Override public String getFullName()	{ return getNameHead() + ". " + getLocalName(); }

   @Override public BudaBubble createBubble()	{
      BconFactory bf = BconFactory.getFactory();
      boolean fg = bump_location.getKey().contains("$");
      return bf.createClassBubble(null,bump_location.getProject(),
				     bump_location.getFile(),
				     bump_location.getSymbolName(),fg);
    }

   @Override public String createPreviewString() {
      return "Create class overview panel for " + getUserSymbolName();
    }

   boolean update(BumpLocation bl) {
      boolean diff = false;
      if (!bl.getProject().equals(bump_location.getProject())) diff = true;
      if (bl.getModifiers() != bump_location.getModifiers()) diff = true;
      if (!bl.getKey().equals(bump_location.getKey())) diff = true;
      if (!bl.getSymbolName().equals(bump_location.getSymbolName())) diff = true;
      bump_location = bl;
      return diff;
    }

}	// end of inner class BconName



/********************************************************************************/
/*										*/
/*	Popup menu handler							*/
/*										*/
/********************************************************************************/

@Override public void addButtons(BudaBubble bb,Point where,JPopupMenu m,
				    String fullname,BassName forname)
{
   BoardProperties bp = BoardProperties.getProperties("Bcon");
   if (bp.getBoolean("Bcon.package.explorer")) {
      int idx = fullname.indexOf("@");
      if (idx >= 0) return;
      idx = fullname.indexOf(":");
      if (idx < 0) return;
      String proj = fullname.substring(0,idx);
      String pkg = fullname.substring(idx+1);
      if (pkg.indexOf("<") >= 0 || pkg.indexOf("(") >= 0) return;
      if (forname == null || forname.getNameType() == BassNameType.PACKAGE) {
	 m.add(new PackageAction(bb,where,proj,pkg));
       }
    }
}




private static class PackageAction extends AbstractAction
{
   private BudaBubble source_bubble;
   private Point      source_point;
   private String     project_name;
   private String     package_name;

   PackageAction(BudaBubble bb,Point wh,String proj,String pkg) {
      super("Create package viewer");
      source_bubble = bb;
      source_point = wh;
      project_name = proj;
      package_name = pkg;
    }

   @Override public void actionPerformed(ActionEvent e) {
      // this should be done in a separate thread
      BoardMetrics.noteCommand("BCON","CreatePackageBubble");
      BudaRoot.hideSearchBubble(e);
      BudaBubble bb = BconFactory.getFactory().createPackageBubble(source_bubble,project_name,
								      package_name);
      if (bb == null) return;

      BassFactory.getFactory().addNewBubble(source_bubble,source_point,bb);
    }

}	// end of inner class PackageAction





/********************************************************************************/
/*										*/
/*	Reload management							*/
/*										*/
/********************************************************************************/

private void reload()
{
   Set<String> found = new HashSet<String>();
   boolean chng = false;

   synchronized (active_names) {
      BassRepository br = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_CODE);
      for (BassName bn : br.getAllNames()) {
	 switch (bn.getNameType()) {
	    case CLASS :
	    case INTERFACE :
	    case ENUM :
	    case THROWABLE :
	    case ANNOTATION :
	       break;
	    default :
	       continue;
	  }

	 BumpLocation bl = bn.getLocation();
	 if (bl == null) continue;
	 String ky = bl.getKey();
	 found.add(ky);
	 BconName bcn = active_names.get(ky);
	 if (bcn == null) {
	    bcn = new BconName(bl,bn.getSubProject());
	    active_names.put(ky,bcn);
	    chng = true;
	  }
	 else {
	    if (bcn.update(bl)) chng = true;
	  }
       }
      for (Iterator<Map.Entry<String,BconName>> it = active_names.entrySet().iterator(); it.hasNext(); ) {
	 Map.Entry<String,BconName> ent = it.next();
	 if (!found.contains(ent.getKey())) {
	    it.remove();
	    chng = true;
	  }
       }
    }

   if (chng) {
      // BassFactory.reloadRepository(this);
    }
}




}	// end of class BconRepository




/* end of BconRepository.java */
