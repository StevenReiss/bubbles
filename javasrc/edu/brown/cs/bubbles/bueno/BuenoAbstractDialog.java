/********************************************************************************/
/*										*/
/*		BuenoAbstractDialog.java					*/
/*										*/
/*	BUbbles Environment New Objects creator abstract new dialog		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingTextField;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;



abstract class BuenoAbstractDialog implements BuenoConstants, ActionListener, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BuenoProperties property_set;
protected BuenoLocation insertion_point;
protected BuenoBubbleCreator bubble_creator;
protected JTextField focus_field;
protected BuenoType	create_type;

protected BuenoValidator the_validator;

protected Font		button_font;
protected Font		title_font;

private Component	source_component;
private Point		start_point;
private JButton 	accept_button;
private JLabel		title_label;
private String		title_text;

private ButtonGroup	protection_group;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BuenoAbstractDialog(Component src,Point p,BuenoProperties known,BuenoLocation insert,
				 BuenoBubbleCreator newer,BuenoType typ)
{
   source_component = src;
   start_point = p;

   property_set = known;
   insertion_point = insert;
   bubble_creator = newer;
   focus_field = null;
   accept_button = null;
   title_label = null;
   title_text = null;

   create_type = typ;
   the_validator = new BuenoValidator(new ValidCallback(),property_set,insertion_point,create_type);

   JLabel lbl = new JLabel();
   button_font = lbl.getFont().deriveFont(10f);
   title_font = lbl.getFont().deriveFont(12f);

   protection_group = new ButtonGroup();
}



/********************************************************************************/
/*										*/
/*	Dialog management							*/
/*										*/
/********************************************************************************/

public void showDialog()
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.setInsets(0);

   JLabel lbl = new JLabel();
   lbl.setFont(button_font);
   pnl.setLabelPrototype(lbl);
   lbl = new JLabel();
   lbl.setFont(title_font);
   pnl.setBannerPrototype(lbl);

   pnl.beginLayout();

   if (title_text == null) title_text = insertion_point.getTitle(create_type);
   title_label = pnl.addBannerLabel(title_text);

   setupPanel(pnl);

   addButtons(pnl);

   BudaBubble bb = new DialogBubble(pnl);
   pnl.setFont(button_font);

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_component);
   if (bba == null) return;

   bba.addBubble(bb,source_component,start_point,
	 PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_MOVETO|PLACEMENT_LOGICAL);
}


abstract void setupPanel(SwingGridPanel pnl);




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public void setLabel(String lbl)
{
   if (title_label != null) title_label.setText(lbl);
   else title_text = lbl;
}



/********************************************************************************/
/*										*/
/*	Creation routines							*/
/*										*/
/********************************************************************************/

abstract void doCreate(BudaBubbleArea bba,Point p);

protected BuenoType getCreationType()		{ return the_validator.getCreationType(); }

private void update()
{
   if (accept_button != null) {
      the_validator.updateParsing();
    }
}


private class ValidCallback implements BuenoValidatorCallback {

   @Override public void validationDone(BuenoValidator bv,boolean fg) {
      if (accept_button != null) {
	 accept_button.setEnabled(fg);
       }
    }

}	// end of inner class ValidCallback



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

void addButtons(SwingGridPanel pnl)
{
   accept_button = pnl.addBottomButton("Create","CREATE",this);
   accept_button.setFont(button_font);
   accept_button.setBorder(BorderFactory.createEmptyBorder());

   JButton btn = pnl.addBottomButton("Cancel","CANCEL",this);
   btn.setFont(button_font);
   btn.setBorder(BorderFactory.createEmptyBorder());

   pnl.addBottomButtons();

   update();
}



/********************************************************************************/
/*										*/
/*	String-valued property button						*/
/*										*/
/********************************************************************************/

protected class StringField extends SwingTextField implements ActionListener, CaretListener {

   private BuenoKey   field_key;
   private static final long serialVersionUID = 1;


   StringField(BuenoKey key)			{ this(key,24); }

   StringField(BuenoKey key,int width) {
      super(property_set.getStringProperty(key),24);
      setFont(button_font);
      field_key = key;
      addActionListener(this);
      addCaretListener(this);
      if (focus_field == null) focus_field = this;
    }

   @Override public void actionPerformed(ActionEvent e) {
      property_set.put(field_key,getText());
    }

   @Override public void caretUpdate(CaretEvent e) {
      property_set.put(field_key,getText());
      BuenoAbstractDialog.this.update();
    }

}	// end of inner class StringField



protected class BooleanField extends JCheckBox implements ActionListener {

   private BuenoKey   field_key;
   private static final long serialVersionUID = 1;

   BooleanField(BuenoKey key) {
      setSelected(property_set.getBooleanProperty(key));
      setFont(button_font);
      field_key = key;
      addActionListener(this);
    }

   @Override public void actionPerformed(ActionEvent e) {
      property_set.put(field_key,isSelected());
      BuenoAbstractDialog.this.update();
    }

}	// end of inner class BooleanField




/********************************************************************************/
/*										*/
/*	Protection modifier property button					*/
/*										*/
/********************************************************************************/

