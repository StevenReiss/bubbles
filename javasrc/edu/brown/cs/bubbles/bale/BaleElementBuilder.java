/********************************************************************************/
/*										*/
/*		BaleElementBuilder.java 					*/
/*										*/
/*	Bubble Annotated Language Editor ast integration into elements		*/
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

import java.util.ArrayList;
import java.util.List;



class BaleElementBuilder implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument for_document;
private BaleElement.Branch root_element;
private List<BaleElement> new_children;
private List<BaleElement> line_elements;
private BaleElement.LineNode cur_line;
private BaleAstNode cur_ast;
private BaleElement.Branch cur_parent;
private BaleTokenState token_state;
private int		num_blank;


private static boolean inside_elements = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleElementBuilder(BaleElement.Branch be,BaleAstNode ast)
{
   for_document = be.getBaleDocument();
   root_element = be;
   root_element.setAstNode(ast);
   new_children = new ArrayList<BaleElement>();
   cur_parent = root_element;
   cur_ast = ast;
   cur_line = null;
   token_state = BaleTokenState.NORMAL;
   line_elements = new ArrayList<BaleElement>();
   num_blank = 0;
}




/********************************************************************************/
/*										*/
/*	Methods to set the contents to be scanned				*/
/*										*/
/********************************************************************************/

