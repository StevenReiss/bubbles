/********************************************************************************/
/*										*/
/*		BaleFactory.java						*/
/*										*/
/*	Bubble Annotated Language Editor factory for creating editors		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko      */
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


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardAttributes;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussBubble;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.StyleContext;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;


/**
 *	This class provides access to bubbles and editor components for code
 *	fragments.
 **/

public class BaleFactory implements BaleConstants, BudaConstants, BuenoConstants,
		BudaConstants.BudaFileHandler, BumpConstants.BumpChangeHandler
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<File,BaleDocumentIde>	file_documents;
private StyleContext			style_context;
private BoardAttributes 		bale_attributes;
private BaleHighlightContext		global_highlights;
private SwingEventListenerList<BaleAnnotationListener> annot_listeners;
private Set<BaleAnnotation>		active_annotations;
private SwingEventListenerList<BaleContextListener> context_listeners;
private BudaRoot			buda_root;

private static BaleFactory	the_factory;

private static BumpClient	bump_client = null;
private static long		format_time = 0;

private static boolean		is_setup = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BaleFactory()
{
   bump_client.waitForIDE();

   file_documents = new HashMap<File,BaleDocumentIde>();
   style_context = new StyleContext();
   bale_attributes = new BoardAttributes("Bale");
   annot_listeners = new SwingEventListenerList<>(BaleAnnotationListener.class);
   active_annotations = new HashSet<BaleAnnotation>();
   context_listeners = new SwingEventListenerList<>(BaleContextListener.class);
   BudaRoot.addFileHandler(this);

   bump_client.addChangeHandler(this);

   addContextListener(new ProblemHover());
}


/**
 *	Return the singular instance of the BaleFactory object.
 **/

public synchronized static BaleFactory getFactory()
{
   if (the_factory == null) the_factory = new BaleFactory();

   return the_factory;
}


/**
 *	This routine initializes the Bale package.  It is called automatically at startup.
 **/

public static void setup()
{
   if (is_setup) return;
   is_setup = true;

   BaleConfigurator bc = new BaleConfigurator();
   BudaRoot.addBubbleConfigurator("BALE",bc);
   BudaRoot.addPortConfigurator("BALE",bc);

   BudaRoot.registerMenuButton("Admin.Admin.Import Java Formats",new FormatImporter());
   BudaRoot.registerMenuButton("Admin.Admin.Import Formats from Project",new ProjectFormatImporter());

   BuenoFactory.getFactory().addInsertionHandler(new BaleInserter());
}



/**
 *	Called to initialize once BudaRoot is setup
 **/

public static void initialize(BudaRoot br)
{
   bump_client = BumpClient.getBump();

   bump_client.addOpenEditorBubbleHandler(new BaleOpenEditorHandler(br));
   getFactory().buda_root = br;
   
   // force key definitions
   new BaleEditorKit(BoardLanguage.JAVA);
   new BaleEditorKit(BoardLanguage.PYTHON);
   new BaleEditorKit(BoardLanguage.JS);
}



/********************************************************************************/
/*										*/
/*	Factory methods for method editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createMethodFragmentEditor(String proj,String method)
{
   List<BumpLocation> locs = bump_client.findMethod(proj,method,false);

   return getEditorFromLocations(locs);
}




BaleFragmentEditor createMethodFragmentEditor(BumpLocation loc)
{
   if (loc == null) return null;

   List<BumpLocation> locs = new ArrayList<BumpLocation>();
   locs.add(loc);

   return getEditorFromLocations(locs);
}



/********************************************************************************/
/*										*/
/*	Factory methods for field editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createFieldFragmentEditor(String proj,File file,String cls)
{
   List<BumpLocation> locs = bump_client.findFields(proj,file,cls);

   return getEditorFromLocations(locs,BaleFragmentType.FIELDS,cls + ".<FIELDS>");
}



/********************************************************************************/
/*										*/
/*	Factory methods for class editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createStaticsFragmentEditor(String proj,String cls,File file)
{
   List<BumpLocation> locs = bump_client.findClassInitializers(proj,cls,file);

   return getEditorFromLocations(locs,BaleFragmentType.STATICS,cls + ".<INITIALIZERS>");
}


BaleFragmentEditor createMainProgramFragmentEditor(String proj,String cls,File file)
{
   List<BumpLocation> locs = bump_client.findClassInitializers(proj,cls,file,true);

   return getEditorFromLocations(locs,BaleFragmentType.MAIN,cls + ".<MAIN>");
}



BaleFragmentEditor createClassPrefixFragmentEditor(String proj,File file,String cls)
{
   List<BumpLocation> locs = bump_client.findClassPrefix(proj,file,cls);

   return getEditorFromLocations(locs,BaleFragmentType.HEADER,cls + ".<PREFIX>");
}



BaleFragmentEditor createClassFragmentEditor(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findClassDefinition(proj,cls);

   return getEditorFromLocations(locs,BaleFragmentType.CLASS,cls);
}



/********************************************************************************/
/*										*/
/*	Factory methods for file editors					*/
/*										*/
/********************************************************************************/

public BaleFragmentEditor createFileEditor(String proj,File fil,String cls)
{
   List<BumpLocation> locs = bump_client.findCompilationUnit(proj,fil,cls);

   return getEditorFromLocations(locs,BaleFragmentType.FILE,cls + ".<FILE>");
}



