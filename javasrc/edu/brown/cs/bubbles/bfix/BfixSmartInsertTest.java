/********************************************************************************/
/*										*/
/*		BfixSmartInsertTest.java					*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpSymbolType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class BfixSmartInsertTest
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BfixCorrector file1_corrector;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public	BfixSmartInsertTest()
{
   String [] args = new String [] { "-m", "BUBBLES_TEST_spr", "-w",
	 "/home/spr/Eclipse/hump" };
   try {
      Class<?> c = Class.forName("edu.brown.cs.bubbles.bema.BemaMain");
      Method m = c.getDeclaredMethod("main",String [].class);
      m.invoke(null,(Object) args);
    }
   catch (Throwable t) { }
}


@Before
public void setupFiles()
{
   BaleFactory bf = BaleFactory.getFactory();
   String proj = "hump";
   File file = new File("/gpfs/main/research/people/spr/hump/javasrc/edu/brown/cs/hump/HumpFile.java");
   BaleWindow bw = bf.createFileEditor(proj,file,
	 "edu.brown.cs.hump.HumpFile");
   file1_corrector = new BfixCorrector(bw,false);
}


/*
 * Test case for edu.brown.cs.bubbles.bfix.BfixSmartInsert.findProperRoot(BfixOrderElement)
 */

@Test public void test_smartInsert()
{
   BfixSmartInsert smartie = new BfixSmartInsert(file1_corrector);
   BfixOrderNewElement newelt = new BfixOrderNewElement("edu.brown.cs.hump.HumpFile.getDocument",
	 BumpSymbolType.FUNCTION,Modifier.PUBLIC,
	 "public Document getDocument() { return base_document; }");
   smartie.smartInsertSetup(newelt);

   int pos = newelt.getInsertPosition();
   Assert.assertTrue(pos > 0);
   String cnts = newelt.getContents();
   Assert.assertTrue(cnts.contains("\n\n"));

   smartie.smartInsertInsert(newelt);
}


}	// end of class BfixSmartInsertTest




/* end of BfixSmartInsertTest.java */

