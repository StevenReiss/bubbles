/********************************************************************************/
/*										*/
/*		BoppOptionBase.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;

import edu.brown.cs.ivy.swing.SwingColorButton;
import edu.brown.cs.ivy.swing.SwingColorRangeChooser;
import edu.brown.cs.ivy.swing.SwingDimensionChooser;
import edu.brown.cs.ivy.swing.SwingFontChooser;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingNumericField;
import edu.brown.cs.ivy.swing.SwingRangeSlider;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


abstract class BoppOptionBase implements BoppConstants.BoppOptionNew, BoppConstants
{


/********************************************************************************/
/*										*/
/*	Static constructors							*/
/*										*/
/********************************************************************************/

static BoppOptionBase getOption(String pkgname,Element ox)
{
   OptionType otyp = IvyXml.getAttrEnum(ox,"TYPE",OptionType.NONE);

   switch (otyp) {
      case NONE :
	 break;
      case BOOLEAN :
	 return new OptionBoolean(pkgname,ox);
      case COLOR :
	 return new OptionColor(pkgname,ox);
      case COMBO :
	 return new OptionChoice(pkgname,ox);
      case DIMENSION :
	 return new OptionDimension(pkgname,ox);
      case DIVIDER :
	 return new OptionDivider(pkgname,ox);
      case DOUBLE  :
	 break;
      case FONT :
	 return new OptionFont(pkgname,ox);
      case INTEGER :
	 return new OptionInteger(pkgname,ox);
      case STRING :
	 return new OptionString(pkgname,ox);
      case RADIO :
	 return new OptionMultiple(pkgname,ox);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected String	option_name;
protected String	option_description;
protected String	package_name;
protected List<String>	option_tabs;
private String		option_keywords;
private BoppOptionSet	option_set;
private boolean 	doing_add;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppOptionBase(String pkgname,Element ox)
{
   package_name = pkgname;
   option_set = null;
   doing_add = false;

   option_name = IvyXml.getAttrString(ox,"NAME");
   option_description = IvyXml.getAttrString(ox,"DESCRIPTION");
   if (option_description == null) option_description = "";
   option_tabs = new ArrayList<String>();
   for (Element tx : IvyXml.children(ox,"TAB")) {
      String nm = IvyXml.getAttrString(tx,"NAME");
      if (nm != null) option_tabs.add(nm);
    }
   option_keywords = "";
   for (Element kx : IvyXml.children(ox,"KEYWORD")) {
      String nm = IvyXml.getAttrString(kx,"TEXT");
      if (nm != null) option_keywords += " " + nm;
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getOptionName() 		{ return option_name; }

@Override public abstract OptionType getOptionType();

void setOptionSet(BoppOptionSet os)		{ option_set = os; }

@Override public Collection<String> getOptionTabs()	{ return option_tabs; }

protected BoardProperties getProperties()
{
   return BoardProperties.getProperties(package_name);
}

void doingAdd(boolean fg)			{ doing_add = fg; }
boolean isDoingAdd()				{ return doing_add; }




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void noteChange()
{
   if (doing_add) return;

   BoppFactory.handleOptionChange(this);

   option_set.noteChange(package_name,option_name);
}

void noteChange(String... props)
{
   if (doing_add) return;

   BoppFactory.handleOptionChange(this);

   for (String prop : props) {
      if (prop == null) continue;
      option_set.noteChange(package_name,prop);
    }
}



void finishChanges()
{
   option_set.finishChanges();
}


void reset()						{ }


/********************************************************************************/
/*										*/
/*	Search methods								*/
/*										*/
/********************************************************************************/

@Override public boolean search(Pattern [] pats)
{
   if (getOptionType() == OptionType.DIVIDER) return false;

   for (Pattern p : pats) {
      if (p != null) {
	 Matcher m1 = p.matcher(option_name);
	 Matcher m2 = p.matcher(option_description);
	 Matcher m3 = p.matcher(option_keywords);
	 if (!(m1.find() || m2.find() || m3.find())) return false;
       }
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Boolean Options 							*/
/*										*/
/********************************************************************************/

private static class OptionBoolean extends BoppOptionBase implements ActionListener {

   private JCheckBox check_box;
   private boolean default_value;

   OptionBoolean(String pkgname,Element ox) {
      super(pkgname,ox);
      check_box = null;
      default_value = IvyXml.getAttrBool(ox,"DEFAULT",false);
    }

   @Override public OptionType getOptionType()		{ return OptionType.BOOLEAN; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      check_box = pnl.addBoolean(option_description,getValue(),this);
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      JCheckBox cbx = (JCheckBox) evt.getSource();
      noteChange();
      setValue(cbx.isSelected());
      finishChanges();
    }

   private boolean getValue() {
      BoardProperties bp = getProperties();
      return bp.getBoolean(option_name,default_value);
    }

   private void setValue(boolean v) {
      BoardProperties bp = getProperties();
      bp.setProperty(option_name,v);
    }

   @Override void reset() {
      if (check_box != null) check_box.setSelected(getValue());
    }

}	// end of inner class OptionBoolean




/********************************************************************************/
/*										*/
/*	Color Options								*/
/*										*/
/********************************************************************************/

private static class OptionColor extends BoppOptionBase implements ActionListener {

   private String from_name;
   private String to_name;
   private SwingColorButton color_button;
   private SwingColorRangeChooser range_button;

   OptionColor(String pkgname,Element ox) {
      super(pkgname,ox);
      Element px = IvyXml.getChild(ox,"PROPERTIES");
      if (px == null) {
	 from_name = option_name;
	 to_name = null;
       }
      else {
	 from_name = IvyXml.getAttrString(px,"FROM");
	 to_name = IvyXml.getAttrString(px,"TO");
       }
      color_button = null;
      range_button = null;
    }

   @Override public OptionType getOptionType()		{ return OptionType.COLOR; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      if (to_name == null) {
	 color_button = pnl.addColorField(option_description,getFromValue(),true,this);
       }
      else {
	 range_button = pnl.addColorRangeField(option_description,getFromValue(),getToValue(),this);
       }
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (isDoingAdd()) return;
      noteChange(from_name,to_name);
      if (to_name == null) {
	 color_button = (SwingColorButton) evt.getSource();
	 setFromValue(color_button.getColor());
       }
      else {
	 range_button = (SwingColorRangeChooser) evt.getSource();
	 setFromValue(range_button.getFirstColor());
	 setToValue(range_button.getSecondColor());
       }
      finishChanges();
    }

   private Color getFromValue() {
      return getProperties().getColor(from_name);
    }

   private Color getToValue() {
      return getProperties().getColor(to_name);
    }

   private void setFromValue(Color c) {
      getProperties().setProperty(from_name,c);
    }

   private void setToValue(Color c) {
      getProperties().setProperty(to_name,c);
    }

   @Override void reset() {
      if (color_button != null) {
	 color_button.setColor(getFromValue());
      }
      else if (range_button != null) {
	 range_button.setColors(getFromValue(),getToValue());
      }
   }
}	// end of inner class OptionColor



/********************************************************************************/
/*										*/
/*	Choice options								*/
/*										*/
/********************************************************************************/

private static class OptionChoice extends BoppOptionBase implements ActionListener {

   private Map<String,String> choice_map;
   private Map<String,String> lookup_map;
   private JComboBox<String> combo_box;

   OptionChoice(String pkgname,Element ox) {
      super(pkgname,ox);
      choice_map = new LinkedHashMap<String,String>();
      lookup_map = new HashMap<String,String>();
      combo_box = null;
      for (Element ce : IvyXml.children(ox,"COMBO")) {
	 String k = IvyXml.getAttrString(ce,"TEXT");
	 String v = IvyXml.getAttrString(ce,"VALUE");
	 if (k != null && v != null) {
	    choice_map.put(k,v);
	    lookup_map.put(v,k);
	  }
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.COMBO; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      combo_box = pnl.addChoice(option_description,choice_map.keySet(),getValue(),this);
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (isDoingAdd()) return;
      JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
      String v = (String) cbx.getSelectedItem();
      if (v == null) return;
      noteChange();
      setValue(v);
      finishChanges();
    }

   private String getValue() {
      String v = getProperties().getProperty(option_name);
      return lookup_map.get(v);
    }

   private void setValue(String v0) {
      if (v0 == null) return;
      String v = choice_map.get(v0);
      if (v == null) BoardLog.logX("BOPP","Empty property " + v0 + " " + v);
      else if (option_name == null) BoardLog.logX("BOPP","Bad property " + option_description);
      else getProperties().setProperty(option_name,v);
    }

   @Override void reset() {
      if (combo_box != null) {
	 combo_box.setSelectedItem(getValue());
      }
   }

}	// end of inner class OptionChoice



/********************************************************************************/
/*										*/
/*	Multiple choice options 					*/
/*										*/
/********************************************************************************/

private static class OptionMultiple extends BoppOptionBase implements ListSelectionListener {

   private Map<String,String> choice_map;
   private Map<String,String> lookup_map;
   private JList<String> selection_box;
   private String separator_text;

   OptionMultiple(String pkgname,Element ox) {
      super(pkgname,ox);
      choice_map = new LinkedHashMap<String,String>();
      lookup_map = new HashMap<String,String>();
      selection_box = null;
      separator_text = IvyXml.getAttrString(ox,"SEPARATOR"," ");
      for (Element ce : IvyXml.children(ox,"COMBO")) {
	 String k = IvyXml.getAttrString(ce,"TEXT");
	 String v = IvyXml.getAttrString(ce,"VALUE");
	 if (k != null && v != null) {
	    choice_map.put(k,v);
	    lookup_map.put(v,k);
	  }
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.RADIO; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      Map<String,Boolean> vec = getValue();
      selection_box = pnl.addButtonSet(option_description,vec,this);
      doingAdd(false);
    }

   @Override public void valueChanged(ListSelectionEvent evt) {
      if (isDoingAdd()) return;
      noteChange();
      List<String> sels = selection_box.getSelectedValuesList();
      String rslt = "";
      for (int i = 0; i < sels.size(); ++i) {
	 if (i > 0) rslt += separator_text;
	 rslt += choice_map.get(sels.get(i));
       }
      setValue(rslt);
      finishChanges();
    }

   private Map<String,Boolean> getValue() {
      Map<String,Boolean> rslt = new LinkedHashMap<>();
      String v = getProperties().getProperty(option_name);
      if (v == null) v = "";
      for (Map.Entry<String,String> ent : choice_map.entrySet()) {
	 rslt.put(ent.getKey(),v.contains(ent.getValue()));
       }
      return rslt;
    }

   private void setValue(String v0) {
      getProperties().setProperty(option_name,v0);
    }

   @Override void reset() {
      if (selection_box != null) {
	 selection_box.clearSelection();
	 String v = getProperties().getProperty(option_name);
	 if (v != null) {
	    int idx = 0;
	    for (Map.Entry<String,String> ent : choice_map.entrySet()) {
	       if (v.contains(ent.getValue())) {
		  selection_box.addSelectionInterval(idx,idx);
		}
	       ++idx;
	     }
	  }
       }
    }

}	// end of inner class OptionChoice




/********************************************************************************/
/*										*/
/*	Dimension Options							*/
/*										*/
/********************************************************************************/

private static class OptionDimension extends BoppOptionBase implements ActionListener {

   private String width_prop;
   private String height_prop;
   private SwingNumericField num_field;
   private SwingDimensionChooser dim_field;

   OptionDimension(String pkgname,Element ox) {
      super(pkgname,ox);
      num_field = null;
      dim_field = null;
      Element px = IvyXml.getChild(ox,"PROPERTIES");
      if (px == null) {
	 width_prop = option_name;
	 height_prop = null;
       }
      else {
	 width_prop = IvyXml.getAttrString(px,"WIDTH");
	 height_prop = IvyXml.getAttrString(px,"HEIGHT");
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.DIMENSION; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      if (height_prop == null) {
	 num_field = pnl.addNumericField(option_description,10,1024,getWidthValue(),this);
       }
      else {
	 dim_field = pnl.addDimensionField(option_description,getWidthValue(),getHeightValue(),this);
       }
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (isDoingAdd()) return;
      noteChange(width_prop,height_prop);
      if (height_prop == null) {
	 SwingNumericField fld = (SwingNumericField) evt.getSource();
	 setWidthValue((int) fld.getValue());
       }
      else {
	 SwingDimensionChooser dim = (SwingDimensionChooser) evt.getSource();
	 setWidthValue(dim.getWidthValue());
	 setHeightValue(dim.getHeightValue());
       }
      finishChanges();
    }


   private int getWidthValue() {
      return getProperties().getInt(width_prop,0);
    }

   private int getHeightValue() {
      return getProperties().getInt(height_prop,0);
    }

   private void setWidthValue(int v) {
      if (width_prop == null) return;
      if (v == 0) getProperties().remove(width_prop);
      else getProperties().setProperty(width_prop,v);
    }

   private void setHeightValue(int v) {
      if (height_prop == null) return;
      if (v == 0) getProperties().remove(height_prop);
      else getProperties().setProperty(height_prop,v);
    }

   @Override void reset() {
      if (num_field != null) {
	 if (height_prop != null) num_field.setValue(getHeightValue());
	 else if (width_prop != null) num_field.setValue(getWidthValue());
       }
      else if (dim_field != null) {
	 dim_field.setValue(getWidthValue(),getHeightValue());
       }
    }

}	// end of inner class OptionDimension




/********************************************************************************/
/*										*/
/*	Divider Options 							*/
/*										*/
/********************************************************************************/

private static class OptionDivider extends BoppOptionBase {

   OptionDivider(String pkgname,Element ox) {
      super(pkgname,ox);
    }

   @Override public OptionType getOptionType()		{ return OptionType.DIVIDER; }

   @Override public void addButton(SwingGridPanel pnl) {
      pnl.addSeparator();
      pnl.addSectionLabel(option_name);
    }

}	// end of inner clss OptionDivider



/********************************************************************************/
/*										*/
/*	Font Options								*/
/*										*/
/********************************************************************************/

private static class OptionFont extends BoppOptionBase implements ActionListener {

   private String font_prop;
   private String family_prop;
   private String size_prop;
   private String style_prop;
   private String color_prop;
   private SwingFontChooser font_chooser;

   OptionFont(String pkgname,Element ox) {
      super(pkgname,ox);
      font_chooser = null;
      Element fx = IvyXml.getChild(ox,"PROPERTIES");
      if (fx == null) {
	 font_prop = option_name;
	 family_prop = null;
	 size_prop = null;
	 style_prop = null;
       }
      else {
	 font_prop = null;
	 family_prop = IvyXml.getAttrString(fx,"FAMILY");
	 size_prop = IvyXml.getAttrString(fx,"SIZE");
	 style_prop = IvyXml.getAttrString(fx,"STYLE");
	 color_prop = IvyXml.getAttrString(fx,"COLOR");
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.FONT; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      int sts = 0;
      if (font_prop == null) {
	 if (family_prop == null) sts |= SwingFontChooser.FONT_FIXED_FAMILY;
	 if (size_prop == null) sts |= SwingFontChooser.FONT_FIXED_SIZE;
	 if (style_prop == null) sts |= SwingFontChooser.FONT_FIXED_STYLE;
	 if (color_prop == null) sts |= SwingFontChooser.FONT_FIXED_COLOR;
       }
      font_chooser = pnl.addFontField(option_description,getFontValue(),getColorValue(),sts,this);
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (isDoingAdd()) return;
      noteChange(font_prop,family_prop,size_prop,style_prop,color_prop);
      SwingFontChooser sfc = (SwingFontChooser) e.getSource();
      setFont(sfc.getFont());
      setColor(sfc.getFontColor());
      finishChanges();
    }

   private Font getFontValue() {
      if (font_prop != null) {
	 return getProperties().getFont(font_prop);
       }

      String fam = "Serif";
      if (family_prop != null) fam = getProperties().getProperty(family_prop,fam);
      int sz = 12;
      if (size_prop != null) sz = getProperties().getInt(size_prop,sz);
      int sty = Font.PLAIN;
      if (style_prop != null) sty = getProperties().getInt(style_prop,sty);
      return BoardFont.getFont(fam,sty,sz);
    }

   private Color getColorValue() {
      if (color_prop == null) return null;
      return getProperties().getColor(color_prop);
    }

   private void setFont(Font ft) {
      if (font_prop != null) {
         getProperties().setProperty(font_prop,ft);
       }
      else {
         if (family_prop != null) getProperties().setProperty(family_prop,ft.getFamily());
         if (size_prop != null) getProperties().setProperty(size_prop,ft.getSize());
         if (style_prop != null) getProperties().setProperty(style_prop,ft.getStyle());
       }
    }

   private void setColor(Color c) {
      if (color_prop != null && c != null)
	 getProperties().setProperty(color_prop,c);
    }

   @Override public void reset() {
      if (font_chooser != null) font_chooser.setFont(getFontValue(),getColorValue());
    }
   
}	// end of inner class OptionFont




/********************************************************************************/
/*										*/
/*	Numeric Options 							*/
/*										*/
/********************************************************************************/

private static class OptionInteger extends BoppOptionBase implements ChangeListener, ActionListener {

   private int min_value;
   private int max_value;
   private int scale_by;
   private boolean range_ok;
   private SwingRangeSlider range_btn;
   private SwingNumericField number_btn;

   OptionInteger(String pkgname,Element ox) {
      super(pkgname,ox);
      range_btn = null;
      number_btn = null;
      min_value = IvyXml.getAttrInt(ox,"MIN",0);
      max_value = IvyXml.getAttrInt(ox,"MAX",0);
      scale_by = IvyXml.getAttrInt(ox,"SCALE",0);
      if (min_value >= max_value) range_ok = false;
      else if (IvyXml.getAttrBool(ox,"SLIDER")) range_ok = true;
      else if (max_value - min_value < 10 && max_value - min_value > 2) range_ok = true;
      else range_ok = false;
    }

   @Override public OptionType getOptionType()		{ return OptionType.INTEGER; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      if (range_ok) {
         range_btn = pnl.addRange(option_description,min_value,max_value,scale_by,getValue(),this);
       }
      else {
         number_btn = pnl.addNumericField(option_description,min_value,max_value,getValue(),this);
       }
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (isDoingAdd()) return;
      noteChange();
      SwingNumericField snf = (SwingNumericField) evt.getSource();
      setValue((int) snf.getValue());
      finishChanges();
    }

   @Override public void stateChanged(ChangeEvent evt) {
      SwingRangeSlider rs = (SwingRangeSlider) evt.getSource();
      setValue((int) rs.getScaledValue());
      noteChange();
    }

   private int getValue() {
      return getProperties().getInt(option_name);
    }

   private void setValue(int v) {
      getProperties().setProperty(option_name,v);
    }

   @Override public void reset() {
      if (range_btn != null) range_btn.setValue(getValue());
      else if (number_btn != null) number_btn.setValue(getValue());
    }

}	// end of inner class OptionInteger



/********************************************************************************/
/*										*/
/*	String options								*/
/*										*/
/********************************************************************************/

private static class OptionString extends BoppOptionBase implements ActionListener {

   private JTextField text_field;

   OptionString(String pkgname,Element ox) {
      super(pkgname,ox);
      text_field = null;
    }

   @Override public OptionType getOptionType()		{ return OptionType.STRING; }

   @Override public void addButton(SwingGridPanel pnl) {
      doingAdd(true);
      text_field = pnl.addTextField(option_description,getValue(),12,this,null);
      doingAdd(false);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (isDoingAdd()) return;
      noteChange();
      JTextField tf = (JTextField) evt.getSource();
      setValue(tf.getText());
      finishChanges();
    }

   private String getValue() {
      return getProperties().getProperty(option_name);
    }

   private void setValue(String v) {
      getProperties().setProperty(option_name,v);
    }

   @Override public void reset() {
      text_field.setText(getValue());
    }

}	// end of inner class OptionString


}	// end of class BoppOptionBase




/* end of BoppOptionBase.java */

