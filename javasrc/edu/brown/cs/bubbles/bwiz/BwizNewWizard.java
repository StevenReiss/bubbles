/********************************************************************************/
/*										*/
/*		BwizNewWizard.java						*/
/*										*/
/*	New class,enum,interface,method wizard implementation			*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 UCF -- Jared Bott				      */
/*	Copyright 2013 Brown University -- Annalia Sunderland		      */
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bwiz;

import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bueno.BuenoValidator;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


abstract class BwizNewWizard  extends SwingGridPanel implements BwizConstants,
		BwizConstants.ISignatureUpdate,
		BwizConstants.IAccessibilityUpdatable,
		BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextField  main_name;	 //Class name, Method Name, Interface Name, or Enum Name
private JTextField  second_name; //Superclass or Return type, in case of Classes or Methods
private BwizAccessibilityPanel accessibility_panel;
private JTextField signature_area;
private BwizHoverButton create_button;
private BwizListEntryComponent list_panel; //Interfaces, or Parameters
private SwingComboBox<String> package_dropdown;
private JComboBox<String> project_dropdown;

protected BuenoProperties property_set;
protected BuenoLocation at_location;
protected BuenoValidator new_validator;
protected BuenoType create_type;
protected BuenoBubbleCreator bubble_creator;

private static final String DEFAULT_PACKAGE = "<default package>";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizNewWizard(BuenoLocation loc,BuenoType type)
{
   at_location = loc;
   create_type = type;

   property_set = new BuenoProperties();
   if (at_location != null) {
      property_set.put(BuenoKey.KEY_PACKAGE,at_location.getPackage());
      property_set.put(BuenoKey.KEY_PROJECT,at_location.getProject());
    }

   new_validator = new BuenoValidator(new ValidCallback(),property_set,at_location,type);

   setup();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Component getFocus()				{ return main_name; }

void setBubbleCreator(BuenoBubbleCreator bbc)	{ bubble_creator = bbc; }


void setInitialName(String nm)
{
   main_name.setText(nm);
}

/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

protected int getAccessibilityInfo()	  { return 0; }

protected abstract Creator getCreator();

protected abstract String getNameText();
protected abstract String getNameHoverText();
protected abstract String getSecondText();
protected abstract String getSecondHoverText();
protected abstract String getListText();
protected abstract String getListHoverText();
protected IVerifier getVerifier()
{
   return new InterfaceVerifier();
}


protected class InterfaceVerifier implements IVerifier {

   @Override public boolean verify(String v) {
      return new_validator.checkInterfaces(v) != null;
    }

   @Override public List<String> results(String v) {
      return new_validator.checkInterfaces(v);
    }

}	// end of inner class InterfaceVerifier


protected class ParameterVerifier implements IVerifier {

   @Override public boolean verify(String v) {
      return new_validator.checkParameters(v) != null;
    }

   @Override public List<String> results(String v) {
      return new_validator.checkParameters(v);
    }

}	// end of inner class ParameterVerifier


private void setup()
{
   BoardColors.setColors(this,"Bwiz.NewBackground");

   //The panel that has the name of the class/method/interface/enum
   JPanel namespanel = new JPanel();
   namespanel.setLayout(new BoxLayout(namespanel, BoxLayout.LINE_AXIS));
   namespanel.setOpaque(false);

   Accessibility defaultaccess = Accessibility.DEFAULT;
   if ((getAccessibilityInfo() & SHOW_PRIVATE) != 0) defaultaccess = Accessibility.PRIVATE;
   setAccess(defaultaccess);

   //second panel, used in Classes and Methods for either extends or return, respectively
   JPanel secondpanel = new JPanel();
   if (getSecondText() != null) {
      secondpanel.setLayout(new BoxLayout(secondpanel, BoxLayout.LINE_AXIS));
      secondpanel.setOpaque(false);
      secondpanel.setBorder(BorderFactory.createEmptyBorder(12,0,12,0));
      BoardColors.setColors(secondpanel,"Bwiz.NewSecondBackground");
    }

   //check if method or interface/class/enum
   if (getListText() != null) {
      list_panel = new BwizListEntryComponent(getVerifier(),getListText());
      list_panel.setTitleFont(getRelativeFont(-3));
      list_panel.setTextFont(getRelativeFont(-3));
    }
   else list_panel = null;

   //The panel that contains the accessibility radiobuttons and the abstract check box.
   JPanel boxespanel = new JPanel(new GridBagLayout());
   boxespanel.setOpaque(false);
   boxespanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   accessibility_panel = new BwizAccessibilityPanel(getAccessibilityInfo());
   accessibility_panel.addAccessibilityActionListener(new AccessibilityChange());
   accessibility_panel.addModifierListener(new ModifierChange());
   accessibility_panel.addAccessibilityActionListener(new AccessibilityChange());
   accessibility_panel.addTypeActionListener(new TypeChange());

   //A panel for the create button and class signature
   JPanel buttonpanel = new JPanel();
   buttonpanel.setLayout(new BoxLayout(buttonpanel, BoxLayout.LINE_AXIS));
   buttonpanel.setOpaque(false);
   buttonpanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   JLabel nameslabel = new JLabel("");
   nameslabel.setFont(BWIZ_FONT_SIZE_MAIN);
   nameslabel.setAlignmentX(Component.LEFT_ALIGNMENT);
   nameslabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);

   namespanel.add(nameslabel);
   namespanel.add(Box.createRigidArea(new Dimension(7, 0)));

   addRawComponent(getNameText(),namespanel);

   //Requires list of available projects
   /* sets up the list of projects and packages */
   if (property_set.getProjectName() == null) {
      setupProject();
    }
   if (property_set.getPackageName() == null && create_type != BuenoType.NEW_PACKAGE) {
      setupPackage();
    }

   //adds components
   if (getSecondText() != null) {
      addRawComponent(getSecondText(),secondpanel);
    }

   if (list_panel != null) {
      addLabellessRawComponent("",list_panel,true,true);
    }
   addLabellessRawComponent("",boxespanel,true,false);
   addLabellessRawComponent("",accessibility_panel,true,false);
   addLabellessRawComponent("",buttonpanel,true,false);

   //Construct the main_name textfield
   //Creates a textfield with the default styling
   main_name = BwizFocusTextField.getStyledField("Enter " + getNameText(), getNameHoverText());
   //Sets the font size
   main_name.setFont(getRelativeFont(-2));
   main_name.setAlignmentX(Component.LEFT_ALIGNMENT);
   main_name.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Adds a handler for when the user is typing
   main_name.getDocument().addDocumentListener(new TextPropertyListener(BuenoKey.KEY_NAME));
   namespanel.add(main_name);

   //This is used to make certain UI elements the same height and to make other elements not change height
   Dimension enMax = new Dimension(Integer.MAX_VALUE,main_name.getPreferredSize().height);

   //check if class or method, superclass or return, respectively; sets up second panel label
   if (getSecondText() != null) {
      JLabel secondLabel = new JLabel();
      secondLabel.setFont(BWIZ_FONT_SIZE_MAIN);
      secondLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      secondLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
      secondpanel.add(secondLabel);
      secondpanel.add(Box.createRigidArea(new Dimension(7, 0)));
      //Construct second textfield
      setupSecondName();
      second_name.setMaximumSize(enMax);
      secondpanel.add(second_name);
    }

   //Adds handlers and sets heights
   if (list_panel != null) {
      list_panel.setHeight(main_name.getPreferredSize().height);
      list_panel.setHoverText(getListHoverText());
      list_panel.addItemChangeEventListener(new ClassItemListener());
    }

   //Creates a textfield that selects all text when it gets focus
   signature_area = new BwizFocusTextField("signature");

   //Styling
   signature_area.setEditable(false);
   signature_area.setFont(getRelativeFont(-6));
   signature_area.setForeground(BoardColors.getColor("Bwiz.NewSecondBackground"));
   signature_area.setOpaque(false);
   signature_area.setBorder(BorderFactory.createEmptyBorder());
   signature_area.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   Dimension d=new Dimension(Integer.MAX_VALUE, signature_area.getPreferredSize().height);
   signature_area.setMaximumSize(d);

   //Creates a button that changes cover when the mouse is over it
   create_button = new BwizHoverButton("Create",BoardColors.getColor("Bwiz.NewCreateDefault"),
	 BoardColors.getColor("Bwiz.NewCreateActive"));
   //Styling
   create_button.setFont(getRelativeFont(4));
   create_button.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   create_button.setMaximumSize(create_button.getPreferredSize());
   create_button.setEnabled(false);

   create_button.addActionListener(getCreator());

   buttonpanel.add(signature_area);
   buttonpanel.add(Box.createRigidArea(new Dimension(5, 0)));
   buttonpanel.add(create_button);
}




