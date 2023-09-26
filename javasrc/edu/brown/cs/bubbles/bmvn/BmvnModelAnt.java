/********************************************************************************/
/*                                                                              */
/*              BmvnModelAnt.java                                               */
/*                                                                              */
/*      Library model for using ANT                                                */
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
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.xml.IvyXml;


class BmvnModelAnt extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean         is_root;
private Element         ant_xml;

private static Set<String> ant_tasks;

private static final String [] GOOD_TASKS = {
   "ant", "antlr", "copy", "exec", "java",
   "javac", "jar", "tar", "war", "zip",
   "jspc", "netrexxc", "rmic", "javadoc", "mail",
   "javacc", "javah", "jjtree", "xslt", "junit",
   "junitreport"
};

static {
   ant_tasks = new HashSet<>();
   for (String s : GOOD_TASKS) {
      ant_tasks.add(s);
    }
         
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelAnt(BmvnProject proj,File file)
{
   super(proj,file,"Ant");
   
   is_root = true;
   for (File dir = file.getParentFile(); dir != null; dir = dir.getParentFile()) {
      if (!dir.canWrite()) break;
      File f1 = new File(dir,"build.xml");
      if (f1.exists() && f1.canWrite()) is_root = false;
    }
   
   ant_xml = IvyXml.loadXmlFromFile(file);
   
   // analyze ant file to find the classpath specification and save it 
   //   or at least its property name
}



/********************************************************************************/
/*                                                                              */
/*      Ant commands                                                            */
/*                                                                              */
/********************************************************************************/

@Override List<BmvnCommand> getCommands(BudaBubble relbbl,Point where)
{
   List<BmvnCommand> rslt = new ArrayList<>();
   
   for (Element target : IvyXml.children(ant_xml,"target")) {
      String nm = IvyXml.getAttrString(target,"name");
      if (nm == null) continue;
      String cond = IvyXml.getAttrString(target,"if");
      if (cond != null) continue;
      for (Element task : IvyXml.children(target)) {
         if (isRelevantTask(task)) {
            rslt.add(new AntCommand(nm,relbbl,where));
          }
       }
    }
   
   return rslt;
}



private boolean isRelevantTask(Element task)
{
   for (String s : GOOD_TASKS) {
      if (IvyXml.isElement(task,s)) return true;
    }
   return false;
}



private class AntCommand extends AbstractAction implements BmvnCommand {
 
   private String ant_goal;
   private BudaBubble relative_bubble;
   private Point relative_point;
   
   AntCommand(String goal,BudaBubble relbbl,Point where) {
      ant_goal = goal;
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public String getName() { 
      if (is_root) return "Ant " + ant_goal;   
      String f = getFile().getParentFile().getName();
      return "Ant " + ant_goal + " in " + f; 
    }
   
   @Override public BmvnModel getModel()        { return BmvnModelAnt.this; }
   
   @Override public void execute() { 
      File wd = getFile().getParentFile();
      String cmd = "ant " + ant_goal;
      runCommand(cmd,wd,ExecMode.USE_STDERR,relative_bubble,relative_point);
    }
   
}       // end of inner class AntCommand



}       // end of class BmvnModelAnt




/* end of BmvnModelAnt.java */

