/********************************************************************************/
/*										*/
/*		BfixAdapterNewField.java					*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoKey;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoType;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.xml.IvyXml;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.w3c.dom.Element;


class BfixAdapterNewField extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private Map<File,FileData> file_map;

private static List<BfixErrorPattern> field_patterns;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterNewField()
{
   super("New Field Chorer");

   file_map = new HashMap<File,FileData>();
   if (field_patterns == null) {
      field_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"NEWFIELD")) {
         field_patterns.add(new BfixErrorPattern(cxml));
       }
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override  public void addChores(BfixCorrector corr,BumpProblem bp,List<BfixChore> rslts)
{
   String fld = getFieldName(corr,bp);
   if (fld == null) return;
   if (checkDone(corr,fld)) return;

   BfixChore chore1 = new GetterChore(corr,bp,fld);
   BfixChore chore2 = new SetterChore(corr,bp,fld);
   BfixChore chore3 = new InitializerChore(corr,bp,fld);

   rslts.add(chore1);
   rslts.add(chore2);
   rslts.add(chore3);
}






@Override public String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String fld = getFieldName(corr,bp);
   if (fld == null) return null;
   if (checkDone(corr,fld)) return null;

   return fld;
}



/********************************************************************************/
/*										*/
/*	Get new field name from error message					*/
/*										*/
/********************************************************************************/

private String getFieldName(BfixCorrector corr,BumpProblem bp)
{
   String msg = bp.getMessage();
   for (BfixErrorPattern pat : field_patterns) {
      String fld = pat.getMatchResult(msg);
      if (fld != null) return fld;
    }
   return null;
}



