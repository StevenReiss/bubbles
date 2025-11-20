/********************************************************************************/
/*                                                                              */
/*              BstyleMonitor.java                                              */
/*                                                                              */
/*      Message interface for BSYTLE checkstyle runner                          */
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.mint.client.MintClient;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BstyleMonitor implements BstyleConstants, MintConstants  
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BstyleMain      bstyle_main;
private MintControl     mint_control;
private boolean         is_done;
private boolean         no_exit;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleMonitor(BstyleMain bm,String mintid)
{
   bstyle_main = bm;
   mint_control = MintClient.create(mintid,MintSyncMode.ONLY_REPLIES);
   is_done = false;
   no_exit = false;
}


 /********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void start()
{
   IvyLog.logD("BSTYLE","Register for messages");
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='_VAR_0' />",new BubblesHandler());
   mint_control.register("<BSTYLE CMD='_VAR_0' />",new BstyleHandler());
   
   new WaitForExit().start();
}
 


/********************************************************************************/
/*                                                                              */
/*      Handle termination                                                      */
/*                                                                              */
/********************************************************************************/

private synchronized void serverDone()
{
   is_done = true;
   notifyAll();
}



private boolean checkEclipse()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PING' />";
   mint_control.send(msg,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
   String r = rply.waitForString(300000);
   IvyLog.logD("BSTYLE","BUBBLES PING " + r);
   if (r == null) return false;
   return true;
}



private class WaitForExit extends Thread {
   
   WaitForExit() {
      super("WaitForExit");
    }
   
   @Override public void run() {
      BstyleMonitor mon = BstyleMonitor.this;
      synchronized (mon) {
         for ( ; ; ) {
            if (checkEclipse()) break;
            try {
               mon.wait(30000L);
             }
            catch (InterruptedException e) { }
          }
         
         while (!is_done) {
            if (!checkEclipse()) is_done = true;
            else {
               try {
                  mon.wait(30000L);
                }
               catch (InterruptedException e) { }
             }
          }
       }
      
      if (!no_exit) System.exit(0);
    }

}       // end of inner class WaitForExit




/********************************************************************************/
/*                                                                              */
/*      Message sending                                                         */
/*                                                                              */
/********************************************************************************/

Element sendCommandWithXmlReply(String cmd,String proj,CommandArgs args,String body)
{
   MintDefaultReply rply = new MintDefaultReply();
   sendCommand(cmd,proj,args,body,rply);
   
   Element xml = rply.waitForXml();
   
   IvyLog.logD("BSTYLE","REPLY FROM BUBBLES: " + IvyXml.convertXmlToString(xml));
   
   return xml;
}



void sendCommand(String cmd,String proj,CommandArgs args,String body,MintReply rply)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBBLES");
   xw.field("DO",cmd);
   xw.field("BID",SOURCE_ID);
   if (proj != null && !proj.isEmpty()) xw.field("PROJECT",proj);
   xw.field("LANG","Eclipse");
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
         xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (body != null) xw.xmlText(body);
   xw.end("BUBBLES");
   String msg = xw.toString();
   xw.close();
   
   IvyLog.logD("BSTYLE","SEND TO BUBBLES: " + msg);
   
   if (rply != null) {
      sendMessage(msg,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
    }
   else {
      sendMessage(msg,null,MintConstants.MINT_MSG_NO_REPLY);
    }
}


void sendMessage(String xml,MintReply rply,int fgs)
{
   mint_control.send(xml,rply,fgs);
}



/********************************************************************************/
/*                                                                              */
/*      Handle sending messages back to Bubbles                                 */
/*                                                                              */
/********************************************************************************/

IvyXmlWriter beginMessage(String cmd)
{ 
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BEDROCK");
   xw.field("SOURCE","ECLIPSE");
   xw.field("TYPE",cmd);
   xw.field("BID",SOURCE_ID);
   return xw;
}


void finishMessage(IvyXmlWriter xw)
{
   xw.end("BEDROCK");
   
   IvyLog.logD("BSTYLE","Fake bedrock message " + xw.toString());
   
   sendMessage(xw.toString(),null,MINT_MSG_NO_REPLY);
}
   


/********************************************************************************/
/*                                                                              */
/*      Error handling                                                          */
/*                                                                              */
/********************************************************************************/

private void handleErrors(String proj,String filename,Element messages)
{
   Map<BstyleFile,Boolean> errmap = new HashMap<>();
   
   if (filename != null) {
      BstyleFile bf = bstyle_main.getFileManager().findFile(filename);
      if (bf != null) errmap.put(bf,false);
    }
   
   List<BstyleFile> redo = new ArrayList<>();
   
   for (Element pelt : IvyXml.children(messages,"PROBLEM")) {
      String cat = IvyXml.getAttrString(pelt,"CATEGORY","IDE");
      if (cat.equals("BSTYLE")) continue;
      String fnm = IvyXml.getAttrString(pelt,"FILE");
      if (fnm != null) {
         BstyleFile bf1 = bstyle_main.getFileManager().findFile(fnm);
         if (bf1 != null && errmap.get(bf1) != Boolean.TRUE) {
            if (IvyXml.getAttrBool(pelt,"ERROR")) {
               errmap.put(bf1,true);
             }
          }
       }
    }
   
   for (Map.Entry<BstyleFile,Boolean> ent : errmap.entrySet()) {
      BstyleFile bf = ent.getKey();
      boolean errs = ent.getValue();
      boolean chng = bf.setHasErrors(errs);
      if (chng) {
         redo.add(bf);
       }
    }
   
   // this is wrong
   if (!redo.isEmpty()) {
      bstyle_main.getStyleChecker().processProject(proj,redo);
    }
}


