/********************************************************************************/
/*										*/
/*		BqstPanel.java							*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Yu Li				*/
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

package edu.brown.cs.bubbles.bqst;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardUpload;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 *	This class is used to create Form Dialog.
 **/
public class BqstPanel extends JDialog implements BqstConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JPanel		  form_panel;
private String		  form_title;
private JScrollPane	     form_scrollpane;
private ArrayList<BqstQuestion> question_list;
private ArrayList<File>  file_list;
private JLabel		  err_msg;
private GridBagConstraints	form_cons;
private boolean 	 screenshot_send_flag = false;

private static final long	serialVersionUID     = 1L;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BqstPanel(Frame frame,String title)
{
   super(frame);
   setLocationRelativeTo(frame);
   setLocation(frame.getX() + (frame.getSize().width - FORM_WIDTH) / 2,
		  frame.getY() + (frame.getSize().height - FORM_HEIGHT) / 2);
   setTitle(title);
   form_title = title;
   question_list = new ArrayList<BqstQuestion>();
   file_list = new ArrayList<File>();
   form_panel = new JPanel();
   form_panel.setLayout(new GridBagLayout());
   form_panel.setBackground(BoardColors.getColor(BG_COLOR_PROP));
   form_panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
   form_cons = new GridBagConstraints();
   form_cons.gridy = 0;
   form_cons.gridx = 0;
   form_cons.fill = GridBagConstraints.HORIZONTAL;
   form_scrollpane = new JScrollPane(form_panel);
   setContentPane(form_scrollpane);

   addFormTitle();
}




void addFormTitle()
{
   JLabel title = new JLabel("<html><FONT size=+1>" + form_title + "</FONT></html>");
   JLabel instr = new JLabel(INSTRUCTION_TEXT);
   err_msg = new JLabel("  ");
   form_panel.add(title, form_cons);
   form_cons.gridy++;
   form_panel.add(instr, form_cons);
   form_cons.gridy++;
   form_panel.add(err_msg, form_cons);
}


/********************************************************************************/
/*										*/
/*	Methods of adding different types of questions				*/
/*										*/
/********************************************************************************/

/**
 * Add a question with a short text field.
 * @param question   the text of question
 * @param help	     the text of help message
 * @param required   whether question is required or not
 **/
public void addShortText(String question,String help,boolean required)
{
   BqstShortText q = new BqstShortText(question,help,required);
   registerQuestion(q);
}



/**
 * Add a question with a long text field.
 * @param question question
 * @param help help message
 * @param required question is optional(false) or required(true)
 **/
public void addLongText(String question,String help,boolean required)
{
   BqstLongText q = new BqstLongText(question,help,required);
   registerQuestion(q);
}



/**
 * Add a question with multiple choices.
 * @param question    the text of question
 * @param help	      the text of help message
 * @param options     list of choices
 * @param icons       list of icons
 * @param sicons      list of selected icons
 * @param others      whether question has "others" choice
 * @param required    whether question is required or not
 **/
public void addMultiChoices(String question,String help,String[] options,Icon[] icons,
			       Icon[] sicons,boolean others,boolean required)
{
   BqstMultiChoices q = new BqstMultiChoices(question,help,options,icons,sicons,required,others);
   registerQuestion(q);
}



/**
 * Add a question with check boxes.
 * @param question   the text of question
 * @param help	     the text of help message
 * @param required   whether question is required or not
 * @param options    list of choices
 * @param others     whether question has "others" choice
 **/
public void addCheckBoxes(String question,String help,String[] options,boolean others,
			     boolean required)
{
   BqstCheckBoxes q = new BqstCheckBoxes(question,help,options,required,others);
   registerQuestion(q);
}



/**
 * Add a question which is invisible to user, but is able to output to the file.
 * @param question   the text of question
 * @param hiddentext the text of hidden text
 **/
public void addHiddenField(String question,String hiddentext)
{
   BqstHiddenField q = new BqstHiddenField(question,hiddentext);
   registerQuestion(q);
}



/**
 * Add a file. this file will be submitted while the form is submitted,
 * and URL will be written in the output file.
 * @param f  the file you want to submit
 **/
public void addOtherSubmitFile(File f)
{
   file_list.add(f);
}



/**
 * If this method is called, the screenshot of current view
 * will be uploaded automatically while the form is submitted,
 * and URL will be written in the output file.
 **/
public void setScreenshotFlag(boolean send)
{
   screenshot_send_flag = send;
}



/********************************************************************************/
/*										*/
/*	Setup assistance methods						*/
/*										*/
/********************************************************************************/

private void addScreenShot()
{
   File f = null;
   try {
      f = File.createTempFile("screenshot", ".pdf");
      // buda_root.exportViewportAsPdf(f);
      file_list.add(f);
    }
   catch (Exception e) {
      BoardLog.logE("BQST", "SCREENSHOT PDF FAILURE", e);
    }
   finally {
      // if (f != null) f.delete();
    }
}




