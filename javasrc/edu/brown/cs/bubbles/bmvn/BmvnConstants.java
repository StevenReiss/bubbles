/********************************************************************************/
/*                                                                              */
/*              BmvnConstants.java                                              */
/*                                                                              */
/*      Definitions for bubbles Make/Version/Naming interface                   */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.bubbles.bmvn;

import java.awt.event.ActionEvent;

import javax.swing.Action;



public interface BmvnConstants
{



/********************************************************************************/
/*                                                                              */
/*      Command that can be executed by library model                           */
/*                                                                              */
/********************************************************************************/

interface BmvnCommand extends Action {
   
   String getName();
   BmvnModel getModel();
   
   @Override default public void actionPerformed(ActionEvent evt) {
      execute();
    }
   
   void execute();
   
}


enum BmvnTool {
   MAVEN("pom.xml",false),
   GRADLE("build.gradle",false),
   ANT("build.xml",true),
   NPM("package.json",false),
   YAML("pubspec.yaml",false);
   
   private String file_name;
   private boolean use_subdirectories;
   
   BmvnTool(String fn,boolean subs) {
      file_name = fn;
      use_subdirectories = subs;
    }
   
   String getFileName()                 { return file_name; }
   boolean useSubdirectories()          { return use_subdirectories; }
   
   
}       // end of inner enum BmvnTool


}       // end of interface BmvnConstants




/* end of BmvnConstants.java */