private boolean checkDone(BfixCorrector corr,String fld)
{
   FileData fd = getFileData(corr,fld);
   if (fd.isDone(fld)) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Field information management						*/
/*										*/
/********************************************************************************/

FieldData getDataForField(BfixCorrector corr,String field,boolean force)
{
   FileData fd = getFileData(corr,field);
   if (fd == null) return null;
   if (force) fd.invalidate();
   return fd.getFieldData(field);
}




/********************************************************************************/
/*										*/
/*	Chore to add getter for field						*/
/*										*/
/********************************************************************************/

private class GetterChore extends BfixChore {

   private String field_name;

   GetterChore(BfixCorrector corr,BumpProblem bp,String fld) {
      super("Create getter for " + fld,corr);
      field_name = fld;
    }

   @Override void execute() {
      BfixCorrector corr = getCorrector();
      if (corr == null) return;
   
      FieldData fd = getDataForField(corr,field_name,false);
      if (fd == null) return;
   
      BuenoProperties props = new BuenoProperties();
      String typ = fd.getTypeName();
   
      StringBuffer buf = new StringBuffer();
      if (typ.equals("boolean")) buf.append("is");
      else buf.append("get");
   
      boolean up = true;
      int idx = field_name.lastIndexOf(".");
      for (int i = idx+1; i < field_name.length(); ++i) {
         char c = field_name.charAt(i);
         if (c == '_') up = true;
         else {
            if (up) c = Character.toUpperCase(c);
            buf.append(c);
            up = false;
         }
      }
      props.put(BuenoKey.KEY_NAME,buf.toString());
      int mods = Modifier.PUBLIC;
      if (fd.isStatic()) mods |= Modifier.STATIC;
      props.put(BuenoKey.KEY_MODIFIERS,mods);
      props.put(BuenoKey.KEY_RETURNS,typ);
      props.put(BuenoKey.KEY_FIELD_NAME,field_name.substring(idx+1));
      props.put(BuenoKey.KEY_FIELD_TYPE,typ);
      props.put(BuenoKey.KEY_FILE,getFile());
      props.put(BuenoKey.KEY_CLASS_NAME,field_name.substring(0,idx));
   
      CharSequence cnts = BuenoFactory.getFactory().setupNew(BuenoType.NEW_GETTER,null,props);
   
      BfixSmartInsert inserter = corr.getInserter();
      BfixOrderNewElement newelt = new BfixOrderNewElement(buf.toString(),
            BumpSymbolType.FUNCTION,mods,cnts.toString());
      inserter.smartInsertSetup(newelt);
      inserter.smartInsertInsert(newelt);
   
      System.err.println("INSERT " + cnts);
    }

   @Override boolean validate(BfixCorrector corr,boolean force) {
      FieldData fd = getDataForField(corr,field_name,force);
      if (fd == null || !fd.isValid() || fd.getHasGetter()) 
         return false;
      return true;
    }

}	// end of inner class GetterChore





/********************************************************************************/
/*										*/
/*	Chore to add setter for field						*/
/*										*/
/********************************************************************************/

private class SetterChore extends BfixChore {

   private String field_name;

   SetterChore(BfixCorrector corr,BumpProblem bp,String fld) {
      super("Create setter for " + fld,corr);
      field_name = fld;
    }

   @Override void execute() {
      BfixCorrector corr = getCorrector();
      if (corr == null) return;
   
      FieldData fd = getDataForField(corr,field_name,false);
      if (fd == null) return;
      String type = fd.getTypeName();
   
      StringBuffer buf = new StringBuffer();
      buf.append("set");
      boolean up = true;
      int idx = field_name.lastIndexOf(".");
      for (int i = idx+1; i < field_name.length(); ++i) {
         char c = field_name.charAt(i);
         if (c == '_') up = true;
         else {
            if (up) c = Character.toUpperCase(c);
            buf.append(c);
            up = false;
          }
       }
   
      BuenoProperties props = new BuenoProperties();
      props.put(BuenoKey.KEY_NAME,buf.toString());
      int mods = Modifier.PUBLIC;
      if (fd.isStatic()) mods |= Modifier.STATIC;
      props.put(BuenoKey.KEY_MODIFIERS,mods);
      props.put(BuenoKey.KEY_FIELD_NAME,field_name.substring(idx+1));
      props.put(BuenoKey.KEY_CLASS_NAME,field_name.substring(0,idx));
      props.put(BuenoKey.KEY_FILE,getFile());
      props.put(BuenoKey.KEY_FIELD_TYPE,type);
   
      CharSequence cnts = BuenoFactory.getFactory().setupNew(BuenoType.NEW_SETTER,null,props);
   
      BfixSmartInsert inserter = corr.getInserter();
      BfixOrderNewElement newelt = new BfixOrderNewElement(buf.toString(),
            BumpSymbolType.FUNCTION,mods,cnts.toString());
      inserter.smartInsertSetup(newelt);
      inserter.smartInsertInsert(newelt);
   
      System.err.println("INSERT " + cnts);
    }

   @Override boolean validate(BfixCorrector corr,boolean force) {
      FieldData fd = getDataForField(corr,field_name,force);
      if (fd == null || !fd.isValid() || fd.getHasSetter()) 
         return false;
      return true;
    }

}	// end of inner class SetterChore



/********************************************************************************/
/*										*/
/*	Chore to add initializer for field					*/
/*										*/
/********************************************************************************/

private class InitializerChore extends BfixChore {

   private String field_name;

   InitializerChore(BfixCorrector corr,BumpProblem bp,String fld) {
      super("Add initializer to constructor for " + fld,corr);
      field_name = fld;
    }

   @Override void execute() {
      BfixCorrector corr = getCorrector();
      if (corr == null) return;
      FieldData fd = getDataForField(corr,field_name,false);
      if (fd == null) return;
      int idx = field_name.lastIndexOf(".");
      String fieldname = field_name.substring(idx+1);
      String type = fd.getTypeName();
      String value = "null";
      switch (type) {
	 case "int" :
	 case "short" :
	 case "long" :
	 case "byte" :
	 case "char" :
	 case "double" :
	 case "float" :// do the work here
	    value = "0";
	    break;
	 case "boolean" :
	    value = "false";
	    break;
       }
      StringBuffer cnts = new StringBuffer();
      cnts.append(fieldname);
      cnts.append(" = ");
      cnts.append(value);
      cnts.append(";");
      String cnt = cnts.toString();
      System.err.println("INSERT " + cnt);
    }

   @Override boolean validate(BfixCorrector corr,boolean force) {
      FieldData fd = getDataForField(corr,field_name,force);
      if (fd == null || !fd.isValid() || fd.getHasIniter()) return false;
      return true;
    }

}	// end of inner class InitializerChore



/********************************************************************************/
/*										*/
/*	Information about a field and its uses					*/
/*										*/
/********************************************************************************/

private FileData getFileData(BfixCorrector corr,String field)
{
   if (corr == null) return null;

   File f = corr.getEditor().getWindowDocument().getFile();
   FileData fd = null;
   synchronized (file_map) {
      fd = getFileData(f);
      if (fd == null) {
	 int idx = field.lastIndexOf(".");
	 String cls = field.substring(0,idx);
	 String proj = corr.getEditor().getWindowDocument().getProjectName();
	 fd = new FileData(f,proj,cls);
	 file_map.put(f,fd);
       }
    }
   if (fd != null) fd.useCorrector(corr);

   return fd;
}


private FileData getFileData(File f)
{
   synchronized (file_map) {
       return file_map.get(f);
    }
}



private static class FileData {

   private File for_file;
   private String for_project;
   private String for_class;
   private Map<String,FieldData> field_data;
   private Set<String> fields_done;
   private Map<BfixCorrector,Boolean> used_correctors;
   private long last_updated;

   FileData(File f,String proj,String cls) {
      for_file = f;
      for_project = proj;
   
      // handle inner classes -- we want the outer class
   
      int idx = cls.indexOf("$");
      if (idx >= 0) cls = cls.substring(0,idx);
      String fnm = f.getName();
      int idx1 = fnm.lastIndexOf(".");
      if (idx1 >= 0) fnm = fnm.substring(0,idx1);
      if (!cls.endsWith(fnm) && cls.contains(fnm)) {
         int idx2 = cls.indexOf(fnm);
         int idx3 = cls.indexOf(".",idx2);
         cls = cls.substring(0,idx3);
       }
   
      for_class = cls;
      field_data = new HashMap<String,FieldData>();
      fields_done = new HashSet<String>();
      used_correctors = new WeakHashMap<BfixCorrector,Boolean>();
      last_updated = 0;
    }

   void useCorrector(BfixCorrector corr) {
      used_correctors.put(corr,true);
    }


   void invalidate() {	
      last_updated = 0;
    }

   boolean isDone(String field) {
      field = getFieldName(field);
      if (fields_done.add(field)) return false;
      return true;
    }

   FieldData getFieldData(String field) {
      field = getFieldName(field);
      validate();
      return field_data.get(field);
    }

   synchronized void validate() {
      long now = System.currentTimeMillis();
      if (now - last_updated < 300000) return;
      last_updated = now;
   
      Set<FieldData> tocheck = new HashSet<FieldData>(field_data.values());
   
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> locs = bc.findFields(for_project,for_class,false,true,true);
      if (locs == null) return;
      List<BumpLocation> loc1 = null;
      List<BumpLocation> loc2 = null;
      for (BumpLocation loc : locs) {
         if (loc.getFile() == null || !loc.getFile().equals(for_file)) continue;
         int off = loc.getOffset();
         loc1 = bc.findRWReferences(for_project,loc.getFile(),off,off,false,5000);
         loc2 = bc.findRWReferences(for_project,loc.getFile(),off,off,true,5000);
         String nm0 = loc.getSymbolName();
         if (nm0 == null) continue;
         String nm = getFieldName(nm0);
         FieldData fd = field_data.get(nm);
         if (fd == null) {
            fd = new FieldData(for_class + "." + nm);
            field_data.put(nm,fd);
          }
         tocheck.remove(fd);
         fd.update(loc,loc1,loc2);
       }
      for (FieldData fd : tocheck) fd.clear();
    }

   private String getFieldName(String field) {
      if (field == null) return null;
      if (field.startsWith(for_class)) {
	 int idx = for_class.length() + 1;
	 field = field.substring(idx);
       }
      else {
	 String pat = "." + for_class + ".";
	 int idx = field.indexOf(pat);
	 if (idx > 0) {
	    int ln = pat.length();
	    field = field.substring(idx+ln);
	  }
       }
      return field;
    }

}	// end of inner class FileData




private static class FieldData {

   private String field_name;
   private String field_proper;
   private BumpLocation def_location;
   private boolean have_getter;
   private boolean have_setter;
   private boolean have_initer;

   FieldData(String field) {
      field_name = field;
      int idx = field.lastIndexOf(".");
      field_proper = field_name.substring(idx+1);
      def_location = null;
      have_getter = false;
      have_setter = false;
      have_initer = false;
    }

   void update(BumpLocation def,List<BumpLocation> rds,List<BumpLocation> wts) {
      def_location = def;
      have_getter = false;
      have_setter = false;
      have_initer = false;
      if (rds != null) {
	 for (BumpLocation rloc : rds) {
	    String where = rloc.getSymbolName();
	    if (where == null) return;
	    int idx = where.lastIndexOf(".");
	    if (idx > 0) where = where.substring(idx+1);
	    where = where.toLowerCase();
	    if (where.startsWith("get") || where.startsWith("is")) have_getter = true;
	    else if (where.equalsIgnoreCase(field_proper)) have_getter = true;
	 }
      }
      if (wts != null) {
	 for (BumpLocation wloc : wts) {
	    String where = wloc.getSymbolName();
	    if (where == null) continue;
	    int idx = where.lastIndexOf(".");
	    if (idx > 0) where = where.substring(idx+1);
	    where = where.toLowerCase();
	    if (wloc.getSymbolType() == BumpSymbolType.CONSTRUCTOR) have_initer = true;
	    else if (wloc.getSymbolType() == BumpSymbolType.FIELD) have_initer = true;
	    else if (where.startsWith("set")) have_setter = true;
	 }
      }
   }

   void clear() {
      def_location = null;
      have_getter = false;
      have_setter = false;
      have_initer = false;
   }

   boolean getHasGetter()			{ return have_getter; }
   boolean getHasSetter()			{ return have_setter; }
   boolean getHasIniter()			{ return have_initer; }
   boolean isValid()				{ return def_location != null; }
   String getTypeName() 			{ return def_location.getReturnType(); }
   boolean isStatic() {
      return Modifier.isStatic(def_location.getModifiers());
   }

}	// end of inner class FieldData



}	// end of class BfixAdapterNewField




/* end of BfixAdapterNewField.java */

