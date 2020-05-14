/********************************************************************************/
/*										*/
/*		BpareBuilder.java						*/
/*										*/
/*	Code to build pattern descriptions from ASTs				*/
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



package edu.brown.cs.bubbles.bpare;

import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


class BpareBuilder implements BpareConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BpareProject	for_project;
private File		raw_file;
private File		output_file;
private IvyXmlWriter	xml_writer;
private Stack<BparePattern> work_queue;
private List<ASTNode>	node_set;
private StartVisitor	start_visitor;
private int		listset_size;
private int		tot_pats;
private File		temp_dir;

private PatternType	pattern_type;
private BpareTrie	pattern_trie;

private static boolean	use_xml = false;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BpareBuilder(BpareProject p,PatternType pt)
{
   for_project = p;
   pattern_type = pt;
   work_queue = new Stack<>();
   start_visitor = new StartVisitor();
   node_set = new ArrayList<>();
   pattern_trie = new BpareTrie();

   xml_writer = null;
   listset_size = LIST_SET_SIZE;
   tot_pats = 0;
   raw_file = p.getDataFile("raw");
   output_file = p.getDataFile(null);
   File f1 = BoardSetup.getBubblesWorkingDirectory();
   temp_dir = new File(f1,"bparetemp");
   temp_dir.mkdir();
   temp_dir.deleteOnExit();
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void process()
{
   try {
      xml_writer = new IvyXmlWriter(raw_file);
      for (File f : for_project.getSourceFiles()) {
	 addFileInformation(f);
       }
      xml_writer.close();
    }
   catch (IOException e) {
      System.err.println("BPARE: Problem creating output file " + raw_file + ": " + e);
    }

   switch (pattern_type) {
      case STRING :
	 finishStringPatterns();
	 break;
      case TRIE :
	 for_project.setTrie(pattern_trie);
	 finishTriePatterns();
	 break;
    }

   // raw_file.delete();
}


private void finishStringPatterns()
{
   System.err.println("BPARE: Total patterns output: " + tot_pats);

   String cmd = "sort -T " + temp_dir + " " + raw_file + " | uniq -c | sort -r -T " + temp_dir;
   cmd = "sh -c '" + cmd + "'"; 

   System.err.println("BPARE: Run: " + cmd);
   try {
      IvyExec ex = new IvyExec(cmd,IvyExec.READ_OUTPUT);
      InputStream is = ex.getInputStream();
      InputStreamReader ir = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(ir);
      try (PrintWriter pw = new PrintWriter(new FileWriter(output_file))) {
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            pw.println(ln);
          }
       }
      int sts = ex.waitFor();
      if (sts != 0) throw new IOException("Bad status returned from sort: " + sts);
    }
   catch (IOException e) {
      System.err.println("BPARE: Problem sorting patterns: " + e);
      System.exit(1);
      return;
    }

   temp_dir.delete();
}


private void finishTriePatterns()
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(output_file);
      // pattern_trie.outputXml(xw);
      xw.close();
    }
   catch (IOException e) {
      System.err.println("BPARE: Problem writing trie: " + e);
      System.exit(1);
    }
}




/********************************************************************************/
/*										*/
/*	Pattern gathering for a file						*/
/*										*/
/********************************************************************************/

private void addFileInformation(File f)
{
   System.err.println("BPARE: Work on " + f);

   ASTNode root = compileFile(f);
   switch (pattern_type) {
      case STRING :
	 processFileStringPatterns(root);
	 break;
      case TRIE :
	 processFileTriePatterns(root);
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Get AST for a code fragment						*/
/*										*/
/********************************************************************************/

static ASTNode getStatementsAst(String frag)
{
   return JcompAst.parseStatement(frag);
}


static ASTNode getCompilationUnitAst(String cnts)
{
   return JcompAst.parseSourceFile(cnts);
}




private void processFileStringPatterns(ASTNode root)
{
   root.accept(start_visitor);
   int mxsz = work_queue.size();
   while (!work_queue.empty()) {
      BparePattern p = work_queue.pop();
      if (p == null) break;
      // System.err.println("WORK ON " + p);
      if (p.isValidPattern()) {
	 if (use_xml) p.outputPatternXml(xml_writer,null);
	 else p.outputPatternString(xml_writer);
	 ++tot_pats;
       }
      p.expandPattern(listset_size,work_queue);
      if (work_queue.size() > mxsz) mxsz = work_queue.size();
    }

   System.err.println("BPARE: Max workqueue size = " + mxsz);
}



private void processFileTriePatterns(ASTNode root)
{
   node_set.clear();
   root.accept(start_visitor);
   for (ASTNode n : node_set) {
      pattern_trie.addToTrie(n);
   }
}




private ASTNode compileFile(File f)
{
   return JcompAst.parseSourceFile(readFile(f));
}




/********************************************************************************/
/*										*/
/*	File utilities								*/
/*										*/
/********************************************************************************/

public char [] readFile(File f)
{
   char [] buf;

   try {
      int l = (int) f.length();
      buf = new char[l];
      Reader fr = new FileReader(f);
      int ln = 0;
      while (ln < l) {
	 int rln = 8192;
	 if (l - ln < rln) rln = (l-ln);
	 int xln = fr.read(buf,ln,rln);
	 ln += xln;
       }
      fr.close();
    }
   catch (Exception e) {
      System.err.println("WEIR: Problem opening file " + f + ": " + e);
      return null;
    }

   return buf;
}





/********************************************************************************/
/*										*/
/*	Tree visitor for computing patterns					*/
/*										*/
/********************************************************************************/

private class StartVisitor extends ASTVisitor {

   StartVisitor()		{ }

   @Override public void preVisit(ASTNode n) {
      switch (n.getNodeType()) {
	 case ASTNode.BLOCK :
	    queueNode(n);
	    break;
	 default :
	    break;
       }
    }

   private void queueNode(ASTNode n) {
      switch (pattern_type) {
	 case STRING :
	    BparePattern pp = new BparePattern(n);
	    work_queue.push(pp);
	    break;
	 case TRIE :
	    node_set.add(n);
	    break;
      }
   }

}	// end of subclass StartVisitor


}	// end of class BpareBuilder



/* end of BpareBuilder.java */


