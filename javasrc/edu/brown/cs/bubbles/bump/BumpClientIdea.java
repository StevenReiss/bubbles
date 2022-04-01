/********************************************************************************/
/*                                                                              */
/*              BumpClientIdea.java                                             */
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



package edu.brown.cs.bubbles.bump;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.exec.IvyExec;

class BumpClientIdea extends BumpClientJava
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean         idea_starting;

private String [] MAC_BINARY = new String [] {
      "Contents/MacOS/idea"
};
        
private String [] OPTIONS_FILE = new String [] {
   "idea.vmoptions", "bin/idea.vmoptions", "Contents/bin/idea.vmoptions"   
};
private static 
boolean run_headless = false;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BumpClientIdea()
{
   idea_starting = false;
   mint_control.register("<BUBJET SOURCE='IDEA' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/



@Override public String getName()		{ return "Idea"; }

@Override public String getServerName() 	{ return "BUBJET"; }



/********************************************************************************/
/*                                                                              */
/*      Idea starupt methods                                                    */
/*                                                                              */
/********************************************************************************/


@Override void localStartIDE()
{
   synchronized (this) {
      if (idea_starting) return;
      idea_starting = true;
    }
   
   ensureRunning();
}



private void ensureRunning()
{
   if (checkIfRunning()) return;
   
   String ideadir = board_properties.getProperty(BOARD_PROP_BASE_IDE_DIR);
   String ws = board_properties.getProperty(BOARD_PROP_WORKSPACE);
   
   File ef = new File(ideadir);
   File ef1 = null;
   for (String s : BOARD_IDEA_START) {
      File ef2 = new File(ef,s);
      if (ef2.exists() && ef2.canExecute()) {
         ef1 = ef2;
         break;
       }
    }
   if (ef1 != null && ef1.isDirectory()) {
      for (String s : MAC_BINARY) {
	 File ef2 = new File(ef1,s);
	 if (ef2.exists() && ef2.canExecute()) {
	    ef1 = ef2;
	    break;
	  }
       }
    }
   if (ef1 == null || !ef1.exists() || !ef1.canExecute()) ef1 = new File(ef,"idea");
   String efp = ef1.getPath();
   if (efp.endsWith(".app") || efp.endsWith(".exe")) efp = efp.substring(0,efp.length()-4);
   String cmd = "'" + efp + "'";
   
   cmd += " nosplash";
   if (ws != null) cmd += " '" + ws + "'";
   
   String eopt = board_properties.getProperty(BOARD_PROP_BASE_IDE_OPTIONS);
   eopt = board_properties.getProperty(BOARD_PROP_IDEA_OPTIONS,eopt);
   if (eopt != null) cmd += " " + eopt;
   
   // mint and other options
   String optfile = setupOptions(ws);
   String [] env = null;
   if (optfile != null) {
      Map<String,String> oenv = new LinkedHashMap<>(System.getenv());
      oenv.put("IDEA_VM_OPTIONS",optfile);
      env = new String[oenv.size()];
      int ct = 0;
      for (Map.Entry<String,String> ent : oenv.entrySet()) {
         String v = ent.getKey() + "=" + ent.getValue();
         env[ct++] = v;
       }
      BoardLog.logD("BOARD","SET environment: IDEA_VM_OPTIONS=" + optfile);
    }
   
   try {
      BoardLog.logD("BOARD","RUN IDEA WITH COMMAND " + cmd);
      IvyExec ex = new IvyExec(cmd,env,IvyExec.ERROR_OUTPUT);
      boolean eok = false;
      for (int i = 0; i < 700; ++i) {
	 synchronized (this) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (tryPing()) {
	    BoardLog.logI("BUMP","IntelliJ idea started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting IntelliJ idea");
	    if (BoardSetup.getSetup().getRunMode() != RunMode.SERVER) {
	       JOptionPane.showMessageDialog(null,
		     "IntelliJ IDEA could not be started. Check the idea log",
		     "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	     }
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","IntelliJ idea doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running IntelliJ idea: " + e);
      System.exit(1);
    }
}



private String setupOptions(String wsname)
{
   String ideadir = board_properties.getProperty(BOARD_PROP_BASE_IDE_DIR);
   File f1 = new File(ideadir);
   
   if (wsname != null) {
      if (wsname.endsWith(File.separator)) {
         wsname = wsname.substring(0,wsname.length()-1);
       }
      int idx = wsname.lastIndexOf(File.separator);
      if (idx > 0) wsname = wsname.substring(idx+1);
      wsname = wsname.replace(" ","_");
    }
   
   File opts = null;
   for (String s : OPTIONS_FILE) {
      String [] comps = s.split("/");
      File f2 = f1;
      for (String c : comps) {
         f2 = new File(f2,c);
       }
      if (f2.exists() && f2.canRead()) {
         opts = f2;
         break;
       }
    }
   if (opts == null) return null;
   Map<String,String> optmap = new LinkedHashMap<>();
   try (BufferedReader br = new BufferedReader(new FileReader(opts))) {
      for ( ; ; ) {
         String s = br.readLine();
         if (s == null) break;
         addOption(s,optmap);
       }
      
    }
   catch (IOException e) {
     BoardLog.logE("BOARD","Can't read option file",e); 
    }
   
   if (wsname == null) {
      addOption("-Dedu.brown.cs.bubbles.MINT=" + mint_name,optmap);
    }
   else {
      String opt = "-Dedu.brown.cs.bubbles.MINT." + wsname;
      addOption(opt + "=" + mint_name,optmap);
    }
   
   if (run_headless) {
      addOption("-Djava.awt.headless=true",optmap);
    }
   String s = board_properties.getProperty(BOARD_PROP_IDEA_VM_OPTIONS);
   if (s != null) {
      StringTokenizer tok = new StringTokenizer(s);
      while (tok.hasMoreTokens()) {
         addOption(tok.nextToken(),optmap);
       }
    }
   
   String rslt = null;
   try {
      File ft = File.createTempFile("idea",".options");
      ft.deleteOnExit();
      PrintWriter pw = new PrintWriter(ft);
      for (String opt : optmap.values()) {
         BoardLog.logD("BUMP","Add idea option: " + opt);
         pw.println(opt);
       }
      pw.close();
      rslt = ft.getAbsolutePath();
    }
   catch (IOException e) { }
   
   return rslt;
}



private void addOption(String opt,Map<String,String> optmap)
{
   if (opt == null || opt.length() == 0) return;
   
   int idx1 = opt.indexOf(":");
   int idx2 = opt.indexOf("=");
   String key = opt;
   int idx0 = -1;
   if (idx1 > 0 && idx2 > 0) idx0 = Math.min(idx1,idx2);
   else if (idx1 > 0) idx0 = idx1;
   else if (idx2 > 0) idx0 = idx2;
   if (idx0 > 0) key = opt.substring(0,idx0);
   optmap.put(key,opt);
}


}       // end of class BumpClientIdea




/* end of BumpClientIdea.java */

