/********************************************************************************/
/*										*/
/*		BaleAstNode.java						*/
/*										*/
/*	Bubble Annotated Language Editor simplified AST-type node information	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class BaleAstNode implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleDocumentIde for_document;
private BaleAstNodeType node_type;
private BaleAstIdType id_type;
private boolean is_error;
private double	elide_priority;
private int	start_position;
private int	end_position;
private List<BaleAstNode> child_nodes;
private BaleAstNode parent_node;
private String	full_name;
private Element hint_data;


private static Map<String,BaleAstNodeType> ast_types;
private static Map<String,BaleAstIdType> id_types;

static {
   ast_types = new HashMap<String,BaleAstNodeType>();
   ast_types.put("BLOCK",BaleAstNodeType.BLOCK);
   ast_types.put("SWBLOCK",BaleAstNodeType.SWITCH_BLOCK);
   ast_types.put("STMT",BaleAstNodeType.STATEMENT);
   ast_types.put("CASE",BaleAstNodeType.STATEMENT);
   ast_types.put("CATCH",BaleAstNodeType.STATEMENT);
   ast_types.put("EXPR",BaleAstNodeType.EXPRESSION);
   ast_types.put("ATYPE",BaleAstNodeType.ANNOTATION);
   ast_types.put("ACLASS",BaleAstNodeType.ANNOTATION);
   ast_types.put("CLASS",BaleAstNodeType.CLASS);
   ast_types.put("ENUM",BaleAstNodeType.CLASS);
   ast_types.put("ENUMC",BaleAstNodeType.FIELD);
   ast_types.put("FIELD",BaleAstNodeType.FIELD);
   ast_types.put("VARIABLE",BaleAstNodeType.VARIABLE);
   ast_types.put("INITIALIZER",BaleAstNodeType.INITIALIZER);
   ast_types.put("ANNOT",BaleAstNodeType.ANNOTATION);
   ast_types.put("COMPUNIT",BaleAstNodeType.FILE);
   ast_types.put("METHOD",BaleAstNodeType.METHOD);
   ast_types.put("FUNCTION",BaleAstNodeType.METHOD);
   ast_types.put("MODULE", BaleAstNodeType.FILE);
   ast_types.put("IMPORT",BaleAstNodeType.IMPORT);
   ast_types.put("CALL",BaleAstNodeType.CALL);
   ast_types.put("CALLEXPR",BaleAstNodeType.CALL_EXPR);

   id_types = new HashMap<String,BaleAstIdType>();
   id_types.put("CALL",BaleAstIdType.CALL);
   id_types.put("CALLA",BaleAstIdType.CALL);
   id_types.put("CALLD",BaleAstIdType.CALL_DEPRECATED);
   id_types.put("CALLU",BaleAstIdType.CALL_UNDEF);
   id_types.put("CALLS",BaleAstIdType.CALL_STATIC);
   id_types.put("TYPE",BaleAstIdType.TYPE);
   id_types.put("FIELD",BaleAstIdType.FIELD);
   id_types.put("FIELDS",BaleAstIdType.FIELD_STATIC);
   id_types.put("FIELDC",BaleAstIdType.FIELDC);
   id_types.put("ENUMC",BaleAstIdType.ENUMC);
   id_types.put("METHODDECL",BaleAstIdType.METHOD_DECL);
   id_types.put("METHODDECLU",BaleAstIdType.METHOD_DECL);
   id_types.put("METHODDECLD",BaleAstIdType.METHOD_DECL);
   id_types.put("METHODDECLS",BaleAstIdType.METHOD_DECL);
   id_types.put("METHODDECLA",BaleAstIdType.METHOD_DECL);
   id_types.put("CLASSDECL",BaleAstIdType.CLASS_DECL);
   id_types.put("CLASSDECLL",BaleAstIdType.CLASS_DECL);
   id_types.put("CLASSDECLM",BaleAstIdType.CLASS_DECL_MEMBER);
   id_types.put("EXCEPTIONDECL",BaleAstIdType.EXCEPTION_DECL);
   id_types.put("FIELDDECL",BaleAstIdType.FIELD_DECL);
   id_types.put("PARAMDECL",BaleAstIdType.LOCAL_DECL);
   id_types.put("LOCALDECL",BaleAstIdType.LOCAL_DECL);
   id_types.put("UNDEF",BaleAstIdType.UNDEF);
   id_types.put("ANNOT",BaleAstIdType.ANNOT);
   id_types.put("BUILTIN",BaleAstIdType.BUILTIN) ;
   id_types.put("MODULE",BaleAstIdType.MODULE);

   // python-specific ids
   id_types.put("VARDECL",BaleAstIdType.FIELD_STATIC);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleAstNode(Element d,BaleDocumentIde doc)
{
   for_document = doc;
   parent_node = null;
   String nt = IvyXml.getAttrString(d,"NODE");
   if (nt == null) node_type = BaleAstNodeType.NONE;
   else node_type = ast_types.get(nt);
   if (node_type == null) {
      BoardLog.logE("BALE","Unknown AST node type " + nt + " from IDE");
      node_type = BaleAstNodeType.NONE;
    }
   String it = IvyXml.getAttrString(d,"TYPE");
   if (it == null) id_type = BaleAstIdType.NONE;
   else id_type = id_types.get(it);
   if (id_type == null) {
      BoardLog.logE("BALE","Unknown AST id type " + it + " from IDE");
      id_type = BaleAstIdType.NONE;
    }

   elide_priority = IvyXml.getAttrDouble(d,"PRIORITY");
   int start = IvyXml.getAttrInt(d,"ESTART");
   int len;
   if (start >= 0) len = IvyXml.getAttrInt(d,"ELENGTH");
   else {
      start = IvyXml.getAttrInt(d,"START");
      len = IvyXml.getAttrInt(d,"LENGTH");
    }
   start_position = doc.mapOffsetToJava(start);
   end_position = doc.mapOffsetToJava(start + len);
   is_error = IvyXml.getAttrBool(d,"ERROR");

   full_name = IvyXml.getAttrString(d,"FULLNAME");
   hint_data = IvyXml.getChild(d,"HINT");
   child_nodes = null;
   for (Element e : IvyXml.children(d,"ELIDE")) {
      if (child_nodes == null) child_nodes = new ArrayList<>(4);
      BaleAstNode nn = new BaleAstNode(e,doc);
      child_nodes.add(nn);
      nn.parent_node = this;
    }
}



BaleAstNode(List<BaleAstNode> nodes)
{
   BaleAstNode base = nodes.get(0);
   BaleAstNode last = nodes.get(nodes.size()-1);

   for_document = base.for_document;
   parent_node = null;
   node_type = BaleAstNodeType.SET;
   id_type = BaleAstIdType.NONE;
   elide_priority = 1.0;
   start_position = base.start_position;
   end_position = last.end_position;

   is_error = false;
   full_name = null;
   child_nodes = new ArrayList<>(nodes);
   for (BaleAstNode bn : nodes) bn.parent_node = this;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BaleAstNodeType getNodeType()			{ return node_type; }
BaleAstIdType getIdType()			{ return id_type; }
boolean getErrorFlag()				{ return is_error; }
int getStart()					{ return start_position; }
int getEnd()					{ return end_position; }
double getElidePriority()			{ return elide_priority; }
BaleAstNode getParent() 			{ return parent_node; }
String getFullName()				{ return full_name; }
Element getHintData()                           { return hint_data; }

boolean isLastChild(BaleAstNode n) {
   if (child_nodes == null) return false;
   int sz = child_nodes.size();
   return child_nodes.get(sz-1) == n;
}

int getLineLength()
{
   return getEndLine() - getStartLine();
}

int getStartLine()
{
   int ln = for_document.findLineNumber(start_position);
   return ln;
}

int getEndLine()
{
   int ln = for_document.findLineNumber(end_position);
   return ln;
}


/********************************************************************************/
/*										*/
/*	Finding methods 							*/
/*										*/
/********************************************************************************/

