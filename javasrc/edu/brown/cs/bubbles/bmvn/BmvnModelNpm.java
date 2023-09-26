/********************************************************************************/
/*                                                                              */
/*              BmvnModelNpm.java                                               */
/*                                                                              */
/*      Library model for using npm and package.json files                      */
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



package edu.brown.cs.bubbles.bmvn;

import java.io.File;


class BmvnModelNpm extends BmvnModel
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BmvnModelNpm(BmvnProject proj,File file)
{
   super(proj,file,"Npm");
}



}       // end of class BmvnModelNpm




/* end of BmvnModelNpm.java */

