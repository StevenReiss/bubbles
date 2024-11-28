/********************************************************************************/
/*										*/
/*		BassCreator.java						*/
/*										*/
/*	Bubble Augmented Search Strategies new object buttons and actions	*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.board.BoardConstants.BoardLanguage;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoFieldDialog;
import edu.brown.cs.bubbles.bueno.BuenoInnerClassDialog;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoModuleDialog;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


class BassCreator implements BassConstants, BuenoConstants, BassConstants.BassPopupHandler {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubble	search_bubble;
private Point		access_point;

private static Element	button_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassCreator()
{
   search_bubble = null;
   access_point = null;

   if (button_data == null) {
      Element xml = BumpClient.getBump().getLanguageData();
      Element pdata = IvyXml.getChild(xml, "PROJECT");
      button_data = IvyXml.getChild(pdata,"BUTTONS");
    }
}




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,String fullname,BassName forname)
{
   addGenericButtons(bb,where,menu,fullname,forname);
}



// CHECKSTYLE:OFF
private void addGenericButtons(BudaBubble bb,Point where,JPopupMenu menu,String fullname,BassName forname)
// CHECKSTYLE:ON
{
   Map<String,Object> actions = new HashMap<>();
   Element bdata = null;
   search_bubble = bb;
   access_point = where;
   if (fullname.startsWith("@")) return;
   if (forname != null && !(forname instanceof BassNameLocation)) return;
   List<BuenoLocation> memblocs = new ArrayList<BuenoLocation>();
   BuenoLocation clsloc = null;

   boolean isproj = false;
   boolean ispkg = true;
   boolean haveimp = false;
   if (forname != null && forname.getNameType() == BassNameType.PROJECT) isproj = true;
   else if (forname == null && fullname != null &&
	 fullname.contains(":") &&
	 (fullname.endsWith(":") || fullname.endsWith("/")) &&
	 !fullname.contains(".") && !fullname.contains("^^")) {
      isproj= true;
      ispkg = false;
    }
   else if (forname == null && fullname != null && fullname.contains(":") &&
	 BoardSetup.getSetup().getLanguage() == BoardLanguage.JAVA) {
      isproj = true;
    }
	

   if (isproj) {
      int pidx = fullname.indexOf(":");
      String pname = fullname.substring(0,pidx);
      String body = fullname.substring(pidx+1);
      body = body.replace("/.","/");
      body = body.replace("^^",".");
      if (body.contains("/")) {
	 int sidx = body.lastIndexOf("/");
	 body = body.substring(0,sidx+1);
       }

      bdata = getButtonData("NEWPACKAGE");
      if (bdata != null) {
	 BuenoLocation dfltloc = BuenoFactory.getFactory().createLocation(fullname,
	       null,null,true);
	 actions.put("NEWPACKAGE",new NewPackageAction(dfltloc));
       }
      bdata = getButtonData("NEWDIRECTORY");
      if (bdata != null) {
	 BuenoLocation dfltloc = BuenoFactory.getFactory().createFileLocation(pname,body);
	 actions.put("NEWDIRECTORY",new NewDirectoryAction(dfltloc));
       }
      bdata = getButtonData("NEWFILE");
      if (forname == null && bdata != null) {
	 BuenoLocation dfltloc = BuenoFactory.getFactory().createFileLocation(pname,body);
	 actions.put("NEWFILE",new NewFileAction(dfltloc));
       }
      bdata = getButtonData("DELETEPROJECT");
      if (bdata != null) {
	 actions.put("DELETEPROJECT",new DeleteProjectAction(fullname,bb));
       }
      bdata = getButtonData("FIXIMPORTS");
      if (bdata != null && forname != null) {
	 if (BASS_PROPERTIES.getBoolean("Bass.fix.imports") && forname.getProject() != null) {
	    Action act = new FixImportsAction("Project " + forname.getProject(),
		  forname.getProject(),fullname,bb);
	    actions.put("FIXIMPORTS",act);
	    haveimp = true;
	  }
       }
      bdata = getButtonData("FORMAT");
      if (bdata != null && forname != null && BASS_PROPERTIES.getBoolean("Bass.fix.format")) {
	 Action act = new FormatAction("Project " + forname.getProject(),
	       forname.getProject(),fullname,bb);
	 actions.put("FORMAT",act);
       }
    }

   if (forname == null && ispkg) {
      String proj = null;
      int idx = fullname.indexOf(":");
      if (idx > 0) {
	 proj = fullname.substring(0,idx);
	 fullname = fullname.substring(idx+1);
       }
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(proj,
	    fullname,null,true);
      String inner = null;
      if (loc.getClassName() != null) {
	 memblocs.add(loc);
	 if (loc.getPackage() != null) clsloc = loc;
	 String cnm = loc.getClassName();
	 String pnm = loc.getPackage();
	 if (IvyXml.getAttrBool(button_data,"PACKAGED")) {
	    String outer;
	    if (pnm == null) outer = "";
	    else outer = cnm.substring(pnm.length() + 1);
	    if (outer.contains(".") || outer.contains("$")) {
	       int xidx = outer.indexOf(".");
	       inner = outer.replace(".", "$");
	       outer = pnm + "." + outer.substring(0,xidx);
	       memblocs.add(BuenoFactory.getFactory().createLocation(proj,outer,
		     inner,false));
	       memblocs.add(BuenoFactory.getFactory().createLocation(proj,outer,
		     inner,true));
	     }
	  }
	 bdata = getButtonData("NEWFILE");
	 if (bdata != null) {
	    actions.put("NEWFILE",new NewFileAction(loc));
	  }
	 bdata = getButtonData("DELETECLASS");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.delete.class")) {
	    if (pnm != null) {
	       cnm = cnm.substring(pnm.length()+1);
	       if (!cnm.contains(".")) {
		  actions.put("DELETECLASS",new DeleteClassAction(proj,loc.getClassName(),bb));
		}
	     }
	    else if (cnm != null) {
	       actions.put("DELETECLASS",new DeleteClassAction(proj,cnm,bb));
	     }
	  }
	 bdata = getButtonData("DELETEFILE");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.delete.file")) {
	    if (!IvyXml.getAttrBool(bdata,"NOCLASS") || actions.get("DELETECLASS") == null) {
	       File f = loc.getFile();
	       if (f != null) {
		  actions.put("DELETEFILE",new DeleteFileAction(proj,f,bb));
		}
	     }
	  }
	 bdata = getButtonData("MOVECLASS");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.move.class")) {
	    if (pnm != null && inner == null) {
	       actions.put("MOVECLASS",new MoveClassAction(proj,loc.getClassName(),pnm,bb));
	     }
	  }
	 if (!haveimp) {
	    bdata = getButtonData("FIXIMPORTS");
	    if (bdata != null  && BASS_PROPERTIES.getBoolean("Bass.fix.imports")) {
	       if (pnm != null && inner == null) {
		  File f = loc.getFile();
		  if (f != null) {
		     actions.put("FIXIMPORTS",new FixImportsAction(proj,f,bb));
		     haveimp = true;
		   }
		}
	     }
	  }
	 bdata = getButtonData("FORMAT");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.fix.format")) {
	    if (pnm != null && inner == null) {
	       File f = loc.getFile();
	       if (f != null) {
		  actions.put("FORMAT",new FormatAction(proj,f,bb));
		}
	     }
	  }
       }
      else if (!fullname.contains("@")) {
	 bdata = getButtonData("DELETEPACKAGE");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.delete.package"))
	    actions.put("DELETEPACKAGE",new DeletePackageAction(proj,fullname,bb));
	 bdata = getButtonData("DELETEPROJECT");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.delete.project")) {
	    actions.put("DELETEPROJECT",new DeleteProjectAction(proj,bb));
	  }
	 if (!haveimp) {
	    bdata = getButtonData("FIXIMPORTS");
	    if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.fix.imports")) {
	       actions.put("FIXIMPORTS",new FixImportsAction("Package " + fullname,proj,fullname,bb));
	     }
	  }
	 bdata = getButtonData("FORMAT");
	 if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.fix.format")) {
	    actions.put("FORMAT",new FormatAction("Package " + fullname,proj,fullname,bb));
	  }
       }
      if (loc.getPackage() != null && clsloc == null) clsloc = loc;
    }
   else if (forname.getNameType() != BassNameType.PROJECT) {
      BuenoLocation loc = new BassNewLocation(forname,false,false);
      memblocs.add(new BassNewLocation(forname,false,true));
      memblocs.add(new BassNewLocation(forname,true,false));
      memblocs.add(loc);
      if (loc.getPackage() != null) clsloc = loc;
      bdata = getButtonData("DELETEMETHOD");
      if (bdata != null && BASS_PROPERTIES.getBoolean("Bass.delete.method")) {
	 switch (forname.getNameType()) {
	    case METHOD :
	    case CONSTRUCTOR :
	       File f = forname.getLocation().getFile();
	       if (f != null) {
		  actions.put("DELETEMETHOD",new DeleteMethodAction(forname,bb));
		}
	       break;
	    default :
	       break;
	  }
       }
    }
   if (memblocs.size() > 0) {
      bdata = getButtonData("NEWMETHOD");
      if (bdata != null) {
	 List<Action> acts = new ArrayList<>();
	 for (BuenoLocation bl : memblocs) {
	    acts.add(new NewMethodAction(bl));
	  }
	 actions.put("NEWMETHOD",acts);
       }
      bdata = getButtonData("NEWFIELD");
      if (bdata != null) {
	 List<Action> acts = new ArrayList<>();
	 for (BuenoLocation bl : memblocs) {
	    acts.add(new NewFieldAction(bl));
	  }
	 actions.put("NEWFIELD",acts);
       }
      bdata = getButtonData("NEWINNERTYPE");
      if (bdata != null) {
	 List<Action> acts = new ArrayList<>();
	 for (BuenoLocation bl : memblocs) {
	    acts.add(new NewInnerTypeAction(bl));
	  }
	 actions.put("NEWINNERTYPE",acts);
       }
    }
   if (clsloc != null) {
      if (BuenoFactory.getFactory().useSeparateTypeButtons()) {
	 bdata = getButtonData("NEWCLASS");
	 if (bdata != null) {
	    actions.put("NEWCLASS",new NewTypeAction(BuenoType.NEW_CLASS,clsloc));
	  }
	 bdata = getButtonData("NEWENUM");
	 if (bdata != null) {
	    actions.put("NEWENUM",new NewTypeAction(BuenoType.NEW_ENUM,clsloc));
	  }
	 bdata = getButtonData("NEWINTERFACE");
	 if (bdata != null) {
	    actions.put("NEWINTERFACE",new NewTypeAction(BuenoType.NEW_INTERFACE,clsloc));
	  }
       }
      else {
	 bdata = getButtonData("NEWTYPE");
	 if (bdata != null) {
	    actions.put("NEWTYPE",new NewTypeAction(clsloc));
	  }
       }
      bdata = getButtonData("NEWPACKAGE");
      if (bdata != null) {
	 actions.put("NEWPACKAGE",new NewPackageAction(clsloc));
       }
    }

   for (Element btn : IvyXml.children(button_data,"BUTTON")) {
      String btyp = IvyXml.getAttrString(btn,"TYPE");
      Object v = actions.get(btyp);
      if (v != null) {
	 if (v instanceof Action) {
	    Action act = (Action) v;
	    String lbl = IvyXml.getAttrString(btn,"LABEL");
	    if (lbl != null) {
	       act.putValue(Action.NAME,lbl);
	     }
	    menu.add(act);
	  }
	 else if (v instanceof Collection<?>) {
	    Collection<?> itms = (Collection<?>) v;
	    String mname = IvyXml.getAttrString(btn,"MENU");
	    if (mname == null) mname = IvyXml.getAttrString(btn,"LABEL");
	    if (mname != null) {
	       JMenu m1 = new JMenu(mname);
	       for (Object o : itms) {
		  Action act = (Action) o;
		  m1.add(act);
		}
	       menu.add(m1);
	     }
	    else {
	       for (Object o : itms) {
		  Action act = (Action) o;
		  menu.add(act);
		}
	     }
	  }
       }
    }
}


private Element getButtonData(String type)
{
   for (Element bdata : IvyXml.children(button_data,"BUTTON")) {
      if (type.equals(IvyXml.getAttrString(bdata,"TYPE"))) {
	 return bdata;
       }
    }
   return null;
}





/********************************************************************************/
/*										*/
/*	Actions for creating a new method					*/
/*										*/
/********************************************************************************/

