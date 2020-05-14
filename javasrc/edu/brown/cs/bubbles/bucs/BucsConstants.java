/********************************************************************************/
/*                                                                              */
/*              BucsConstants.java                                              */
/*                                                                              */
/*      Bubbles Code Search constants                                           */
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



package edu.brown.cs.bubbles.bucs;


import java.awt.image.BufferedImage;
import java.util.List;


public interface BucsConstants
{

   
/********************************************************************************/
/*                                                                              */
/*      Search Request Handller                                                 */
/*                                                                              */
/********************************************************************************/

interface BucsSearchRequest {
   
   void handleSearchFailed();
   void handleSearchSucceeded(List<BucsSearchResult> result);
   void handleSearchInputs(List<BucsSearchInput> result);

}       // end of inner interface SearchRequest   



interface BucsSearchResult {
   
   String getResultName();
   String getCode();
   String getSource();
   int getNumLines();
   int getCodeSize();
   String getLicenseUid();
   
}       // end of inner interface BucsSearchResult



interface BucsSearchInput {
   
   BufferedImage getImage();
   
}       // end of inner interface BucsSearchInput




/********************************************************************************/
/*                                                                              */
/*      Context definitions                                                     */
/*                                                                              */
/********************************************************************************/

enum UserFileType {
   READ, WRITE, DIRECTORY
}


}       // end of interface BucsConstants




/* end of BucsConstants.java */

