/********************************************************************************/
/*										*/
/*		NobaseElider.java						*/
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



package edu.brown.cs.bubbles.nobase;

import org.eclipse.wst.jsdt.core.dom.*;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class NobaseElider implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<ElidePriority> elide_pdata;
private List<ElideRegion> elide_rdata;

private static Prioritizer down_priority;

private static final double	UP_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_COUNT = 0.95;
private static final double	DOWN_DEFAULT_ITEM  = 0.99;
private static final double	SWITCH_BLOCK_SCALE = 0.90;

static {
   Prioritizer dflt = new DefaultPrioritizer(DOWN_DEFAULT_SCALE,DOWN_DEFAULT_COUNT,DOWN_DEFAULT_ITEM);
   Prioritizer same = new DefaultPrioritizer(1.0,1.0,1.0);
   NodePrioritizer p1 = new NodePrioritizer(dflt);
   p1.addPriority(IfStatement.class,same);
   StructuralPrioritizer p0 = new StructuralPrioritizer(dflt);
   p0.addPriority(IfStatement.class,IfStatement.ELSE_STATEMENT_PROPERTY,p1);
   down_priority = p0;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseElider()
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
   ASTNode root = isd.getRootNode();

   if (root == null || elide_rdata.isEmpty()) return false;

   ElidePass1 ep1 = null;
   if (!elide_pdata.isEmpty()) {
      ep1 = new ElidePass1();
      try {
	 root.accept(ep1);
      }
      catch (Exception e) {
	 NobaseMain.logE("Problem with elision pass 1",e);
       }
    }

   ElidePass2 ep2 = new ElidePass2(ep1,xw);
   try {
      root.accept(ep2);
   }
   catch (Exception e) {
      NobaseMain.logE("Problem with elision pass 2",e);
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Access methods for elision information					*/
/*										*/
/********************************************************************************/

private double getElidePriority(ASTNode n)
{
   for (ElidePriority ep : elide_pdata) {
      if (ep.useForPriority(n)) return ep.getPriority();
    }

   return 0;
}



private boolean isActiveRegion(int soff,int eoff)
{
   for (ElideRegion er : elide_rdata) {
      if (er.overlaps(soff,eoff)) return true;
    }

   return false;
}



private boolean isRootRegion(int soff,int eoff)
{
   for (ElideRegion er : elide_rdata) {
      if (er.contains(soff,eoff)) return true;
    }

   return false;
}




private double scaleUp(ASTNode n)
{
   return UP_DEFAULT_SCALE;
}




/********************************************************************************/
/*										*/
/*	Main priority function							*/
/*										*/
/********************************************************************************/

private double computePriority(double parprior,ASTNode base,double pass1prior)
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

private String getFormatType(ASTNode n)
{
   String typ = null;
   boolean isdef = true;

   if (n == null) return null;

   NobaseSymbol nsp = NobaseAst.getDefinition(n);
   if (nsp == null) {
      nsp = NobaseAst.getReference(n);
      if (nsp != null) isdef = false;
    }
   if (nsp == null) {
      if (n instanceof Name) {
	 nsp = NobaseAst.getDefinition(n.getParent());
       }
    }
   if (nsp != null) {
      switch (nsp.getNameType()) {
	 case MODULE :
	    typ = "MODULE";
	    break;
	 case FUNCTION :
	    typ = (isdef ? "METHODDECL" : "CALL");
	    break;
	 case LOCAL :
	    typ = (isdef ? "VARDECL" : null);
	    break;
	 case VARIABLE :
	    typ = (isdef ? "VARDECL" : "FIELD");
	    break;
       }
    }
   else if (n instanceof Name) {
      ASTNode p = n.getParent();
      ASTNode pp = p.getParent();
      StructuralPropertyDescriptor spd = n.getLocationInParent();
      switch (p.getNodeType()) {
	 case ASTNode.FUNCTION_INVOCATION :
	    if (n.getLocationInParent() == FunctionInvocation.NAME_PROPERTY) {
	       typ = "CALL";
	     }
	    break;
	 case ASTNode.SIMPLE_TYPE :
	 case ASTNode.QUALIFIED_TYPE :
	    typ = "TYPE";
	    break;
	 case ASTNode.FUNCTION_DECLARATION :
	    if (spd == FunctionDeclaration.METHOD_NAME_PROPERTY) {
	       typ = "METHODDECL";
	     }
	    break;
	 case ASTNode.SINGLE_VARIABLE_DECLARATION :
	    if (pp.getNodeType() == ASTNode.CATCH_CLAUSE) typ = "EXCEPTIONDECL";
	    else typ = "PARAMDECL";
	    break;
	 case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
	    if (pp.getNodeType() == ASTNode.FIELD_DECLARATION) typ = "FIELDDECL";
	    else typ = "LOCALDECL";
	    break;
	 case ASTNode.TYPE_DECLARATION :
	    typ = "CLASSDECL";
	    break;
       }
    }

   return typ;
}



private String getNodeType(ASTNode n)
{
   String typ = null;

   if (n instanceof FunctionExpression || n instanceof FunctionDeclaration) {
      if (n.getParent() instanceof VariableDeclaration) typ = "EXPR";
      else typ = "FUNCTION";
    }
   else if (n instanceof SimpleName) ;
   else if (n instanceof Expression) {
      ASTNode p = n.getParent();
      if (p != null && !(p instanceof Expression)) typ = "EXPR";
    }
   else if (n instanceof VariableDeclaration) {
      VariableDeclaration d = (VariableDeclaration) n;
      NobaseSymbol ns = NobaseAst.getDefinition(d);
      if (ns != null) {
	 switch (ns.getNameType()) {
	    case FUNCTION :
	       typ = "FUNCTION";
	       break;
	    case LOCAL :
	       break;
	    case MODULE :
	       break;
	    case VARIABLE :
	       if (d.getInitializer() != null) typ = "INITIALIZER";
	       else typ = "FIELD";
	       break;
	  }
       }
   }
   else if (n instanceof JavaScriptUnit) {
      typ = "MODULE";
    }
   else if (n instanceof Block) {
      typ = "BLOCK";
    }
   else if (n instanceof SwitchCase) {
      typ = "CASE";
    }
   else if (n instanceof CatchClause) {
      typ = "CATCH";
    }
   else if (n instanceof Statement) {
      typ = "STMT";
    }

   return typ;
}



/********************************************************************************/
/*										*/
/*	Tree walk for setting initial priorities				*/
/*										*/
/********************************************************************************/

private class ElidePass1 extends ASTVisitor {

   private Map<ASTNode,Double> result_value;
   private int inside_count;

   ElidePass1() {
      result_value = new HashMap<>();
      inside_count = 0;
    }

   @Override public void preVisit(ASTNode n) {
      if (inside_count > 0) {
	 ++inside_count;
       }
      else {
	 double p = getElidePriority(n);
	 if (p != 0) {
	    result_value.put(n,p);
	    ++inside_count;
	  }
       }
    }

   @Override public void postVisit(ASTNode n) {
      if (inside_count > 0) {
	 --inside_count;
	 return;
       }

      List<?> l = n.structuralPropertiesForType();
      double p = 0;
      for (Iterator<?> it = l.iterator(); it.hasNext(); ) {
	 StructuralPropertyDescriptor sp = (StructuralPropertyDescriptor) it.next();
	 if (sp.isChildProperty()) {
	    ASTNode cn = (ASTNode) n.getStructuralProperty(sp);
	    p = merge(p,cn);
	  }
	 else if (sp.isChildListProperty()) {
	    List<?> cl = (List<?>) n.getStructuralProperty(sp);
	    for (Iterator<?> it1 = cl.iterator(); it1.hasNext(); ) {
	       ASTNode cn = (ASTNode) it1.next();
	       p = merge(p,cn);
	     }
	  }
       }
      if (p > 0) {
	 p *= scaleUp(n);
	 result_value.put(n,p);
       }
    }

   @Override public boolean visit(FunctionDeclaration n) {
      int sp = n.getStartPosition();
      return isActiveRegion(sp,sp+n.getLength());
    }

   @Override public boolean visit(Block n) {
      int sp = n.getStartPosition();
      return isActiveRegion(sp,sp+n.getLength());
    }

   @Override public boolean visit(VariableDeclarationFragment n) {
      int sp = n.getStartPosition();
      return isActiveRegion(sp,sp+n.getLength());
    }

   double getPriority(ASTNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

   private double merge(double p,ASTNode n) {
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
/********************************************************************************/

private class ElidePass2 extends ASTVisitor {

   private ElidePass1 up_values;
   private Map<ASTNode,Double> result_value;
   private ASTNode active_node;
   private IvyXmlWriter xml_writer;
   private boolean last_case;
   private Stack<ASTNode> switch_stack;
   private JavaScriptUnit tree_root;

   ElidePass2(ElidePass1 pass1,IvyXmlWriter xw) {
      up_values = pass1;
      xml_writer = xw;
      result_value = new HashMap<ASTNode,Double>();
      active_node = null;
      last_case = false;
      switch_stack = new Stack<ASTNode>();
      tree_root = null;
    }

   @Override public void preVisit(ASTNode n) {
      if (tree_root == null && n instanceof JavaScriptUnit) tree_root = (JavaScriptUnit) n;
      if (active_node == null) {
	 int sp = n.getStartPosition();
	 if (isRootRegion(sp,sp+n.getLength())) {
	    active_node = n;
	    result_value.put(n,1.0);
	    outputXmlStart(n);
	  }
	 return;
       }
      double v = getPriority(n.getParent());
      double v0 = 0;
      if (up_values != null) v0 = up_values.getPriority(n);
      double p = computePriority(v,n,v0);
      if (p != 0) {
	 result_value.put(n,p);
	 checkSwitchBlock(n);
	 outputXmlStart(n);
       }
    }

   @Override public void postVisit(ASTNode n) {
      if (active_node == n) active_node = null;
      if (xml_writer != null && result_value.get(n) != null && result_value.get(n) > 0) {
	 xml_writer.end("ELIDE");
       }
      checkEndSwitchBlock(n);
    }

   @Override public boolean visit(FunctionDeclaration n) {
      int sp = n.getStartPosition();
      return isActiveRegion(sp,sp+n.getLength());
    }

   @Override public boolean visit(VariableDeclarationFragment n) {
      int sp = n.getStartPosition();
      return isActiveRegion(sp,sp+n.getLength());
    }

   double getPriority(ASTNode n) {
      Double dv = result_value.get(n);
      if (dv != null) return dv.doubleValue();
      return 0;
    }

   private void outputXmlStart(ASTNode n) {
      if (xml_writer != null) {
	 xml_writer.begin("ELIDE");
	 int sp = n.getStartPosition();
	 int ep = sp + n.getLength();
	 int esp = tree_root.getExtendedStartPosition(n);
	 int eep = esp + tree_root.getExtendedLength(n);
	 xml_writer.field("START",sp);
	 if (esp != sp) xml_writer.field("ESTART",esp);
	 xml_writer.field("LENGTH",ep-sp);
	 xml_writer.field("ELENGTH",eep-esp);
	 double p = result_value.get(n);
	 for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
	 xml_writer.field("PRIORITY",p);
	 String typ = getFormatType(n);
	 if (typ != null) {
	    xml_writer.field("TYPE",typ);
	    if (typ.startsWith("METHODDECL") || typ.startsWith("VARDECL")) {
	       outputDeclInfo(n);
	     }
	  }
	 String ttyp = getNodeType(n);
	 if (ttyp != null) xml_writer.field("NODE",ttyp);
       }
    }

   private void outputDeclInfo(ASTNode name) {
      NobaseSymbol nsp = NobaseAst.getDefinition(name);
      if (nsp == null) nsp = NobaseAst.getReference(name);
      if (nsp == null) return;

      switch (nsp.getNameType()) {
	 case MODULE :
	    break;
	 case FUNCTION :
	    xml_writer.field("FULLNAME",nsp.getBubblesName());
	    break;
	 case VARIABLE :
	    xml_writer.field("FULLNAME",nsp.getBubblesName());
	    break;
	 case LOCAL :
	    xml_writer.field("FULLNAME",nsp.getBubblesName());
	    break;
       }
    }

   private void checkSwitchBlock(ASTNode n) {
      if (!last_case || xml_writer == null) return;
      last_case = false;
      if (result_value.get(n) == null) return;
      if (n instanceof SwitchCase) return;
      ASTNode last = null;
      if (n instanceof Statement) {
	 ASTNode pn = n.getParent();
	 if (pn.getNodeType() != ASTNode.SWITCH_STATEMENT) return;
	 List<?> l = (List<?>) pn.getStructuralProperty(SwitchStatement.STATEMENTS_PROPERTY);
	 int idx = l.indexOf(n);
	 if (idx < 0) return;
	 int lidx = idx;
	 while (lidx+1 < l.size()) {
	    if (l.get(lidx+1) instanceof SwitchCase) break;
	    else if (l.get(lidx+1) instanceof Statement) ++lidx;
	    else return;
	  }
	 if (lidx - idx >= 2) last = (ASTNode) l.get(lidx);
       }
      if (last == null) return;
      xml_writer.begin("ELIDE");
      int sp = n.getStartPosition();
      int esp = tree_root.getExtendedStartPosition(n);
      int ln = n.getLength();
      int eln = tree_root.getExtendedLength(n);
      xml_writer.field("START",sp);
      if (esp != sp) xml_writer.field("ESTART",esp);
      xml_writer.field("LENGTH",ln);
      if (eln != ln) xml_writer.field("ELENGTH",eln);
      double p = result_value.get(n);
      for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
      xml_writer.field("PRIORITY",p);
      xml_writer.field("NODE","SWBLOCK");
      switch_stack.push(last);
    }

   private void checkEndSwitchBlock(ASTNode n) {
      while (!switch_stack.isEmpty() && n == switch_stack.peek()) {
	 switch_stack.pop();
	 xml_writer.end("ELIDE");
       }
      last_case = (n instanceof SwitchCase);
    }

}	// end of innerclass ElidePass2



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

   boolean contains(int soff,int eoff) {
      return (start_offset <= soff && end_offset >= eoff);
    }

   boolean useForPriority(ASTNode n) {
      int sp = n.getStartPosition();
      int ep = sp + n.getLength();
      if (start_offset != end_offset) return contains(sp,ep);
      if (!overlaps(sp,ep)) return false;
      // check if any child overlaps, use child if so
      for (Iterator<?> it = n.structuralPropertiesForType().iterator(); it.hasNext(); ) {
	 StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) it.next();
	 if (spd.isSimpleProperty()) ;
	 else if (spd.isChildProperty()) {
	    ASTNode cn = (ASTNode) n.getStructuralProperty(spd);
	    if (cn != null) {
	       if (overlaps(cn.getStartPosition(),cn.getLength())) return false;
	     }
	  }
	 else {
	    List<?> lcn = (List<?>) n.getStructuralProperty(spd);
	    for (Iterator<?> it1 = lcn.iterator(); it1.hasNext(); ) {
	       ASTNode cn = (ASTNode) it1.next();
	       if (overlaps(cn.getStartPosition(),cn.getLength())) return false;
	     }
	  }
       }
      return true;
    }

   boolean overlaps(int soff,int eoff) {
      if (start_offset >= eoff) return false;
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

   abstract double getPriority(double ppar,ASTNode base);

}


private static class DefaultPrioritizer extends Prioritizer {

   private double base_value;
   private double count_scale;
   private double item_scale;

   DefaultPrioritizer(double v,double scl,double iscl) {
      base_value = v;
      count_scale = scl;
      item_scale = iscl;
    }

   @Override double getPriority(double ppar,ASTNode base) {
      double dv = base_value;
      StructuralPropertyDescriptor spd = base.getLocationInParent();

      if (base.getParent() == null) return ppar * dv;

      if (item_scale != 1) {
	 List<?> pl = base.getParent().structuralPropertiesForType();
	 int ct = pl.size();
	 for (int i = 0; i < ct; ++i) {
	    if (pl.get(i) == spd) break;
	    dv *= item_scale;
	  }
       }

      if (count_scale != 1 && spd.isChildListProperty()) {
	 List<?> cl = (List<?>) base.getParent().getStructuralProperty(spd);
	 int ct = cl.size();
	 boolean fnd = false;
	 for (int i = 0; i < ct; ++i) {
	    if (cl.get(i) == base) fnd = true;
	    dv *= count_scale;
	    if (!fnd) dv *= item_scale;
	  }
       }

      return ppar * dv;
    }

}	// end of innerclass DefaultPrioritizer



private static class PropertyDescriptor {

   private Class<?> parent_type;
   private StructuralPropertyDescriptor child_index;;

   PropertyDescriptor(Class<?> par,StructuralPropertyDescriptor idx) {
      parent_type = par;
      child_index = idx;
    }

   boolean match(ASTNode n) {
      if (n.getParent() == null) return false;
      if (n.getParent().getClass() != parent_type) return false;
      if (n.getLocationInParent() != child_index) return false;
      return true;
    }
}


private static class StructuralPrioritizer extends Prioritizer {

   private Prioritizer base_prioritizer;
   private Map<PropertyDescriptor,Prioritizer> priority_map;

   StructuralPrioritizer(Prioritizer base) {
      base_prioritizer = base;
      priority_map = new HashMap<PropertyDescriptor,Prioritizer>();
    }

   void addPriority(Class<?> par,StructuralPropertyDescriptor spd,Prioritizer p) {
      PropertyDescriptor pd = new PropertyDescriptor(par,spd);
      priority_map.put(pd,p);
    }

   @Override double getPriority(double ppar,ASTNode base) {
      Prioritizer p = base_prioritizer;
      for (Map.Entry<PropertyDescriptor,Prioritizer> ent : priority_map.entrySet()) {
	 if (ent.getKey().match(base)) {
	    p = ent.getValue();
	    break;
	  }
       }
      return p.getPriority(ppar,base);
    }

}	// end of innerclass StructuralPrioritizer

private static class NodePrioritizer extends Prioritizer {

   private Prioritizer base_prioritizer;
   private Map<Class<?>,Prioritizer> priority_map;

   NodePrioritizer(Prioritizer base) {
      base_prioritizer = base;
      priority_map = new HashMap<Class<?>,Prioritizer>();
    }

   void addPriority(Class<?> c,Prioritizer p) {
      priority_map.put(c,p);
    }

   @Override double getPriority(double ppar,ASTNode base) {
      Prioritizer p = priority_map.get(base.getClass());
      if (p == null) p = base_prioritizer;
      return p.getPriority(ppar,base);
    }

}	// end of innerclass NodePrioritizer


}	// end of class NobaseElider




/* end of NobaseElider.java */

