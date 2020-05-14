/********************************************************************************/
/*										*/
/*		BassRepositoryMerge.java					*/
/*										*/
/*	Bubble Augmented Search Strategies store for all possible names 	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import java.util.ArrayList;



/**
 *	Provide a repository that merges two other Bass repositories.
 **/

public class BassRepositoryMerge implements BassConstants.BassRepository, BassConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BassRepository first_repository;
private BassRepository second_repository;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Create a new repository that contains all the elements of the first
 *	followed by all the elements of the second.
 **/

public BassRepositoryMerge(BassRepository p1,BassRepository p2)
{
   first_repository = p1;
   second_repository = p2;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   ArrayList<BassName> rslt = new ArrayList<BassName>();

   if (first_repository != null) {
      for (BassName bn : first_repository.getAllNames()) rslt.add(bn);
    }
   if (second_repository != null) {
      for (BassName bn : second_repository.getAllNames()) rslt.add(bn);
    }

   return rslt;
}



@Override public boolean includesRepository(BassRepository br)
{
   if (br == this) return true;
   if (first_repository != null && first_repository.includesRepository(br)) return true;
   if (second_repository != null && second_repository.includesRepository(br)) return true;

   return false;
}



}	// end of class BassRepositoryMerge




/* end of BassRepositoryMerge.java */