protected class ProtectionButton extends JRadioButton implements ChangeListener {

   private int modifier_value;
   private static final long serialVersionUID = 1;


   ProtectionButton(String name,int val) {
      super(name);
      setFont(button_font);
      modifier_value = val;
      protection_group.add(this);
      if ((property_set.getModifiers() & val) != 0) setSelected(true);
      addChangeListener(this);
    }

   @Override public void stateChanged(ChangeEvent e) {
      int v0 = property_set.getModifiers();
      if (isSelected()) {
	 v0 |= modifier_value;
       }
      else {
	 v0 &= ~modifier_value;
       }
      property_set.put(BuenoKey.KEY_MODIFIERS,v0);
      BuenoAbstractDialog.this.update();
    }

}	// end of inner class ProtectionButton





protected class ModifierButton extends JToggleButton implements ActionListener {

   private int modifier_value;
   private static final long serialVersionUID = 1;

   ModifierButton(String name,int val) {
      super(name);
      setFont(button_font);
      modifier_value = val;
      if ((property_set.getModifiers() & val) != 0) setSelected(true);
      addActionListener(this);
   }

   @Override public void actionPerformed(ActionEvent e) {
      int v0 = property_set.getModifiers();
      if (isSelected()) {
	 v0 |= modifier_value;
       }
      else {
	 v0 &= ~modifier_value;
       }
      property_set.put(BuenoKey.KEY_MODIFIERS,v0);
      BuenoAbstractDialog.this.update();
    }

}	// end of inner class ModiferButton



/********************************************************************************/
/*										*/
/*	Bubble container							*/
/*										*/
/********************************************************************************/

private class DialogBubble extends BudaBubble {

   DialogBubble(JPanel pnl) {
      setContentPane(pnl,focus_field);
      pnl.addMouseListener(new BudaConstants.FocusOnEntry(focus_field));
    }

}	// end of inner class DialogBubble



/********************************************************************************/
/*										*/
/*	Accept/cancel option management 					*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   String cmd = evt.getActionCommand();
   BudaBubble bb = BudaRoot.findBudaBubble((Component) evt.getSource());
   if (bb == null) return;
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   if (bba == null) return;
   Point where = bb.getLocation();

   if (cmd == null) ;
   else if (cmd.equals("CANCEL")) {
      bb.setVisible(false);
    }
   else {				// accept from text box as well
      if (!the_validator.checkParsing()) return;
      bb.setVisible(false);
      BubbleCreator bc = new BubbleCreator(bba,where);
      SwingUtilities.invokeLater(bc);
    }
}


private class BubbleCreator implements Runnable {
   
   private BudaBubbleArea bubble_area;
   private Point show_where;
   
   BubbleCreator(BudaBubbleArea bba,Point where) {
      bubble_area = bba;
      show_where = where;
    }
   
   @Override public void run() {
      doCreate(bubble_area,show_where);
    }
   
}       // end of inner class BubbleCreator



/********************************************************************************/
/*										*/
/*	Class Signature parsing methods 					*/
/*										*/
/********************************************************************************/

protected void parseClassSignature(String txt) throws BuenoException
{
   StreamTokenizer tok = new StreamTokenizer(new StringReader(txt));

   parseModifiers(tok);

   if (checkNextToken(tok,"class")) {
      create_type = BuenoType.NEW_CLASS;
    }
   else if (checkNextToken(tok,"enum")) {
      create_type = BuenoType.NEW_ENUM;
    }
   else if (checkNextToken(tok,"interface")) {
      create_type = BuenoType.NEW_INTERFACE;
    }
   else throw new BuenoException("No class/enum/interface keyword");

   parseName(tok);
   parseGenerics(tok);
   parseExtends(tok);
   parseImplements(tok);
   parseEnd(tok);
}



protected void parseGenerics(StreamTokenizer tok) throws BuenoException
{
   if (!checkNextToken(tok,'<')) return;
   parseType(tok);
   if (!checkNextToken(tok,'>')) throw new BuenoException("Unclosed generic specification");
}



protected void parseExtends(StreamTokenizer tok) throws BuenoException
{
   if (checkNextToken(tok,"extends")) {
      if (create_type == BuenoType.NEW_INTERFACE) {
	 List<String> rslt = new ArrayList<String>();
	 for ( ; ; ) {
	    String typ = parseType(tok);
	    rslt.add(typ);
	    if (!checkNextToken(tok,',')) break;
	  }
	 property_set.put(BuenoKey.KEY_EXTENDS,rslt);
       }
      else {
	 String typ = parseType(tok);
	 property_set.put(BuenoKey.KEY_EXTENDS,typ);
       }
    }
}



protected void parseImplements(StreamTokenizer tok) throws BuenoException
{
   if (checkNextToken(tok,"implements")) {
      if (create_type == BuenoType.NEW_INTERFACE)
	 throw new BuenoException("Interfaces don't use implements");
      List<String> rslt = new ArrayList<String>();
      for ( ; ; ) {
	 String typ = parseType(tok);
	 rslt.add(typ);
	 if (!checkNextToken(tok,',')) break;
       }
      property_set.put(BuenoKey.KEY_IMPLEMENTS,rslt);
    }
}




