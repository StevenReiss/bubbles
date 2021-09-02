/********************************************************************************/
/*                                                                              */
/*              BvfmRepoName.java                                               */
/*                                                                              */
/*      Name for package explorer search                                        */
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



package edu.brown.cs.bubbles.bvfm;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleGroup;

class BvfmRepoName extends BassNameBase implements BvfmConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected BvfmVirtualFile virtual_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BvfmRepoName(BvfmVirtualFile vf) 
{
   virtual_file = vf;
   name_type = BassNameType.VIRTUAL_FILE;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BvfmVirtualFile getVirtualFile()                { return virtual_file; }



/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public String getProject()
{
   return null;
}


@Override public String getSymbolName()
{
   return null;
}


@Override public String getParameters()
{
   return null;
}


@Override public String getKey()
{
   return null;
}


@Override public Icon getDisplayIcon()
{
   return BoardImage.getIcon("virtualfile");
}


@Override public BudaBubble createBubble()
{
   return null;
}


@Override public BudaBubble createPreviewBubble()
{
   return null;
}


@Override public BudaBubbleGroup createBubbleGroup(BudaBubbleArea bba)
{
   List<BudaBubble> bbls = new ArrayList<>();
   for (BvfmFileElement elt : virtual_file.getElements()) {
      BudaBubble bb = elt.createBubble(bba);
      if (bb != null) {
         bb.setVisible(false);
         bbls.add(bb);
       }
    }
   
   BudaBubbleGroup grp = bba.createHiddenBubbleGroup(virtual_file.getName(),bbls);
   
   return grp;
}



}       // end of class BvfmRepoName




/* end of BvfmRepoName.java */

