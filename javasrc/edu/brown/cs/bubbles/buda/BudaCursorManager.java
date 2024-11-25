/********************************************************************************/
/*										*/
/*		BudaCursorManager.java						*/
/*										*/
/*	BUblles Display Area cursor management routines 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman		      */
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



package edu.brown.cs.bubbles.buda;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;




/**
 * A Class to manage the cursors of components
 *
 */

public final class BudaCursorManager {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<Component, Cursor> default_cursors;
private Set<Component> global_cursor_set;
private Cursor         global_cursor;


private static BudaCursorManager the_manager = new BudaCursorManager();




/********************************************************************************/
/*										*/
/*	Top level static methods						*/
/*										*/
/********************************************************************************/

/**
* Sets up cursors for all components contained in this component
* @param comp
*/

public static void setupDefaults(Component comp)
{
   the_manager.setupCursorsForComponent(comp);
}



/**
* Sets the cursor for the component and also sets it up as the default cursor for that component
* @param comp
* @param curs
*/

public static void setCursor(Component comp, Cursor curs)
{
   the_manager.setDefaultCursor(comp, curs);
}



/**
* Sets the cursor fo the component
* @param comp
* @param curs
*/

public static void setTemporaryCursor(Component comp, Cursor curs)
{
// Throwable t = new Throwable("SET TEMP " + curs);
// t.printStackTrace();
   
   if (curs == null && the_manager.global_cursor_set.contains(comp)) { 
      curs = the_manager.global_cursor;
    }
   if (curs == null) curs = the_manager.default_cursors.get(comp);
   if (curs == null) curs = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
   comp.setCursor(curs);
}



/**
* Sets up one cursor for the component and all components contained in the component.
* @param comp
* @param curs
*/

public static void setGlobalCursorForComponent(Component comp, Cursor curs)
{
   the_manager.global_cursor = curs;
   the_manager.localSetGlobalCursorForComponent(comp, curs);
}



/**
 * resets the cursor for the component and all components in this component
 * @param comp
 */

public static void resetDefaults(Component comp)
{
   the_manager.global_cursor = null;
   the_manager.resetDefaultCursor(comp);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BudaCursorManager()
{
   default_cursors = new WeakHashMap<Component, Cursor>();
   global_cursor_set = new HashSet<Component>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupCursorsForComponent(Component comp) 
{
   if (comp.isCursorSet()) {
      default_cursors.put(comp, comp.getCursor());
      if (comp.getParent() != null && global_cursor_set.contains(comp.getParent())) {
	 comp.setCursor(comp.getParent().getCursor());
	 global_cursor_set.add(comp);
       }
    }
   if (!(comp instanceof Container)) return;
   Container cont = (Container) comp;
   for (Component c : cont.getComponents()) {
      setupCursorsForComponent(c);
    }
}



private void setDefaultCursor(Component comp, Cursor curs)
{
   if (!global_cursor_set.contains(comp)) comp.setCursor(curs);
   default_cursors.put(comp, curs);
}



private void resetDefaultCursor(Component comp)
{
   if (comp == null) return;
   comp.setCursor(default_cursors.get(comp));
   global_cursor_set.remove(comp);
   if (!(comp instanceof Container)) return;
   Container cont = (Container) comp;
   for (Component c : cont.getComponents()) {
      resetDefaultCursor(c);
    }
}



private void localSetGlobalCursorForComponent(Component comp, Cursor curs)
{
   comp.setCursor(curs);
   global_cursor_set.add(comp);
   if (!(comp instanceof Container)) return;
   Container cont = (Container) comp;
   for (Component c : cont.getComponents()) {
      localSetGlobalCursorForComponent(c, curs);
    }
}



}	// end of class BudaCursorManager



/* end of BudaCursorManager.java */
