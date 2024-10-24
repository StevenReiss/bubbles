/********************************************************************************/
/*                                                                              */
/*              BstyleFileManager.java                                          */
/*                                                                              */
/*      Handle files                                                            */
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



package edu.brown.cs.bubbles.bstyle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.ivy.file.IvyFile;

class BstyleFileManager implements BstyleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BstyleMain      bstyle_main;
private Map<File,BstyleFile> file_map;
private List<File>      exclude_files;
private List<Pattern>   exclude_patterns; 



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleFileManager(BstyleMain bm)
{
   bstyle_main = bm;
   file_map = new HashMap<>();
   exclude_files = new ArrayList<>();
   exclude_patterns = new ArrayList<>();
   
   BoardProperties bp = BoardProperties.getProperties("Bstyle");
   for (String nm : bp.stringPropertyNames()) {
      if (nm.startsWith("Bstyle.file.exclude")) {
         String val = bp.getProperty(nm);
         StringTokenizer tok = new StringTokenizer(val," ,;\t");
         while (tok.hasMoreTokens()) {
            String fnm = tok.nextToken();
            File ff = new File(fnm);
            if (ff.isAbsolute()) ff = IvyFile.getCanonical(ff);
            exclude_files.add(ff);
          }
       }
      else if (nm.startsWith("Bstyle.regexp.exclude")) {
         String val = bp.getProperty(nm);
         StringTokenizer tok = new StringTokenizer(val);
         while (tok.hasMoreTokens()) {
            String fnm = tok.nextToken();
            exclude_patterns.add(Pattern.compile(fnm));
          }
       }
    }
   
}



/********************************************************************************/
/*                                                                              */
/*      Add a file for project                                                  */
/*                                                                              */
/********************************************************************************/

BstyleFile addFile(String project,String path,boolean isopen)
{
   File add = new File(path);
   add = IvyFile.getCanonical(add);
   if (!useFile(add)) return null;
  
   BstyleFile bf = new BstyleFile(bstyle_main,project,add); 
   
   
   if (bf != null) {
      if (isopen) {
         bf.startFile();
       }
      file_map.put(add,bf);
    }
   
   return bf;
}


void removeFile(BstyleFile bf)
{
   file_map.remove(bf.getFile());
}



/********************************************************************************/
/*                                                                              */
/*      Find file                                                               */
/*                                                                              */
/********************************************************************************/

BstyleFile findFile(String filename)
{
   return findFile(new File(filename));
}


BstyleFile findFile(File f)
{
   File fc = IvyFile.getCanonical(f);
   
   return file_map.get(fc);
}



/********************************************************************************/
/*                                                                              */
/*      Check if file is relevant                                               */
/*                                                                              */
/********************************************************************************/

private boolean useFile(File f)
{
   String fnm = f.getName();
   String fpath = f.getPath();
   if (!fnm.toLowerCase().endsWith(".java")) return false;
   for (File excl : exclude_files) {
      if (!excl.isAbsolute()) {
         String par = excl.getParent();
         if (par == null || par.equals("*") || par.equals("**")) {
            if (fnm.equals(excl.getName())) return false;
          }
       }
      else if (f.equals(excl)) return false;
    }
   for (Pattern pat : exclude_patterns) {
      Matcher m = pat.matcher(fpath);
      if (m.matches()) return false;
    } 
   
   return true;
}



}       // end of class BstyleFileManager




/* end of BstyleFileManager.java */

