/********************************************************************************/
/*										*/
/*		BicexConstants.java						*/
/*										*/
/*	Constants for Bubbles Interface for Continuous EXecution		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bicex;

import java.awt.Graphics2D;
import java.io.File;
import java.util.Collection;
import java.util.EventListener;
import java.util.Map;

import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.buda.BudaBubble;

public interface BicexConstants 
{

   
   
/********************************************************************************/
/*                                                                              */
/*      Graphics settings                                                       */
/*                                                                              */
/********************************************************************************/

String BICEX_EVAL_TOP_COLOR_PROP = "Bicex.EvalTopColor";
String BICEX_EVAL_BOTTOM_COLOR_PROP = "Bicex.EvalBottomColor";

int BICEX_EVAL_WIDTH = 600;
int BICEX_EVAL_HEIGHT = 300;
   
   
String BICEX_EVAL_SCROLL_COLOR_PROP = "Bicex.EvalScrollColor";
String BICEX_EVAL_SCROLL_CONTEXT_COLOR_PROP = "Bicex.EvalScrollContextColor";


String BICEX_EXECUTE_ANNOT_COLOR_PROP = "Bicex.annot.color";



/********************************************************************************/
/*                                                                              */
/*      Evaluation exit types                                                   */
/*                                                                              */
/********************************************************************************/

enum ExitType {
   NONE, ERROR, EXCEPTION, TIMEOUT, COMPILER_ERROR, RETURN, HALTED, PENDING, WAIT
}   
   


/********************************************************************************/
/*                                                                              */
/*      Update interface                                                        */
/*                                                                              */
/********************************************************************************/

interface BicexEvaluationUpdated extends EventListener {
    
   void evaluationUpdated(BicexRunner bex);
   void contextUpdated(BicexRunner bex);;
   void timeUpdated(BicexRunner bex);
   void evaluationReset(BicexRunner bex);
   String inputRequest(BicexRunner bex,String file);
   String valueRequest(BicexRunner bex,String var);
   void editorAdded(BudaBubble bw);
   
}       // end of interface BicexEvaluationHandler








interface DisplayModel {
   int getWidth();
   int getHeight();
   String getName();
   boolean useTime();
   void paintToTime(Graphics2D g,long when);
}


/********************************************************************************/
/*                                                                              */
/*      External access to executions                                           */
/*                                                                              */
/********************************************************************************/

interface BicexRunner {
   
   void addUpdateListener(BicexEvaluationUpdated upd);
   void removeUpdateListener(BicexEvaluationUpdated upd);
   
   void startContinuousExecution() throws BicexException;
   void startExecution() throws BicexException;
   void addFiles(Collection<File> files);
   void remove();
   
   BicexResult getEvaluation();
   
}



interface BicexResult {
   
}


interface BicexResultContext {
   
   String getMethod();
   String getShortName();
   
   BicexCountData getCountData();
   
}


interface BicexCountData extends Map<String,Map<Integer,int []>> { }





interface BicexPopupCallback extends EventListener {
   
   void addPopupButtons(JPopupMenu menu,BicexResultContext ctx,String method,long when);
   
}


interface BicexTreeNode {
   
   boolean isUpdatedCurrently();
   
}




}	// end of interface BicexConstants




/* end of BicexConstants.java */