/********************************************************************************/
/*										*/
/*	Factory methods for local file editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createLocalFileEditor(String proj,File file,String cnm,String fnm,int lno)
{
   BaleDocumentIde fdoc = getDocument(proj,file,true);
   BaleRegion rgn = null;

   int spos = 0;
   int epos = (int) file.length();

   if (lno > 0 && fnm != null) {
      Element ed = fdoc.getReadonlyElisionData();
      int off = fdoc.findLineOffset(lno);
      // BoardLog.logD("BALE","FIND " + fnm + " " + off + " IN " + IvyXml.convertXmlToString(ed));

      Element me = findMethodElement(ed,off);
      if (me != null) {
	 spos = IvyXml.getAttrInt(me,"START");
	 epos = IvyXml.getAttrInt(me,"LENGTH") + spos + 1;
       }
   }

   try {
      Position pos0 = fdoc.createPosition(spos);
      Position pos1 = fdoc.createPosition(epos);
      rgn = new BaleRegion(pos0,pos1);
    }
   catch (BadLocationException e) { }
   List<BaleRegion> rgns = new ArrayList<BaleRegion>();
   rgns.add(rgn);

   BaleFragmentEditor bfe = new BaleFragmentEditor(proj,file,fnm,fdoc,BaleFragmentType.ROFILE,rgns);

   noteEditorAdded(bfe);

   return bfe;
}


private Element findMethodElement(Element e,int off)
{
   if (e == null) return null;

   if (IvyXml.isElement(e,"ELIDE")) {
      int spos = IvyXml.getAttrInt(e,"START");
      int epos = IvyXml.getAttrInt(e,"LENGTH") + spos - 1;
      if (off < spos || off > epos) return null;

      String nty = IvyXml.getAttrString(e,"NODE");
      if (nty != null && nty.equals("METHOD")) return e;
    }

   for (Element ce : IvyXml.children(e,"ELIDE")) {
      Element fe = findMethodElement(ce,off);
      if (fe != null) return fe;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Generic methods for editors						*/
/*										*/
/********************************************************************************/

/**
 *	Create a code bubble corresponding to the current location.  The bubble
 *	should be for the same bubble area as the given src component.	If uselnk
 *	is true, then a link will be created from the bubble associated with the
 *	source component.  If the position p is given, this link will be associated
 *	with the line containing p, otherwise it will be a general link from the source
 *	bubble.  The at point if non-null specifies where to locate the bubble.  If it
 *	is null, the bubble will be located near the source bubble, close enough to be
 *	in the same group if the near flag is set, a little further away otherwise.
 *	Finally, if the add flag is set, the bubble will be added to the bubble area
 *	and displayed.
 *	@param src component identifying the source bubble
 *	@param p optional location in the source for creating a link
 *	@param at optional display location for the new bubble
 *	@param near flag indicating whether the new bubble should be close (same group) or
 *	further away from the src bubble if not specific point is given
 *	@param bl location identifying the code fragment to create a bubble for
 *	@param uselnk if true, then a link will be created between the source bubble and the
 *	new bubble
 *	@param add if true, then the bubble will be added to the bubble area and displayed
 *	@param marknew if true, then the bubble will be marked as new
 **/
// Modified By Hsu-Sheng Ko

BudaBubble createLocationEditorBubble(Component src,Position p,Point at,
						      boolean near,
						      BumpLocation bl,
						      boolean uselnk,boolean add, boolean marknew)
{
   if (bl == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   if (bba == null) return null;

   BaleFragmentEditor fed = null;
   switch (bl.getSymbolType()) {
      case FUNCTION :
      case CONSTRUCTOR :
	 fed = createMethodFragmentEditor(bl);
	 break;
      case FIELD :
      case ENUM_CONSTANT :
      case GLOBAL :
	 String fnm = bl.getSymbolName();
	 int idx = fnm.lastIndexOf(".");
	 if (idx > 0) {
	    String cnm = fnm.substring(0,idx);
	    fed = createFieldFragmentEditor(bl.getSymbolProject(),bl.getFile(),cnm);
	  }
	 break;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
	 fed = createClassFragmentEditor(bl.getSymbolProject(),bl.getSymbolName());
	 break;
      case STATIC_INITIALIZER :
	 fnm = bl.getSymbolName();
	 idx = fnm.lastIndexOf(".");
	 if (idx > 0) {
	    String cnm = fnm.substring(0,idx);
	    fed = createStaticsFragmentEditor(bl.getSymbolProject(),cnm,bl.getFile());
	  }
	 break;
      case MODULE :
	 // fed = createFileEditor(bl.getSymbolProject(),bl.getFile(),null);
	 break;
      case EXPORT :
      case IMPORT :
      case PROGRAM :
	 // fed = create...
	 break;
      case UNKNOWN :
	 fed = createFileEditor(bl.getProject(),bl.getFile(),null);
	 break;
      default:
	 break;
    }

   if (fed == null) return null;

   BaleEditorBubble bb = new BaleEditorBubble(fed);
   if (add) {
      int place = PLACEMENT_PREFER | PLACEMENT_MOVETO;
      if (near) place |= PLACEMENT_GROUPED;
      if (marknew) place |= PLACEMENT_NEW;

      BudaBubble obbl = BudaRoot.findBudaBubble(src);
      // BudaRoot root = BudaRoot.findBudaRoot(src);
      // int offset = (near ? BUBBLE_CREATION_NEAR_SPACE : BUBBLE_CREATION_SPACE);
      Point lp = null;
      if (uselnk && obbl != null) {
	 BudaConstants.LinkPort port0;
	 if (p == null) port0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);
	 else {
	    port0 = new BaleLinePort(src,p,null);
	    lp = port0.getLinkPoint(obbl,obbl.getLocation());
	 }
	 if (at == null) {
	    bba.addBubble(bb,src,lp,place);
	    // Rectangle loc = BudaRoot.findBudaLocation(src);
	    // if (lp != null) loc.y = lp.y;
	    // root.add(bb,new BudaConstraint(loc.x+loc.width+offset,loc.y));
	  }
	 BudaConstants.LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	 BudaBubbleLink lnk = new BudaBubbleLink(obbl,port0,bb,port1);
	 bba.addLink(lnk);
       }
      else if (at != null) {
	 bba.addBubble(bb,null,at,PLACEMENT_MOVETO);
	 // root.add(bb,new BudaConstraint(at));
       }
      else {
	 bba.addBubble(bb,src,null,place);
	 // root.add(bb,new BudaConstraint(loc.x+loc.width+offset,loc.y));
       }
    }

   return bb;
}