private abstract class NewAction extends AbstractAction implements Runnable {

   protected transient BuenoLocation for_location;
   protected BuenoProperties property_set;
   protected BuenoType create_type;
   private BudaBubble result_bubble;
   private BudaBubbleArea result_area;
   private Point result_point;

   private static final long serialVersionUID = 1;

   NewAction(BuenoType typ,BuenoLocation loc) {
      super(loc.getTitle(typ));
      create_type = typ;
      for_location = loc;
      property_set = new BuenoProperties();
      result_bubble = null;
      result_area = null;
      result_point = null;
      if (loc.getProject() != null)
	 property_set.put(BuenoKey.KEY_PROJECT,loc.getProject());
      if (loc.getPackage() != null)
	 property_set.put(BuenoKey.KEY_PACKAGE,loc.getPackage());
    }

   protected void addNewBubble(BudaBubble bb,BudaBubbleArea bba,Point pt) {
      if (bb == null) return;
      result_bubble = bb;
      result_area = bba;
      result_point = pt;
      SwingUtilities.invokeLater(this);
    }

   @Override public void run() {
      if (result_bubble != null) {
	 result_area.add(result_bubble,new BudaConstraint(result_point));
       }
    }

}	// end of inner class NewAction



private class NewMethodAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   NewMethodAction(BuenoLocation loc) {
      super(BuenoType.NEW_METHOD,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewMethod");
      BudaRoot.hideSearchBubble(e);
      BuenoFactory.getFactory().createMethodDialog(search_bubble,access_point,property_set,
						      for_location,null,this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb != null) {
	 addNewBubble(bb,bba,p);
	 File f1 = bb.getContentFile();
	 if (f1 != null) BumpClient.getBump().saveFile(proj,f1);
	 BudaRoot br = BudaRoot.findBudaRoot(bb);
	 if (br != null) br.handleSaveAllRequest();
       }
   }

}	// end of inner class NewMethodAction