void removeErrors(BstyleFile bf) 
{
   List<BstyleFile> redo = List.of(bf);
   bstyle_main.getStyleChecker().processProject(bf.getProject(),redo);
}



/********************************************************************************/
/*                                                                              */
/*      Change handling                                                         */
/*                                                                              */
/********************************************************************************/

private void handleEdit(MintMessage msg,String bid,File file,int len,int offset,
      boolean complete,boolean remove,String txt)
{
   BstyleFile bf = bstyle_main.getFileManager().findFile(file);
   if (bf == null) {
      IvyLog.logD("BSTYLE","File not found for edit " + file);
      return;
    }
   
   bf.editFile(len,offset,txt,complete);
   bf.setHasErrors(true);
   
// List<BstyleFile> redo = null;   
// if (len != 0 || (txt != null && !txt.isEmpty())) {
//    redo = List.of(bf);
//    redo.add(bf);
//  }
// 
// if (redo != null) {
//    bstyle_main.getStyleChecker().processProject(bf.getProject(),redo);
//  }
}


private void handleStartFile(String proj,String fnm)
{ 
   File file = new File(fnm);
   file = IvyFile.getCanonical(file);
   
   BstyleFile bf = bstyle_main.getFileManager().findFile(file);
   if (bf == null) return;
   
   bf.startFile();
   
   List<BstyleFile> redo = List.of(bf);
   bstyle_main.getStyleChecker().processProject(bf.getProject(),redo);
}



private void handleFinishFile(String proj,String file)
{
   // can't tell if the file is actually closed here -- don't close it for bstyle 
}