/********************************************************************************/
/*										*/
/*	Handle Projects 							*/
/*										*/
/********************************************************************************/

private void setupProject() {
   List<String> items = getProjects();
   ChooseProject ca = new ChooseProject();
   project_dropdown = addChoice("Project",items,0,ca);
   ca.set();
}


private List<String> getProjects()
{
   List<String> rslt = new ArrayList<String>();
   BumpClient bc = BumpClient.getBump();
   Element e = bc.getAllProjects();
   for (Element pe : IvyXml.children(e,"PROJECT")) {
      String nm = IvyXml.getAttrString(pe,"NAME");
      rslt.add(nm);
    }
   return rslt;
}



private class ChooseProject implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd==null);
      else set();
      if (package_dropdown != null) setupPackage();
    }

   void set() {
      if (project_dropdown != null) {
	 Object obj = project_dropdown.getSelectedItem();
	 if (obj != null) {
	    String proj = obj.toString();
	    property_set.put(BuenoKey.KEY_PROJECT,proj);
	    updateSignature();
	  }
      }
    }

}	// end of inner class ChooseProject




/********************************************************************************/
/*										*/
/*	Handle Packages 							*/
/*										*/
/********************************************************************************/

private void setupPackage() {
   if (package_dropdown == null) {
      ChoosePackage ca = new ChoosePackage();
      package_dropdown = addChoice("Package",new String [] { },0,true,ca);
      ca.set();
    }
   PackageFinder pf = new PackageFinder();
   BoardThreadPool.start(pf);
}