private class NewFieldAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   NewFieldAction(BuenoLocation loc) {
      super(BuenoType.NEW_FIELD,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewField");
      BudaRoot.hideSearchBubble(e);
      BuenoFieldDialog bfd = new BuenoFieldDialog(search_bubble,access_point,
						       property_set,for_location,this);
      bfd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      int idx = name.lastIndexOf(".");
      String cnm = name.substring(0,idx);
      BudaBubble bb = BaleFactory.getFactory().createFieldsBubble(proj,for_location.getFile(),cnm);
      addNewBubble(bb,bba,p);
   }

}	// end of inner class NewFieldAction




private class NewTypeAction extends NewAction implements BuenoConstants.BuenoBubbleCreator, Runnable {

   private static final long serialVersionUID = 1;

   NewTypeAction(BuenoType typ,BuenoLocation loc) {
      super(typ,loc);
    }

   NewTypeAction(BuenoLocation loc) {
      super(BuenoType.NEW_TYPE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewType");
      BudaRoot.hideSearchBubble(e);
      BuenoFactory.getFactory().createClassDialog(search_bubble,access_point,
	    create_type,property_set,for_location,null,this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,null,name);
      addNewBubble(bb,bba,p);
   }


}	// end of inner class NewTypeAction




private class NewInnerTypeAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   NewInnerTypeAction(BuenoLocation loc) {
      super(BuenoType.NEW_INNER_TYPE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewInnerType");
      BudaRoot.hideSearchBubble(e);
      BuenoInnerClassDialog bcd = new BuenoInnerClassDialog(search_bubble,access_point,create_type,
						     property_set,for_location,this);
      bcd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createClassBubble(proj,name);
      addNewBubble(bb,bba,p);
   }

}	// end of inner class NewInnerTypeAction




private class NewPackageAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   NewPackageAction(BuenoLocation loc) {
      super(BuenoType.NEW_PACKAGE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewPackage");
      BudaRoot.hideSearchBubble(e);
      BuenoFactory.getFactory().createPackageDialog(search_bubble,access_point,create_type,
							 property_set,for_location,null,this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,null,name);
      addNewBubble(bb,bba,p);
   }

}	// end of inner class NewPackageAction



private class NewDirectoryAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   NewDirectoryAction(BuenoLocation loc) {
      super(BuenoType.NEW_DIRECTORY,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewDirectory");
      BudaRoot.hideSearchBubble(e);
      BuenoFactory.getFactory().createDirectoryDialog(search_bubble,access_point,create_type,
	    property_set,for_location,null,this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,null,name);
      addNewBubble(bb,bba,p);
    }

}	// end of inner class NewDirectoryAction






private class NewFileAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   NewFileAction(BuenoLocation loc) {
      super(BuenoType.NEW_FILE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewModule");
      BudaRoot.hideSearchBubble(e);
      BuenoModuleDialog bpd = new BuenoModuleDialog(search_bubble,access_point,
	    property_set,for_location,this);
      bpd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      File f = new File(name);
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,f,null);
      addNewBubble(bb,bba,p);
    }

}	// end of inner class NewFileAction






