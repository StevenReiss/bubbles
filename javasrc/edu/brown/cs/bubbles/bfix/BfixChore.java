/********************************************************************************/
/*                                                                              */
/*              BfixChore.java                                                  */
/*                                                                              */
/*      Generic chore that can be created by an adapter                         */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bfix;

import java.io.File;
import java.lang.ref.WeakReference;


public abstract class BfixChore implements BfixConstants

{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String chore_name;
private WeakReference<BfixCorrector> corrector_ref;


  
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BfixChore(String name,BfixCorrector corr)
{ 
   chore_name = name;
   corrector_ref = new WeakReference<BfixCorrector>(corr);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

protected BfixCorrector getCorrector()
{
   return corrector_ref.get();
}


File getFile()
{
   BfixCorrector corr = getCorrector();
   if (corr == null) return null;
   return corr.getEditor().getWindowDocument().getFile();
}



/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

abstract void execute();

boolean validate(boolean force)
{
   BfixCorrector corr = getCorrector();
   if (corr == null) return false;
   return validate(corr,force);
}

abstract boolean validate(BfixCorrector corr,boolean force);



/********************************************************************************/
/*                                                                              */
/*      Ouptut methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return chore_name;
}


/********************************************************************************/
/*                                                                              */
/*      Equals and hashCode for chores                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o) 
{
   if (!(o instanceof BfixChore)) return false;
   BfixChore bt = (BfixChore) o;
   return bt.chore_name.equals(chore_name) && bt.getCorrector() == getCorrector();
}


@Override public int hashCode()
{
   return chore_name.hashCode() + System.identityHashCode(getCorrector());
}


}       // end of class BfixChore




/* end of BfixChore.java */

