/********************************************************************************/
/*										*/
/*		BuenoClassSetup.java						*/
/*										*/
/*	Compute initial imports and methods for a class 			*/
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.lang.reflect.Modifier;
import java.util.List;


class BuenoClassSetup implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BuenoProperties property_set;
private BumpClient	bump_client;
private String		class_name;
private String		package_name;
private String		full_class_name;
private boolean 	is_abstract;
private String		abstract_methods;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoClassSetup(BuenoProperties props,BuenoType what)
{
   property_set = props;
   class_name = props.getStringProperty(BuenoKey.KEY_NAME);
   package_name = props.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (package_name != null && package_name.length() > 0) {
      full_class_name = package_name + "." + class_name;
    }
   else full_class_name = class_name;

   if (what == BuenoType.NEW_INTERFACE) {
      is_abstract = true;
    }
   else {
      int mods = props.getModifiers();
      if (Modifier.isAbstract(mods)) is_abstract = true;
      else is_abstract = false;
    }

   abstract_methods = null;

   bump_client = BumpClient.getBump();
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void extendProperties()
{
   String [] ext = property_set.getExtends();
   if (ext != null) {
      for (String s : ext) addTypeReference(s);
    }
   String [] impl = property_set.getImplements();
   if (impl != null) {
      for (String s : impl) addTypeReference(s);
    }

   // get constructor code if needed for superclass

   if (abstract_methods != null) property_set.put(BuenoKey.KEY_CONTENTS,abstract_methods);
}


/********************************************************************************/
/*										*/
/*	Handle Type references							*/
/*										*/
/********************************************************************************/

private void addTypeReference(String typ)
{
   if (typ.equals(class_name) || typ.equals(full_class_name)) return;

   BumpLocation bl = findLocation(typ);
   if (bl == null) return;

   String nm = bl.getSymbolName();
   int idx = nm.lastIndexOf(".");
   String npfx = "";
   if (idx >= 0) {
      npfx = nm.substring(0,idx);
    }
   if (npfx != null && !npfx.equals(package_name) && !npfx.equals("java.lang") && !typ.equals(nm)) {
      boolean fnd = false;
      String imp = "import " + nm + ";";
      if (property_set.getImports() != null) {
	 for (String s : property_set.getImports()) {
	    if (s.equals(imp)) fnd = true;
	 }
      }
      if (!fnd) property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp);
    }

   if (!is_abstract) {
      String mpat = nm + ".*";
      // TODO: TRY TO AVOID USING BUMP HERE -- use BASS instead
      BuenoFactory bf = BuenoFactory.getFactory();
      List<BumpLocation> mthds = bf.findClassMethods(nm);
      if (mthds == null) {
	 mthds = bump_client.findMethods(bl.getProject(),mpat,false,true,false,true);
       }
      if (mthds != null) {
	 for (BumpLocation mbl : mthds) {
	    int mods = mbl.getModifiers();
	    if (Modifier.isAbstract(mods)) {
               String m = mbl.getSymbolName();
               int idx0 = m.lastIndexOf(".");
               if (idx0 > 0) m = m.substring(idx+1);
               if (m.startsWith("<") || m.contains("$")) continue;
	       String mtxt = getMethodCode(mbl);
	       if (mtxt != null) {
		  if (abstract_methods == null) {
		     StringBuffer buf = new StringBuffer();
		     buf.append("\n\n");
		     BuenoCreator bc = BuenoFactory.getFactory().getCreator();
		     BuenoProperties cprops = new BuenoProperties();
		     cprops.put(BuenoKey.KEY_COMMENT,"Abstract Method Implementations");
		     bc.setupMarquisComment(buf,cprops);
		     abstract_methods = buf.toString();
		   }
		  abstract_methods += mtxt + "\n\n";
		}
	     }
	  }
       }
    }
}




private BumpLocation findLocation(String typ)
{
   List<BumpLocation> locs = bump_client.findAllTypes(typ);

   if (locs == null || locs.size() == 0) return null;
   BumpLocation pref = null;
   String pfx = null;
   for (BumpLocation bl : locs) {
      String nm = bl.getSymbolName();
      int idx = nm.lastIndexOf(".");
      String npfx = "";
      if (idx >= 0) {
	 npfx = nm.substring(0,idx);
	 nm = nm.substring(idx+1);
       }
      if (!nm.equals(typ)) continue;
      boolean use = false;
      if (pref == null) use = true;
      else if (package_name != null && npfx.equals(package_name)) return bl;
      else if (package_name == null && npfx.equals("")) return bl;
      else if (npfx.startsWith("java.lang")) use = true;
      else if (npfx.startsWith("java.util")) use = true;
      else if (npfx.startsWith("java.io")) use = true;
      else if (npfx.startsWith("org.w3c.dom")) use = true;
      else if (npfx.startsWith("java") && pfx != null && !pfx.startsWith("java")) use = true;
      else if (npfx.startsWith("java") && pfx != null && pfx.startsWith("java") &&
	       npfx.length() < pfx.length()) use = true;
      if (use) {
	 pref = bl;
	 pfx = npfx;
      }
   }

   return pref;
}



private String getMethodCode(BumpLocation mloc)
{
   // modifiers and return values seem wrong
   // also too much white space before and/or after
   BuenoProperties props = new BuenoProperties();
   props.put(BuenoKey.KEY_RETURNS,mloc.getReturnType());
   props.put(BuenoKey.KEY_PROJECT,mloc.getProject());
   props.put(BuenoKey.KEY_PACKAGE,property_set.getStringProperty(BuenoKey.KEY_PACKAGE));
   props.put(BuenoKey.KEY_FILE,property_set.getStringProperty(BuenoKey.KEY_FILE));
   String mname = mloc.getSymbolName();
   int idx = mname.lastIndexOf(".");
   if (idx > 0) mname = mname.substring(idx+1);
   props.put(BuenoKey.KEY_NAME,mname);
   int mods = mloc.getModifiers();
   mods &= Modifier.PUBLIC|Modifier.PROTECTED;
   mods |= MODIFIER_OVERRIDES;
   props.put(BuenoKey.KEY_MODIFIERS,mods);
   String params = mloc.getParameters();
   if (params != null) {
      List<String> plist = BumpLocation.getParameterList(params);
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < plist.size(); ++i) {
         if (i > 0) buf.append(",");
         buf.append(plist.get(i));
         buf.append(" arg" + i);
       }
      props.put(BuenoKey.KEY_PARAMETERS,buf.toString());
    }

   BuenoCreator bc = BuenoFactory.getFactory().getCreator();
   StringBuffer buf = new StringBuffer();
   bc.setupMethod(buf,props);

   return buf.toString();
}



}	// end of class BuenoClassSetup




/* end of BuenoClassSetup.java */

