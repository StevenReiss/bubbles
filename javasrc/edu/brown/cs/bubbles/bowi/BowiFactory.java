/********************************************************************************/
/*										*/
/*		BowiFactory.java						*/
/*										*/
/*	BowiFactory								*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman		      	*/
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

package edu.brown.cs.bubbles.bowi;

import edu.brown.cs.bubbles.buda.BudaRoot;


/**
 * 
 * A class to handle inter-package communication with respect to visual feedback
 * when actions take significant amounts of time
 *
 */
public class BowiFactory {
   /********************************************************************************/
   /*										*/
   /*	Private Storage 							*/
   /*										*/
   /********************************************************************************/
   private static BowiFactory the_factory = new BowiFactory();
   private static BowiTaskManager my_task_manager;

   /********************************************************************************/
   /*										*/
   /*	Constructors								*/
   /*										*/
   /********************************************************************************/

   /**
    *	Return the singleton instance of the Bass search bubble factory.
    **/

   public static BowiFactory getFactory()
   {
      return the_factory;
   }

   private BowiFactory()				{ }

   /********************************************************************************/
   /*										*/
   /*	Setup methods								*/
   /*										*/
   /********************************************************************************/
   
   /**
    *	Called by initialization to ensure that the search package is set up correctly.
    **/
   
   public static void setup()
   {
      // work is done by the static initializer
   }

   /**
    *	Called to initialize once BudaRoot is setup
    **/

   public static void initialize(BudaRoot br)
   {
      my_task_manager = new BowiTaskManager(br);
   }
   
   /********************************************************************************/
   /*										*/
   /*	Task methods								*/
   /*										*/
   /********************************************************************************/
   
   /**
    * Method to start a task
    */
   public static void startTask() {
      if (my_task_manager != null)
         my_task_manager.startTask();
   }
   
   /**
    * Method to end a task
    * @param tostop
    */
   public static void stopTask() {
      if (my_task_manager != null)
         my_task_manager.stopTask();
   }

}