private void handleBuildDone(Element xml)
{
   List<BstyleFile> redo = new ArrayList<>();
   Set<BstyleFile> haderrors = new HashSet<>();
   String proj = IvyXml.getAttrString(xml,"PROJECT");
   List<BstyleFile> allbf = bstyle_main.getProjectManager().getAllFiles(proj); 
   for (BstyleFile bf : allbf) {
      if (bf.getHasErrors()) {
         haderrors.add(bf);
         bf.setHasErrors(false);
       }
    }
   IvyLog.logD("BSTYLE","Build done A " + haderrors.size());
   Element probs = IvyXml.getChild(xml,"PROBLEMS");
   for (Element pe : IvyXml.children(probs,"PROBLEM")) {
      if (IvyXml.getAttrBool(pe,"ERROR")) {
         String fnm = IvyXml.getAttrString(pe,"FILE");
         BstyleFile bf = bstyle_main.getFileManager().findFile(fnm);
         if (bf == null) continue;
         boolean chng = bf.setHasErrors(true);
         if (chng) redo.add(bf);
         haderrors.remove(bf);
       }
    }
   IvyLog.logD("BSTYLE","Build done B " + haderrors.size() + " " + redo.size());
   redo.addAll(haderrors);
   
   if (!redo.isEmpty()) {
      bstyle_main.getStyleChecker().processProject(proj,redo);
    }
}



 
private void handleResourceChange(Element res)
{
   String k = IvyXml.getAttrString(res,"KIND");
   Element re = IvyXml.getChild(res,"RESOURCE");
   String rtyp = IvyXml.getAttrString(res,"TYPE");
   BstyleFile bf = null;
   List<BstyleFile> redo = new ArrayList<>();
   if (rtyp != null && rtyp.equals("FILE")) {
      String fp = IvyXml.getAttrString(re,"LOCATION");
      String proj = IvyXml.getAttrString(re,"PROJECT");
      switch (k) {
         case "ADDED" :
         case "ADDED_PHANTOM" :
            bf = bstyle_main.getProjectManager().addFile(proj,fp); 
            if (bf != null) {
               redo.add(bf);
             }
            break;
         case "REMOVED" :
         case "REMOVED_PHANTOM" :
            bf = bstyle_main.getProjectManager().removeFile(proj,fp);
            if (bf != null) {
               redo.add(bf);
             }
            break;
         default :
            IvyLog.logI("BSTYLE","CHANGE FILE " + fp + " IN " + proj);
            bf = bstyle_main.getFileManager().findFile(fp); 
            if (bf != null) bf.noteChanged();
            // TODO: remove old binary from jcode
            break;
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Process commands to BSTYLE                                              */
/*                                                                              */
/********************************************************************************/

private String processCommand(String cmd,Element e)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");
   switch (cmd) {
      case "PING" :
         xw.text("PONG");
         break;
      default :
         IvyLog.logE("BSTYLE","Unknown bstyle command " + cmd);
         break;
    }
   xw.end("RESULT");
   String rslt = xw.toString();
   xw.close();
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Message handlers                                                         */
/*                                                                              */
/********************************************************************************/

private final class BstyleHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      IvyLog.logI("BSTYLE","Process BSTYLE command: " + msg.getText());
      String cmd = args.getArgument(0);
      Element e = msg.getXml();
      String rslt = null;
      
      try {
         rslt = processCommand(cmd,e);
         IvyLog.logI("BSTYLE","COMMAND RESULT: " + rslt);
       }
      
      catch (Throwable t) {
         String xmsg = "Problem processing command " + cmd + ": " + t;
         IvyLog.logE("BSTYLE",xmsg,t);
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         Throwable xt = t;
         for ( ; xt.getCause() != null; xt = xt.getCause());
         if (xt != null && xt != t) {
            pw.println();
            xt.printStackTrace(pw);
          }
         IvyLog.logE("BSTYLE","TRACE: " + sw.toString());
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.begin("ERROR");
         xw.textElement("MESSAGE",xmsg);
         xw.cdataElement("EXCEPTION",t.toString());
         xw.cdataElement("STACK",sw.toString());
         xw.end("ERROR");
         rslt = xw.toString();
         xw.close();
         pw.close();
       }
      msg.replyTo(rslt);
   }

}       // end of inner class BstyleHandler


private final class EclipseHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element e = msg.getXml();
      
      switch (cmd) {
         case "ELISION" :
            return;
       }
      
      IvyLog.logD("BSTYLE","Message from eclipse: " + cmd + " " + msg.getText());
      
      switch (cmd) {
         case "PING" :
         case "PING1" :
         case "PING2" :
         case "PING3" :
            msg.replyTo("<PONG/>");
            break;
         case "EDITERROR" :
         case "FILEERROR" :
            try {
               handleErrors(IvyXml.getAttrString(e,"PROJECT"),
                     IvyXml.getAttrString(e,"FILE"),
                     IvyXml.getChild(e,"MESSAGES"));
             }
            catch (Throwable t) {
               IvyLog.logE("BSTYLE","Problem handling errors",t);
             }
            break;
         case "EDIT" :
            String bid = IvyXml.getAttrString(e,"BID");
            if (bid != null && !bid.equals(SOURCE_ID)) {
               msg.replyTo();
               break;
             }
            String txt = IvyXml.getText(e);
            boolean complete = IvyXml.getAttrBool(e,"COMPLETE");
            boolean remove = IvyXml.getAttrBool(e,"REMOVE");
            if (complete) {
               byte [] data = IvyXml.getBytesElement(e,"CONTENTS");
               if (data != null) txt = new String(data);
               else remove = true;
             }
            try {
               handleEdit(msg,bid,
                     new File(IvyXml.getAttrString(e,"FILE")),
                     IvyXml.getAttrInt(e,"LENGTH"),
                     IvyXml.getAttrInt(e,"OFFSET"),
                     complete,remove,txt);
             }
            catch (Throwable t) {
               IvyLog.logE("BSTYLE","Problem handling edits",t);
             }
            msg.replyTo("<OK/>");
            break;
         case "RESOURCE" :
            for (Element re : IvyXml.children(e,"DELTA")) {
               handleResourceChange(re);
             }
            break;
         case "PROGRESS" :
            msg.replyTo();
            break;
         case "BUILDDONE" :
            handleBuildDone(e);
            break;
         default :
         case "EVALUATION" :
         case "CONSOLE" :
            msg.replyTo();
            break;
         case "STOP" :
            IvyLog.logD("BSTYLE","Eclipse Message: " + msg.getText());
            serverDone();
            msg.replyTo();
            break;
       }
    }

}       // end of inner class EclipseHandler


private final class BubblesHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element xml = msg.getXml();
      String bid = IvyXml.getAttrString(xml,"BID");
      IvyLog.logD("BSTYLE","BUBBLES COMMAND: " + cmd);
      // Element e = msg.getXml();
      switch (cmd) {
         case "STARTFILE" :
            if (bid != null && !bid.equals(SOURCE_ID)) {
               handleStartFile(IvyXml.getAttrString(xml,"PROJECT"),
                     IvyXml.getAttrString(xml,"FILE"));
             } 
            break;
         case "FINISHFILE" :
            if (bid != null && !bid.equals(SOURCE_ID)) {
               handleFinishFile(IvyXml.getAttrString(xml,"PROJECT"),
                     IvyXml.getAttrString(xml,"FILE"));
               // handle close file
             } 
            break;
         case "EXIT" :
            serverDone();
            break;
       }
      
      msg.replyTo();
    }
   
}       // end of inner class MintHandler


}       // end of class BstyleMonitor




/* end of BstyleMonitor.java */