/********************************************************************************/
/*										*/
/*	BuenoLocation based on a bass name					*/
/*										*/
/********************************************************************************/

private static class BassNewLocation extends BuenoLocation {

   private BassName for_name;
   private boolean is_after;
   private boolean is_before;

   BassNewLocation(BassName nm,boolean after,boolean before) {
      for_name = nm;
      is_after = after;
      is_before = before;
    }

   @Override public String getProject() 		{ return for_name.getProject(); }
   @Override public String getPackage() 		{ return for_name.getPackageName(); }
   @Override public String getClassName() {
      String pkg = for_name.getPackageName();
      String cls = for_name.getClassName();
      if (cls == null) return null;
      if (pkg != null) cls = pkg + "." + cls;
      return cls;
    }

   @Override public String getInsertAfter() {
      if (is_after) return for_name.getFullName();
      return null;
    }

   @Override public String getInsertBefore() {
      if (is_before) return for_name.getFullName();
      return null;
    }

   @Override public String getInsertAtEnd()		{ return for_name.getClassName(); }

}	// end of inner class BassNewLocation



/********************************************************************************/
/*										*/
/*	Delete actions								*/
/*										*/
/********************************************************************************/

private static class DeleteProjectAction extends AbstractAction implements Runnable {

   private String project_name;

