/********************************************************************************/
/*										*/
/*		BanalPackageHierarchy.java					*/
/*										*/
/*	Compute package hierarchy						*/
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



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


class BanalPackageHierarchy extends BanalDefaultVisitor implements BanalConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	project_name;
private DependNode cur_package;
private Map<String,DependNode> node_map;
private int	cycle_counter;
private Set<BanalMethod> possible_methods;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BanalPackageHierarchy(BanalProjectManager pm, String proj)
{
   project_name = proj;
   node_map = new HashMap<String,DependNode>();
   cur_package = null;
   cycle_counter = 0;
   possible_methods = new HashSet<BanalMethod>();
}


BanalPackageHierarchy(Element xml)
{
   cycle_counter = 0;
   cur_package = null;
   possible_methods = new HashSet<BanalMethod>();
   
   node_map = new HashMap<String,DependNode>();
   for (Element ne : IvyXml.children(xml,"NODE")) {
      DependNode dn = new DependNode(ne);
      node_map.put(dn.getName(),dn);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods 								*/
/*										*/
/********************************************************************************/

Map<String,BanalHierarchyNode> getHierarchy()
{
   return new HashMap<String,BanalHierarchyNode>(node_map);
}



/********************************************************************************/
/*										*/
/*	Scanning methods							*/
/*										*/
/********************************************************************************/

@Override public boolean checkUseProject(String proj)
{
   if (project_name == null) return true;

   return project_name.equals(proj);
}


@Override public void visitClass(BanalClass bc,String sign,int acc)
{
   cur_package = findDepend(findPackage(bc));
}


@Override public void visitSuper(BanalClass bc,BanalClass sup,boolean iface)
{
   // should add all members of the supertype to the set of possible overrides here
   reference(findPackage(sup));
}


@Override public void visitClassField(BanalField bf,BanalClass typ,String gen,int acc,Object val)
{
   reference(findPackage(typ));
}


@Override public void visitInnerClass(BanalClass ocls,BanalClass icls,int acc)
{
   reference(findPackage(icls));
}


@Override public void visitClassMethod(BanalMethod bm,String sgn,int acc,BanalClass [] excs)
{
   if (Modifier.isPublic(acc) || Modifier.isProtected(acc)) possible_methods.add(bm);

   for (BanalClass ebc : excs) reference(findPackage(ebc));
   for (BanalClass abc : bm.getArgumentTypes()) reference(findPackage(abc));
   reference(findPackage(bm.getReturnType()));
}


@Override public void visitAlloc(BanalMethod bm,BanalClass alloc)
{
   reference(findPackage(alloc));
}

@Override public void visitRemoteTypeAccess(BanalMethod bm,BanalClass bc)
{
   reference(findPackage(bc));
}


@Override public void visitRemoteFieldAccess(BanalMethod bm,BanalField bf)
{
   reference(findPackage(bf.getOwnerClass()));
}


@Override public void visitCall(BanalMethod bm,BanalMethod called)
{
   reference(findPackage(called.getOwnerClass()));
   reference(findPackage(called.getReturnType()));
   // should we reference the argument types here?
}


@Override public void visitLocalVariable(BanalMethod bm,BanalClass typ,String sgn,boolean prm)
{
   reference(findPackage(typ));
}


@Override public void visitCatch(BanalMethod bm,BanalClass ctch)
{
   reference(findPackage(ctch));
}


@Override public void finish()
{
   doAnalysis();

   // Go through all possible_methods to find ones that do an override
   // where the method being overriden is in an inferior package (directly
   // or indirectly)
}




/********************************************************************************/
/*										*/
/*	Helper functions							*/
/*										*/
/********************************************************************************/

private String findPackage(BanalClass bc)
{
   String nm = bc.getInternalName();

   return findPackageType(nm);
}



private String findPackage(String nm)
{
   if (nm == null) return null;

   nm = nm.replace("/",".");
   if (nm.startsWith("[")) {
      return findPackageType(nm);
    }

   int idx = nm.lastIndexOf(".");
   if (idx < 0) return null;
   // if (!for_project.isProjectClass(nm)) return null;

   return nm.substring(0,idx);
}


private String findPackageType(String desc)
{
   if (desc == null) return null;
   if (desc.startsWith("L") && desc.endsWith(";")) {
      String nm = desc.substring(1,desc.length()-1);
      nm = nm.replace('/','.');
      return findPackage(nm);
    }
   else if (desc.startsWith("[")) {
      return findPackageType(desc.substring(1));
    }
   return null;
}



private DependNode findDepend(String nm)
{
   DependNode dn = node_map.get(nm);
   if (dn == null) {
      dn = new DependNode(nm);
      node_map.put(nm,dn);
    }
   return dn;
}



private void reference(String pkg)
{
   if (pkg == null || cur_package == null) return;
   DependNode dn = findDepend(pkg);
   if (dn == cur_package) return;
   cur_package.addDepend(dn);
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

private void doAnalysis()
{
   Set<DependNode> todo = new HashSet<DependNode>(node_map.values());
   List<DependNode> done = new ArrayList<DependNode>();
   Queue<DependNode> work = new LinkedList<DependNode>();
   cycle_counter = 0;

   for (DependNode dn : node_map.values()) {
      if (dn.getNumDepend() == 0) work.add(dn);
    }

   while (!todo.isEmpty()) {
      if (work.isEmpty()) {
	 work.addAll(removeCycle(todo));
       }
      while (!work.isEmpty()) {
	 DependNode nd = work.remove();
	 todo.remove(nd);
	 done.add(nd);
	 Collection<DependNode> next = nd.useNode();
	 if (next != null) work.addAll(next);
       }
    } 
}



private Collection<DependNode> removeCycle(Set<DependNode> nodes)
{
   Collection<DependNode> best = null;

   for (DependNode dn : nodes) {
      Collection<DependNode> cyc = findCycle(dn,nodes);
      if (cyc != null) {
	 if (best == null || cyc.size() < best.size()) best = cyc;
       }
    }

   Collection<DependNode> rslt = new HashSet<DependNode>();

   if (best == null) {
      System.err.println("RSTAT: Problem computing cycles");
    }
   else {
      int cct = ++cycle_counter;
      for (DependNode dn : best) rslt.addAll(dn.merge(best,cct));
    }

   return rslt;
}



private Collection<DependNode> findCycle(DependNode dn,Collection<DependNode> nodes)
{
   Set<DependNode> done = new HashSet<DependNode>();
   Set<DependNode> cycle = new HashSet<DependNode>();

   dn.cycleDfs(dn,done,cycle);

   if (cycle.isEmpty()) return null;

   for (DependNode on : nodes) {
      if (!cycle.contains(on)) {
	 if (!on.checkCycle(cycle)) return null;
       }
    }

   return cycle;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void output(IvyXmlWriter xw)
{
   xw.begin("HIERARCHY");
   for (DependNode dn : node_map.values()) {
      dn.output(xw);
    }
   // output all methods that do an override
   xw.end("HIERARCHY");
}



/********************************************************************************/
/*										*/
/*	Graph structure for dependcies						*/
/*										*/
/********************************************************************************/

private class DependNode implements BanalHierarchyNode {

   private String node_name;
   private Set<DependNode> depends_on;
   private int num_depend;
   private int node_level;
   private int cycle_id;

   DependNode(String nm) {
      node_name = nm;
      depends_on = new HashSet<DependNode>();
      node_level = 0;
      cycle_id = 0;
    }

   DependNode(Element xml) {
      node_name = IvyXml.getAttrString(xml,"PACKAGE");
      num_depend = 0;
      depends_on = null;
      node_level = IvyXml.getAttrInt(xml,"LEVEL");
      cycle_id = IvyXml.getAttrInt(xml,"CYCLE");
    }


   @Override public String getName()	{ return node_name; }
   @Override public int getLevel()	{ return node_level; }
   @Override public int getCycle()	{ return cycle_id; }
   
   int getNumDepend()			{ return num_depend; }

   void addDepend(DependNode n) {
      if (depends_on.add(n)) {
	 ++n.num_depend;
	 System.err.println("BANAL: " + getName() + " <== " + n.getName());
       }
    }

   Collection<DependNode> useNode() {
      Collection<DependNode> rslt = null;
      for (DependNode dn : depends_on) {
         dn.node_level = Math.max(dn.node_level,node_level+1);
         if (--dn.num_depend == 0) {
            if (rslt == null) rslt = new ArrayList<>();
            rslt.add(dn);
          }
       }
      return rslt;
    }

   Collection<DependNode> merge(Collection<DependNode> cyc,int cct) {
      Collection<DependNode> rslt = new ArrayList<DependNode>();
      cycle_id = cct;
      for (DependNode dn : cyc) {
         node_level = Math.max(node_level,dn.node_level);
         if (depends_on.remove(dn)) {
            if (--dn.num_depend == 0) rslt.add(dn);
          }
       }
      return rslt;
    }


   boolean cycleDfs(DependNode start,Set<DependNode> done,Set<DependNode> cycle) {
      done.add(this);
      boolean cyc = false;
      for (DependNode dn : depends_on) {
	 if (dn == start) {
	    cyc = true;
	  }
	 else if (!done.contains(dn)) {
	    cyc |= dn.cycleDfs(start,done,cycle);
	  }
       }
      if (cyc) cycle.add(this);
      return cyc;
    }


   boolean checkCycle(Set<DependNode> cycle) {
      for (DependNode dn : depends_on) {
	 if (cycle.contains(dn)) return false;
       }
      return true;
    }


   @Override public String toString() {
      return node_name;
    }

   void output(IvyXmlWriter xw) {
      xw.begin("NODE");
      xw.field("PACKAGE",getName());
      xw.field("LEVEL",getLevel());
      xw.field("CYCLE",cycle_id);
      xw.end("NODE");
    }

}	// end of inner class DependNode




}	// end of class BanalPackageHierarchy




/* end of BanalPackageHierarchy.java */

