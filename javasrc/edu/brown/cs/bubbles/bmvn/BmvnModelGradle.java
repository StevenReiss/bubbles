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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import edu.brown.cs.bubbles.buda.BudaBubble;


class BmvnModelGradle extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String [] GRADLE_COMMANDS = {
   "build", "clean", "jar", "publish"
};

private static List<String> gradle_commands;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelGradle(BmvnProject proj,File grad)
{
   super(proj,grad,"Gradle");
   
   // might want to run gradle tasks to get list of available tasks
   // then restrict to viable subset using GRADLE_COMMANDS
   gradle_commands = new ArrayList<>();
   for (String s : GRADLE_COMMANDS) {
      gradle_commands.add(s);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override List<BmvnCommand> getCommands(BudaBubble relbbl,Point where)
{
   List<BmvnCommand> rslt = new ArrayList<>();
   
   for (String s : gradle_commands) {
      rslt.add(new GradleCommand(s,relbbl,where));
    }
   
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
      //in wd of the model file, execute "gradle <goal>"
      // put up a bubble if there are errors/warnings to display
    }
   
}       // end of inner class MavenCommand


}       // end of class BmvnGradleModel




/* end of BmvnGradleModel.java */