   private static final long serialVersionUID = 1;

   DeleteProjectAction(String proj,BudaBubble bb) {
      super("Delete Project " + proj);
      project_name = proj;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (BASS_PROPERTIES.getBoolean("Bass.delete.confirm",true)) {
	 int sts = JOptionPane.showConfirmDialog(null,"Do you really want to delete project " + project_name,
						    "Confirm Delete Project",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }
      BoardThreadPool.start(this);
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"PROJECT",project_name,BASS_PROPERTIES.getBoolean("Bass.delete.rebuild",true));
    }
}	// end of inner class DeleteProjectAction





private static class DeletePackageAction extends AbstractAction implements Runnable {

   private String project_name;
   private String package_name;

   private static final long serialVersionUID = 1;

   DeletePackageAction(String proj,String pkg,BudaBubble bb) {
      super("Delete Package " + pkg);
      project_name = proj;
      package_name = pkg;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (BASS_PROPERTIES.getBoolean("Bass.delete.confirm",true)) {
	 int sts = JOptionPane.showConfirmDialog(null,"Do you really want to delete all of package " + package_name,
						    "Confirm Delete Package",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"PACKAGE",package_name,BASS_PROPERTIES.getBoolean("Bass.delete.rebuild",true));
    }

}	// end of inner class DeletePackageAction




private static class DeleteFileAction extends AbstractAction implements Runnable {

   private String project_name;
   private File file_name;

   private static final long serialVersionUID = 1;

   DeleteFileAction(String proj,File fil,BudaBubble bb) {
      super("Delete File " + fil.getName());
      project_name = proj;
      file_name = fil;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (BASS_PROPERTIES.getBoolean("Bass.delete.confirm",true)) {
	 int sts = JOptionPane.showConfirmDialog(null,"Do you really want to delete file " + file_name.getPath(),
						    "Confirm Delete File",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"FILE",file_name.getAbsolutePath(),BASS_PROPERTIES.getBoolean("Bass.delete.rebuild",true));
    }
}	// end of inner class DeleteFileAction



private static class FixImportsAction extends AbstractAction {

   private String project_name;
   private File file_name;
   private String name_prefix;

   private static final long serialVersionUID = 1;