private List<String> getPackages()
{
   String proj = property_set.getProjectName();
   Set<String> rslt = new TreeSet<String>();

   BassRepository br = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_CODE);
   for (BassName bn : br.getAllNames()) {
      if (proj != null && !proj.equals(bn.getProject())) continue;
      switch (bn.getNameType()) {
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	 case ANNOTATION :
	    break;
	 default :
	    continue;
      }
      String pkg = bn.getNameHead();
      if (pkg == null) continue;
      int idx = pkg.lastIndexOf(".");
      if (idx < 0) pkg = DEFAULT_PACKAGE;
      else pkg = pkg.substring(0,idx);
      if (rslt.contains(pkg)) continue;
      BumpLocation bl = bn.getLocation();
      if (bl == null) continue;
      String key = bl.getKey();
      if (key.contains("$")) continue;
      rslt.add(pkg);
   }

   return new ArrayList<String>(rslt);
}



private class PackageFinder implements Runnable {

   private List<String> all_packages;
   PackageFinder() {
      all_packages = null;
    }

   @Override public void run() {
      if (all_packages == null) {
	 all_packages = getPackages();
	 SwingUtilities.invokeLater(this);
       }
      else if (package_dropdown != null) {
	 package_dropdown.setContents(all_packages);
	 if (all_packages.size() > 0) {
	    package_dropdown.setSelectedIndex(0);
	  }
       }
    }

}	// end of inner class PackageFinder



private class ChoosePackage implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd==null);
      else set();
    }

   void set() {
      if (package_dropdown != null && package_dropdown.getSelectedItem() != null) {
         String pkg = package_dropdown.getSelectedItem().toString();
         if (pkg.equals(DEFAULT_PACKAGE)) pkg = null;
         property_set.put(BuenoKey.KEY_PACKAGE,pkg);
         updateSignature();
      }
   }

}	// end of inner class ChoosePackage



/********************************************************************************/
/*										*/
/*	Handle name areas							*/
/*										*/
/********************************************************************************/

