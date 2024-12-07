/********************************************************************************/
/*										*/
/*		BvcrChangeSet.java						*/
/*										*/
/*	Bubble Version Collaboration Repository external change management	*/
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


package edu.brown.cs.bubbles.bvcr;


import edu.brown.cs.bubbles.board.BoardProperties;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEParameterSpec;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.InflaterInputStream;


class BvcrChangeSet implements BvcrConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BvcrMain		for_main;
private String			project_name;
private String			repo_id;
private String			user_id;
private SecretKey		repo_key;

private long			last_update;
private Map<File,FileChanges>	change_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrChangeSet(BvcrMain bm,String proj,String rid,String uid,SecretKey sk)
{
   for_main = bm;
   project_name = proj;
   repo_id = rid;
   user_id = uid;
   repo_key = sk;

   last_update = 0;
   change_map = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void findChanges(File f,IvyXmlWriter xw)
{
   update();

   FileChanges fc = change_map.get(f);

   if (fc != null && xw != null) fc.outputXml(xw);
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

private void update()
{
   long now = System.currentTimeMillis();
   InputStream ins = BvcrUpload.download(user_id,repo_id,last_update);
   if (ins == null) return;

   try {
      int [] lens = readHeader(ins);
      if (lens == null) return;
      for (int len : lens) {
         InputStream sins = null;
         InputStream sins1 = null;
         try {
            sins = new SubInputStream(ins,len);
            if (repo_key != null) {
               try {
                  Cipher c = Cipher.getInstance(repo_key.getAlgorithm());
                  AlgorithmParameterSpec ps = new PBEParameterSpec(KEY_SALT,KEY_COUNT);
                  c.init(Cipher.DECRYPT_MODE,repo_key,ps);
                  sins1 = new CipherInputStream(sins,c);
                }
               catch (Exception e) {
                  IvyLog.logE("BVCR","Unable to create cipher",e);
                }
               if (KEY_COMPRESS) {
                  if (sins1 != null) sins1 = new InflaterInputStream(sins1);
                  else sins = new InflaterInputStream(sins);
                }
               IvyLog.logD("BVCR","Work on: " + user_id + " " + repo_id + " " + len);
               Element e = null;
               if (sins1 != null) e = IvyXml.loadXmlFromStream(sins1);
               else e = IvyXml.loadXmlFromStream(sins);
               addChanges(e);
             }
          }
         finally {
            if (sins != null) sins.close();
            if (sins1 != null) sins1.close();
          }
       }
      ins.close();
      last_update = now;
    }
   catch (IOException e) {
      IvyLog.logE("BVCR","Problem downloading data",e);
    }
}




private int [] readHeader(InputStream ins) throws IOException
{
   StringBuffer buf = new StringBuffer();
   for ( ; ; ) {
      for ( ; ; ) {
	 int v = ins.read();
	 if (v < 0) return null;
	 char ch = (char) v;
	 if (ch == '\n') break;
	 buf.append(ch);
       }
      IvyLog.logD("BVCR","READ HEADER: " + buf);
      if (buf.length() > 0 && Character.isDigit(buf.charAt(0))) break;
      buf = new StringBuffer();
    }

   StringTokenizer tok = new StringTokenizer(buf.toString());
   if (!tok.hasMoreTokens()) return null;
   int ct = Integer.parseInt(tok.nextToken());
   if (ct == 0) return null;
   int [] lengths = new int[ct];
   int j = 0;
   while (tok.hasMoreTokens()) {
      lengths[j++] = Integer.parseInt(tok.nextToken());
    }
   if (ct != j) return null;

   return lengths;
}



private static class SubInputStream extends FilterInputStream {

   private int sub_length;
   private int sub_ptr;

   SubInputStream(InputStream ins,int len) {
      super(ins);
      sub_length = len;
      sub_ptr = 0;
    }

   @Override public int read() throws IOException {
      if (sub_ptr >= sub_length) return -1;
      int rslt = super.read();
      if (rslt >= 0) ++sub_ptr;
      IvyLog.logD("BVCR","Read: " + rslt);
      return rslt;
    }

   @Override public int read(byte [] b,int off,int len) throws IOException {
      if (sub_ptr >= sub_length) return -1;
      if (sub_ptr + len > sub_length) len = sub_length - sub_ptr;
      int ct = super.read(b,off,len);
      sub_ptr += ct;
      IvyLog.logD("BVC","READ " + off + " " + len + " " + ct);
      return ct;
    }

   @Override public int available() throws IOException {
      return sub_length - sub_ptr;
    }

   @Override public void close() {
      sub_length = 0;
      sub_ptr = 0;
    }

   @Override public boolean markSupported()		{ return false; }

}	// end of inner class SubInputStream





/********************************************************************************/
/*										*/
/*	Methods to add all the changes in a change set for a user		*/
/*										*/
/********************************************************************************/

private void addChanges(Element xml)
{
   IvyLog.logD("BVCR","CHANGE SET = " + IvyXml.convertXmlToString(xml));

   BoardProperties bp = BoardProperties.getProperties("Bvcr");
   long days = bp.getLong("Bvcr.max.days",183);
   long delta = days*24*60*60*1000;
   long now = System.currentTimeMillis();
   File root = for_main.getRootDirectory(project_name);
   String oroot = IvyXml.getAttrString(xml,"ROOT");
   String user = IvyXml.getAttrString(xml,"USER");

   for (Element fe : IvyXml.children(xml,"FILE")) {
      File f1 = new File(root,IvyXml.getAttrString(fe,"NAME"));
      
      String fnm = f1.getName();
      int idx = fnm.lastIndexOf(".");
      if (idx < 0) continue;
      String ext = fnm.substring(idx);
      if (!ext.equalsIgnoreCase(".java")) continue;
      
      long dlm = IvyXml.getAttrLong(fe,"DLM",0);
      if (dlm != 0 && delta != 0 && dlm < now - delta) {
         IvyLog.logD("BVCR","Ignore old change to " + f1 + " for " + user); 
         continue;
       }
      
      FileChanges fc = change_map.get(f1);
      if (fc == null) {
	 fc = new FileChanges();
	 change_map.put(f1,fc);
       }
      BvcrDifferenceFile df = new BvcrDifferenceFile(fe);
      fc.addChange(user,oroot,df);
    }
}




/********************************************************************************/
/*										*/
/*	Representation of all changes to a file 				*/
/*										*/
/********************************************************************************/

private static class FileChanges {

   private Map<String,BvcrDifferenceFile> change_items;

   FileChanges() {
      change_items = new HashMap<String,BvcrDifferenceFile>();
    }

   void addChange(String user,String root,BvcrDifferenceFile df) {
      String id = user + "@" + root;
      change_items.put(id,df);
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("CHANGESET");
      for (Map.Entry<String,BvcrDifferenceFile> ent : change_items.entrySet()) {
         xw.begin("USERCHANGE");
         xw.field("USER",ent.getKey());
         ent.getValue().outputXml(xw);
         xw.end("USERCHANGE");
       }
      xw.end("CHANGESET");
    }

}	// end of inner class FileChanges




}	// end of class BvcrChangeSet




/* end of BvcrChangeSet.java */
