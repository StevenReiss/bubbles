/********************************************************************************/
/*										*/
/*		BanalPackageGraph.java						*/
/*										*/
/*	Bubbles ANALysis package visitor to create a package graph		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.bubbles.org.objectweb.asm.Opcodes;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


class BanalPackageGraph extends BanalDefaultVisitor implements BanalConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String			project_name;
private String			package_name;
private Map<String,ClassData>	class_nodes;
private Map<String,MethodData>	method_nodes;
private AtomicInteger		id_counter;
private boolean 		same_class;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BanalPackageGraph(String proj,String pkg,boolean usemethods,boolean sameclass)
{
   project_name = proj;
   package_name = pkg;
   class_nodes = new HashMap<String,ClassData>();
   id_counter = new AtomicInteger();

   if (usemethods) method_nodes = new HashMap<String,MethodData>();
   else method_nodes = null;
   same_class = sameclass;
}


BanalPackageGraph(Element xml)
{
   project_name = IvyXml.getAttrString(xml,"PROJECT");
   package_name = IvyXml.getAttrString(xml,"PACKAGE");
   class_nodes = new HashMap<String,ClassData>();
   if (IvyXml.getAttrBool(xml,"METHODS"))
      method_nodes = new HashMap<String,MethodData>();
   else
      method_nodes = null;

   HashMap<String,NodeData> idmap = new HashMap<String,NodeData>();

   for (Element e : IvyXml.children(xml,"NODE")) {
      String typ = IvyXml.getAttrString(e,"TYPE");
      String nm = IvyXml.getAttrString(e,"NAME");
      String id = IvyXml.getAttrString(e,"ID");
      int mod = IvyXml.getAttrInt(e,"MOD");
      if (typ.equals("CLASS")) {
	 ClassData cd = new ClassData(nm);
	 class_nodes.put(nm,cd);
	 cd.setAccess(mod);
	 idmap.put(id,cd);
       }
      else if (typ.equals("METHOD")) {
	 MethodData md = new MethodData(nm);
	 method_nodes.put(nm,md);
	 md.setAccess(mod);
	 idmap.put(id,md);
       }
    }

   for (Element e : IvyXml.children(xml,"NODE")) {
      String id = IvyXml.getAttrString(e,"ID");
      NodeData nd1 = idmap.get(id);
      if (nd1 == null) continue;
      for (Element le : IvyXml.children(e,"LINK")) {
	 String id2 = IvyXml.getAttrString(le,"TOID");
	 NodeData nd2 = idmap.get(id2);
	 if (nd2 == null) continue;
	 LinkData ld = nd1.createLinkTo(nd2);
	 if (ld == null) continue;
	 for (PackageRelationType rt : PackageRelationType.values()) {
	    int ct = IvyXml.getAttrInt(le,rt.toString());
	    if (ct >= 0) ld.addRelation(rt,ct);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Collection<BanalPackageClass> getClassNodes()
{
   return new ArrayList<BanalPackageClass>(class_nodes.values());
}



Collection<BanalPackageNode> getAllNodes()
{
   List<BanalPackageNode> rslt = new ArrayList<BanalPackageNode>();
   rslt.addAll(class_nodes.values());
   if (method_nodes != null) rslt.addAll(method_nodes.values());

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Visitors to handle start/stop/checking relevance			*/
/*										*/
/********************************************************************************/

@Override public boolean checkUseProject(String proj)
{
   if (project_name == null) return true;

   return project_name.equals(proj);
}




