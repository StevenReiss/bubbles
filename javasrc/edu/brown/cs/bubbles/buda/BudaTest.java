/********************************************************************************/
/*										*/
/*		BudaTest.java							*/
/*										*/
/*	BUblles Display Area test program					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import javax.swing.JEditorPane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.IOException;



public class BudaTest implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BudaTest bt = new BudaTest(args);

   bt.runTest();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BudaTest(String [] args)
{
}




/********************************************************************************/
/*										*/
/*	Test methods								*/
/*										*/
/********************************************************************************/

private void runTest()
{
   // test00();

   test01();
}





/********************************************************************************/
/*										*/
/*	Bubble frame test							*/
/*										*/
/********************************************************************************/

private void test01()
{
   BudaRoot root = new BudaRoot("Bubble Test");
   BudaBubble [] bbls = new BudaBubble[40];
   int bct = 0;

   for (int i = 0; i < 100; ++i) {
      for (int j = 0; j < 4; ++j) {
	 BudaBubble bb = new EditorBubble();
	 if (bct < bbls.length) bbls[bct++] = bb;

	 root.add(bb,new BudaConstraint(100+i*300+BUBBLE_DISPLAY_START_X,100+j*200+BUBBLE_DISPLAY_START_Y));
       }
    }

   BudaBubble sb = bbls[9];
   for (int i = 12; i < 16; ++i) {
      BudaBubbleLink lnk = new BudaBubbleLink(sb,
						 new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true),
						 bbls[i],
						 new BudaDefaultPort(BudaPortPosition.BORDER_W,true));
      root.addLink(lnk);
    }

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbls[0]);
   root.defineWorkingSet(bba,"Sample Set",new Rectangle(10240,0,2048,BUBBLE_DISPLAY_HEIGHT),null);

   root.setVisible(true);

   try {
      BudaXmlWriter xw = new BudaXmlWriter("test01.buda");
      xw.begin("BUDA");
      root.outputXml(xw,true);
      xw.end("BUDA");
      xw.close();
    }
   catch (IOException e) {
      System.err.println("BUDATEST: Problem with save: " + e);
    }
}







/********************************************************************************/
/*										*/
/*	Test editor bubble							*/
/*										*/
/********************************************************************************/

public static class EditorBubble extends BudaBubble {

   private static final long serialVersionUID = 1L;

   private Font base_font;

   public EditorBubble() {
      JEditorPane jp = new BubbleEditor();
      base_font = jp.getFont();
      jp.setSize(new Dimension(200,175));
      // jp.setOpaque(false);
      setContentPane(jp);
      setInteriorColor(Color.WHITE);
    }

   @Override public void setScaleFactor(double v) {
      float fsz = base_font.getSize2D();
      fsz *= (float) v;
      Font f1 = base_font.deriveFont(fsz);
      getContentPane().setFont(f1);
    }

}	// end of inner class EditorBubble



private static final String test_text =
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n" +
  "This is a test\nof the editor pane\nin a bubble\n";



public static class BubbleEditor extends JEditorPane {

   private static final long serialVersionUID = 1L;

   public BubbleEditor() {
      super("text/plain",test_text);
      addMouseListener(new FocusOnEntry());
    }


}	// end of inner class BubbleEditor


}	// end of class BudaTest




/* end of BudaTest.java */

