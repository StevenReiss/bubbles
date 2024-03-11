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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;

class BmvnModelMaven extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<Dependency>        known_dependencies;

private static final String [] MAVEN_COMMANDS = {
   "clean",
   "compile",
   "test",
   "package",
   "install",
};

private static Pattern DEP_PATTERN = 
   Pattern.compile("^([-A-Za-z0-9_.]+)\\:([-A-Za-z0-9_.]*)\\:([-A-Za-z0-9_.]*)\\:([-A-Za-z0-9_.]*)\\:" + 
         "([-A-Za-z0-9_.]*)\\:(.*) -- module (.*) ");


         
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelMaven(BmvnProject proj,File pom)
{
   super(proj,pom,BmvnTool.MAVEN);
   
   updateDependencies();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void updateDependencies()
{
   List<Dependency> newdeps = new ArrayList<>();
   String cmd =  "mvn dependency:list -DoutputFile=AAAA " +
      "-Dsilent=true -DoutputAbsoluteArtifactFilename=true --log-file BBBB -q";
   File wd = getFile().getParentFile();
   try {
      File t1 = File.createTempFile("mvn",".output");
      File t2 = File.createTempFile("mvn",".log");
      t1.deleteOnExit();
      t2.deleteOnExit();
      cmd.replace("AAAA",t1.getPath());
      cmd.replace("BBBB",t2.getPath());
      IvyExec exec = new IvyExec(cmd,wd,IvyExec.IGNORE_OUTPUT);
      exec.waitFor();
      t2.delete();
      try (BufferedReader br = new BufferedReader(new FileReader(t1))) {
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            Dependency dep = new Dependency(ln);
            if (dep.isValid()) newdeps.add(dep);
          }
       }
      t1.delete();
    }
   catch (IOException e) {
    }
   known_dependencies = newdeps;
}



/********************************************************************************/
/*                                                                              */
/*      Check library command                                                   */
/*                                                                              */
/********************************************************************************/

@Override boolean canCheckLibraries()
{
   if (known_dependencies == null || known_dependencies.isEmpty()) return false;
   
   return true;
}


@Override void doCheckLibraries(BudaBubble bbl,Point pt)
{
   Set<File> origlibs = getProject().getLibraries();
   Map<File,File> updated = new LinkedHashMap<>();
   Set<File> added = new LinkedHashSet<>();
   Set<File> removed = new LinkedHashSet<>(); 
   
   for (Dependency dep : known_dependencies) {
      String jname = dep.getSystemPath();
      if (jname == null) continue;
      File jfil = new File(jname);
      if (!jfil.exists() || !jfil.canRead()) continue;
      jfil = IvyFile.getCanonical(jfil);
      if (!origlibs.contains(jfil)) {
         for (File f1 : origlibs) {
            String fnm = f1.getName();
            if (fnm.startsWith(dep.getJarName()) && fnm.endsWith("." + dep.getType())) {
               updated.put(f1,jfil);
               break;
             }
          }
         added.add(jfil);
       }
    }  
   getProject().updateLibraries(updated,added,removed);
}



/********************************************************************************/
/*                                                                              */
/*      Add library command                                                     */
/*                                                                              */
/********************************************************************************/

// need to be able to write pom.xml files
@Override boolean canAddLibrary()                       { return false; }


/********************************************************************************/
/*                                                                              */
/*      Remove library command                                                  */
/*                                                                              */
/********************************************************************************/

// need to be able to write pom.xml files
@Override boolean canRemoveLibrary()
{
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Maven commands                                                          */
/*                                                                              */
/********************************************************************************/

@Override List<BmvnCommand> getCommands(String name,BudaBubble relbbl,Point where)
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
   private static final long serialVersionUID = 1;
   
   MavenCommand(String goal,BudaBubble relbbl,Point where) {
      maven_goal = goal;
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public String getName()            { return "Maven " + maven_goal; }
   
   @Override public BmvnModel getModel()        { return BmvnModelMaven.this; }
   
   @Override public void execute() { 
      File wd = getFile().getParentFile();
      String cmd = "mvn " + maven_goal;
      runCommand(cmd,wd,ExecMode.USE_STDOUT_STDERR,relative_bubble,relative_point);
    }
   
}       // end of inner class MavenCommand


@Override protected String filterOutputLine(String ln)
{
   if (ln.startsWith("[ERROR]")) return ln.substring(8);
   else return null;   
}



/********************************************************************************/
/*                                                                              */
/*      Dependency Information                                                  */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private class Dependency {
   
   private String dep_name;
   private String dep_jar;
   private String dep_type;
   private String dep_version;
   private String dep_state;
   private String dep_path;
   private String dep_module;
         
   Dependency(String depinfo) {
      depinfo = depinfo.trim();
      Matcher m = DEP_PATTERN.matcher(depinfo);
      if (m.find()) {
         dep_name = m.group(1);
         dep_jar = m.group(2);
         dep_type = m.group(3);
         dep_version = m.group(4);
         dep_state = m.group(5);
         dep_path = m.group(6);
         dep_module = m.group(7);
         if (dep_type == null || !dep_type.equals("jar")) dep_name = null;
       }
      else { 
         dep_name = null;
       }
    }
   
   boolean isValid()                            { return dep_name != null; }
   
   String getSystemPath()                       { return dep_path; }
   String getJarName()                          { return dep_jar; }
   String getType()                             { return dep_type; }
   String getVersion()                          { return dep_version; }
   String getState()                            { return dep_state; }
   String getModule()                           { return dep_module; }
   
}       // end of inner class Depenency




}       // end of class BmvnMavenModel




/* end of BmvnMavenModel.java */

