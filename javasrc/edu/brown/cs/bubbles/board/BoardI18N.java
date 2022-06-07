/********************************************************************************/
/*                                                                              */
/*              BoardI18N.java                                                  */
/*                                                                              */
/*      Standard calls for handling internationalization                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.bubbles.board;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class BoardI18N implements BoardConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static String   LABEL_BUNDLE = "Labels";

private static Locale   use_locale = Locale.getDefault();

private static Map<String,ResourceBundle> known_bundles;


static {
   use_locale = Locale.getDefault();
   known_bundles = new HashMap<>();
   addBundle(LABEL_BUNDLE);
}


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

public static void setLocale(Locale lcl) 
{
   if (lcl == null) lcl = Locale.getDefault();
   use_locale = lcl;
   for (Map.Entry<String,ResourceBundle> ent : known_bundles.entrySet()) {
      String nm = ent.getKey();
      ent.setValue(ResourceBundle.getBundle(nm,lcl));
    }
}


public static void addBundle(String nm)
{
   known_bundles.put(nm,ResourceBundle.getBundle(nm,use_locale));
}


/********************************************************************************/
/*                                                                              */
/*      Handle User Interface Text mappings                                     */
/*                                                                              */
/********************************************************************************/

public static String getLabel(String key)
{
   return getString(LABEL_BUNDLE,key);
}


public static String getString(String bndl,String key)
{
   ResourceBundle bdl = known_bundles.get(bndl);
   if (bdl == null) {
      BoardLog.logX("BOARD","Bundle " + bndl + " unknown");
      return "???";      
    }
   
   String s = bdl.getString(key);
   if (s != null) return s;
   
   BoardLog.logX("BOARD","Unknown i18n string for " + bndl + "." + key);
   
   return "???";
}



}       // end of class BoardI18N




/* end of BoardI18N.java */

