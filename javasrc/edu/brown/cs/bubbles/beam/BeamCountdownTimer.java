/********************************************************************************/
/*										*/
/*		BeamCountdownTimer.java 					*/
/*										*/
/*	Countdown timer bubble							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*	Based on code by 0sx4Rayal in github				      */
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



package edu.brown.cs.bubbles.beam;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.Timer;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingButton;
import edu.brown.cs.ivy.swing.SwingComboBox;


class BeamCountdownTimer extends BudaBubble implements BeamConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long cur_hours;
private long cur_minutes;
private long cur_seconds;
private long input_time;
private long last_tick_time;
private long running_time;
private long time_left;

private JLabel	label_time;

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
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamCountdownTimer()
{
   super(null,BudaBorder.RECTANGLE);
   cur_hours = 0;
   cur_minutes = 0;
   cur_seconds = 0;

   setContentPane(new CountdownPanel());
}




/********************************************************************************/
/*										*/
/*	Timer methods								*/
/*										*/
/********************************************************************************/

private void changeLabelTimer()
{
   label_time.setForeground(Color.BLACK);
   String text = time_formatter.format(cur_hours) + " : " +
	 time_formatter.format(cur_minutes)  + " : " +
	 time_formatter.format(cur_seconds);
   label_time.setText(text);
}



/********************************************************************************/
/*										*/
/*	Panel to hold the timer 						*/
/*										*/
/********************************************************************************/

private class CountdownPanel extends Box {

   private static final long serialVersionUID = 1;

   CountdownPanel() {
      super(BoxLayout.Y_AXIS);
      
      Color c = BoardColors.getColor("Beam.TimerColor");
      setBackground(c);
      setOpaque(true);
   
      label_time = new JLabel("",JLabel.CENTER);
      label_time.setFont(new Font("Arial",Font.PLAIN,36));
      changeLabelTimer();
      Box lblbox = Box.createHorizontalBox();
      lblbox.add(Box.createHorizontalGlue());
      lblbox.add(label_time);
      lblbox.add(Box.createHorizontalGlue());
   
      Vector<String> tfour = new Vector<>();
      for (int i = 0; i <= 24; ++i) {
         tfour.add(time_formatter.format(i));
       }
      Vector<String> sixty = new Vector<>();
      for (int i = 0; i <= 60; ++i) {
         sixty.add(time_formatter.format(i));
       }
   
      Font cbxfnt = new Font("Arial",Font.PLAIN,16);
      TimeAction tact = new TimeAction();
      hours_input = new SwingComboBox<>(tfour);
      hours_input.addActionListener(tact);
      hours_input.setFont(cbxfnt);
      minutes_input = new SwingComboBox<>(sixty);
      minutes_input.addActionListener(tact);
      minutes_input.setFont(cbxfnt);
      seconds_input = new SwingComboBox<>(sixty);
      seconds_input.addActionListener(tact);
      seconds_input.setFont(cbxfnt);
   
      Font lblfnt = new Font("Arial",Font.ITALIC,13);
      Box timebox = Box.createHorizontalBox();
      timebox.add(Box.createHorizontalGlue());
      Box vbox = Box.createVerticalBox();
      vbox.add(hours_input);
      JLabel hl = new JLabel("h",JLabel.CENTER);
      hl.setFont(lblfnt);
      vbox.add(hl);
      timebox.add(vbox);
      timebox.add(Box.createHorizontalStrut(2));
      vbox = Box.createVerticalBox();
      vbox.add(minutes_input);
      JLabel ml = new JLabel("min",JLabel.CENTER);
      ml.setFont(lblfnt);
      vbox.add(ml);
      timebox.add(vbox);
      timebox.add(Box.createHorizontalStrut(2));
      vbox = Box.createVerticalBox();
      vbox.add(seconds_input);
      JLabel sl = new JLabel("sec",JLabel.CENTER);
      sl.setFont(lblfnt);
      vbox.add(sl);
      timebox.add(vbox);
      timebox.add(Box.createHorizontalGlue());
   
      reset_button = new SwingButton(BoardImage.getIcon("timer-reset",50,50));
      reset_button.setContentAreaFilled(false);
      reset_button.addActionListener(new ResetAction());
      reset_button.setEnabled(false);
      start_button = new SwingButton(BoardImage.getIcon("timer-play",50,50));
      start_button.setContentAreaFilled(false);
      start_button.addActionListener(new StartAction());
      pause_button = new SwingButton(BoardImage.getIcon("timer-pause",50,50));
      pause_button.setContentAreaFilled(false);
      pause_button.addActionListener(new PauseAction());
      pause_button.setEnabled(false);
   
      Box btnbox = Box.createHorizontalBox();
      btnbox.add(Box.createHorizontalGlue());
      btnbox.add(reset_button);
      btnbox.add(Box.createHorizontalGlue());
      btnbox.add(start_button);
      btnbox.add(Box.createHorizontalGlue());
      btnbox.add(pause_button);
   
      add(Box.createVerticalGlue());
      add(lblbox);
      add(Box.createVerticalGlue());
      add(timebox);
      add(Box.createVerticalGlue());
      add(btnbox);
      add(Box.createVerticalGlue());
   
      BoardProperties bp = BoardProperties.getProperties("Beam");
      int w = bp.getInt("Beam.timer.width",250);
      int h = bp.getInt("Beam.timer.height",300);
      setPreferredSize(new Dimension(w,h));
   
      reset();
    }

}	// end of inner class CountdownPanel