BaleAstNode getChild(int pos)
{
   if (pos < start_position || pos >= end_position) return null;

   if (child_nodes != null) {
      for (BaleAstNode c : child_nodes) {
	 BaleAstNode c1 = c.getChild(pos);
	 if (c1 != null) return c1;
       }
    }

   return this;
}



BaleAstNode getNode(int spos,int epos)
{
   if (spos < start_position || epos > end_position) return null;

   if (child_nodes != null) {
      for (BaleAstNode c : child_nodes) {
	 BaleAstNode c1 = c.getNode(spos,epos);
	 if (c1 != null) return c1;
       }
    }

   return this;
}




BaleAstNode getChildNode(int spos,int epos)
{
   if (spos < start_position || epos > end_position) return null;

   if (child_nodes != null) {
      for (BaleAstNode c : child_nodes) {
	 if (spos >= c.start_position && epos <= c.end_position) return c;
       }
    }

   return this;
}



int countImports()
{
   if (child_nodes == null) return 0;
   int ctr = 0;
   for (BaleAstNode c : child_nodes) {
     if (c.getNodeType() == BaleAstNodeType.IMPORT && !c.getErrorFlag()) ++ctr;
   }
   return ctr;
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   addToBuffer(buf,0);

   return buf.toString();
}


private void addToBuffer(StringBuffer buf,int indent)
{
   for (int i = 0; i < indent; ++i) buf.append("  ");
   buf.append("[" + getNodeType() + "," + getIdType() + "," + getErrorFlag());
   buf.append(" :: " + getStart() + ":" + getEnd());
   if (full_name != null) buf.append(" :: NAME=" + full_name);
   buf.append(" :: " + getElidePriority() + "]");
   buf.append("\n");
   if (child_nodes != null) {
      for (BaleAstNode cn : child_nodes) {
	 cn.addToBuffer(buf,indent+1);
       }
    }
}









}	// end of class BaleAstNode




/* end of BaleAstNode.java */
