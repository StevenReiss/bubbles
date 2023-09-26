/********************************************************************************/
/*                                                                              */
/*              BmvnModelNpm.java                                               */
/*                                                                              */
/*      Library model for using npm and package.json files                      */
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


class BmvnModelNpm extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String [] NPM_COMMANDS = {
   "audit", "rebuild", "update"
};


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelNpm(BmvnProject proj,File file)
{
   super(proj,file,"Npm");
}


/********************************************************************************/
/*                                                                              */
/*      NPM commands                                                            */
/*                                                                              */
/********************************************************************************/

@Override List<BmvnCommand> getCommands(BudaBubble relbbl,Point where)
{
   List<BmvnCommand> rslt = new ArrayList<>();
   
   for (String s : NPM_COMMANDS) {
      rslt.add(new NpmCommand(s,relbbl,where));
    }
   
   return rslt;
}


private class NpmCommand extends AbstractAction implements BmvnCommand {
   
   private String npm_task;
   private BudaBubble relative_bubble;
   private Point relative_point;
   
   NpmCommand(String goal,BudaBubble relbbl,Point where) {
      npm_task = goal;
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public String getName()            { return "Npm " + npm_task; }
   
   @Override public BmvnModel getModel()        { return BmvnModelNpm.this; }
   
   @Override public void execute() { 
      File wd = getFile().getParentFile();
      String cmd = "npm " + npm_task;
      runCommand(cmd,wd,ExecMode.USE_STDOUT_STDERR,relative_bubble,relative_point);
    }

}       // end of inner class NpmCommand


}       // end of class BmvnModelNpm




/* end of BmvnModelNpm.java */

