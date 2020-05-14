/********************************************************************************/
/*										*/
/*		StringUtils.java						*/
/*										*/
/*	Python Bubbles Base string utilities					*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/09/2005
 */


package edu.brown.cs.bubbles.pybase.symbols;


import org.python.pydev.core.Tuple;
import org.python.pydev.core.cache.LRUCache;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public final class StringUtils {

/**
 * @author fabioz
 *
 */


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

public static final Object EMPTY = "";





/**
 * Formats a string, replacing %s with the arguments passed.
 *
 * %% is also changed to %.
 *
 * If % is followed by any other char, the % and the next char are ignored.
 *
 * @param str string to be formatted
 * @param args arguments passed
 * @return a string with the %s replaced by the arguments passed
 */
public static String format(final String str, Object... args)
{
   final int length = str.length();
   StringBuilder buffer = new StringBuilder(length+(16*args.length));
   int j = 0;
   int i = 0;

   int start = 0;

   for (; i < length; i++) {
      char c = str.charAt(i);
      if (c == '%') {
	 if(i + 1 < length){
	    if(i > start){
	       buffer.append(str.substring(start, i));
	     }
	    char nextC = str.charAt(i + 1);
		
	    switch (nextC) {
	       case 's':
		  buffer.append(args[j]);
		  j++;
		  break;
	       case '%':
		  buffer.append('%');
		  j++;
		  break;
	     }
	    i++;
	    start = i+1;
	  }
       }
    }

   if(i > start){
      buffer.append(str.substring(start, i));
    }
   return buffer.toString();
}




/**
 * Counts the number of %s in the string
 *
 * @param str the string to be analyzed
 * @return the number of %s in the string
 */
public static int countPercS(final String str)
{
   int j = 0;

   final int len = str.length();
   for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (c == '%' && i + 1 < len) {
	 char nextC = str.charAt(i + 1);
	 if (nextC == 's') {
	    j++;
	    i++;
	  }
       }
    }
   return j;
}




/**
 * Given a string remove all from the rightmost '.' onwards.
 *
 * E.g.: bbb.t would return bbb
 *
 * If it has no '.', returns the original string unchanged.
 */
public static String stripExtension(String input) {
   return stripFromRigthCharOnwards(input, '.');
}




public static int rFind(String input, char ch)
{
   int len = input.length();
   int st = 0;
   int off = 0;

   while ((st < len) && (input.charAt(off + len - 1) != ch)) {
      len--;
    }
   len--;
   return len;
}



private static String stripFromRigthCharOnwards(String input, char ch)
{
   int len = rFind(input, ch);
   if(len == -1){
      return input;
    }
   return input.substring(0, len);
}




public static String stripFromLastSlash(String input)
{
   return stripFromRigthCharOnwards(input, '/');
}




/**
 * Removes the occurrences of the passed char in the beggining of the string.
 */
public static String rightTrim(String input, char charToTrim)
{
   int len = input.length();
   int st = 0;
   int off = 0;

   while ((st < len) && (input.charAt(off + len - 1) == charToTrim)) {
      len--;
    }
   return input.substring(0, len);
}




/**
 * Removes the occurrences of the passed char in the start and end of the string.
 */
public static String leftAndRightTrim(String input, char charToTrim)
{
   return rightTrim(leftTrim(input, charToTrim), charToTrim);
}




/**
 * Removes the occurrences of the passed char in the end of the string.
 */
public static String leftTrim(String input, char charToTrim)
{
   int len = input.length();
   int off = 0;

   while ((off < len) && (input.charAt(off) == charToTrim)) {
      off++;
    }
   return input.substring(off, len);
}




/**
 * Changes all backward slashes (\) for forward slashes (/)
 *
 * @return the replaced string
 */
public static String replaceAllSlashes(String string) {
   int len = string.length();
   char c = 0;

   for (int i = 0; i < len; i++) {
      c = string.charAt(i);

      if (c == '\\') {
	 char[] ds = string.toCharArray();
	 ds[i] = '/';
	 for (int j = i; j < len; j++) {
	    if (ds[j] == '\\') {
	       ds[j] = '/';
	     }
	  }
	 return new String(ds);
       }

    }
   return string;
}



/**
 * Splits the given string in a list where each element is a line.
 *
 * @param string string to be split.
 * @return list of strings where each string is a line.
 *
 * @note the new line characters are also added to the returned string.
 *
 * IMPORTANT: The line returned will be a substring of the initial line, so, it's recommended that a copy
 * is created if it should be kept in memory (otherwise the full initial string will also be kept in memory).
 */

public static Iterable<String> iterLines(final String string)
{
   return new Iterable<String>() {
	
      @Override public Iterator<String> iterator() {
	 return new IterLines(string);
       }
    };

}






/**
 * Splits the passed string based on the toSplit string.
 */
public static List<String> split(final String string, final String toSplit)
{
   if(toSplit.length() == 1){
      return split(string, toSplit.charAt(0));
    }
   ArrayList<String> ret = new ArrayList<String>();
   if(toSplit.length() == 0){
      ret.add(string);
      return ret;
    }

   int len = string.length();

   int last = 0;

   char c = 0;

   for (int i = 0; i < len; i++) {
      c = string.charAt(i);
      if(c == toSplit.charAt(0) && matches(string, toSplit, i)){
	 if(last != i){
	    ret.add(string.substring(last, i));
	  }
	 last = i+toSplit.length();
	 i+= toSplit.length() -1;
       }
    }

   if(last < len){
      ret.add(string.substring(last, len));
    }

   return ret;
}



