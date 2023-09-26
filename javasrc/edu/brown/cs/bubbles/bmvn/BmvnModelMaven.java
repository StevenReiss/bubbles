/********************************************************************************/
/*                                                                              */
/*              BmvnMavenModel.java                                             */
/*                                                                              */
/*      Container and interface for MAVEN model                                 */
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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;

class BmvnModelMaven extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Model   maven_model;

private static final String [] MODEL_FILES = { "pom.xml" };

private static final String [] MAVEN_COMMANDS = {
   "clean",
   "compile",
   "test",
   "package",
   "install",
};



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelMaven(BmvnProject proj,File pom)
{
   super(proj,pom,"Maven");
   
   try (FileReader fr = new FileReader(pom)) {
      MavenXpp3Reader rdr = new MavenXpp3Reader();
      maven_model = rdr.read(fr);
    }
   catch (XmlPullParserException e) { 
    }
   catch (IOException e) {
    }
   
   BoardLog.logD("BMVN","Build maven model " + maven_model);
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Check library command                                                   */
/*                                                                              */
/********************************************************************************/

@Override boolean canCheckLibraries()
{
   List<Dependency> deps = maven_model.getDependencies();
   if (deps == null || deps.isEmpty()) return false;
   
   return true;
}


@Override void doCheckLibraries(BudaBubble bbl,Point pt)
{
   // get class path
   // for each dependency
   //     find on class path
   //     update if wrong jar file
   //     OR add if missing jar file
}



/********************************************************************************/
/*                                                                              */
/*      Add library command                                                     */
/*                                                                              */
/********************************************************************************/

@Override boolean canAddLibrary()                       { return true; }

@Override void doAddLibrary(BudaBubble bbl,Point pt)
{
   // create a JOptionPane asking for Library name and version
   // on getting name -- update list of versions available
   // on accept -- add to class path, add dependency, rewrite model
}


/********************************************************************************/
/*                                                                              */
/*      Remove library command                                                  */
/*                                                                              */
/********************************************************************************/

@Override boolean canRemoveLibrary()
{
   List<Dependency> deps = maven_model.getDependencies();
   if (deps == null || deps.isEmpty()) return false;
   
   return true;
}

@Override void doRemoveLibrary(BudaBubble bbl,Point pt)
{
   // create a JOptionPane with list of current dependencies
   //   allow user to choose one or more of these
   //   on accept -- remove dependencies and rewrite model and update classpath
}



/********************************************************************************/
/*                                                                              */
/*      Maven commands                                                          */
/*                                                                              */
/********************************************************************************/

@Override List<BmvnCommand> getCommands(BudaBubble relbbl,Point where)
{
   List<BmvnCommand> rslt = new ArrayList<>();
   
   for (String s : MAVEN_COMMANDS) {
      rslt.add(new MavenCommand(s,relbbl,where));
    }
   
   return rslt;
}


private class MavenCommand extends AbstractAction implements BmvnCommand {
   
   private String maven_goal;
   private BudaBubble relative_bubble;
   private Point relative_point;
   
   MavenCommand(String goal,BudaBubble relbbl,Point where) {
      maven_goal = goal;
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public String getName()            { return "Maven " + maven_goal; }
   
   @Override public BmvnModel getModel()        { return BmvnModelMaven.this; }
   
   @Override public void execute() { 
      //in wd of the model file, execute "mvn <goal>"
      // put up a bubble if there are errors/warnings to display
    }
   
}       // end of inner class MavenCommand


}       // end of class BmvnMavenModel




/* end of BmvnMavenModel.java */

