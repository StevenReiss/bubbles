/********************************************************************************/
/*                                                                              */
/*              BaleLanguageKitDefault.java                                     */
/*                                                                              */
/*      Generic language kit for BaleEditorKit                                  */
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



package edu.brown.cs.bubbles.bale;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.text.Keymap;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.swing.SwingKey;
import edu.brown.cs.ivy.xml.IvyXml;

class BaleLanguageKitDefault implements BaleConstants, BaleConstants.BaleLanguageKit
{




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BaleHinter      bale_hinter;

private Map<String,String> content_map;
private Set<String>     post_content;

private Action []       local_actions;
private String          command_base;
private Map<String,Action> action_keys;

private static Map<BoardLanguage,BaleLanguageKitDefault> kit_map = new HashMap<>();





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

static BaleLanguageKit getKitForLanguage(BoardLanguage lang)
{
   synchronized (kit_map) {
      BaleLanguageKitDefault kit = kit_map.get(lang);
      if (kit == null) {
         kit = new BaleLanguageKitDefault(lang);
         kit_map.put(lang,kit);
       }
      return kit;
    }
}


private BaleLanguageKitDefault(BoardLanguage lang)
{
   Element xml = BumpClient.getBump().getLanguageData(lang);
   
   String lng = IvyXml.getAttrString(xml,"NAME");
   command_base = lng.toUpperCase() + "EDIT";

   content_map = new HashMap<>();
   Element exml = IvyXml.getChild(xml,"EDITING");
   Element kxml = IvyXml.getChild(exml,"KEYPAIRS");
   for (Element k : IvyXml.children(kxml,"KEY")) {
      String s = IvyXml.getAttrString(k,"START");
      String e = IvyXml.getAttrString(k,"END");
      content_map.put(s,e);
    }
   post_content = new HashSet<>();
   for (String val : content_map.values()) {
      for (int i = 0; i < val.length(); ++i) {
         String c = val.substring(i,i+1);
         post_content.add(c);
       }
    }
   
   bale_hinter = null;
   String hintclass = IvyXml.getTextElement(exml,"HINTER");
   if (hintclass != null) {
      try {
         Class<?> cls = Class.forName(hintclass);
         bale_hinter = (BaleHinter) cls.getConstructor().newInstance();
       }
      catch (Throwable t) {
         BoardLog.logE("BALE","Problem setting up language hinter " + hintclass,t);
       }
    }
   
   List<Action> actlst = new ArrayList<>();
   action_keys = new HashMap<>();
   for (Element axml : IvyXml.children(exml,"ACTION")) {
      String clsnm = IvyXml.getAttrString(axml,"CLASS");
      String fldnm = IvyXml.getAttrString(axml,"FIELD");
      String keynm = IvyXml.getAttrString(axml,"KEY");
      try {
         Class<?> cls = Class.forName(clsnm);
         Action act = null;
         if (fldnm == null) {
            act = (Action) cls.getConstructor().newInstance();
          }
         else {
           Field fld = cls.getField(fldnm); 
           act = (Action) fld.get(null);
          }
         if (act != null) {
            actlst.add(act);
            if (keynm != null) action_keys.put(keynm,act);
          }
       }
      catch (Throwable t) {
         BoardLog.logE("BALE","Problem loading action " + clsnm + " " + fldnm,t);
       }
    }
   local_actions = new Action[actlst.size()];
   local_actions = actlst.toArray(local_actions);
}



/********************************************************************************/
/*										*/
/*	LanguageKit methods					                */
/*										*/
/********************************************************************************/

@Override public Action [] getActions()
{
   return local_actions;
}


@Override public Keymap getKeymap(Keymap base)
{
   for (Map.Entry<String,Action> ent : action_keys.entrySet()) {
      String k = ent.getKey();
      Action a = ent.getValue();
      SwingKey sk = new SwingKey(command_base,null,a,k);
      sk.addToKeyMap(base);
    }
   
   return base;
}


@Override public BaleHinter getHinter()
{
   return bale_hinter;
   
}

@Override public String getPostContent(String content)
{
   if (!BALE_PROPERTIES.getBoolean(BALE_AUTO_INSERT_CLOSE)) return null;
   return content_map.get(content);
}



@Override public boolean checkContent(String content)
{
   if (!BALE_PROPERTIES.getBoolean(BALE_AUTO_INSERT_CLOSE)) return false;
   return post_content.contains(content);
}




}       // end of class BaleLanguageKitDefault




/* end of BaleLanguageKitDefault.java */

