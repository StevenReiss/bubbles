/********************************************************************************/
/*                                                                              */
/*              BattRecorder.java                                               */
/*                                                                              */
/*      Record a history of test case usage                                     */
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



package edu.brown.cs.bubbles.batt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;

class BattRecorder implements BattConstants, BattConstants.BattModelListener 
{

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File    record_file;
private Map<BattTestCase,Boolean> known_tests;
private long    last_write;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BattRecorder(BattFactory bf)
{
   File f1 = BoardSetup.getBubblesWorkingDirectory();
   record_file = new File(f1,"tests.csv");
   known_tests = new HashMap<>();
   bf.addBattModelListener(this);
   last_write = 0;
}




/********************************************************************************/
/*                                                                              */
/*      Handle model updates                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void battModelUpdated(BattModeler bm)
{
   List<BattTestCase> tests = bm.getAllTests();
   boolean outtests = (tests.size() == known_tests.size());
   boolean ignore = true;
   if (tests.size() == 0) return;
   
   // make sure all tests are stable
   StringBuffer buf = new StringBuffer();
   Set<BattTestCase> fnd = new HashSet<>();
   
   for (BattTestCase btc : tests) {
      fnd.add(btc);
      Boolean testfg = known_tests.get(btc);
      if (testfg == null) outtests = true;
      switch (btc.getState()) {
         case CANT_RUN :
         case IGNORED :
            break;
         case EDITED :
         case NEEDS_CHECK :
         case PENDING :
         case TO_BE_RUN :
         case RUNNING :
         case STOPPED :
         case UNKNOWN :
            return;
         case UP_TO_DATE :
            break;
       }
      switch (btc.getStatus()) {
         case SUCCESS :
            buf.append(",1");
            if (testfg != Boolean.TRUE) ignore = false; 
            known_tests.put(btc,true);
            break;
         case FAILURE :
            buf.append(",0");
            if (testfg != Boolean.FALSE) ignore = false;
            known_tests.put(btc,false);
            break;
         case UNKNOWN :
            return;
       }
    }
   if (outtests) {
      for (Iterator<BattTestCase> it = known_tests.keySet().iterator(); it.hasNext(); ) {
         BattTestCase btc = it.next();
         if (!fnd.contains(btc)) it.remove();
       }
      ignore = false;
    }
   if (System.currentTimeMillis() - last_write > 1000*60*60*12) ignore = false;
   
   if (ignore) return;
   
   try {
      PrintWriter pw = new PrintWriter(new FileWriter(record_file,true));
      pw.println("TIME," + System.currentTimeMillis() + "," + new Date());
      if (outtests) {
         int idx = 0;
         for (BattTestCase btc : tests) {
            pw.println("TEST," + (idx++) + "," + btc.getClassName() + "," + btc.getName());
          }
       }
      pw.println("STATUS" + buf.toString());
      pw.close();
    }
   catch (IOException e) {
      BoardLog.logE("BATT","Problem writing test history",e);
    }
}




}       // end of class BattRecorder




/* end of BattRecorder.java */