void addChild(BaleElement be)
{
   if (be.isLeaf()) {
      line_elements.add(be);
      if (be.isEndOfLine()) {
	 addLine();
	 line_elements.clear();
       }
    }
   else {
      for (int i = 0; i < be.getElementCount(); ++i) {
	 BaleElement ce = be.getBaleElement(i);
	 if (ce.getStartOffset() >= 0)
	    addChild(ce);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to finish up scanning and update the element tree		*/
/*										*/
/********************************************************************************/

BaleElementEvent fixup()
{
   if (line_elements.size() > 0) addLine();

   BaleElementEvent ee = new BaleElementEvent(root_element,new_children);
   root_element.clear();
   for (BaleElement be : new_children) root_element.add(be);
   BoardLog.logD("BALE","ELEMENT FIXUP ROOT " + new_children.size());

   return ee;
}




/********************************************************************************/
/*										*/
/*	Methods to handle the actual element-ast construction			*/
/*										*/
/********************************************************************************/

private void addLine()
{
   BaleElement first = null;
   BaleElement last = null;
   BaleElement eol = null;
   boolean havecmmt = false;
   for (BaleElement ce : line_elements) {
      eol = ce;
      if (!ce.isEmpty() && !ce.isComment()) {
	 if (first == null) first = ce;
	 last = ce;
       }
      else if (ce.isComment()) havecmmt = true;
    }

   if (first != null && cur_parent.isComment()) cur_parent = cur_parent.getBaleParent();
   if (first == null && !cur_parent.isComment() && !havecmmt) ++num_blank;
   else if (first != null || cur_parent.isComment()) num_blank = 0;

   // fix outer parent to ensure it includes this line
   fixOuterParent(first,last,eol);

   // Create nodes for block comments
   BaleElement.Branch cpar = null;
   if (first == null) {
      if (!cur_parent.isComment() && (havecmmt || num_blank >= 2)) {
	 cpar = new BaleElement.BlockCommentNode(for_document,cur_parent);
	 addElementNode(cpar,num_blank);
	 cpar.setAstNode(cur_ast);
	 cpar.setStartTokenState(token_state);
	 cur_parent = cpar;
       }
    }

   if (cpar != null && cur_parent != cpar) {
      BoardLog.logE("BALE","UNUSED COMMENT");
    }

   // Add the current line
   BaleElement.Branch lastpar = cur_parent;
   cur_line = new BaleElement.LineNode(for_document,cur_parent);
   cur_line.setAstNode(cur_ast);
   cur_line.setStartTokenState(token_state);
   addElementNode(cur_line,0);
   cur_parent = cur_line;

   boolean haveindent = true;
   for (BaleElement ce : line_elements) {
      fixInnerParent(ce);
      ce = fixLeafElement(ce,haveindent);
      haveindent = false;
      addElementNode(ce,0);
      token_state = ce.getEndTokenState();
    }

   cur_line.setEndTokenState(token_state);
   cur_parent = lastpar;
   cur_line = null;
}





private void fixOuterParent(BaleElement fbe,BaleElement lbe,BaleElement eol)
{
   // fbe is the first non-empty, non-comment leaf for this line
   // lbe is the last non-empty, non-comment leaf for this line
   // eol is the last leaf for the line (the EOL token)

   // First check for end of top-level unit so comments go elsewhere
   if (lbe == null && eol != null) {
      BaleElement relt = cur_parent;
      while (relt != null && relt.getBubbleType() == BaleFragmentType.NONE) {
	 relt = relt.getBaleParent();
       }
      if (relt != null) {
	 if (eol.getDocumentEndOffset() >= relt.getAstNode().getEnd()) {
	    cur_parent = relt.getBaleParent();
	    cur_ast = cur_parent.getAstNode();
	    num_blank = 0;
	  }
       }
    }

   if (lbe == null) {			// nothing significant on the line
      if (cur_ast != null) {		// ensure comments aren't included in prior block
	 int epos = cur_ast.getEnd();
	 int tpos = eol.getDocumentEndOffset();
	 while (tpos > epos) {
	    if (cur_parent == root_element) break;
	    cur_parent = cur_parent.getBaleParent();
	    cur_ast = cur_parent.getAstNode();
	    num_blank = 0;
	    if (cur_ast == null) break;
	    epos = cur_ast.getEnd();
	  }
       }
      return;
    }

   // then remove any parent that doesn't include this AST node
   BaleAstNode lsn = getAstChild(lbe);
   while (lsn == null) {
      if (cur_parent == root_element) break;
      cur_parent = cur_parent.getBaleParent();
      cur_ast = cur_parent.getAstNode();
      lsn = getAstChild(lbe);
    }

   // next start any external elements that span multiple lines
   BaleAstNode fsn = getAstChild(fbe);
   while (fsn != null &&
	     (fsn != cur_ast || cur_parent.isDeclSet()) &&
	     (fsn == lsn || fsn.getLineLength() > 1)) {
      BaleElement.Branch nbe = createOuterBranch(fsn);
      if (nbe == null) break;
      addElementNode(nbe,0);
      nbe.setAstNode(fsn);
      cur_parent = nbe;
      cur_ast = fsn;
      fsn = getAstChild(fbe);
    }
}




private void fixInnerParent(BaleElement be)
{
   // first exit any internal parents that are done
   BaleAstNode sn = getAstChild(be);
   while (cur_parent != cur_line && sn == null) {
      cur_parent = cur_parent.getBaleParent();
      cur_ast = cur_parent.getAstNode();
      sn = getAstChild(be);
    }

   // now see if there are any internal elements that should be used
   while (sn != null && sn != cur_ast && sn.getLineLength() == 0) {
      BaleElement.Branch nbe = createInnerBranch(sn);
      if (nbe == null) break;
      addElementNode(nbe,0);
      nbe.setAstNode(sn);
      cur_parent = nbe;
      cur_ast = sn;
      sn = getAstChild(be);
    }
}




/********************************************************************************/
/*										*/
/*	Node creation methods based on ast node type				*/
/*										*/
/********************************************************************************/

private BaleElement.Branch createOuterBranch(BaleAstNode sn)
{
   switch (sn.getNodeType()) {
      case FILE :
	 return new BaleElement.CompilationUnitNode(for_document,cur_parent);
      case CLASS :
	 return new BaleElement.ClassNode(for_document,cur_parent);
      case METHOD :
	 return new BaleElement.MethodNode(for_document,cur_parent);
      case FIELD :
	 return new BaleElement.FieldNode(for_document,cur_parent);
      case ANNOTATION :
	 return new BaleElement.AnnotationNode(for_document,cur_parent);
      case STATEMENT :
	 return new BaleElement.SplitStatementNode(for_document,cur_parent);
      case EXPRESSION :
	 return new BaleElement.SplitExpressionNode(for_document,cur_parent);
      case BLOCK :
	 return new BaleElement.BlockNode(for_document,cur_parent);
      case SWITCH_BLOCK :
	 return new BaleElement.SwitchBlockNode(for_document,cur_parent);
      case INITIALIZER :
	 return new BaleElement.InitializerNode(for_document,cur_parent);
      case SET :
	 // return new BaleElement.DeclSet(for_document,cur_parent);
	 break;
      default:
	 break;
    }
   return null;
}




private BaleElement.Branch createInnerBranch(BaleAstNode sn)
{
   if (sn.getLineLength() != 0) return null;
   if (inside_elements) {
      switch (sn.getNodeType()) {
	 case STATEMENT :
	    return new BaleElement.StatementNode(for_document,cur_parent);
	 case EXPRESSION :
	    return new BaleElement.ExpressionNode(for_document,cur_parent);
	 case BLOCK :
	 case SWITCH_BLOCK :
	    return new BaleElement.InternalBlockNode(for_document,cur_parent);
	 default :
	    break;
       }
    }
   return null;
}



/********************************************************************************/
/*										*/
/*	Methods to fix up leaf nodes (identifiers mainly)			*/
/*										*/
/********************************************************************************/

private BaleElement fixLeafElement(BaleElement be,boolean first)
{
   // handle indents
   BaleAstNode sn = cur_ast.getChild(be.getDocumentStartOffset());

   if (be.isEmpty() && !be.isEndOfLine()) {
      if (first && be instanceof BaleElement.Space) {
	 be = new BaleElement.Indent(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
       }
      else if (!first && be instanceof BaleElement.Indent) {
	 be = new BaleElement.Space(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
       }
    }
   else if (be.isIdentifier()) {
      BaleAstIdType ityp = BaleAstIdType.NONE;
      if (sn != null) ityp = sn.getIdType();
      switch (ityp) {
	 case FIELD :
	    if (!(be instanceof BaleElement.FieldId)) {
	       be = new BaleElement.FieldId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case FIELD_STATIC :
	    if (!(be instanceof BaleElement.StaticFieldId)) {
	       be = new BaleElement.StaticFieldId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case CALL :
	    if (!(be instanceof BaleElement.CallId)) {
	       be = new BaleElement.CallId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case CALL_STATIC :
	    if (!(be instanceof BaleElement.StaticCallId)) {
	       be = new BaleElement.StaticCallId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case CALL_UNDEF :
	    if (!(be instanceof BaleElement.UndefCallId)) {
	       be = new BaleElement.UndefCallId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case CALL_DEPRECATED :
	    if (!(be instanceof BaleElement.DeprecatedCallId)) {
	       be = new BaleElement.DeprecatedCallId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case TYPE :
	    if (!(be instanceof BaleElement.TypeId)) {
	       be = new BaleElement.TypeId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case ENUMC :
	 case FIELDC :
	    if (!(be instanceof BaleElement.ConstId)) {
	       be = new BaleElement.ConstId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case CLASS_DECL :
	    if (!(be instanceof BaleElement.ClassDeclId)) {
	       be = new BaleElement.ClassDeclId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case CLASS_DECL_MEMBER :
	    if (!(be instanceof BaleElement.ClassDeclMemberId)) {
	       be = new BaleElement.ClassDeclMemberId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case METHOD_DECL :
	    if (!(be instanceof BaleElement.MethodDeclId)) {
	       be = new BaleElement.MethodDeclId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case EXCEPTION_DECL :
	 case LOCAL_DECL :
	    if (!(be instanceof BaleElement.LocalDeclId)) {
	       be = new BaleElement.LocalDeclId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case FIELD_DECL :
	    if (!(be instanceof BaleElement.FieldDeclId)) {
	       be = new BaleElement.FieldDeclId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case ANNOT :
	    if (!(be instanceof BaleElement.AnnotationId)) {
	       be = new BaleElement.AnnotationId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case UNDEF :
	    if (!(be instanceof BaleElement.UndefId)) {
	       be = new BaleElement.UndefId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 case BUILTIN :
	    if (!(be instanceof BaleElement.BuiltinId)) {
	       be = new BaleElement.BuiltinId(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
	 default :
            if (be.getClass() != BaleElement.Identifier.class) {
	       be = new BaleElement.Identifier(for_document,cur_parent,be.getStartOffset(),be.getEndOffset());
	     }
	    break;
       }
    }

   be.setAstNode(sn);
   return be;
}



/********************************************************************************/
/*										*/
/*	Tree management methods 						*/
/*										*/
/********************************************************************************/

private void addElementNode(BaleElement nbe,int prior)
{
   if (nbe.getBubbleType() != BaleFragmentType.NONE) {
      BaleElement.Branch bbe = (BaleElement.Branch) nbe;
      if (cur_parent == root_element) {
	 int j = -1;
	 for (int i = new_children.size() - 1; i >= 0; --i) {
	    BaleElement celt = new_children.get(i);
	    if (celt.isComment() || celt.isEmpty()) j = i;
	    else break;
	  }
	 if (j >= 0) {
	    while (j < new_children.size()) {
	       BaleElement celt = new_children.remove(j);
	       bbe.add(celt);
	     }
	  }
       }
      else {
	 int j = -1;
	 for (int i = cur_parent.getElementCount() - 1; i >= 0; --i) {
	    BaleElement celt = cur_parent.getBaleElement(i);
	    if (celt.isComment() || celt.isEmpty()) j = i;
	    else break;
	  }
	 if (j >= 0) {
	    while (j < cur_parent.getElementCount()) {
	       BaleElement celt = cur_parent.getBaleElement(j);
	       cur_parent.remove(j,j);
	       bbe.add(celt);
	     }
	  }
       }
    }
   else if (nbe.isComment() && prior > 0 && cur_parent != root_element) {
      BaleElement.Branch bbe = (BaleElement.Branch) nbe;
      int n = cur_parent.getElementCount();
      prior -= 1;			// is this what we want?
      int j = n-prior;
      for (int i = 0; i < prior; ++ i) {
	 BaleElement celt = cur_parent.getBaleElement(j);
	 cur_parent.remove(j,j);
	 bbe.add(celt);
      }
   }
   else if (nbe.isComment() && prior > 0 && cur_parent == root_element) {
      BaleElement.Branch bbe = (BaleElement.Branch) nbe;
      int n = new_children.size();
      int j = n-prior+1;
      for (int i = 0; i < prior-1; ++ i) {
	 BaleElement celt = new_children.get(j);
	 new_children.remove(j);
	 bbe.add(celt);
      }
   }

   if (cur_parent == root_element) {
      new_children.add(nbe);
    }
   else {
      cur_parent.add(nbe);
    }
}



private BaleAstNode getAstChild(BaleElement be)
{
   if (cur_ast == null) return null;

   return cur_ast.getChildNode(be.getDocumentStartOffset(),be.getDocumentEndOffset());
}




}	// end of class BaleElementBuilder




/* end of BaleElementBuilder.java */
