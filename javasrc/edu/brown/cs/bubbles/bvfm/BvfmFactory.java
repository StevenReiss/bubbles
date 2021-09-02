/********************************************************************************/
/*                                                                              */
/*              BvfmFactory.java                                                */
/*                                                                              */
/*      Main accesses point for Bubbles Virtual File Manager                    */
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

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassPopupHandler;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleGroup;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

public class BvfmFactory implements BvfmConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BvfmLibrary             vfm_library;

private static BvfmFactory      the_factory = null;


/********************************************************************************/
/*                                                                              */
/*      Static entries                                                          */
/*                                                                              */
/********************************************************************************/

public static void setup()
{
   getFactory();
}


public static void initialize(BudaRoot br)
{
}


public static synchronized BvfmFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BvfmFactory();
    }
   return the_factory;
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BvfmFactory() 
{
   vfm_library = new BvfmLibrary();
   vfm_library.loadLibrary();
   BudaRoot.addBubbleViewCallback(new ViewCallback());
   BassFactory.getFactory().addPopupHandler(new BvfmButtonHandler());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BvfmLibrary getLibrary()                        { return vfm_library; }



/********************************************************************************/
/*                                                                              */
/*      Bubble view callback                                                    */
/*                                                                              */
/********************************************************************************/

private class ViewCallback implements BudaConstants.BubbleViewCallback {
   
   @Override public void addGroupButtons(BudaBubbleGroup grp,JPopupMenu menu) {
      if (grp == null || menu == null) return;
      String ttl = grp.getTitle();
      if (ttl == null) return;
      BvfmVirtualFile curf = vfm_library.getVirtualFileForName(ttl);
      if (curf == null) menu.add(new CreateAction(grp));
      else {
         menu.add(new UpdateAction(curf,grp));
         menu.add(new RemoveAction(curf));
       }
    }
   
   @Override public void noteNamedGroup(BudaBubbleGroup grp,String oldname) {
      if (oldname == null && grp.getTitle() != null) {
         // might want to autmoatically create a group here
       }
    }
   
}       // end of inner class ViewCallback



private class CreateAction extends AbstractAction {
   
   private BudaBubbleGroup bubble_group;
   
   CreateAction(BudaBubbleGroup grp) {
      super("Create Virtual File for " + grp.getTitle());
      bubble_group = grp;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      BvfmVirtualFile vf = new BvfmVirtualFile(bubble_group);
      vfm_library.addVirtualFile(vf);
    }
        
}       // end of inner class CreateAction


private class UpdateAction extends AbstractAction {
   
   private BudaBubbleGroup bubble_group;
   private BvfmVirtualFile virtual_file;
   
   UpdateAction(BvfmVirtualFile vf,BudaBubbleGroup grp) {
      super("Update Virtual File " + vf.getName());
      bubble_group = grp;
      virtual_file = vf;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      if (bubble_group != null) {
         // update vf based on bubble_group
       }
      if (virtual_file != null) {
         vfm_library.editVirtualFile(virtual_file);
       }
    }
   
}       // end of inner class CreateAction


private class RemoveAction extends AbstractAction {
   
   private BvfmVirtualFile virtual_file;
   
   RemoveAction(BvfmVirtualFile vf) {
      super("Remove Virtual File " + vf.getName());
      virtual_file = vf;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      vfm_library.removeVirtualFile(virtual_file);
    }

}       // end of inner class CreateAction




/********************************************************************************/
/*                                                                              */
/*      Handle popup menu requests                                              */
/*                                                                              */
/********************************************************************************/

private class BvfmButtonHandler implements BassPopupHandler {
   
   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,
         String fullname,BassName forname) {
      if (!(forname instanceof BvfmRepoName)) return;
      BvfmRepoName rnm = (BvfmRepoName) forname;
      BvfmVirtualFile vf = rnm.getVirtualFile();
      if (vf != null) {
         menu.add(new RemoveAction(vf));
       }
    }
   
}       // end of inner class BvfmButtonHandler



}       // end of class BvfmFactory




/* end of BvfmFactory.java */