   FixImportsAction(String proj,File fil,BudaBubble bb) {
      super("Fix Imports for " + fil.getName());
      project_name = proj;
      file_name = fil;
      name_prefix = null;
    }

   FixImportsAction(String desc,String proj,String pfx,BudaBubble bb) {
      super("Fix Imports for " + desc);
      project_name = proj;
      name_prefix = pfx;
      file_name = null;
    }


   @Override public void actionPerformed(ActionEvent e) {
      // dialog with options???
      if (file_name != null) {
	 ImportFixer fixer = new ImportFixer(project_name,file_name);
	 BoardThreadPool.start(fixer);
       }
      else if (name_prefix != null) {
	 BassFactory bf = BassFactory.getFactory();
	 Collection<File> files = bf.findAssociatedFiles(project_name,name_prefix);
	 for (File f : files) {
	    ImportFixer fixer = new ImportFixer(project_name,f);
	    BoardThreadPool.start(fixer);
	  }
       }
   }

}	// end of inner class FixImportsAction



private static class FormatAction extends AbstractAction {

   private String project_name;
   private File file_name;
   private String name_prefix;

   private static final long serialVersionUID = 1;

   FormatAction(String proj,File fil,BudaBubble bb) {
      super("Format " + fil.getName());
      project_name = proj;
      file_name = fil;
      name_prefix = null;
    }

   FormatAction(String desc,String proj,String pfx,BudaBubble bb) {
      super("Format " + desc);
      project_name = proj;
      name_prefix = pfx;
      file_name = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (file_name != null) {
	 Formatter fixer = new Formatter(project_name,file_name);
	 BoardThreadPool.start(fixer);
       }
      else if (name_prefix != null) {
	 BassFactory bf = BassFactory.getFactory();
	 Collection<File> files = bf.findAssociatedFiles(project_name,name_prefix);
	 for (File f : files) {
	    Formatter fixer = new Formatter(project_name,f);
	    BoardThreadPool.start(fixer);
	 }
      }
    }

}	// end of inner class FormatAction


private static class ImportFixer implements Runnable  {

   private String project_name;
   private File file_name;
   private Element edit_result;

   ImportFixer(String proj,File file) {
      project_name = proj;
      file_name = file;
      edit_result = null;
    }

   @Override public void run() {
      if (edit_result == null) {
	 String order = BASS_PROPERTIES.getString("Bass.import.order");
	 int demand = BASS_PROPERTIES.getInt("Bass.import.ondemand.threshold");
	 int sdemand = BASS_PROPERTIES.getInt("Bass.import.static.ondemand.threshold");
	 BumpClient bc = BumpClient.getBump();
	 Element edits = bc.fixImports(project_name,file_name,order,demand,sdemand,null);
	 if (edits != null) {
	    edit_result = edits;
	    SwingUtilities.invokeLater(this);
	  }
       }
      else {
	 BaleFactory.getFactory().applyEdits(file_name,edit_result);
	 edit_result = null;
       }
    }

}	// end of inner class ImportFixer


private static class Formatter implements Runnable  {

   private String project_name;
   private File file_name;
   private Element edit_result;

   Formatter(String proj,File file) {
      project_name = proj;
      file_name = file;
      edit_result = null;
   }

   @Override public void run() {
      if (edit_result == null) {
	 BumpClient bc = BumpClient.getBump();
	 Element edits = bc.format(project_name,file_name,0,(int) file_name.length());
	 if (edits != null) {
	    edit_result = edits;
	    SwingUtilities.invokeLater(this);
	 }
      }
      else {
	 BaleFactory.getFactory().applyEdits(file_name,edit_result);
	 edit_result = null;
      }
   }

}	// end of inner class Formatter





private static class DeleteClassAction extends AbstractAction implements Runnable {

   private String project_name;
   private String class_name;

   private static final long serialVersionUID = 1;

   DeleteClassAction(String proj,String cls,BudaBubble bb) {
      super("Delete Class " + cls);
      project_name = proj;
      class_name = cls;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (BASS_PROPERTIES.getBoolean("Bass.delete.confirm",true)) {
	 int sts = JOptionPane.showConfirmDialog(null,"Do you really want to delete the class " + class_name,
						    "Confirm Delete Class",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
      }
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      if (!bc.delete(project_name,"CLASS",class_name,BASS_PROPERTIES.getBoolean("Bass.delete.rebuild",true))) {
	 JOptionPane.showMessageDialog(null, "Class Delete Failed");
      }
    }

}	// end of inner class DeleteClassAction



private static class DeleteMethodAction extends AbstractAction {

