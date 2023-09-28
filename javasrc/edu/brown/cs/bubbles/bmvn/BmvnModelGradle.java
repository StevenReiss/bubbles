/********************************************************************************/
/*                                                                              */
/*              BmvnGradleModel.java                                            */
/*                                                                              */
/*      Model for using gradle                                                  */
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



package edu.brown.cs.bubbles.bmvn;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.exec.IvyExec;


class BmvnModelGradle extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static List<String> gradle_commands;


private static final String [] GRADLE_COMMANDS = {
   "build", "clean", "jar", "publish", "test", "check", "distZip"
};

private static final Set<String> gradle_uses;

static {
   gradle_uses = new HashSet<>();
   for (String s : GRADLE_COMMANDS) {
      gradle_uses.add(s);
    }
   // possibly add commands from properties file
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelGradle(BmvnProject proj,File grad)
{
   super(proj,grad,BmvnTool.GRADLE);
  
   gradle_commands = new ArrayList<>();
   
   setupTasks();
   // might want to run gradle tasks to get list of available tasks
   // then restrict to viable subset using GRADLE_COMMANDS
   for (String s : GRADLE_COMMANDS) {
      gradle_commands.add(s);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Find appropriate tasks                                                  */
/*                                                                              */
/********************************************************************************/

private void setupTasks() 
{
   SetupTasks st = new SetupTasks();
   st.start();
}


private class SetupTasks extends Thread {

   SetupTasks() {
      super("Setup Gradle for " + getFile().getParentFile().getName());
    }
   
   @Override public void run() {
      
      File wd = getFile().getParentFile();
      try {
         IvyExec exec = new IvyExec("gradle tasks",wd,IvyExec.READ_OUTPUT);
         BufferedReader br = new BufferedReader(new InputStreamReader(exec.getInputStream()));
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            int idx = ln.indexOf(" - ");
            if (idx < 0) continue;
            String cmd = ln.substring(0,idx);
            if (gradle_uses.contains(cmd)) gradle_commands.add(cmd);
          }
         br.close();
         exec.waitFor();
       }
      catch (IOException e) { }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override List<BmvnCommand> getCommands(String name,BudaBubble relbbl,Point where)
{
   List<BmvnCommand> rslt = new ArrayList<>();
   
   for (String s : gradle_commands) {
      rslt.add(new GradleCommand(s,relbbl,where));
    }
   
   if (rslt.isEmpty()) return null;
   
   return rslt;
}


private class GradleCommand extends AbstractAction implements BmvnCommand {

   private String gradle_task;
   private BudaBubble relative_bubble;
   private Point relative_point;
   
   GradleCommand(String goal,BudaBubble relbbl,Point where) {
      gradle_task = goal;
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public String getName()            { return "Gradle " + gradle_task; }
   
   @Override public BmvnModel getModel()        { return BmvnModelGradle.this; }
   
   @Override public void execute() { 
      File wd = getFile().getParentFile();
      String cmd = "mvn " + gradle_task;
      runCommand(cmd,wd,ExecMode.USE_STDOUT_STDERR,relative_bubble,relative_point);
    }
   
}       // end of inner class MavenCommand


}       // end of class BmvnGradleModel




/* end of BmvnGradleModel.java */

