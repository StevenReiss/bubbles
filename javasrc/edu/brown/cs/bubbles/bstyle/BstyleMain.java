/********************************************************************************/
/*                                                                              */
/*              BstyleMain.java                                                 */
/*                                                                              */
/*      Main program for BSTYLE process to run checkstyle as needed             */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;

public final class BstyleMain implements BstyleConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   BstyleMain bm = new BstyleMain(args);
   
   bm.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BstyleMonitor our_monitor;
private BstyleProjectManager project_manager;
private BstyleFileManager file_manager;
private BstyleChecker bstyle_checker;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BstyleMain(String [] args) 
{
   our_monitor = null;
   
   // setup logger
   
   scanArgs(args);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BstyleProjectManager getProjectManager()                { return project_manager; }

BstyleFileManager getFileManager()                      { return file_manager; }

BstyleChecker getStyleChecker()                         { return bstyle_checker; }




/********************************************************************************/
/*                                                                              */
/*      Argument scanning                                                       */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (args[i].startsWith("-m") && i+1 < args.length) {           // -mint <mintid>
            String mid = args[++i];
            our_monitor = new BstyleMonitor(this,mid); 
          }
         else if (args[i].startsWith("-D")) {                           // -Debug
            IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
          }             
         else if (args[i].startsWith("-O")) {                           // -Output
            IvyLog.useStdErr(true);
          }
         else if (args[i].startsWith("-L") && i+1 < args.length) {      // -L logfile
            IvyLog.setLogFile(new File(args[++i]));
          }
         else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("Bstyle: BstyleMain -m <message_id>");
   System.exit(1);
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{ 
   if (our_monitor == null) return;
   file_manager = new BstyleFileManager(this);
   project_manager = new BstyleProjectManager(this,file_manager); 
   project_manager.setup();
   our_monitor.start();
   bstyle_checker = new BstyleChecker(this);
   
   project_manager.processAllProjects(); 
}



/********************************************************************************/
/*                                                                              */
/*      Send message to bubbles/bedrock                                         */
/*                                                                              */
/********************************************************************************/

void sendCommand(String cmd,String proj,CommandArgs fields,String body,MintReply rply)  
{
   our_monitor.sendCommand(cmd,proj,fields,body,rply);  
}


Element sendCommandWithXmlReply(String cmd,String proj,CommandArgs fields,String body)
{
   return our_monitor.sendCommandWithXmlReply(cmd,proj,fields,body);
}

}       // end of class BstyleMain




/* end of BstyleMain.java */