/********************************************************************************/
/*										*/
/*	Action handling 							*/
/*										*/
/********************************************************************************/

private final class TimeAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      JComboBox<?> jbx = (JComboBox<?>) e.getSource();
      int val = Integer.parseInt((String) jbx.getItemAt(jbx.getSelectedIndex()));
      if (jbx == hours_input) {
         cur_hours = val;
       }
      else if (jbx == minutes_input) {
         cur_minutes = val;
       }
      else if (jbx == seconds_input) {
         cur_seconds = val;
       }
      changeLabelTimer();
    }

}	// end of inner class TimeAction



private final class StartAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      reset_button.setEnabled(true);
      pause_button.setEnabled(true);
      hours_input.setEnabled(false);
      minutes_input.setEnabled(false);
      seconds_input.setEnabled(false);
      input_time = ((cur_hours * 60 + cur_minutes)*60 + cur_seconds) * 1000;
      last_tick_time = System.currentTimeMillis();
      our_timer = new Timer(1000,new TickAction());
      our_timer.start();
    }

}	// end of inner class StartAction



private final class PauseAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      if (our_timer == null) return;
      our_timer.stop();
      pause_button.setEnabled(false);
      start_button.setEnabled(true);
    }

}	// end of inner class PauseAction



private void reset()
{
   cur_hours = 0;
   cur_minutes = 0;
   cur_seconds = 0;
   changeLabelTimer();
   reset_button.setEnabled(false);
   pause_button.setEnabled(false);
   start_button.setEnabled(true);
   hours_input.setSelectedIndex(0);
   minutes_input.setSelectedIndex(0);
   seconds_input.setSelectedIndex(0);
   hours_input.setEnabled(true);
   minutes_input.setEnabled(true);
   seconds_input.setEnabled(true);
}


private final class ResetAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      if (our_timer != null) our_timer.stop();
      try {
	 Thread.sleep(1);		// wait for timer to finish
       }
      catch (InterruptedException ex) { }
      reset();
    }

}	// end of inner class ResetAction


private final class TickAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      running_time = System.currentTimeMillis() - last_tick_time;
      time_left = input_time - running_time;
      Duration duration = Duration.ofMillis(time_left);
      cur_hours = duration.toHours();
      duration = duration.minusHours(cur_hours);
      cur_minutes = duration.toMinutes();
      duration = duration.minusMinutes(cur_minutes);
      cur_seconds = duration.toMillis()/1000;
      changeLabelTimer();

      if (cur_hours <= 0 && cur_minutes <= 0 && cur_seconds <= 0) {
         Alarm alarm = new Alarm();
         alarm.start();
         our_timer.stop();
	 reset();
       }
    }

}	// end of inner class TickAction



private static class Alarm extends Thread {
   
   Alarm() {
      super("CountdownTimeAlarm");
      setDaemon(true);
    }
   
   @Override public void run() {
      for (int i = 0; i < 10; ++i) {
         Toolkit.getDefaultToolkit().beep();
         try {
            Thread.sleep(500);
          }
         catch (InterruptedException ex) { }
       }
    }
   
}       // end of inner class Alarm



}	// end of class BeamCountdownTimer




/* end of BeamCountdownTimer.java */