/********************************************************************************/
/*										*/
/*	Signature parsing methods						*/
/*										*/
/********************************************************************************/

protected void parseModifiers(StreamTokenizer stok)
{
   int mods = 0;

   for ( ; ; ) {
      if (nextToken(stok) != StreamTokenizer.TT_WORD) {
	 stok.pushBack();
	 break;
       }
      if (stok.sval.equals("public")) mods |= Modifier.PUBLIC;
      else if (stok.sval.equals("protected")) mods |= Modifier.PROTECTED;
      else if (stok.sval.equals("private")) mods |= Modifier.PRIVATE;
      else if (stok.sval.equals("static")) mods |= Modifier.STATIC;
      else if (stok.sval.equals("abstract")) mods |= Modifier.ABSTRACT;
      else if (stok.sval.equals("final")) mods |= Modifier.FINAL;
      else if (stok.sval.equals("native")) mods |= Modifier.NATIVE;
      else if (stok.sval.equals("synchronized")) mods |= Modifier.SYNCHRONIZED;
      else if (stok.sval.equals("transient")) mods |= Modifier.TRANSIENT;
      else if (stok.sval.equals("volatile")) mods |= Modifier.VOLATILE;
      else if (stok.sval.equals("strictfp")) mods |= Modifier.STRICT;
      else {
	 stok.pushBack();
	 break;
       }
    }

   property_set.put(BuenoKey.KEY_MODIFIERS,mods);
}




protected void parseName(StreamTokenizer stok) throws BuenoException
{
   if (nextToken(stok) != StreamTokenizer.TT_WORD) {
      throw new BuenoException("Name missing");
    }

   property_set.put(BuenoKey.KEY_NAME,stok.sval);
}


protected String parseType(StreamTokenizer stok) throws BuenoException
{
   String rslt = null;

   if (checkNextToken(stok,"byte") || checkNextToken(stok,"short") ||
	  checkNextToken(stok,"int") || checkNextToken(stok,"long") ||
	  checkNextToken(stok,"char") || checkNextToken(stok,"float") ||
	  checkNextToken(stok,"double") || checkNextToken(stok,"boolean") ||
	  checkNextToken(stok,"void")) {
      rslt = stok.sval;
    }
   else if (checkNextToken(stok,'?')) {
      rslt = "?";
      if (nextToken(stok) != StreamTokenizer.TT_WORD) {
	 stok.pushBack();
       }
      else if (checkNextToken(stok,"extends") || checkNextToken(stok,"super")) {
	 String ext = stok.sval;
	 String ntyp = parseType(stok);
	 rslt = rslt + " " + ext + " " + ntyp;
       }
      else {
	 stok.pushBack();
       }
    }
   else if (nextToken(stok) == StreamTokenizer.TT_WORD) {
      String tnam = stok.sval;
      for ( ; ; ) {
	 if (!checkNextToken(stok,'.')) break;
	 if (nextToken(stok) != StreamTokenizer.TT_WORD)
	    throw new BuenoException("Illegal qualified name");
	 tnam += "." + stok.sval;
       }
      rslt = tnam;
    }
   else throw new BuenoException("Type expected");

   if (checkNextToken(stok,'<')) {
      String ptyp = null;
      for ( ; ; ) {
	 String atyp = parseType(stok);
	 if (ptyp == null) ptyp = atyp;
	 else ptyp += "," + atyp;
	 if (checkNextToken(stok,'>')) break;
	 else if (!checkNextToken(stok,',')) throw new BuenoException("Bad parameterized argument");
       }
      if (ptyp == null) throw new BuenoException("Parameterized type list missing");
      rslt += "<" + ptyp + ">";
    }

   while (checkNextToken(stok,'[')) {
      if (!checkNextToken(stok,']')) throw new BuenoException("Missing right bracket");
      rslt += "[]";
    }

   return rslt;
}




protected boolean checkNextToken(StreamTokenizer stok,String tok)
{
   if (nextToken(stok) == StreamTokenizer.TT_WORD && stok.sval.equals(tok)) return true;

   stok.pushBack();
   return false;
}




protected boolean checkNextToken(StreamTokenizer stok,char tok)
{
   if (nextToken(stok) == tok) return true;

   stok.pushBack();
   return false;
}




protected boolean checkEnd(StreamTokenizer stok)
{
   if (nextToken(stok) == StreamTokenizer.TT_EOF) return true;

   stok.pushBack();
   return false;
}




protected void parseEnd(StreamTokenizer stok) throws BuenoException
{
   if (nextToken(stok) != StreamTokenizer.TT_EOF) throw new BuenoException("Excess at end");
}




protected int nextToken(StreamTokenizer stok)
{
   try {
      return stok.nextToken();
    }
   catch (IOException e) {
      return StreamTokenizer.TT_EOF;
    }
}






}	// end of class BuenoAbstractDialog




/* end of BuenoAbstractDialog.java */
