/********************************************************************************/
/*										*/
/*		BgtaLoginName.java						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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


package edu.brown.cs.bubbles.bgta;

import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.buda.BudaBubble;

import java.util.Vector;


class BgtaLoginName extends BassNameBase implements BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Vector<BgtaManager>	manager_list;
private String			my_text;
private BgtaRepository		my_repository;
private int			sort_prior;
private boolean 		has_bubble;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaLoginName(String txt,Vector<BgtaManager> mans,BgtaRepository rep,int prior)
{
   super();

   name_type = BassNameType.CHAT_LOGIN;

   manager_list = mans;
   my_text = BGTA_BUDDY_PREFIX + txt;
   my_repository = rep;
   sort_prior = prior;
   has_bubble = false;
}



/********************************************************************************/
/*										*/
/*	BassNameBase methods							*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble()
{
   if (!has_bubble) {
      has_bubble = true;
      return new BgtaLoginBubble(manager_list,my_repository,this);
    }
   return null;
}



@Override public BudaBubble createPreviewBubble()
{
   return createBubble();
}



void setHasBubble(boolean has)
{
   has_bubble = has;
}



@Override protected String getKey()
{
   return my_text;
}


@Override protected String getParameters()
{
   return null;
}


@Override public String getProject()
{
   return null;
}


@Override protected String getSymbolName()
{
   return my_text;
}


@Override public String getFullName()
{
   return " " + my_text;
}


@Override public int getSortPriority()
{
   return sort_prior;
}



}	// end of class BgtaLoginName



/* end of BgtaLoginName.java */
