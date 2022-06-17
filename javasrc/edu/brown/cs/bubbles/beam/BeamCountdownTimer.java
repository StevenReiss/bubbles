/********************************************************************************/
/*                                                                              */
/*              BeamCountdownTimer.java                                         */
/*                                                                              */
/*      Countdown timer bubble                                                  */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*      Based on code by 0sx4Rayal in github                                  */
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



package edu.brown.cs.bubbles.beam;

import java.awt.Image;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingButton;
import edu.brown.cs.ivy.swing.SwingComboBox;

class BeamCountdownTimer extends BudaBubble implements BeamConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private long input_hours;
private long input_miunutes;
private long input_seconds;
private long input_time;
private long last_tick_time;
private long running_time;
private long time_left;

private JLabel  label_time;
private JLabel  label_hours;
private JLabel  label_minutes;
private JLabel  label_seconds;

private SwingButton reset_button;
private SwingButton start_button;
private SwingButton pause_button;
private SwingComboBox<String> hours_input;
private SwingComboBox<String> minutes_input;
private SwingComboBox<String> seconds_input;

private Timer our_timer;

private static DecimalFormat time_formatter = new DecimalFormat("00");

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BeamCountdownTimer()
{
   super(null,BudaBorder.RECTANGLE);
   input_hours = 0;
   input_miunutes = 0;
   input_seconds = 0;
   
   BoardProperties bp = BoardProperties.getProperties("Beam");
   int w = bp.getInt("Beam.timer.width",250);
   int h = bp.getInt("Beam.timer.height",300);
   
}

}       // end of class BeamCountdownTimer




/* end of BeamCountdownTimer.java */