   private transient BassName method_location;

   private static final long serialVersionUID = 1;

   DeleteMethodAction(BassName mthd,BudaBubble bb) {
      super("Delete Method " + mthd.getName());
      method_location = mthd;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> bl = bc.findMethod(method_location.getProject(),
	    method_location.getFullName(),false);
      if (bl.size() != 1) return;
      BumpLocation bloc = bl.get(0);
      BaleConstants.BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(bloc.getProject(),bloc.getFile());
      if (bfo == null) return;

      if (BASS_PROPERTIES.getBoolean("Bass.delete.confirm",true)) {
	 int sts = JOptionPane.showConfirmDialog(null,"Do you really want to delete the method " +
	       method_location.getDisplayName(),
	       "Confirm Delete Class",
	       JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }

      int off = bfo.mapOffsetToJava(bloc.getDefinitionOffset());
      int eoff = bfo.mapOffsetToJava(bloc.getDefinitionEndOffset());
      int len = eoff - off;

      try {
	 bfo.remove(off,len);
      }
      catch (Exception ex) { }
    }

}



private static class MoveClassAction extends AbstractAction implements Runnable {

   private String project_name;
   private String class_name;
   private String from_package;
   private String to_package;
   private SwingComboBox<String> combo_box;
   private BudaBubble base_bubble;

   private static final long serialVersionUID = 1;

   MoveClassAction(String proj,String cls,String pkg,BudaBubble bb) {
      super("Move Class " + cls);
      project_name = proj;
      class_name = cls;
      to_package = null;
      from_package = pkg;
      combo_box = null;
      base_bubble = bb;
   }

   @Override public void actionPerformed(ActionEvent e) {
      combo_box = new SwingComboBox<String>("Package",
	    new String [] { "Generating list of available and relevant packages" });
      SwingUtilities.invokeLater(this);
      String cnm = class_name;
      int idx = cnm.lastIndexOf(".");
      if (idx >= 0) cnm = cnm.substring(idx+1);
      int sts = JOptionPane.showOptionDialog(base_bubble,combo_box,
	       "Select Target Package for " + cnm,
	       JOptionPane.OK_CANCEL_OPTION,
	       JOptionPane.QUESTION_MESSAGE,null,null,null);
      if (sts != 0) return;
      String rslt = (String) combo_box.getSelectedItem();
      if (rslt.startsWith("Generating ")) return;
      to_package = rslt;
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      if (to_package == null && combo_box != null) {
	 combo_box.setContents(getPackages());
      }
      else {
	 BumpClient bc = BumpClient.getBump();
	 List<BumpLocation> locs = bc.findClassDefinition(project_name,class_name);
	 if (locs == null || locs.size() == 0) return;
	 BumpLocation bloc = null;
	 for (BumpLocation bl1 : locs) {
	    if (bl1.getFile() != null && bl1.getFile().exists()) {
	       bloc = bl1;
	       break;
	     }
	  }

	 Element edits = bc.moveClass(project_name,class_name,bloc,to_package);
	 BaleFactory.getFactory().applyEdits(edits);
      }
   }

   private List<String> getPackages() {
      Set<String> rslt = new TreeSet<>();

      BassRepository br = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_CODE);
      for (BassName bn : br.getAllNames()) {
	 if (project_name != null && !project_name.equals(bn.getProject())) continue;
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
	 String pkg = bn.getNameHead();
	 if (pkg == null) continue;
	 int idx = pkg.lastIndexOf(".");
	 if (idx < 0) continue;
	 else pkg = pkg.substring(0,idx);
	 if (rslt.contains(pkg)) continue;
	 BumpLocation bl = bn.getLocation();
	 if (bl == null) continue;
	 String key = bl.getKey();
	 if (key.contains("$")) continue;
	 if (pkg.equals(from_package)) continue;

	 rslt.add(pkg);
       }

      return new ArrayList<>(rslt);
    }

}	// end of inner class MoveClassAction




}	// end of class BassCreator




/* end of BassCreator.java */
