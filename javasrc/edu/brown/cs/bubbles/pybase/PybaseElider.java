/********************************************************************************/
/*										*/
/*		PybaseElider.java						*/
/*										*/
/*	Handle elision computation for Bubbles from Python			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.Found;
import edu.brown.cs.bubbles.pybase.symbols.GenAndTok;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.jython.ast.modType;
import org.python.pydev.parser.jython.ast.name_contextType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class PybaseElider implements PybaseConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<ElidePriority> elide_pdata;
private List<ElideRegion> elide_rdata;

private static Prioritizer down_priority;

private static final double	UP_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_COUNT = 0.95;
private static final double	DOWN_DEFAULT_ITEM  = 0.99;


static {
   Prioritizer dflt = new DefaultPrioritizer(DOWN_DEFAULT_SCALE,DOWN_DEFAULT_COUNT,DOWN_DEFAULT_ITEM);

   down_priority = dflt;
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseElider()
{
   elide_pdata = new ArrayList<ElidePriority>();
   elide_rdata = new ArrayList<ElideRegion>();
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining elision information				*/
/*										*/
/********************************************************************************/


void clearElideData()
{
   elide_pdata.clear();
   elide_rdata.clear();
}



void addElidePriority(int soff,int eoff,double pri)
{
   ElidePriority ed = new ElidePriority(soff,eoff,pri);
   elide_pdata.add(ed);
}


void addElideRegion(int soff,int eoff)
{
   ElideRegion er = new ElideRegion(soff,eoff);
   elide_rdata.add(er);
}



void noteEdit(int soff,int len,int rlen)
{
   for (Iterator<ElidePriority> it = elide_pdata.iterator(); it.hasNext(); ) {
      ElidePriority ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }

   for (Iterator<ElideRegion> it = elide_rdata.iterator(); it.hasNext(); ) {
      ElideRegion ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }
}



/********************************************************************************/
/*										*/
/*	Elision computaton methods						*/
/*										*/
/********************************************************************************/