/********************************************************************************/
/*										*/
/*	Generic methods for creating bubbles/bubble stack from locations	*/
/*										*/
/********************************************************************************/

/**
 *	Creates an appropriate bubble for the given set of locations.  If the set
 *	is small (i.e. 1 or 2 elements), the explicit bubbles are created.  Otherwise
 *	a bubble stack is created.  The stack/bubbles are linked to the given source
 *	bubble at the given position if the link flag is set.  The position of the
 *	new bubbles is either at the explicit point or close to the source bubble, with
 *	the closeness dependent on the near flag.
 **/

public void createBubbleStack(Component src,Position p,Point pt,boolean near,
			     Collection<BumpLocation> locs,BudaLinkStyle link)
{
   BaleBubbleStack.createBubbles(src,p,pt,near,BaleStackType.NORMAL,locs,link);
}


public BussBubble createBubbleStackForced(Component src,Position p,Point pt,boolean near,
      Collection<BumpLocation> locs,BudaLinkStyle link)
{
   return BaleBubbleStack.createBubbles(src,p,pt,near,BaleStackType.FORCE,locs,link);
}




/********************************************************************************/
/*										*/
/*	Methods for creating bubbles for code fragments 			*/
/*										*/
/********************************************************************************/

/**
 *	Return the code bubble for a method code fragment.   The fragment is
 *	given by its fully qualified name (including parameter types).	If the
 *	method is ambiguous, the corresponding editor will contain code fragments
 *	for all instances separated by budding lines.
 **/

public BudaBubble createMethodBubble(String proj,String fct)
{
   if (fct.contains(".<clinit>()")) {
      BoardLog.logD("BALE","Creating method bubble for static initializer");
      int idx = fct.indexOf(".<clinit>()");
      String cls = fct.substring(0,idx);
      return createStaticsBubble(proj,cls,null);
    }

   BaleFragmentEditor bfe = createMethodFragmentEditor(proj,fct);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}





/**
 *	Return the code bubble for a system code fragment.   The fragment is
 *	given by its fully qualified name (including parameter types).	The source
 *	file to be used is passed in.
 **/

public BudaBubble createSystemMethodBubble(String proj,String fct,File src,int lno)
{
   String cnm = fct;
   int idx = fct.indexOf("(");
   if (idx >= 0) {
      int idx1 = fct.lastIndexOf(".",idx);
      cnm = fct.substring(0,idx1);
   }

   BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,src,cnm,fct,lno);

   return bb;
}



/**
 *	Return the code bubble for all the fields of the given class.
 **/

