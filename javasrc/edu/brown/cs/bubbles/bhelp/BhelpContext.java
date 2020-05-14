/********************************************************************************/
/*										*/
/*		BhelpContext.java						*/
/*										*/
/*	Global context for actions						*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.*;

import javax.swing.SwingUtilities;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


class BhelpContext implements BhelpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storag								*/
/*										*/
/********************************************************************************/

private BhelpDemo		for_demo;
private Map<String,Object>	value_map;
private BudaBubbleArea		buda_area;
private BudaRoot		buda_root;
private Robot			event_robot;
private Point			current_mouse;
private boolean 		is_stopped;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BhelpContext(BudaBubbleArea bba,BhelpDemo demo)
{
   for_demo = demo;
   value_map = new HashMap<String,Object>();
   buda_area = bba;
   buda_root = BudaRoot.findBudaRoot(bba);
   is_stopped = false;

   try {
      event_robot = new Robot();
      event_robot.setAutoDelay(0);
      // event_robot.setAutoWaitForIdle(true);
    }
   catch (AWTException e) {
      BoardLog.logE("BHELP","ROBOT not available");
    }

   Point pt = MouseInfo.getPointerInfo().getLocation();
   SwingUtilities.convertPointFromScreen(pt,buda_root);
   current_mouse = new Point(pt);
   setValue("StartPoint",pt);
   setValue("BubbleArea",buda_root.getCurrentBubbleArea());
   Rectangle r = buda_root.getCurrentBubbleArea().getViewport();
   setValue("StartViewport",r);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BudaBubbleArea getBubbleArea()		{ return buda_area; }
BudaRoot getBudaRoot()			{ return buda_root; }
Point getMouse()			{ return new Point(current_mouse); }

void setStopped()			{ is_stopped = true; }
boolean isStopped()			{ return is_stopped; }



private void setMouse(double x,double y)
{
   current_mouse.setLocation(x,y);

   buda_root.setDemonstrationPoint(current_mouse);

}




/********************************************************************************/
/*										*/
/*	Robot methods								*/
/*										*/
/********************************************************************************/

void mouseMove(int x,int y) throws BhelpException
{
   // BoardLog.logD("BHELP","MOVE MOUSE " + x + " " + y);

   checkMouse();

   Point sp = new Point(x,y);
   convertPointToScreen(sp);
   getRobot().mouseMove(sp.x,sp.y);
   setMouse(x,y);
}


boolean checkMouse()
{
   PointerInfo pi = MouseInfo.getPointerInfo();
   Point cp = null;
   if (pi == null) {
      Rectangle r = buda_root.getBounds();
      cp = new Point(r.width/2,r.height/2);
    }
   else {
      cp = pi.getLocation();
      SwingUtilities.convertPointFromScreen(cp,buda_root);
      // BoardLog.logD("BHELP","MOUSE RESULT " + cp);
    }

   int diff = Math.abs(cp.x - current_mouse.x) + Math.abs(cp.y - current_mouse.y);
   // BoardLog.logD("BHELP","TEST MOUSE " + cp + " " + current_mouse + " " + diff);
   if (diff > 10) {
      // BoardLog.logD("BHELP","CHECK MOUSE " + cp + " " + current_mouse);
      for_demo.stopDemonstration();
    }


   return !is_stopped;
}



void mousePress(int btns) throws BhelpException
{
   getRobot().mousePress(btns);
}


void mouseRelease(int btns) throws BhelpException
{
   getRobot().mouseRelease(btns);
}

void delay(int ms) throws BhelpException
{
   getRobot().delay(ms);
}

void keyPress(int keycode) throws BhelpException
{
   getRobot().keyPress(keycode);
}


void keyRelease(int keycode) throws BhelpException
{
   getRobot().keyRelease(keycode);
}



private Robot getRobot() throws BhelpException
{
   if (event_robot == null) throw new BhelpException("Event Simulator not available");
   return event_robot;
}



private void convertPointToScreen(Point pt)
{
   PointerInfo pi = MouseInfo.getPointerInfo();

   if (pi != null) {
      GraphicsDevice gd = pi.getDevice();
      GraphicsConfiguration gc = gd.getDefaultConfiguration();
      Rectangle r = gc.getBounds();
      pt.x -= r.x;
      pt.y -= r.y;
    }

   // BoardLog.logD("BHELP","Convert " + pt);
   SwingUtilities.convertPointToScreen(pt,buda_root);
   // BoardLog.logD("BHELP","Convert result " + pt);
}



/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

void setValue(String name,BudaBubble bbl)
{
   if (name != null) value_map.put(name,bbl);
}


void setValue(String name,Point pt)
{
   if (name != null) value_map.put(name,new Point(pt));
}


void setValue(String name,Rectangle r)
{
   if (name != null) value_map.put(name,new Rectangle(r));
}


void setValue(String name,Component c)
{
   if (name != null) value_map.put(name,c);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Point getPoint(String var)
{
   if (var == null) return null;

   Object val = value_map.get(var);
   if (val == null) return null;
   if (val instanceof Point) return ((Point) val);
   else if (val instanceof Rectangle) {
      Rectangle r = (Rectangle) val;
      return new Point(r.x + r.width/2,r.y + r.height/2);
    }
   else if (val instanceof Component) {
      BudaRoot br = BudaRoot.findBudaRoot((Component) val);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(((Component) val));
      Rectangle r = BudaRoot.findBudaLocation(((Component) val));
      Point pt = new Point(r.x + r.width/2,r.y + r.height/2);
      pt = SwingUtilities.convertPoint(bba,pt,br);
      return pt;
    }

   return null;
}


Rectangle getRectangle(String var)
{
   if (var == null) return null;

   Object val = value_map.get(var);
   if (val == null) return null;
   if (val instanceof Point) {
      Point pt = (Point) val;
      return new Rectangle(pt.x,pt.y,1,1);
    }
   else if (val instanceof Rectangle) {
      return ((Rectangle) val);
    }
   else if (val instanceof Component) {
      Rectangle r = BudaRoot.findBudaLocation(((Component) val));
      return r;
    }

   return null;
}


Component getComponent(String var)
{
   if (var == null) return null;

   Object val = value_map.get(var);
   if (val == null) return null;
   if (val instanceof Component) {
      return ((Component) val);
    }
   return null;
}

/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

void reset()
{
   Point pt = getPoint("StartPoint");
   Rectangle r = getRectangle("StartViewport");
   Component bba = getComponent("BubbleArea");

   try {
      if (pt != null) mouseMove(pt.x,pt.y);
      BudaBubbleArea bbac = buda_root.getCurrentBubbleArea();
      if (bba == bbac && r != null) {
	 Rectangle r1 = bbac.getViewport();
	 if (!r.equals(r1)) buda_root.setViewport(r.x,r.y);
       }
    }
   catch (BhelpException e) { }
}







}	// end of class BhelpContext




/* end of BhelpContext.java */
