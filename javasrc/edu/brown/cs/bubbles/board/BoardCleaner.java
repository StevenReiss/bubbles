/********************************************************************************/
/*                                                                              */
/*              BoardCleaner.java                                               */
/*                                                                              */
/*      Shared Cleaner class                                                    */
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



package edu.brown.cs.bubbles.board;

// import java.lang.ref.Cleaner;
import java.lang.reflect.Method;

public class BoardCleaner implements BoardConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Object board_cleaner;
// private static Cleaner board_cleaner;

private static Method register_method = null;

static {
   // board_cleaner = Cleaner.create();
   try {
      Class<?> c = Class.forName("java.lang.ref.Cleaner");
      Method m1 = c.getMethod("create");
      register_method = c.getMethod("register",Object.class,Runnable.class);
      board_cleaner = m1.invoke(null);
    }
   catch (Throwable t) { }
}


/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

public static void register(Object obj,Runnable action)
{
   if (board_cleaner != null && register_method != null) {
      try {
         register_method.invoke(board_cleaner,obj,action);
       }
      catch (Throwable t) { }
    }
}


}       // end of class BoardCleaner




/* end of BoardCleaner.java */