private void setupSecondName()
{
   //Creates a textfield with the default styling
   second_name = BwizFocusTextField.getStyledField("", getSecondHoverText());
   second_name.setFont(getRelativeFont(-2));
   second_name.setAlignmentX(Component.LEFT_ALIGNMENT);
   second_name.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Adds a handler for when the user is typing
   BuenoKey k = null;
   switch (create_type) {
      case NEW_CLASS :
      case NEW_INNER_CLASS :
	 k = BuenoKey.KEY_EXTENDS;
	 break;
      case NEW_METHOD :
	 k = BuenoKey.KEY_RETURNS;
	 break;
      case NEW_PACKAGE :
         k = BuenoKey.KEY_CLASS_NAME;
         break;
      default :
	 break;
    }
   if (k != null) {
      second_name.getDocument().addDocumentListener(new TextPropertyListener(k));
    }
}



@Override public void updateSignature()
{
   if (create_button != null) create_button.setEnabled(false);

   if (signature_area != null) {
      signature_area.setText(new_validator.getSignature());
      signature_area.moveCaretPosition(0);
      new_validator.updateParsing();
    }
}



@Override public void updateAccessibility(ActionEvent e)
{
   //Sets what radiobutton is selected in the backing data structure
   String command = e.getActionCommand();
   Accessibility a = Accessibility.fromString(command);

   setAccess(a);
}


private void setAccess(Accessibility a)
{
   int mods = property_set.getModifiers();
   mods &= ~(Modifier.PUBLIC|Modifier.PRIVATE|Modifier.PROTECTED);
   switch (a) {
      case PUBLIC :
	 mods |= Modifier.PUBLIC;
	 break;
      case PRIVATE :
	 mods |= Modifier.PRIVATE;
	 break;
      case PROTECTED :
	 mods |= Modifier.PROTECTED;
	 break;
      case DEFAULT :
	 break;
    }
   property_set.put(BuenoKey.KEY_MODIFIERS,mods);
}


private Font getRelativeFont(int x)
{
   //Uses deriveFont to derive a new font based on MAIN font given an int deviation
   int size = BWIZ_FONT_SIZE_MAIN.getSize();
   int newsize = size + x;
   float f = newsize;
   if (newsize<18) {
      return BWIZ_FONT_SIZE_MAIN.deriveFont(1,f);
    }
   return BWIZ_FONT_SIZE_MAIN.deriveFont(0,f);
}



private class ValidCallback implements BuenoValidatorCallback {

   @Override public void validationDone(BuenoValidator v,boolean pass) {
      if (create_button == null) return;
      if (list_panel != null && list_panel.isActive()) pass = false;
      if (pass) {
         create_button.setEnabled(true);
       }
      else {
         create_button.setEnabled(false);
       }
    }

}	// end of inner class ValidCallback





/********************************************************************************/
/*										*/
/*	Item change listener							*/
/*										*/
/********************************************************************************/

private class ClassItemListener implements ItemChangeListener {

   private BuenoKey using_key;

   ClassItemListener() {
      switch (create_type) {
         case NEW_CLASS :
         case NEW_INNER_CLASS :
         case NEW_ENUM :
         case NEW_INNER_ENUM :
            using_key = BuenoKey.KEY_IMPLEMENTS;
            break;
         case NEW_INTERFACE :
         case NEW_INNER_INTERFACE :
            using_key = BuenoKey.KEY_EXTENDS;
            break;
         case NEW_METHOD :
         case NEW_CONSTRUCTOR :
            using_key = BuenoKey.KEY_PARAMETERS;
            break;
         case NEW_PACKAGE :
            using_key = BuenoKey.KEY_CLASS_NAME;
            break;
         default :
            using_key = null;
       }
    }
   
   @Override public void itemAdded(String item) {
      if (using_key != null && list_panel != null) {
         property_set.put(using_key,list_panel.getListElements());
         updateSignature();
       }
    }

   @Override public void itemRemoved(String item) {
      if (using_key != null && list_panel != null) {
         property_set.put(using_key,list_panel.getListElements());
         updateSignature();
       }
    }

}	// end of inner class ClassItemListener