private void registerQuestion(BqstQuestion q)
{
   q.setup();
   form_cons.gridy++;
   form_panel.add(q, form_cons);
   question_list.add(q);
}




/********************************************************************************/
/*										*/
/*	Methods for displaying							*/
/*										*/
/********************************************************************************/

/**
 * Display form, this method should be called after adding different questions.
 **/
public void display()
{
   form_cons.gridy++;
   form_panel.add(makeButtonPanel(), form_cons);
   form_scrollpane.setPreferredSize(new Dimension(FORM_WIDTH + 40,FORM_HEIGHT));
   setSize(new Dimension(FORM_WIDTH + 40,FORM_HEIGHT));
   setVisible(true);
}




private JPanel makeButtonPanel()
{
   JPanel buttonpanel = new JPanel();
   JButton sb = new JButton("submit");
   sb.addActionListener(new SubmitListener());
   JButton cl = new JButton("cancel");
   cl.addActionListener(new CancelListener());
   JButton rs = new JButton("reset");
   rs.addActionListener(new ResetListener());
   buttonpanel.add(sb);
   buttonpanel.add(cl);
   buttonpanel.add(rs);
   buttonpanel.setPreferredSize(BUTTONPANE_SIZE);
   buttonpanel.setOpaque(false);
   return buttonpanel;
}




/********************************************************************************/
/*										*/
/*	Methods for checking whether the required fields are filled or not	*/
/*										*/
/********************************************************************************/

/**
 * Return whether all required questions are filled
 **/

private boolean checkForm()
{
   boolean filledflag = true;
   for (int i = 0; i < question_list.size(); i++) {
      if (question_list.get(i).getAnswer() == null) {
	 question_list.get(i).markUnfilled();
	 filledflag = false;
       }
      else {
	 question_list.get(i).markFilled();
       }
    }
   return filledflag;
}




/********************************************************************************/
/*										*/
/*	Methods for generating an xml file of questions and user's input        */
/*										*/
/********************************************************************************/

/**
 * Generate output file and upload it to server
 **/

private void outputFile()
{
   File f = null;
   try {
      BoardProperties bp = BoardProperties.getProperties("Metrics");
      String userid = bp.getProperty(BoardConstants.BOARD_METRIC_PROP_USERID);
      String name = form_title.replaceAll("\\s", "_") + "_" + userid + "_";
      f = File.createTempFile(name, ".xml");
      IvyXmlWriter xw = new IvyXmlWriter(f);
      xw.begin(form_title);
      Date d = new Date();
      xw.field("DATE", d.toString());
      xw.field("TIME", d.getTime());
      for (int i = 0; i < question_list.size(); i++) {
	 xw.textElement(question_list.get(i).getTitle(), question_list.get(i).getAnswer());
       }
      xw.end(form_title);
      xw.close();

      BoardUpload uploader = new BoardUpload(f,"FORM",form_title.replaceAll("\\s", "_"));
      BoardLog.logI("BQST", form_title + ": " + uploader.getFileURL());
    }
   catch (IOException e) {}
   finally {
      if (f != null) f.delete();
    }

}



private void uploadOtherFiles()
{
   BoardUpload uploader;
   if (file_list.size() > 0) {
      for (int i = 0; i < file_list.size(); i++) {
	 try {
	    uploader = new BoardUpload(file_list.get(i),"FORM",form_title.replaceAll("\\s","_"));
	    addHiddenField(file_list.get(i).getName(), uploader.getFileURL());
	    BoardLog.logI("BQST", file_list.get(i).getName() + ": " + uploader.getFileURL());
	  }
	 catch (IOException e) {
	    BoardLog.logE("File uploading error: ", e.getMessage());
	  }
       }
    }
}



private void reset()
{
   err_msg.setText("  ");
   for (int i = 0; i < question_list.size(); i++) {
      question_list.get(i).reset();
    }
   form_scrollpane.getViewport().setViewPosition(new Point(0,0));
}




/********************************************************************************/
/*										*/
/*	Action listeners							*/
/*										*/
/********************************************************************************/

private final class SubmitListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      if (checkForm()) {
	 if (screenshot_send_flag) addScreenShot();
	 uploadOtherFiles();
	 outputFile();
	 reset();
	 err_msg.setText(THANKS_TEXT);
	 form_scrollpane.getViewport().setViewPosition(new Point(0,0));
       }
      else {
	 err_msg.setText(UNFILLED_TEXT);
	 form_scrollpane.getViewport().setViewPosition(new Point(0,0));
	 validate();
       }
    }

}	// end of inner class SubmitListener




private final class ResetListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      reset();
    }


}	// end of inner class ResetListener




private final class CancelListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      dispose();
    }

}	// end of inner class CanceltListener




}	// end of class BqstPanel




/* end of class BqstPanel.java */
