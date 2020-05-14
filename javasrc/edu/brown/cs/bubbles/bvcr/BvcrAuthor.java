/********************************************************************************/
/*                                                                              */
/*              BvcrAuthor.java                                                 */
/*                                                                              */
/*      description of class                                                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.BoardProperties;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


class BvcrAuthor implements BvcrConstants, Comparable<BvcrAuthor>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          author_name;
private Color           use_color;

private static Map<String,String> author_alias = null;
private static Map<String,BvcrAuthor> author_map = new HashMap<String,BvcrAuthor>();



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BvcrAuthor(String id)
{
   author_name = id;
   int idx = author_map.size();
   
   author_map.put(id,this);
   
   use_color = getColor(idx);
}


static synchronized BvcrAuthor getAuthor(String id) 
{
   if (author_alias == null) loadAliases();
   String sid = author_alias.get(id);
   if (sid != null) id = sid;
   BvcrAuthor bv = author_map.get(id);
   if (bv == null) {
      bv = new BvcrAuthor(id);
      author_map.put(id,bv);
    }
   return bv;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getName()                        { return author_name; }
Color getColor()                        { return use_color; }


@Override public int compareTo(BvcrAuthor ba) 
{
   return getName().compareTo(ba.getName());
}

/********************************************************************************/
/*                                                                              */
/*      Color assignments                                                       */
/*                                                                              */
/********************************************************************************/

private static Color getColor(int idx)
{
   double v = 0.5;
   int p1 = 1;
   int p0 = idx+1;
   
   for (int p = p0; p > 1; p /= 2) {
      v /= 2.0;  
      p0 -= p1;
      p1 *= 2;
    }
    
   double h = v + v*(2*(p0-1));
   int rgb = Color.HSBtoRGB((float) h,1f,1f);
   
   return new Color(rgb);
}


/********************************************************************************/
/*                                                                              */
/*      Alias handling                                                          */
/*                                                                              */
/********************************************************************************/

private static void loadAliases()
{
   author_alias = new HashMap<>();
   
   String prop = BoardProperties.getProperties("BVCR").getProperty("author.alias");
   if (prop == null) return;
   
   StringTokenizer ltok = new StringTokenizer(prop,"\n\r");
   while (ltok.hasMoreTokens()) {
      String alias = ltok.nextToken();
      String [] als = alias.split("=");
      if (als.length > 1) {
         String key = als[0].trim();
         for (int i = 1; i < als.length; ++i) {
            String val = als[i].trim();
            author_alias.put(val,key);
          }
       }
    }
}
      

}       // end of class BvcrAuthor




/* end of BvcrAuthor.java */

