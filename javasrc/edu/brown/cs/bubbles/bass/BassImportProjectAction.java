/********************************************************************************/
/*										*/
/*		BassImportProjectAction.java							*/
/*										*/
/*	Action for importing an external project				*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

package edu.brown.cs.bubbles.bass;


import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaErrorBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


class BassImportProjectAction extends AbstractAction {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1L;

private File	      proj_dir;
private Component	 the_source;


/********************************************************************************/
/*										*/
/*     Constructors								*/
/*										*/
/********************************************************************************/

BassImportProjectAction()
{
   super("Import Project");
}


/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   BoardMetrics.noteCommand("BASS", "ImportProject");
   BudaRoot.hideSearchBubble(e);
   JFileChooser chooser = new JFileChooser(fsv);

   chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
   chooser.setDialogTitle("Choose a project directory to import");
   the_source = (Component) e.getSource();

   int returnVal = chooser.showOpenDialog(null);
   if (returnVal == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      File f1 = fsv.createFileObject(BoardSetup.getSetup().getDefaultWorkspace());
      proj_dir = fsv.createFileObject(f1,f.getName());

      try {
	 if (!Arrays.asList(f.list()).contains(".project"))
	    throw new Exception("Directory is not an eclipse project");
	 copyR(f, proj_dir);
       }
      catch (Throwable t) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(the_source);
	 BudaErrorBubble bub = new BudaErrorBubble("Error importing project: " + t.getMessage());
	 if (bba != null) {
	    bba.addBubble(bub, the_source, null, BudaConstants.PLACEMENT_LEFT);
	  }
	 return;
       }
      BumpClient bc = BumpClient.getBump();

      bc.importProject(f.getName());

      // import working sets
      File wsdir = fsv.createFileObject(proj_dir,"workingsets");
      String[] wlist = wsdir.list();
      if (wlist != null && wlist.length > 0) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(the_source);
	 if (bba != null) {
	    bba.addBubble(new WorkingSetConfirmDialog(),the_source, null, BudaConstants.PLACEMENT_LEFT);
	  }
       }
    }
}

private class WorkingSetConfirmDialog extends BudaBubble implements ActionListener {
   private static final long serialVersionUID = 1L;

   WorkingSetConfirmDialog() {
      setContentPane(new ConfirmContentPane());
    }

   private class ConfirmContentPane extends JPanel {

      private static final long serialVersionUID = 1L;

      private ConfirmContentPane() {
         add(new JLabel("Load and display working sets from project?"));
         JButton okB = new JButton("Yes");
         okB.setActionCommand("Yes");
         okB.addActionListener(WorkingSetConfirmDialog.this);
      
         JButton noB = new JButton("No");
         noB.setActionCommand("No");
         noB.addActionListener(WorkingSetConfirmDialog.this);
      
         add(okB);
         add(noB);
       }

    }	    // end of inner class ConfirmContentPane



   @Override public void actionPerformed(ActionEvent e)
      {
      if (e.getActionCommand().equals("Yes")) {
	 loadWorkingSets();
       }
      this.setVisible(false);
    }

}	// end of inner class WorkingSetConfirmDialog




private void loadWorkingSets()
{
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   File wsdir = fsv.createFileObject(proj_dir,"workingsets");
   for (String filename : wsdir.list()) {
      File wsf = fsv.createFileObject(wsdir,filename);
      if (wsf.getName().contains(".")
	     && wsf.getName().substring(wsf.getName().lastIndexOf('.')).equals(".xml")) {
	 Element xml = IvyXml.loadXmlFromFile(wsf);
	 fixFilePaths(xml, proj_dir.getName());

	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(the_source);
	 if (bba != null) bba.addWorkingSet(xml);
       }
    }
}




/**
 * Copies the given file or directory recursively including its contents
 * @param src
 * @param dest
 * @throws IOException
 */
private void copyR(File src,File dest) throws IOException
{
   if (src.isDirectory()) {
      FileSystemView fsv = BoardFileSystemView.getFileSystemView();
      if (!dest.exists()) dest.mkdir();
      for (String file : src.list()) {
	 copyR(fsv.createFileObject(src,file), fsv.createFileObject(dest,file));
       }
    }
   else {
      InputStream in = new FileInputStream(src);
      OutputStream out = new FileOutputStream(dest);

      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0)
	 out.write(buf, 0, len);

      in.close();
      out.close();
    }

}




private void fixFilePaths(Element xml,String proj)
{
   String attr = xml.getAttribute("FILE");
   if (attr != null && attr.contains(proj)) {
      xml.setAttribute("FILE", BoardSetup.getSetup().getDefaultWorkspace()
			  + File.separator + attr.substring(attr.indexOf(proj)));
    }

   NodeList nl = xml.getChildNodes();
   for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
	 fixFilePaths((Element) n, proj);
       }
    }
}


} // end of class BassImportProjectAction


/* end of BassImportProjectAction.java */