public BudaBubble createFieldsBubble(String proj,File file,String cls)
{
   BaleFragmentEditor bfe = createFieldFragmentEditor(proj,file,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}




/**
 *	Return the code bubble for all static initializers of the given class.
 **/

public BudaBubble createStaticsBubble(String proj,String cls,File file)
{
   BaleFragmentEditor bfe = createStaticsFragmentEditor(proj,cls,file);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}


public BudaBubble createMainProgramBubble(String proj,String cls,File file)
{
   BaleFragmentEditor bfe = createMainProgramFragmentEditor(proj,cls,file);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}




/**
 *	Return the code bubble for the class prefix of the given class.  This includes
 *	the header information (e.g. package, imports) as well as the class declaration.
 **/

public BudaBubble createClassPrefixBubble(String proj,File file,String cls)
{
   BaleFragmentEditor bfe = createClassPrefixFragmentEditor(proj,file,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}




/**
 *	Return the class code bubble for the given class.
 **/

public BudaBubble createClassBubble(String proj,String cls)
{
   if (cls == null) return null;

   BaleFragmentEditor bfe = createClassFragmentEditor(proj,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}



/**
 *	Return the file code bubble for the given class.
 **/

public BudaBubble createFileBubble(String proj,File fil,String cls)
{
   BaleFragmentEditor bfe = createFileEditor(proj,fil,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}


public BudaBubble createFileBubble(String proj,File file,String cnm,String fnm,int lno)
{
   BaleFragmentEditor bfe = createLocalFileEditor(proj,file,cnm,fnm,lno);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}





/********************************************************************************/
/*										*/
/*	Factory methods for file access 					*/
/*										*/
/********************************************************************************/

/**
 *	Return a handle to the internal representation of an open file based on
 *	the project and file name.
 **/

public BaleFileOverview getFileOverview(String proj,File file)
{
   return getDocument(proj,file);
}

public BaleFileOverview getFileOverview(String proj,File file,boolean lcl)
{
   return getDocument(proj,file,lcl);
}



/********************************************************************************/
/*										*/
/*	Calls for creating new methods						*/
/*										*/
/********************************************************************************/

/**
 *	Create a new method and a bubble displaying that method.  The bubble is
 *	added to the display
 *
 *	@param proj project containing the class
 *	@param name fully qualified name of the new method
 *	@param params list of parameter types (and names if desired)
 *	@param returns return type
 *	@param modifiers modifier flags (in Java reflection format)
 *	@param comment insert a comment before the method if true
 *	@param after element in the source that the new method should follow. If null, then
 *	the new method is inserted at the end of the class.
 *	@param source component identifying the source bubble and bubble area
 *	@param pos the optional position in the source component to be used for linking
 *	@param link if true, then a link is created between the source bubble and the newly
 *	created bubble.
 **/

public BudaBubble createNewMethod(String proj,
					   String name,
					   String params,
					   String returns,
					   int modifiers,
					   boolean comment,
					   String after,
					   Component source,
					   Position pos,
					   boolean link,
					   boolean add)
{
   String clsnm,mthdnm;
   int idx = name.lastIndexOf(".");
   if (idx < 0) {
      clsnm = null;
      mthdnm = name;
   }
   else {
      clsnm = name.substring(0,idx);
      mthdnm = name.substring(idx+1);
   }

   BuenoProperties bp = new BuenoProperties();
   bp.put(BuenoKey.KEY_NAME,mthdnm);
   if (params != null) bp.put(BuenoKey.KEY_PARAMETERS,params);
   if (returns != null) bp.put(BuenoKey.KEY_RETURNS,returns);
   bp.put(BuenoKey.KEY_MODIFIERS,modifiers);
   if (comment) bp.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
   BuenoLocation bl = BuenoFactory.getFactory().createLocation(proj,clsnm,after,true);

   BuenoFactory.getFactory().createNew(BuenoType.NEW_METHOD,bl,bp);
   if (bl.getInsertionFile() == null) return null;

   BaleDocumentIde doc = BaleFactory.getFactory().getDocument(proj,bl.getInsertionFile());
   List<BumpLocation> blocs = bump_client.findMethod(proj,name,false);
   if (blocs == null || blocs.size() == 0) return null;

   BumpLocation loc = null;
   for (BumpLocation bloc : blocs) {
      BaleRegion rgn = doc.getRegionFromLocation(bloc);
      if (rgn == null) continue;
      int rs = rgn.getStart();
      int re = rgn.getEnd();
      if (bl.getInsertionOffset() < re && bl.getInsertionOffset() + bl.getInsertionLength() > rs) {
	 loc = bloc;
	 break;
      }
   }

   if (loc == null) return null;

   return createLocationEditorBubble(source,pos,null,true,loc,link,add,true);
}



double getScaleFactor()
{
   return buda_root.getCurrentBubbleArea().getScaleFactor();
}



/********************************************************************************/
/*										*/
/*	Methods for creating links						*/
/*										*/
/********************************************************************************/

/**
 *	Create a link for a bale bubble given at the given line
 **/

public BudaConstants.LinkPort findPortForLine(BudaBubble bb,int line)
{
   if (!(bb instanceof BaleEditorBubble)) return null;

   BaleFragmentEditor bfe = (BaleFragmentEditor) bb.getContentPane();
   BaleDocument bd = bfe.getDocument();
   int loff = bd.findLineOffset(line);

   try {
      Position p = bd.createPosition(loff);
      return new BaleLinePort(bb,p,null);
    }
   catch (BadLocationException e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	Style methods								*/
/*										*/
/********************************************************************************/

StyleContext getStyleContext()			{ return style_context; }



/********************************************************************************/
/*										*/
/*	Attribute methods							*/
/*										*/
/********************************************************************************/

AttributeSet getAttributes(String id)
{
   return bale_attributes.getAttributes(id);
}


static long getFormatTime()	{ return format_time; }


/********************************************************************************/
/*										*/
/*	Highlighting methods							*/
/*										*/
/********************************************************************************/

synchronized BaleHighlightContext getGlobalHighlightContext()
{
   if (global_highlights == null) {
      global_highlights = new BaleHighlightContext();
    }

   return global_highlights;
}




/********************************************************************************/
/*										*/
/*	IDE File Document methods						*/
/*										*/
/********************************************************************************/

BaleDocumentIde getDocument(String proj,File f)
{
   return getDocument(proj,f,false);
}


BaleDocumentIde getDocument(String proj,File f,boolean lcl)
{
   BaleDocumentIde fdoc = null;

   if (f == null) return null;

   synchronized (file_documents) {
      fdoc = file_documents.get(f);
      if (fdoc == null) {
	 fdoc = new BaleDocumentIde(proj,f,lcl);
	 file_documents.put(f,fdoc);
       }
    }

   if (lcl && fdoc.isEditable()) {
      fdoc.setReadonly();
   }

   return fdoc;
}


@Override public void handleSaveRequest()
{
   Collection<BaleDocumentIde> docs;
   synchronized (file_documents) {
      docs = new ArrayList<BaleDocumentIde>(file_documents.values());
    }
   for (BaleDocumentIde doc : docs) {
      if (doc.canSave()) doc.save();
   }
}


@Override public void handleSaveDone()				{ }



@Override public void handleCommitRequest()
{
   Collection<BaleDocumentIde> docs;
   synchronized (file_documents) {
      docs = new ArrayList<>(file_documents.values());
    }
   for (BaleDocumentIde doc : docs) {
      if (doc.canSave()) doc.commit();
    }
   for (BaleDocumentIde doc : docs) {
      if (doc.canSave()) doc.compile();
    }
}




@Override public void handleCheckpointRequest()
{
   Collection<BaleDocumentIde> docs;
   synchronized (file_documents) {
      docs = new ArrayList<BaleDocumentIde>(file_documents.values());
    }
   for (BaleDocumentIde doc : docs) {
      if (doc.canSave()) doc.checkpoint();
   }
}




@Override public boolean handleQuitRequest()
{
   return true;
}


@Override public void handlePropertyChange()
{
   bale_attributes.reload();
}



@Override public void handleFileStarted(String proj,String file)
{ }

@Override public void handleFileChanged(String proj,String file)
{ }

@Override public void handleFileAdded(String proj,String file)
{ }

@Override public void handleProjectOpened(String proj)
{ }

@Override public void handleFileRemoved(String proj,String file)
{
   File f = new File(file);

   synchronized (file_documents) {
      BaleDocumentIde fdoc = file_documents.remove(f);
      if (fdoc != null) {
	 fdoc.dispose();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Annotation methods							*/
/*										*/
/********************************************************************************/

void addAnnotationListener(BaleAnnotationListener bal)
{
   annot_listeners.add(bal);
}


void removeAnnotationListener(BaleAnnotationListener bal)
{
   annot_listeners.remove(bal);
}


List<BaleAnnotation> getAnnotations(BaleDocument bd)
{
   List<BaleAnnotation> rslt = new ArrayList<BaleAnnotation>();
   synchronized (active_annotations) {
      for (BaleAnnotation ba : active_annotations) {
	 if (ba.getFile() == null) continue;
	 if (bd == null || ba.getFile().equals(bd.getFile())) rslt.add(ba);
       }
    }

   return rslt;
}



/**
 *	Add a new annotation
 **/

public void addAnnotation(BaleAnnotation ba)
{
   synchronized (active_annotations) {
      if (active_annotations.add(ba)) {
	 for (BaleAnnotationListener bal : annot_listeners) {
	    bal.annotationAdded(ba);
	  }
       }
    }
}



/**
 *	Remove an annotation
 **/

public void removeAnnotation(BaleAnnotation ba)
{
   synchronized (active_annotations) {
      if (active_annotations.remove(ba)) {
	 for (BaleAnnotationListener bal : annot_listeners) {
	    bal.annotationRemoved(ba);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Popup/Hover context methods						*/
/*										*/
/********************************************************************************/

/**
 *	Add a context listener for popup and hover events
 **/

public void addContextListener(BaleContextListener bcl)
{
   context_listeners.add(bcl);
}


/**
 *	Remove a context listener for popup and hover events
 **/

public void removeContextListener(BaleContextListener bcl)
{
   context_listeners.remove(bcl);
}



BudaBubble getContextHoverBubble(BaleContextConfig cfg)
{
   for (BaleContextListener bcl : context_listeners) {
      BudaBubble bb = bcl.getHoverBubble(cfg);
      if (bb != null) return bb;
    }

   return null;
}



void addContextMenuItems(BaleContextConfig cfg,JPopupMenu menu)
{
   LinkedList<BaleContextListener> lstn = new LinkedList<>();
   for (BaleContextListener bcl : context_listeners) {
      lstn.addFirst(bcl);
    }
   for (BaleContextListener bcl : lstn) {
      bcl.addPopupMenuItems(cfg,menu);
    }
}



String getContextToolTip(BaleContextConfig cfg)
{
   StringBuffer buf = null;

   for (BaleContextListener bcl : context_listeners) {
      String t = bcl.getToolTipHtml(cfg);
      if (t != null) {
	 if (buf == null) {
	    buf = new StringBuffer();
	    buf.append("<html><body>");
	  }
	 else buf.append("<br>");
	 buf.append(t);
       }
    }

   if (buf == null) return null;
   buf.append("\n<br>\n");

   return buf.toString();
}



void noteEditorAdded(BaleWindow win)
{
   for (BaleContextListener bcl : context_listeners) {
      bcl.noteEditorAdded(win);
    }
}


void noteEditorRemoved(BaleWindow win)
{
   for (BaleContextListener bcl : context_listeners) {
      bcl.noteEditorRemoved(win);
    }
}



/********************************************************************************/
/*										*/
/*	Handle refactoring edit application					*/
/*										*/
/********************************************************************************/

public void applyEdits(Element edits)
{
   if (edits == null) return;

   BaleApplyEdits bae = new BaleApplyEdits();
   bae.applyEdits(edits);
}


public boolean applyEdits(File file,Element edits)
{
   if (file == null || edits == null) return false;

   BaleDocument bd = getDocument(null,file);
   if (bd == null) return false;

   bd.setupDummyEditor();

   BaleApplyEdits bae = new BaleApplyEdits(bd);
   bae.applyEdits(edits);

   return true;
}


JTextComponent getTextComponent(BaleDocument doc)
{
   if (doc == null) return null;
   
   BudaBubble use = null;
   for (BudaBubble bb : buda_root.getCurrentBubbleArea().getBubbles()) {
      Document d = bb.getContentDocument();
      if (d == null) continue;
      if (d == doc) {
         use = bb;
         break;
       }
      else if (d instanceof BaleDocument) {
         BaleDocument bd = (BaleDocument) d;
         if (bd.getBaseEditDocument() == doc) use = bb;
       }
    }
   if (use == null) return null;
   
   Component c = use.getContentPane();
   if (c instanceof BaleFragmentEditor) {
      BaleFragmentEditor bfe = (BaleFragmentEditor) c;
      return bfe.getEditor();
    }
   
   return null;
}

/********************************************************************************/
/*										*/
/*	Methods to handle reset 						*/
/*										*/
/********************************************************************************/

List<BaleRegion> getFragmentRegions(BaleDocument doc)
{
   List<BumpLocation> locs = null;
   String proj = doc.getProjectName();
   String nam = doc.getFragmentName();
   File fil = doc.getFile();
   if (nam == null) return null;
   int idx = nam.lastIndexOf(".");
   String cnam = nam;
   if (idx > 0) cnam = nam.substring(0,idx);

   switch (doc.getFragmentType()) {
      case METHOD :
	 locs = bump_client.findMethod(proj,nam,false);
	 break;
      case CLASS :
	 locs = bump_client.findClassDefinition(proj,nam);
	 break;
      case FILE :
	 locs = bump_client.findCompilationUnit(proj,fil,cnam);
	 break;
      case FIELDS :
	 locs = bump_client.findFields(proj,fil,cnam);
	 break;
      case STATICS :
	 locs = bump_client.findClassInitializers(proj,cnam,fil,false);
	 break;
      case MAIN :
	 locs = bump_client.findClassInitializers(proj,cnam,fil,true);
	 break;
      case HEADER :
	 locs = bump_client.findClassPrefix(proj,fil,cnam);
	 break;
      default :
	 BoardLog.logE("BALE","Unknown fragment type : " + doc.getFragmentType());
	 break;
    }

   if (locs == null) return null;

   List<BaleRegion> rgns = getRegionsFromLocations(locs);

   return rgns;
}




/********************************************************************************/
/*										*/
/*	Methods to create editor from a list of Bump Locations			*/
/*										*/
/********************************************************************************/

private BaleFragmentEditor getEditorFromLocations(List<BumpLocation> locs)
{
   if (locs == null || locs.size() == 0) return null;

   BumpLocation loc0 = locs.get(0);

   BaleFragmentType ftyp;
   String fragname = loc0.getSymbolName();

   switch (loc0.getSymbolType()) {
      case FUNCTION :
      case CONSTRUCTOR :
	 ftyp = BaleFragmentType.METHOD;
	 String prms = loc0.getParameters();
	 if (prms != null) fragname += prms;
	 else fragname += "(...)";
	 break;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
	 ftyp = BaleFragmentType.CLASS;
	 break;
      case STATIC_INITIALIZER :
      case IMPORT :
      case EXPORT :
	 ftyp = BaleFragmentType.STATICS;
	 break;
      case MAIN_PROGRAM :
      case PROGRAM :
	 ftyp = BaleFragmentType.MAIN;
	 break;
      case ENUM_CONSTANT :
      case FIELD :
      case GLOBAL :
	 ftyp = BaleFragmentType.FIELDS;
	 int idx = fragname.lastIndexOf(".");
	 fragname = fragname.substring(0,idx) + ".< FIELDS >";
	 break;
      case MODULE :
	 ftyp = BaleFragmentType.FILE;
	 fragname = loc0.getFile().getPath();
	 break;
      default :
	 return null;
    }

   return getEditorFromLocations(locs,ftyp,fragname);
}



private List<BaleRegion> getRegionsFromLocations(List<BumpLocation> locs)
{
   if (locs == null || locs.size() == 0) return null;

   BumpLocation loc0 = locs.get(0);
   String proj = loc0.getSymbolProject();
   File f = loc0.getFile();
   BaleDocumentIde fdoc = getDocument(proj,f);
   BoardLanguage lang = BoardSetup.getSetup().getLanguage();

   Segment s = new Segment();
   try {
      fdoc.getText(0,fdoc.getLength(),s);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Bad document: " + e);
      return null;
    }

   List<BaleRegion> rgns = new ArrayList<BaleRegion>();
   BaleRegion lastrgn = null;

   for (BumpLocation bl : locs) {
      if (bl.getDefinitionOffset() < 0) continue;
      File f1 = bl.getFile();
      if (f1 != null && !f1.equals(f)) continue;

      int soffset = fdoc.mapOffsetToJava(bl.getDefinitionOffset());
      int eoffset = fdoc.mapOffsetToJava(bl.getDefinitionEndOffset())+1;
      if (soffset >= s.length()) continue;

      if (lang == BoardLanguage.JS) {
	 // handle VAR which might be missing in a declaration location context
	 int decloff = soffset;
	 while (decloff >= 3) {
	    if (!Character.isWhitespace(s.charAt(decloff-1))) {
	       if (s.charAt(decloff-1) == 'r' && s.charAt(decloff-2) == 'a' &&
		     s.charAt(decloff-3) == 'v' &&
		     (decloff == 3 || Character.isWhitespace(s.charAt(decloff-4)))) {
		   soffset = decloff-3;
		}
	       break;
	     }
	    --decloff;
	  }
       }
      // extend the logical regions and note if it ends with a new line
      while (soffset > 0) {
	 if (s.charAt(soffset-1) == '\n') break;
	 else if (!Character.isWhitespace(s.charAt(soffset-1))) break;
	 --soffset;
       }
      // and remove initial blank lines if necessary
      while (soffset > 0 && soffset < s.length() && s.charAt(soffset) == '\n') ++soffset;

      boolean havecmmt = false;
      if (eoffset > 0 && eoffset < s.length() && s.charAt(eoffset-1) != '\n') {        // extend if we don't end on eol
	 while (eoffset < fdoc.getLength()) {
	    if (s.charAt(eoffset) == '\n') {
	       ++eoffset;
	       break;
	     }
	    else if (havecmmt) ;
	    else if (Character.isWhitespace(s.charAt(eoffset))) ;
	    else if ((lang == BoardLanguage.JAVA || lang == BoardLanguage.JAVA_IDEA) &&
		  s.charAt(eoffset) == '/' && s.charAt(eoffset+1) == '/') {
	       havecmmt = true;
	     }
	    else if (lang == BoardLanguage.JS &&
		  s.charAt(eoffset) == '/' && s.charAt(eoffset+1) == '/') {
	       havecmmt = true;
	     }
	    else if (lang == BoardLanguage.JS && s.charAt(eoffset) == ';') ;
	    else if (lang == BoardLanguage.REBUS &&
		  s.charAt(eoffset) == '/' && s.charAt(eoffset+1) == '/') {
	       havecmmt = true;
	     }
	    else if (lang == BoardLanguage.PYTHON && s.charAt(eoffset) == '#') {
	       havecmmt = true;
	     }
	    else break;
	    ++eoffset;
	  }
       }

      boolean haveeol;
      if (eoffset > s.length()) {
	 haveeol = true;
	 eoffset = s.length();
       }
      else haveeol = s.charAt(eoffset-1) == '\n';

      BaleRegion br = null;

      try {
	 Position spos = BaleStartPosition.createStartPosition(fdoc,soffset);
	 Position epos = fdoc.createPosition(eoffset);
	 br = new BaleRegion(spos,epos,haveeol);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Bad location from eclipse for fragment: " + e);
	 continue;
       }

      if (lastrgn != null) {
	 BaleRegion nbr = mergeRegions(lastrgn,br,s);
	 if (nbr != null) {
	    rgns.remove(lastrgn);
	    br = nbr;
	  }
       }

      rgns.add(br);
      lastrgn = br;
    }

   return rgns;
}



private BaleFragmentEditor getEditorFromLocations(List<BumpLocation> locs,
						     BaleFragmentType ftyp,
						     String fragname)
{
   if (locs == null || locs.size() == 0) return null;

   List<BaleRegion> rgns = getRegionsFromLocations(locs);
   if (rgns == null || rgns.size() == 0) return null;

   BumpLocation loc0 = locs.get(0);
   String proj = loc0.getSymbolProject();
   File f = loc0.getFile();
   BaleDocumentIde fdoc = getDocument(proj,f);

   BaleFragmentEditor bfe = new BaleFragmentEditor(proj,f,fragname,fdoc,ftyp,rgns);

   noteEditorAdded(bfe);

   return bfe;
}



BaleFragmentEditor getEditorFromRegions(String proj,File f,String fragname,
					   List<BaleRegion> locs,
					   BaleFragmentType ftyp)
{
   if (locs == null || locs.size() == 0) return null;

   List<BaleRegion> rgns = new ArrayList<BaleRegion>();
   BaleRegion lastrgn = null;

   BaleDocumentIde fdoc = getDocument(proj,f);

   Segment s = new Segment();
   try {
      fdoc.getText(0,fdoc.getLength(),s);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Bad document: " + e);
      return null;
    }

   for (BaleRegion br : locs) {
      if (lastrgn != null) {
	 BaleRegion nbr = mergeRegions(lastrgn,br,s);
	 if (nbr != null) {
	    rgns.remove(lastrgn);
	    br = nbr;
	  }
       }
      else {
	 int soff = br.getStart();
	 int eoff = br.getEnd();
	 int soff1 = soff;
	 while (soff1 > 0) {
	    if (s.charAt(soff1) == '\n') {
	       ++soff1;
	       break;
	    }
	    --soff1;
	 }
	 int eoff1 = eoff;
	 while (eoff1 < s.length()) {
	    if (s.charAt(eoff1) == '\n') {
	       ++eoff1;
	       break;
	    }
	    else if (Character.isWhitespace(s.charAt(eoff1))) {
	       --eoff1;
	    }
	    else {
	       eoff1 = eoff;
	       break;
	    }
	 }
	 if (soff1 != soff || eoff1 != eoff) {
	    try {
	       br = fdoc.createDocumentRegion(soff1, eoff1, true);
	    }
	    catch (BadLocationException ex) { }
	 }
      }

      rgns.add(br);
      lastrgn = br;
    }

   BaleFragmentEditor bfe = new BaleFragmentEditor(proj,f,fragname,fdoc,ftyp,rgns);

   noteEditorAdded(bfe);

   return bfe;
}





private BaleRegion mergeRegions(BaleRegion r1,BaleRegion r2,Segment txt)
{
   int s1 = r1.getStart();
   int e1 = r1.getEnd();
   int s2 = r2.getStart();
   int e2 = r2.getEnd();

   if (e2 < s1) {		// second ends before first, try opposite order
      return mergeRegions(r2,r1,txt);
    }
   if (s2 < s1) return null;
   int delta = s2-e1;
   if (delta > 4) return null;	// not even close -- don't merge

   boolean merge = false;
   if (e1 > s2 || e1 == s2 || e1+1 == s2) merge = true; 	// easy cases
   else {
      merge = true;
      for (int i = e1; merge && i < s2; ++i) {
	 char c = txt.charAt(i);
	 // Might want to allow comments here (with a larger delta)
	 if (!Character.isWhitespace(c)) merge = false;
       }
    }

   if (!merge) return null;

   BaleRegion br = null;

   if (e2 < e1) br = r1;
   else br = new BaleRegion(r1.getStartPosition(),r2.getEndPosition(),r2.includesEol());

   return br;
}




/********************************************************************************/
/*										*/
/*	Class to handle hovering over problems					*/
/*										*/
/********************************************************************************/

private static class ProblemHover implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg)	{ return null; }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      if (cfg.inAnnotationArea()) return;
      BaleDocument bd = (BaleDocument) cfg.getDocument();
      List<BumpProblem> probs = bd.getProblemsAtLocation(cfg.getOffset());
      if (probs != null) {
	 for (BumpProblem bp : probs) {
	    menu.add(new QuickFix(cfg.getEditor(),bp));
	  }
       }
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      BaleDocument bd = (BaleDocument) cfg.getDocument();
      List<BumpProblem> probs = bd.getProblemsAtLocation(cfg.getOffset());
      if (probs == null || probs.size() == 0) return null;

      StringBuffer buf = new StringBuffer();
      for (BumpProblem bp : probs) {
	 buf.append("<p>");
	 buf.append(bp.getMessage());
       }
      return buf.toString();
    }

   @Override public void noteEditorAdded(BaleWindow cfg) { }
   @Override public void noteEditorRemoved(BaleWindow cfg) { }

}	// end of inner class ProblemHover




/********************************************************************************/
/*										*/
/*	Quick fix button action 						*/
/*										*/
/********************************************************************************/

static class QuickFix extends AbstractAction {

   private Component for_editor;
   private transient BumpProblem for_problem;

   private static final long serialVersionUID = 1;

   QuickFix(Component root,BumpProblem bp) {
      super("Quick Fix: " + fixMessage(bp));
      for_problem = bp;
      for_editor = root;
    }

   @Override public void actionPerformed(ActionEvent e) {
      List<BaleFixer> fixes = new ArrayList<BaleFixer>();
      List<BumpFix> fixlist = for_problem.getFixes();
      if (fixlist != null) {
         for (BumpFix bf : fixlist) {
            BaleFixer fixer = new BaleFixer(for_problem,bf);
            if (fixer.isValid()) fixes.add(fixer);
         }
      }
      if (fixes.isEmpty()) {
         JOptionPane.showMessageDialog(for_editor,"No quick fixes available");
         return;
       }
   
      BaleFixer fix = null;
      Collections.sort(fixes);
      Object [] fixalts = fixes.toArray();
      fix = (BaleFixer) JOptionPane.showInputDialog(for_editor,"Select Quick Fix",
        					       "Quick Fix Selector",
        					       JOptionPane.QUESTION_MESSAGE,
        					       null,fixalts,fixes.get(0));
      if (fix == null) return;
   
      fix.actionPerformed(e);
      BoardMetrics.noteCommand("BALE","QuickFixOption");
    }

}	// end of inner class QuickFix


private static String fixMessage(BumpProblem bp)
{
   String msg = bp.getMessage();
   int idx = msg.indexOf(". ");
   if (idx >= 0) msg = msg.substring(0,idx);
   return msg;
}



/********************************************************************************/
/*										*/
/*	Import format handling							*/
/*										*/
/********************************************************************************/

private static class FormatImporter implements BudaConstants.ButtonListener {

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fc.setDialogTitle("Select Saved Eclipse/Idea XML Formats");
      int sts = fc.showOpenDialog(BudaRoot.findBudaRoot(bba));
      if (sts != JFileChooser.APPROVE_OPTION) return;
      File f = fc.getSelectedFile();
      if (f == null) return;
      Element xml = IvyXml.loadXmlFromFile(f);
      if (xml == null) return;
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("OPTIONS");
      Element n1 = xml;
      if (!IvyXml.isElement(xml,"profiles")) n1 = IvyXml.getChild(xml,"profiles");
      if (n1 != null) {
         Element n2 = IvyXml.getChild(n1,"profile");
         for (Element n3 : IvyXml.children(n2,"setting")) {
            xw.begin("OPTION");
            xw.field("NAME",IvyXml.getAttrString(n3,"id"));
            xw.field("VALUE",IvyXml.getAttrString(n3,"value"));
            xw.end("OPTION");
          }
       }
      else if (IvyXml.isElement(xml,"code_scheme")) {
         for (Element n4 : IvyXml.elementsByTag(xml,"option")) {
            xw.begin("IDEAOPTION");
            xw.field("NAME",IvyXml.getAttrString(n4,"name"));
            xw.field("VALUE",IvyXml.getAttrString(n4,"value"));
            xw.end("IDEAOPTION");
          }
       }
      xw.end("OPTIONS");
      
      bump_client.loadPreferences(null,xw.toString());
      String v = BALE_PROPERTIES.getProperty("indent.tabulation.size");
      if (v == null) {
         v = BumpClient.getBump().getOption("org.eclipse.jdt.core.formatter.tabulation.size");
         if (v != null) {
            try {
               if (v != null) BaleTabHandler.setBaseTabSize(Integer.parseInt(v));
             }
            catch (NumberFormatException e) { }
          }
       }
      xw.close();
      format_time = System.currentTimeMillis();
    }

}	// end of inner class FormatImporter



/********************************************************************************/
/*										*/
/*	Import format from project                                              */
/*										*/
/********************************************************************************/

private static class ProjectFormatImporter implements BudaConstants.ButtonListener {

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
   BoardProperties bp = BoardProperties.getProperties("System");
   String rec = bp.getProperty("edu.brown.cs.bubbles.recents");
   Map<String,File> recs = new TreeMap<>();
   StringTokenizer tok = new StringTokenizer(rec,";");
   String cur = BoardSetup.getSetup().getDefaultWorkspace();
   while (tok.hasMoreTokens()) {
      String s = tok.nextToken();
      s = s.trim();
      if (s.equals(cur)) continue;
      int idx = s.lastIndexOf(File.separator);
      String s1 = s;
      if (idx > 0) s1 = s.substring(idx+1);
      if (s.length() == 0) continue;
      File f1 = new File(s);
      File f2 = new File(f1,".metadata");
      File f3 = new File(f2,".plugins");
      File f4 = new File(f3,"org.eclipse.core.runtime");
      File f5 = new File(f4,".settings");
      File f6 = new File(f5,"org.eclipse.jdt.core.prefs");
      if (!f6.exists()) continue;
      recs.put(s1,f6);
    }
   if (recs.size() == 0) return;
   String [] opts = new String[recs.size()];
   opts = recs.keySet().toArray(opts);
   Object sel = JOptionPane.showInputDialog(BudaRoot.findBudaRoot(bba),
         "From Project","Select Project to Import From",
         JOptionPane.QUESTION_MESSAGE, null, opts,opts[0]);
   if (sel == null) return;      
   File path = recs.get((String) sel);
   if (path == null) return;
   Properties props = new Properties();
   try (FileInputStream fr = new FileInputStream(path)) {
      props.load(fr);
    }
   catch (IOException e) { 
      return;
    }
   Element xml = null;
   if (xml == null) return;
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("OPTIONS");
   Element n1 = xml;
   if (!IvyXml.isElement(xml,"profiles")) n1 = IvyXml.getChild(xml,"profiles");
   if (n1 != null) {
      Element n2 = IvyXml.getChild(n1,"profile");
      for (Element n3 : IvyXml.children(n2,"setting")) {
         xw.begin("OPTION");
         xw.field("NAME",IvyXml.getAttrString(n3,"id"));
         xw.field("VALUE",IvyXml.getAttrString(n3,"value"));
         xw.end("OPTION");
       }
    }
   else if (IvyXml.isElement(xml,"code_scheme")) {
      for (Element n4 : IvyXml.elementsByTag(xml,"option")) {
         xw.begin("IDEAOPTION");
         xw.field("NAME",IvyXml.getAttrString(n4,"name"));
         xw.field("VALUE",IvyXml.getAttrString(n4,"value"));
         xw.end("IDEAOPTION");
       }
    }
   xw.end("OPTIONS");
   bump_client.loadPreferences(null,xw.toString());
   
   String v = BALE_PROPERTIES.getProperty("indent.tabulation.size");
   if (v == null) {
      v = BumpClient.getBump().getOption("org.eclipse.jdt.core.formatter.tabulation.size");
      if (v != null) {
         try {
            if (v != null) BaleTabHandler.setBaseTabSize(Integer.parseInt(v));
          }
         catch (NumberFormatException e) { }
       }
    }
   xw.close();
   format_time = System.currentTimeMillis();
}

}	// end of inner class FormatImporter


}	// end of class BaleFactory




/* end of BaleFactory.java */
