/********************************************************************************/
/*										*/
/*		BgtaLoginInfoRepository.java					*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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


package edu.brown.cs.bubbles.bgta;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;


class BgtaLoginInfoRepository implements BgtaConstants, BassConstants.BassRepository {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<BgtaLoginName>  all_info;
private Vector<BgtaManager> manager_list;



/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

BgtaLoginInfoRepository(Vector<BgtaManager> mans,BgtaRepository rep)
{
   manager_list = mans;
   all_info = new HashSet<BgtaLoginName>();
   all_info.add(new BgtaLoginName("zzzz#Manage Accounts",manager_list,rep,
	    BGTA_GEN_ACCOUNT_PRIORITY));
}



/********************************************************************************/
/*										*/
/*	BassRepository methods							*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   return new ArrayList<BassName>(all_info);
}

@Override public boolean includesRepository(BassRepository br)
{
   return br == this;
}



}	// end of class BgtaLoginInfoRepository



/* end of BgtaLoginInfoRepository.java */