private static boolean matches(final String string, final String toSplit, int i)
{
   int length = string.length();
   int toSplitLen = toSplit.length();
   if(length-i >= toSplitLen){
      for(int j=0;j<toSplitLen;j++){
	 if(string.charAt(i+j) != toSplit.charAt(j)){
	    return false;
	  }
       }
      return true;
    }
   return false;
}




/**
 * Splits some string given some char (that char will not appear in the returned strings)
 * Empty strings are also never added.
 */
public static List<String> split(String string, char toSplit)
{
   ArrayList<String> ret = new ArrayList<String>();
   int len = string.length();

   int last = 0;

   char c = 0;

   for (int i = 0; i < len; i++) {
      c = string.charAt(i);
      if(c == toSplit){
	 if(last != i){
	    ret.add(string.substring(last, i));
	  }
	 while(c == toSplit && i < len-1){
	    i++;
	    c = string.charAt(i);
	  }
	 last = i;
       }
    }
   if(c != toSplit){
      if(last == 0 && len > 0){
	 ret.add(string); //it is equal to the original (no char to split)
	
       }
      else if(last < len){
	 ret.add(string.substring(last, len));
       }
    }
   return ret;
}





public static List<String> splitAndRemoveEmptyTrimmed(String string, char c)
{
   List<String> split = split(string, c);
   for(int i=split.size()-1;i>=0;i--){
      if(split.get(i).trim().length() == 0){
	 split.remove(i);
       }
    }
   return split;
}




/**
 * Splits some string given some char in 2 parts. If the separator is not found,
 * everything is put in the 1st part.
 */
public static Tuple<String, String> splitOnFirst(String fullRep, char toSplit)
{
   int i = fullRep.indexOf(toSplit);
   if(i != -1){
      return new Tuple<String, String>(
	 fullRep.substring(0, i),
	    fullRep.substring(i+1));
    }
   else{
      return new Tuple<String, String>(fullRep,"");
    }
}




/**
 * Splits some string given some char in 2 parts. If the separator is not found,
 * everything is put in the 1st part.
 */
public static Tuple<String, String> splitOnFirst(String fullRep, String toSplit)
{
   int i = fullRep.indexOf(toSplit);
   if(i != -1){
      return new Tuple<String, String>(
	 fullRep.substring(0, i),
	    fullRep.substring(i+toSplit.length()));
    }else{
	return new Tuple<String, String>(fullRep,"");
      }
}


/**
 * Splits the string as would string.split("\\."), but without yielding empty strings
 */
public static List<String> dotSplit(String string)
{
   return splitAndRemoveEmptyTrimmed(string, '.');
}



public static String join(String delimiter, Object ... splitted)
{
   String [] newSplitted = new String[splitted.length];
   for(int i=0;i<splitted.length;i++){
      Object s = splitted[i];
      if(s == null){
	 newSplitted[i] = "null";
       }else{
	   newSplitted[i] = s.toString();
	 }
    }
   return join(delimiter, newSplitted);
}




/**
 * Same as Python join: Go through all the paths in the string and join them with the passed delimiter.
 */
public static String join(String delimiter, String[] splitted)
{
   StringBuilder buf = new StringBuilder(splitted.length*100);
   boolean first = true;
   for (String string : splitted) {
      if(!first){
	 buf.append(delimiter);
       }else{
	   first = false;
	 }
	buf.append(string);
    }
   return buf.toString();
}








public static int count(String name, char c)
{
   int count=0;
   final int len = name.length();
   for(int i=0;i<len;i++){
      if(name.charAt(i) == c){
	 count++;
       }
    }
   return count;
}






private static final Object md5CacheLock = new Object();
private static final LRUCache<String, String> md5Cache = new LRUCache<String, String>(1000);

public static String md5(String str)
{
   synchronized (md5CacheLock) {
      String obj = md5Cache.getObj(str);
      if(obj != null){
	 return obj;
       }
      try {
	 byte[] bytes = str.getBytes("UTF-8");
	 MessageDigest md = MessageDigest.getInstance("MD5");
	 //MAX_RADIX because we'll generate the shorted string possible... (while still
	 //using only numbers 0-9 and letters a-z)
	 String ret = new BigInteger(1, md.digest(bytes)).toString(Character.MAX_RADIX).toLowerCase();
	 md5Cache.add(str, ret);
	 return ret;
       } catch (Exception e) {
	    throw new RuntimeException(e);
	  }
    }
}




private static final class IterLines implements Iterator<String> {
   private final String string;
   private final int len;
   private int i;
   private boolean calculatedNext;
   private boolean hasNext;
   private String next;

   private IterLines(String s) {
      this.string = s;
      this.len = s.length();
    }

   @Override public boolean hasNext() {
      if(!calculatedNext){
	 calculatedNext = true;
	 hasNext = calculateNext();
       }
      return hasNext;
    }

   private boolean calculateNext() {
      next = null;
      char c;
      int start = i;
	
      for (;i < len; i++) {
	 c = string.charAt(i);
	
	
	 if (c == '\r') {
	    if (i < len - 1 && string.charAt(i + 1) == '\n') {
	       i++;
	     }
	    i++;
	    next = string.substring(start, i);
	    return true;
	  }
	 if (c == '\n') {
	    i++;
	    next = string.substring(start, i);
	    return  true;
	  }
       }
      if (start != i) {
	 next = string.substring(start, i);
	 i++;
	 return true;
       }
      return false;
    }

   @Override public String next() {
      if(!hasNext()){
	 throw new NoSuchElementException();
       }
      String n = next;
      calculatedNext = false;
      next = null;
      return n;
    }

   @Override public void remove() {
      throw new UnsupportedOperationException();
    }
}


}