@Override public boolean checkUseClass(String cls)
{
   if (package_name == null) return true;

   if (package_name.length() == 0) {
      if (!cls.contains(".")) return true;
    }
   else {
      if (!cls.startsWith(package_name)) return false;
      int ln = package_name.length();
      if (ln == cls.length() || cls.charAt(ln) == '.') return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Basic class data							*/
/*										*/
/********************************************************************************/

@Override public void visitClass(BanalClass bc,String sign,int access)
{
   ClassData cd = findClass(bc);

   cd.setAccess(access);
}



private ClassData findClass(BanalClass bc)
{
   String k = bc.getInternalName();

   ClassData cd = class_nodes.get(k);
   if (cd == null) {
      cd = new ClassData(bc.getJavaName());
      class_nodes.put(k,cd);
    }

   return cd;
}


private MethodData findMethod(BanalMethod bm)
{
   if (method_nodes == null) return null;

   String k = bm.getFullName();
   MethodData md = method_nodes.get(k);
   if (md == null) {
      md = new MethodData(k);
      method_nodes.put(k,md);
    }

   return md;
}



private boolean isClassRelevant(BanalClass bc)
{
   String nm = bc.getJavaName();

   return checkUseClass(nm);
}



private LinkData findLink(BanalClass frm,BanalClass to)
{
   ClassData fc = findClass(frm);
   ClassData tc = findClass(to);

   LinkData ld = fc.createLinkTo(tc);

   return ld;
}


private LinkData findLink(BanalMethod frm,BanalMethod to)
{
   MethodData fm = findMethod(frm);
   MethodData tm = findMethod(to);
   if (fm == null || tm == null) {
      return findLink(frm.getOwnerClass(),to.getOwnerClass());
    }

   LinkData ld = fm.createLinkTo(tm);

   return ld;
}


private LinkData findLink(BanalMethod frm,BanalClass to)
{
   MethodData fm = findMethod(frm);
   if (fm == null) {
      return findLink(frm.getOwnerClass(),to);
    }
   ClassData tc = findClass(to);

   LinkData ld = fm.createLinkTo(tc);

   return ld;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("PACKAGE_GRAPH");
   xw.field("PROJECT",project_name);
   if (package_name != null) xw.field("PACKAGE",package_name);
   for (ClassData cd : class_nodes.values()) {
      cd.outputXml(xw);
    }
   xw.end("PACKAGE_GRAPH");
}




/********************************************************************************/
/*										*/
/*	Hierarchy visiting methods						*/
/*										*/
/********************************************************************************/

@Override public void visitSuper(BanalClass cls,BanalClass sup,boolean isiface)
{
   ClassData cd = findClass(cls);
   LinkData ld = findLink(cls,sup);

   PackageRelationType rtyp = PackageRelationType.SUPERCLASS;
   if (isiface) rtyp = PackageRelationType.IMPLEMENTS;
   if (cd.isInterface()) rtyp = PackageRelationType.EXTENDS;

   ld.addRelation(rtyp);
}


@Override public void visitInnerClass(BanalClass cls,BanalClass icls,int acc)
{
   LinkData ld = findLink(cls,icls);
   if (ld == null) return;
   PackageRelationType rtyp = PackageRelationType.INNERCLASS;

   ld.addRelation(rtyp);
}



/********************************************************************************/
/*										*/
/*	Method and field handling						*/
/*										*/
/********************************************************************************/

@Override public void visitClassMethod(BanalMethod bm,String gen,int acc,BanalClass [] excepts)
{
   MethodData md = findMethod(bm);
   if (md == null) return;

   md.setAccess(acc);

   LinkData ld = findLink(bm,bm.getOwnerClass());
   ld.addRelation(PackageRelationType.CLASSMETHOD);
}



@Override public void visitClassField(BanalField bf,BanalClass bc,String gen,int acc,Object val)
{ }



/********************************************************************************/
/*										*/
/*	Access visiting methods 						*/
/*										*/
/********************************************************************************/

@Override public void visitRemoteFieldAccess(BanalMethod bm,BanalField bf)
{
   BanalClass frm = bm.getOwnerClass();
   BanalClass to = bf.getOwnerClass();
   if (frm == to) return;
   if (!isClassRelevant(to)) return;

   LinkData ld = findLink(bm,to);
   if (ld != null) ld.addRelation(PackageRelationType.ACCESSES);
}



@Override public void visitRemoteTypeAccess(BanalMethod bm,BanalClass bc)
{
   BanalClass frm = bm.getOwnerClass();
   if (!isClassRelevant(bc)) return;
   if (frm == bc) return;

   LinkData ld = findLink(bm,bc);
   if (ld != null) ld.addRelation(PackageRelationType.FIELD);
}



@Override public void visitLocalVariable(BanalMethod bm, BanalClass bc,
      String sgn,boolean prm)
{
   BanalClass frm = bm.getOwnerClass();
   if (!isClassRelevant(bc)) return;
   if (frm == bc) return;

   LinkData ld = findLink(bm,bc);
   if (ld != null) ld.addRelation(PackageRelationType.LOCAL);
}



@Override public void visitCall(BanalMethod frm,BanalMethod cld)
{
   BanalClass fc = frm.getOwnerClass();
   BanalClass tc = cld.getOwnerClass();
   if (!isClassRelevant(tc) || (tc == fc && !same_class)) return;

   LinkData ld = findLink(frm,cld);
   if (ld != null) ld.addRelation(PackageRelationType.CALLS);
}



@Override public void visitAlloc(BanalMethod bm,BanalClass allocd)
{
   BanalClass fc = bm.getOwnerClass();
   if (!isClassRelevant(allocd) || (fc == allocd && !same_class)) return;

   LinkData ld = findLink(bm,allocd);
   if (ld != null) ld.addRelation(PackageRelationType.ALLOCATES);
}



@Override public void visitCatch(BanalMethod bm,BanalClass exc)
{
   BanalClass fc = bm.getOwnerClass();
   if (!isClassRelevant(exc) || (fc == exc && !same_class)) return;

   LinkData ld = findLink(bm,exc);
   if (ld != null) ld.addRelation(PackageRelationType.CATCHES);
}




/********************************************************************************/
/*										*/
/*	Node information							*/
/*										*/
/********************************************************************************/

private abstract class NodeData implements BanalPackageNode {

   private String node_name;
   private int node_id;
   private Map<NodeData,LinkData> out_links;
   private Map<NodeData,LinkData> in_links;
   private int class_access;
   protected Set<ClassType> class_types;

   NodeData(String nm) {
      node_name = nm;
      out_links = new HashMap<>();
      in_links = new HashMap<>();
      class_access = -1;
      class_types = null;
      if (id_counter != null) node_id = id_counter.incrementAndGet();
    }

   @Override public String getName()		{ return node_name; }
   int getId()					{ return node_id; }
   @Override public int getModifiers()		{ return class_access; }
   @Override public Collection<BanalPackageLink> getInLinks() {
      return new ArrayList<BanalPackageLink>(in_links.values());
    }
   @Override public Collection<BanalPackageLink> getOutLinks() {
      return new ArrayList<BanalPackageLink>(out_links.values());
    }

   @Override public String getProjectName()	{ return project_name; }
   @Override public String getMethodName()	{ return null; }
   @Override public String getClassName()	{ return null; }
   @Override public String getPackageName() {
      String nm = getClassName();
      int idx = nm.lastIndexOf(".");
      if (idx < 0) return null;
      return nm.substring(0,idx);
    }

   protected Set<ClassType> getBasicTypes() {
      Set<ClassType> rslt = EnumSet.noneOf(ClassType.class);

      int mod = class_access;
      if (mod == -1) mod = Modifier.PUBLIC;

      if ((mod & Opcodes.ACC_PRIVATE) != 0) rslt.add(ClassType.PRIVATE);
      else if ((mod & Opcodes.ACC_PROTECTED) != 0) rslt.add(ClassType.PROTECTED);
      else if ((mod & Opcodes.ACC_PUBLIC) != 0) rslt.add(ClassType.PUBLIC);
      else rslt.add(ClassType.PACKAGE_PROTECTED);

      if ((mod & Opcodes.ACC_STATIC) != 0) rslt.add(ClassType.STATIC);
      if ((mod & Opcodes.ACC_ABSTRACT) != 0) rslt.add(ClassType.ABSTRACT);
      if ((mod & Opcodes.ACC_FINAL) != 0) rslt.add(ClassType.FINAL);

      return rslt;
    }

   void setAccess(int acc) {
      class_access = acc;
      class_types = null;
    }
   boolean isInterface() {
      if (class_access == -1) return false;
      return Modifier.isInterface(class_access);
    }

   LinkData createLinkTo(NodeData cd) {
      if (cd == this || cd == null) return null;
      LinkData ld = out_links.get(cd);
      if (ld == null) {
	 ld = new LinkData(this,cd);
	 out_links.put(cd,ld);
	 cd.in_links.put(this,ld);
       }
      return ld;
    }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("NODE");
      xw.field("NAME",node_name);
      xw.field("ID",node_id);
      if (getMethodName() != null) xw.field("TYPE","METHOD");
      else if (getClassName() != null) xw.field("TYPE","CLASS");

      if (class_access != -1) xw.field("MOD",class_access);
      for (LinkData ld : out_links.values()) {
	 ld.outputXml(xw);
       }
      xw.end("NODE");
    }

   @Override public String toString() {
      return "[" + node_name + "]";
    }

   protected boolean isThrowable() {
      if (node_name.equals("java.lang.Throwable")) return true;
      if (node_name.equals("java.lang.Error")) return true;
      if (node_name.equals("java.lang.Exception")) return true;
      for (LinkData ld : out_links.values()) {
	 if (ld.getTypes().containsKey(PackageRelationType.SUPERCLASS)) {
	    ClassData cd =  (ClassData) ld.getToNode();
	    return cd.isThrowable();
	  }
       }
      return false;
    }

}	// end of inner class ClassData



private class ClassData extends NodeData implements BanalPackageClass {

   ClassData(String nm) {
      super(nm);
    }

   @Override public String getClassName()		{ return getName(); }

   @Override public Set<ClassType> getTypes() {
      if (class_types != null) return class_types;

      class_types = getBasicTypes();

      int mod = getModifiers();
      if ((mod & Opcodes.ACC_ENUM) != 0) class_types.add(ClassType.ENUM);
      else if ((mod & Opcodes.ACC_INTERFACE) != 0) class_types.add(ClassType.INTERFACE);
      else if ((mod & Opcodes.ACC_ANNOTATION) != 0) class_types.add(ClassType.ANNOTATION);
      else class_types.add(ClassType.CLASS);

      String s = getName();
      int idx = s.indexOf("<");
      if (idx >= 0) s = s.substring(0,idx);
      idx = s.indexOf("$");
      if (idx >= 0) class_types.add(ClassType.INNER);
      if (isThrowable()) class_types.add(ClassType.THROWABLE);
      return class_types;
    }

}	// end of inner class ClassData




private class MethodData extends NodeData implements BanalPackageMethod {

   MethodData(String nm) {
      super(nm);
    }

   @Override public Set<ClassType> getTypes() {
      if (class_types != null) return class_types;
      class_types = getBasicTypes();
      class_types.add(ClassType.METHOD);
      return class_types;
    }

   @Override public String getMethodName() {
      return getName();
    }

   @Override public String getClassName() {
      String nm = getName();
      int idx1 = nm.indexOf("(");
      int idx2 = (idx1 < 0 ? nm.lastIndexOf(".") : nm.lastIndexOf(".",idx1));
      return nm.substring(0,idx2);
    }

}




/********************************************************************************/
/*										*/
/*	Link information							*/
/*										*/
/********************************************************************************/

private static class LinkData implements BanalPackageLink {

   private NodeData from_class;
   private NodeData to_class;
   private Map<PackageRelationType,Integer> type_count;

   LinkData(NodeData fn,NodeData tn) {
      from_class = fn;
      to_class = tn;
      type_count = new EnumMap<PackageRelationType,Integer>(PackageRelationType.class);
    }

   @Override public BanalPackageNode getFromNode()		{ return from_class; }
   @Override public BanalPackageNode getToNode()		{ return to_class; }
   @Override public Map<PackageRelationType,Integer> getTypes() { return type_count; }

   void addRelation(PackageRelationType rt) {
      Integer id = type_count.get(rt);
      int idv = 0;
      if (id != null) idv = id;
      type_count.put(rt,idv+1);
    }

   void addRelation(PackageRelationType rt,int ct) {
      if (ct <= 0) return;
      Integer id = type_count.get(rt);
      int idv = ct;
      if (id != null) idv += id;
      type_count.put(rt,idv);
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("LINK");
      xw.field("FROM",from_class.getName());
      xw.field("FROMID",from_class.getId());
      xw.field("TO",to_class.getName());
      xw.field("TOID",to_class.getId());
      for (Map.Entry<PackageRelationType,Integer> ent : type_count.entrySet()) {
	 PackageRelationType rt = ent.getKey();
	 int ct = ent.getValue();
	 xw.field(rt.toString(),ct);
       }
      xw.end("LINK");
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<");
      buf.append(from_class.getName());
      buf.append("--(");
      int ctr = 0;
      for (Map.Entry<PackageRelationType,Integer> ent : type_count.entrySet()) {
	 if (ent.getValue() > 0) {
	    if (ctr++ > 0) buf.append(",");
	    buf.append(ent.getKey());
	  }
       }
      buf.append(")-->");
      buf.append(to_class.getName());
      buf.append(">");
      return buf.toString();
    }

}	// end of inner class LinkData




}	// end of class BanalPackageGraph




/* end of BanalPackageGraph.java */