boolean computeElision(ISemanticData isd,IvyXmlWriter xw)
{
   SimpleNode root = isd.getRootNode();

   if (root == null || elide_rdata.isEmpty()) return false;

   ElidePass1 ep1 = null;
   if (!elide_pdata.isEmpty()) {
      ep1 = new ElidePass1(isd);
      try {
	 root.accept(ep1);
      }
      catch (Exception e) {
	 PybaseMain.logE("Problem with elision pass 2",e);
       }
    }

   ElidePass2 ep2 = new ElidePass2(isd,ep1);
   try {
      root.accept(ep2);
   }
   catch (Exception e) {
      PybaseMain.logE("Problem with elision pass 2",e);
    }

   ElidePass3 ep3 = new ElidePass3(isd,ep2,xw);
   try {
      root.accept(ep3);
   }
   catch (Exception e) {
      PybaseMain.logE("Problem with elision pass 3",e);
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Access methods for elision information					*/
/*										*/
/********************************************************************************/

private double getElidePriority(IFileData ifd,SimpleNode n)
{
   for (ElidePriority ep : elide_pdata) {
      if (ep.useForPriority(ifd,n)) return ep.getPriority();
    }

   return 0;
}



private boolean isActiveRegion(int soff,int len)
{
   for (ElideRegion er : elide_rdata) {
      if (er.overlaps(soff,len)) return true;
    }

   return false;
}



private boolean isRootRegion(int soff,int len)
{
   for (ElideRegion er : elide_rdata) {
      if (er.contains(soff,len)) return true;
    }

   return false;
}



private double scaleUp(SimpleNode n)
{
   return UP_DEFAULT_SCALE;
}



/********************************************************************************/
/*										*/
/*	Main priority function							*/
/*										*/
/********************************************************************************/

private double computePriority(double parprior,SimpleNode base,double pass1prior)
{
   double p = down_priority.getPriority(parprior,base);

   if (pass1prior > p) p = pass1prior;

   return p;
}



/********************************************************************************/
/*										*/
/*	Formatting type function						*/
/*										*/
/********************************************************************************/

private String getFormatType(SimpleNode n,PybaseScopeItems scp)
{
   String typ = null;

   if (n == null || scp == null) return null;

   Found f = scp.findByToken(n);
   GenAndTok gat = null;
   AbstractToken tok = null;
   if (f != null) {
      gat = f.getSingle();
      if (gat != null) tok = gat.getToken();
   }

   if (n instanceof Name) {
      switch (((Name) n).ctx) {
	 case expr_contextType.Artificial :
	 case expr_contextType.AugLoad :
	 case expr_contextType.AugStore :
	 case expr_contextType.Del :
	 case expr_contextType.Load :
	 case expr_contextType.Store :
	    break;
	 case expr_contextType.KwOnlyParam :
	 case expr_contextType.Param :
	    return "PARAMDECL";
       }
      SimpleNode pn = n.parent;
      if (pn != null && pn instanceof Call) {
	 Call cn = (Call) pn;
	 if (cn.func == n) return "CALL";
	 // check for undefined here
       }
      else if (pn != null && pn instanceof Attribute && (((Attribute) pn).attr) == n) {
	 SimpleNode ppn = pn.parent;
	 if (ppn != null && ppn instanceof Call) return "CALL";
      }
      if (tok == null) return "UNDEF";
      switch (tok.getType()) {
	 case BUILTIN :
	    typ = "BUILTIN";
            break;
	 case CLASS :
	    typ =  "TYPE";
            break;
	 case UNKNOWN :
	    typ = "UNDEF";
            break;
         case RELATIVE_IMPORT :
         case IMPORT :
            break;
         case FUNCTION :
            typ = "CALLU";
            break;
         case EPYDOC :
         case ATTR :
            typ = "FIELD";
            break;
         case PACKAGE :
            break;
         case LOCAL :
            break;
         case OBJECT_FOUND_INTERFACE :
            typ = "TYPE";
            break;
	 default:
	    break;
      }
      if (gat != null && gat.getScopeId() == 1) typ = "BUILTIN";
    }
   else if (n instanceof NameTok) {
      switch (((NameTok) n).ctx) {
	 case name_contextType.Attrib :
	    typ = "FIELD";
	    SimpleNode pn = n.parent;
	    if (pn != null && pn instanceof Attribute && (((Attribute) pn).attr) == n) {
	       SimpleNode ppn = pn.parent;
	       if (ppn != null && ppn instanceof Call) {
		  typ = "CALL";
	       }
	    }
	    break;
	 case name_contextType.ClassName :
	    typ = "TYPE";
	    break;
	 case name_contextType.FunctionName :
	    typ = "METHODDECL";
	    break;
	 case name_contextType.GlobalName :
	    typ = "VARDECL";
	    break;
	 case name_contextType.ImportModule :
	    typ = "PACKAGE";
	    break;
	 case name_contextType.ImportName :
	    typ = "MODULE";
	    break;
	 case name_contextType.KeywordName :
	    typ = "KEYWORD";
	    break;
	 case name_contextType.KwArg :
	    typ = "PARAMDECL";
	    break;
	 case name_contextType.NonLocalName :
	    typ = "VARDECL";
	    break;
	 case name_contextType.VarArg :
	    typ = "PARAMDECL";
	    break;
      }
    }

   return typ;
}



private String getNodeType(SimpleNode n)
{
   String typ = null;

   if (n instanceof NameTok) ;
   else if (n instanceof exprType) {
      SimpleNode p = n.parent;
      if (!(p instanceof exprType)) typ = "EXPR";
    }
   else if (n instanceof ClassDef) {
      typ = "CLASS";
    }
   else if (n instanceof FunctionDef) {
      typ = "FUNCTION";
    }
   else if (n instanceof modType) {
      typ = "MODULE";
    }
   else if (n instanceof commentType) {
      typ = "COMMENT";
    }
   else if (n instanceof stmtType || n instanceof suiteType) {
      typ = "STMT";
    }

   return typ;
}



/********************************************************************************/
/*										*/
/*	Tree walk for setting initial priorities				*/
/*										*/
/********************************************************************************/

private class ElidePass1 extends Visitor {

   private IFileData file_data;
   private Map<SimpleNode,Double> result_value;
   private int inside_count;
   private Stack<Double> p_stack;

   ElidePass1(ISemanticData isd) {
      file_data = isd.getFileData();
      result_value = new HashMap<>();
      inside_count = 0;
      p_stack = new Stack<>();
    }

   @Override public void traverse(SimpleNode n) throws Exception {
      preVisit(n);

      if (inside_count == 0 && !isActiveRegion(file_data.getStartOffset(n),file_data.getLength(n))) {
	 return;
       }

      super.traverse(n);

      postVisit(n);
    }

   public void preVisit(SimpleNode n) {
      if (inside_count > 0) {
	 ++inside_count;
	 return;
       }

      double p = getElidePriority(file_data,n);
      if (p != 0) {
	 result_value.put(n,p);
	 ++inside_count;
       }

      p_stack.push(0.0);
    }

   public void postVisit(SimpleNode n) {
      if (inside_count > 0) {
	 --inside_count;
	 return;
       }

      double p = p_stack.pop();
      if (p > 0) {
	 p *= scaleUp(n);
	 result_value.put(n,p);
       }

      SimpleNode par = n.parent;
      if (par != null) {
	 double pp = p_stack.pop();
	 pp = merge(pp,n);
	 p_stack.push(pp);
       }
    }

   double getPriority(SimpleNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

   private double merge(double p,SimpleNode n) {
      Double dv = result_value.get(n);
      if (dv == null) return p;
      double q = 1 - (1-p)*(1-dv.doubleValue());
      return q;
    }

}	// end of innerclass ElidePass1




/********************************************************************************/
/*										*/
/*	Tree walk for setting final priorities					*/
/*										*/
/*	Visitation:								*/
/*	   If visit<xxx> is defined, it should call traverse(node)		*/
/*	   Otherwise, it or unhandled_node is called				*/
/*	   Followed by traversal of children					*/
/*	   Followed by postVisit						*/
/*										*/
/********************************************************************************/

private class ElidePass2 extends Visitor {

   private IFileData file_data;
   private ElidePass1 up_values;
   private Map<SimpleNode,Double> result_value;
   private SimpleNode active_node;

   ElidePass2(ISemanticData isd,ElidePass1 pass1) {
      file_data = isd.getFileData();
      up_values = pass1;
      result_value = new HashMap<SimpleNode,Double>();
      active_node = null;
    }

   Map<SimpleNode,Double> getResultValues()		{ return result_value; }

   @Override public void traverse(SimpleNode n) throws Exception {
      super.traverse(n);
      postVisit(n);
    }

   @Override protected Object unhandled_node(SimpleNode n) {
      if (active_node == null) {
	 if (isRootRegion(file_data.getStartOffset(n),file_data.getLength(n))) {
	    active_node = n;
	    result_value.put(n,1.0);
	  }
	 return n;
       }
      else if (active_node == null) return n;

      double v = getPriority(n.parent);
      double v0 = 0;
      if (up_values != null) v0 = up_values.getPriority(n);
      double p = computePriority(v,n,v0);
      if (p != 0) {
	 result_value.put(n,p);
       }
      return n;
    }

   public void postVisit(SimpleNode n) {
      if (active_node == n) active_node = null;
    }

   @Override public Object visitIf(If n) throws Exception {
      Object rslt = unhandled_node(n);
      traverse(n);
      return rslt;
    }


   double getPriority(SimpleNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

}	// end of inner class ElidePass2




/********************************************************************************/
/*										*/
/*	Class for handling elision output with a tree walk			*/
/*										*/
/********************************************************************************/

private class ElidePass3 extends Visitor {

   private ISemanticData sem_data;
   private IFileData file_data;
   private Map<SimpleNode,Double> result_value;
   private SimpleNode active_node;
   private IvyXmlWriter xml_writer;

   ElidePass3(ISemanticData isd,ElidePass2 pass2,IvyXmlWriter xw) {
      sem_data = isd;
      xml_writer = xw;
      file_data = isd.getFileData();
      result_value = pass2.getResultValues();
      active_node = null;
    }

   @Override public void traverse(SimpleNode n) throws Exception {
      super.traverse(n);
      postVisit(n);
    }

   @Override public Object unhandled_node(SimpleNode n) {
      if (active_node == null) {
	 if (isRootRegion(file_data.getStartOffset(n),file_data.getLength(n))) {
	    active_node = n;
	    result_value.put(n,1.0);
	    outputXmlStart(n);
	    return n;
	  }
	 else return null;
       }
      else if (active_node == null) return null;

      double p = getPriority(n);
      if (p <= 0) return null;

      result_value.put(n,p);
      outputXmlStart(n);

      return n;
    }

   @Override public Object visitClassDef(ClassDef n) throws Exception {
      Object ret = unhandled_node(n);
      handleNode(n.name);
      handleArray(n.bases);
      handleBlock(n.body);
      handleArray(n.decs);
      handleArray(n.keywords);
      handleNode(n.starargs);
      handleNode(n.kwargs);
      postVisit(n);
      return ret;
    }

   @Override public Object visitFor(For n) throws Exception {
      Object ret = unhandled_node(n);
      handleNode(n.target);
      handleNode(n.iter);
      handleBlock(n.body);
      handleNode(n.orelse);
      postVisit(n);
      return ret;
    }

   @Override public Object visitFunctionDef(FunctionDef n) throws Exception {
      Object ret = unhandled_node(n);
      handleNode(n.name);
      handleNode(n.args);
      handleBlock(n.body);
      handleArray(n.decs);
      handleNode(n.returns);
      postVisit(n);
      return ret;
    }

   @Override public Object visitIf(If n) throws Exception {
      Object ret = unhandled_node(n);
      handleNode(n.test);
      handleBlock(n.body);
      handleNode(n.orelse);
      postVisit(n);
      return ret;
    }

   @Override public Object visitSuite(Suite n) throws Exception {
      Object ret = unhandled_node(n);
      handleBlock(n.body);
      postVisit(n);
      return ret;
    }

   @Override public Object visitTryExcept(TryExcept n) throws Exception {
      Object ret = unhandled_node(n);
      handleBlock(n.body);
      if (n.handlers != null) {
	 for (int i = 0; i < n.handlers.length; ++i) {
	    excepthandlerType h = n.handlers[i];
	    if (h != null) {
	       handleNode(h.type);
	       handleNode(h.name);
	       handleBlock(n.body);
	     }
	  }
       }
      handleNode(n.orelse);
      postVisit(n);
      return ret;
    }

   @Override public Object visitTryFinally(TryFinally n) throws Exception {
      Object ret = unhandled_node(n);
      handleBlock(n.body);
      handleNode(n.finalbody);
      postVisit(n);
      return ret;
    }

   @Override public Object visitWhile(While n) throws Exception {
      Object ret = unhandled_node(n);
      handleNode(n.test);
      handleBlock(n.body);
      handleNode(n.orelse);
      postVisit(n);
      return ret;
    }

   @Override public Object visitNameTok(NameTok n) throws Exception {
      Object ret = unhandled_node(n);
      PybaseScopeItems scp = sem_data.getGlobalScope();
      if (ret != null && scp != null) {
         Found fnd = scp.findByToken(n);
         if (fnd != null) {
            GenAndTok gat = fnd.getSingle();
            AbstractToken at = gat.getGenerator();
            String s1 = at.getAsAbsoluteImport();
            xml_writer.field("FULLNAME",s1);
          }
       }
      postVisit(n);
      return ret;
   }

   private void postVisit(SimpleNode n) {
      if (active_node != null && xml_writer != null && result_value.get(n) != null &&
	     result_value.get(n) > 0) {
	 xml_writer.end("ELIDE");
       }
      if (active_node == n) active_node = null;
    }

   private double getPriority(SimpleNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

   private boolean outputXmlStart(SimpleNode n) {
      if (active_node == null || xml_writer == null || result_value.get(n) == null ||
               result_value.get(n) <= 0)
         return false;
   
      xml_writer.begin("ELIDE");
      int sp = file_data.getStartOffset(n);
      int ln = file_data.getLength(n);
      xml_writer.field("START",sp);
      xml_writer.field("LENGTH",ln);
      double p = result_value.get(n);
      xml_writer.field("PRIORITY",p);
      String typ = getFormatType(n,sem_data.getGlobalScope());
      if (typ != null) xml_writer.field("TYPE",typ);
      String ttyp = getNodeType(n);
      if (ttyp != null) xml_writer.field("NODE",ttyp);
   
      return true;
    }

   private void handleBlock(stmtType [] stmts) throws Exception {
      if (active_node != null && xml_writer != null && stmts != null && stmts.length > 0 &&
	     stmts[0] != null &&
	     result_value.get(stmts[0]) != null && result_value.get(stmts[0]) > 0) {
	 xml_writer.begin("ELIDE");
	 int sp = file_data.getStartOffset(stmts[0]);
	 int ep = file_data.getEndOffset(stmts[stmts.length-1]);
	 xml_writer.field("START",sp);
	 xml_writer.field("LENGTH",ep-sp+1);
	 double p = result_value.get(stmts[0]);
	 xml_writer.field("PRIORITY",p);
	 xml_writer.field("NODE","BLOCK");
	 for (stmtType st : stmts) {
	    if (st != null) st.accept(this);
	  }
	 xml_writer.end("ELIDE");
       }
      else if (stmts != null) {
	 for (stmtType st : stmts) {
	    if (st != null) st.accept(this);
	 }
      }
    }

   private void handleNode(SimpleNode n) throws Exception {
      if (n == null) return;
      n.accept(this);
    }

   private void handleArray(SimpleNode [] nds) throws Exception {
      if (nds == null) return;
      for (int i = 0; i < nds.length; ++i) {
	 if (nds[i] != null) nds[i].accept(this);
       }
    }

}	// end of innerclass ElidePass3




/********************************************************************************/
/*										*/
/*	Classes for elision region and priorities				*/
/*										*/
/********************************************************************************/

private abstract class ElideData {

   private int start_offset;
   private int end_offset;

   ElideData(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
    }

   boolean contains(int soff,int len) {
      return (start_offset <= soff && end_offset >= soff+len-1);
    }

   boolean useForPriority(IFileData ifd,SimpleNode n) {
      int sp = ifd.getStartOffset(n);
      int ln = ifd.getLength(n);
      if (sp == 0) return false;
      if (start_offset != end_offset) return contains(sp,ln);
      if (!overlaps(sp,ln)) return false;


      return true;
    }

   boolean overlaps(int soff,int len) {
      if (start_offset >= soff+len-1) return false;
      if (end_offset <= soff) return false;
      return true;
    }

   boolean noteEdit(int soff,int len,int rlen) {
      if (end_offset <= soff) ; 			// before the change
      else if (start_offset > soff + len - 1) { 	// after the change
	 start_offset += rlen - len;
	 end_offset += rlen - len;
       }
      else if (start_offset <= soff && end_offset >= soff+len-1) {	// containing the change
	 end_offset += rlen -len;
       }
      else return false;				     // in the edit -- remove it
      return true;
    }

}	// end of inner abstract class ElideData




private class ElideRegion extends ElideData {

   ElideRegion(int soff,int eoff) {
      super(soff,eoff);
    }

}	// end of innerclass ElideData




private class ElidePriority extends ElideData {

   private double elide_priority;

   ElidePriority(int soff,int eoff,double pri) {
      super(soff,eoff);
      elide_priority = pri;
    }

   double getPriority() 			{ return elide_priority; }

}	// end of innerclass ElideData




/********************************************************************************/
/*										*/
/*	Priority computation classes						*/
/*										*/
/********************************************************************************/

private abstract static class Prioritizer {

   abstract double getPriority(double ppar,SimpleNode base);

}


private static class DefaultPrioritizer extends Prioritizer {

   private double base_value;

   DefaultPrioritizer(double v,double scl,double iscl) {
      base_value = v;
    }

   @Override double getPriority(double ppar,SimpleNode base) {
      double dv = base_value;
      // need to take number of children into account
      if (base.parent != null) return ppar * dv;
      return ppar;
    }

}	// end of inner class DefaultPrioritizer






}	 // end of class PybaseElider




/* end of PybaseElider.java */

