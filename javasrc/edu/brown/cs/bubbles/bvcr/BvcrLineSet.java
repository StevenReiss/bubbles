/********************************************************************************/
/*                                                                              */
/*              BvcrLineSet.java                                                */
/*                                                                              */
/*      Set of changed lines                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.                            *
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class BvcrLineSet implements BvcrConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<LineData>  changed_lines;

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrLineSet()
{
   changed_lines = new ArrayList<LineData>();
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void addChange(LineData ld)
{
   changed_lines.add(ld);
}



/********************************************************************************/
/*                                                                              */
/*      Transform methods                                                       */
/*                                                                              */
/********************************************************************************/

BvcrLineSet computeChangeLineSet(String user,BvcrDifferenceFile diff,boolean fwd)
{
   List<BvcrFileChange> chngs = diff.getChanges();
   
   BvcrLineSet nset = new BvcrLineSet();
   int cidx = 0;
   int delta = 0;
   
   for (BvcrFileChange ch : chngs) {
      int ln = (fwd ? ch.getSourceLine() : ch.getTargetLine());
      // first copy old changes with new delta
      while (cidx < changed_lines.size()) {
         LineData ld = changed_lines.get(cidx);
         int oln = ld.getLineNumber();
         if (oln >= ln) break;
         LineData nld = new LineData(oln+delta,ld.getUsers());
         nset.addChange(nld);
         ++cidx;
       }
      String [] del = (fwd ? ch.getDeletedLines() : ch.getAddedLines());
      String [] add = (fwd ? ch.getAddedLines() : ch.getDeletedLines());
      int ch0 = ln;
      int ch1 = ch0;
      if (del != null) ch1 += del.length;
      for (int i = ch0; i <= ch1; ++i) {
         if (cidx < changed_lines.size()) {
            LineData ld = changed_lines.get(cidx);
            int oln = ld.getLineNumber();
            if (oln == i) {
               ld.addUser(user);
               ++cidx;
               continue;
             }
          }
         nset.addChange(new LineData(i,user));
       }
      if (del != null) delta -= del.length;
      if (add != null) delta += add.length;
    }
   
   return nset;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(IvyXmlWriter xw) 
{
   xw.begin("LINESET");
   for (LineData ld : changed_lines) {
      ld.outputXml(xw);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Information about a changed line                                        */
/*                                                                              */
/********************************************************************************/

private static class LineData implements BvcrLineChange,Cloneable {
   
   private int line_number;
   private Set<String> by_user;
   
   LineData(int lno,String user) {
      line_number = lno;
      by_user = new HashSet<String>();
      if (user != null) by_user.add(user);
    }
   
   LineData(int lno,Collection<String> users) {
      line_number = lno;
      by_user = new HashSet<String>(users);
    }
   
   @Override public int getLineNumber()                 { return line_number; }
   Set<String> getUsers()                               { return by_user; }
   void addUser(String u)                               { by_user.add(u); }
   
   void outputXml(IvyXmlWriter xw) {
      xw.begin("LINE");
      xw.field("LNO",line_number);
      for (String u : by_user) xw.textElement("USER",u);
      xw.end("LINE");
    }
   
}       // end of inner class LineData

}       // end of class BvcrLineSet




/* end of BvcrLineSet.java */