/********************************************************************************/
/*										*/
/*	Accessility selection listener						*/
/*										*/
/********************************************************************************/

private class AccessibilityChange implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      updateAccessibility(e);
      updateSignature();
    }

}	// end of inner class Accessibility Change


private class TypeChange implements ActionListener {
   
   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      property_set.put(BuenoKey.KEY_TYPE,cmd.toLowerCase());
      updateSignature();
    }
}



/********************************************************************************/
/*										*/
/*	Abstract selection listener						*/
/*										*/
/********************************************************************************/

private class ModifierChange implements ItemListener {

   @Override public void itemStateChanged(ItemEvent e) {
      JCheckBox cbx = (JCheckBox) e.getItem();
      String cmd = cbx.getActionCommand();
      if (cmd.equals("abstract")) {
         setModifier(Modifier.ABSTRACT,e.getStateChange() == ItemEvent.SELECTED);
       }
      else if (cmd.equals("final")) {
         setModifier(Modifier.FINAL,e.getStateChange() == ItemEvent.SELECTED);
       }
      else if (cmd.equals("@Override")) {
         setModifier(BuenoConstants.MODIFIER_OVERRIDES,e.getStateChange() == ItemEvent.SELECTED);
       }
   
      updateSignature();
    }

}	// end of inner class ModifierChange


private void setModifier(int mod,boolean fg)
{
   int mods = property_set.getModifiers();
   mods &= ~mod;
   if (fg) mods |= mod;
   property_set.put(BuenoKey.KEY_MODIFIERS,mods);
   updateSignature();
}



/********************************************************************************/
/*										*/
/*	Action handler for doing the creation					*/
/*										*/
/********************************************************************************/

protected abstract class Creator implements ActionListener, Runnable {

   private Point bubble_point;
   private BudaBubbleArea bubble_area;

   Creator() {
      bubble_point = null;
      bubble_area = null;
   }

   @Override public void actionPerformed(ActionEvent e) {
      if (list_panel != null && list_panel.isActive()) {
         list_panel.addCurrentItem();
       }
      if (!new_validator.checkParsing()) return;
   
      BudaBubble bbl = BudaRoot.findBudaBubble(BwizNewWizard.this);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
      if (bbl == null || bba == null) return;
   
      Rectangle r = BudaRoot.findBudaLocation(bbl);
      Point pt = r.getLocation();
      bba.removeBubble(bbl);
   
      bubble_point = pt;
      bubble_area = bba;
   
      BoardThreadPool.start(this);
   }

   abstract protected BudaBubble doCreate(BudaBubbleArea bba,Point pt,String nm,BuenoProperties bp);

   @Override public void run() {
      BowiFactory.startTask();
      try {
         String pkg = property_set.getPackageName();
         String cls = property_set.getStringProperty(BuenoKey.KEY_NAME);
         String fcls = (pkg == null ? cls : pkg + "." + cls);
   
         BudaBubble nbbl = doCreate(bubble_area,bubble_point,fcls,property_set);
   
         if (nbbl != null) {
            bubble_area.add(nbbl,new BudaConstraint(bubble_point));
          }
       }
      finally {
         BowiFactory.stopTask();
       }
   }
}	// end of inner class Creator



/********************************************************************************/
/*										*/
/*	Handle text changes							*/
/*										*/
/********************************************************************************/

private class TextPropertyListener implements DocumentListener {

   private BuenoKey property_key;

   TextPropertyListener(BuenoKey key) {
      property_key = key;
    }

   @Override public void insertUpdate(DocumentEvent e) {
      updateAll(e);
    }


   @Override public void removeUpdate(DocumentEvent e) {
      updateAll(e);
    }

   @Override public void changedUpdate(DocumentEvent e) { }

   private void updateAll(DocumentEvent e) {
      try {
	 Document doc = e.getDocument();
	 String data = doc.getText(0, doc.getLength());

	 if (data != "") {
	    property_set.put(property_key,data);
	  }

	 updateSignature();
       }
      catch (BadLocationException ex) { }
    }

}	// end of inner class TextPropertyListener



}	// end of class BwizClassWizard




/* end of BwizClassWizard.java */
