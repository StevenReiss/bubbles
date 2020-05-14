/********************************************************************************/
/*										*/
/*		PybaseFileSystem.java						*/
/*										*/
/*	Python Bubbles Base class for interacting with the file system		*/
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
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.StringUtils;

import edu.brown.cs.ivy.file.IvyFile;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;


/**
 * @author Fabio Zadrozny
 */

public class PybaseFileSystem implements PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/
/**
 * Regular expression for finding the encoding in a python file.
 */
private static final Pattern ENCODING_PATTERN = Pattern.compile("coding[:=][\\s]*([-\\w.]+)");


/**
 * Characters that files in the filesystem cannot have.
 */
public static char[]  INVALID_FILESYSTEM_CHARS = {
   '!', '@', '#', '$', '%', '^', '&',
   '*', '(', ')', '[', ']', '{', '}', '=', '+', '.', ' ', '`', '~', '\'', '"', ',',
   ';'
};


/**
 * This is usually what's on disk
 */
public static String BOM_UTF8	 = new String(new char[] { 0xEF, 0xBB, 0xBF });



/********************************************************************************/
/*										*/
/*	Methods to get the contents of a file					*/
/*										*/
/********************************************************************************/

/**
 * @param file the file we want to read
 * @return the contents of the file as a string
 */
public static String getFileContents(File file)
{
   return (String) getFileContentsCustom(file, String.class);
}

/**
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
public static Object getFileContentsCustom(File file,Class<? extends Object> returnType)
{
   FileInputStream stream = null;
   try {
      stream = new FileInputStream(file);
      return getStreamContents(stream, null, returnType);
    }
   catch (Exception e) {
      throw new RuntimeException(e);
    }
   finally {
      try {
	 if (stream != null) stream.close();
       }
      catch (Exception e) {
	 PybaseMain.logE("Error reading file",e);
       }
    }
}

/**
 * Get the contents from a given stream.
 * @param returnType the class that specifies the return type of this method.
 * If null, it'll return in the fastest possible way available.
 * Valid options are:
 *	String.class
 *	IDocument.class
 *	StringBuilder.class
 *
 */
private static Object getStreamContents(InputStream contentStream,String encoding,
					   Class<? extends Object> returnType)
	throws IOException
{
   Reader in = null;
   try {
      final int DEFAULT_FILE_SIZE = 15 * 1024;

      // discover how to actually read the passed input stream.
      if (encoding == null) {
	 in = new BufferedReader(new InputStreamReader(contentStream),DEFAULT_FILE_SIZE);
       }
      else {
	 try {
	    in = new BufferedReader(new InputStreamReader(contentStream,encoding),
				       DEFAULT_FILE_SIZE);
	  }
	 catch (UnsupportedEncodingException e) {
	    PybaseMain.logE("Error reading stream",e);
	    // keep going without the encoding
	    in = new BufferedReader(new InputStreamReader(contentStream),
				       DEFAULT_FILE_SIZE);
	  }
       }

      // fill a buffer with the contents
      StringBuilder buffer = new StringBuilder(DEFAULT_FILE_SIZE);
      char[] readBuffer = new char[2048];
      int n = in.read(readBuffer);
      while (n > 0) {
	 buffer.append(readBuffer, 0, n);
	 n = in.read(readBuffer);
       }

      // return it in the way specified by the user
      if (returnType == null || returnType == StringBuilder.class) {
	 return buffer;
       }
      else if (returnType == IDocument.class) {
	 Document doc = new Document(buffer.toString());
	 return doc;
       }
      else if (returnType == String.class) {
	 return buffer.toString();
       }
      else {
	 throw new RuntimeException("Don't know how to handle return type: " + returnType);
       }
    }
   finally {
      try {
	 if (in != null) in.close();
       }
      catch (Exception e) { }
    }
}





/********************************************************************************/
/*										*/
/*	Methods to write to a file						*/
/*										*/
/********************************************************************************/

/**
 * Appends the contents of the passed string to the given file.
 */
public static void appendStrToFile(String str,String file)
{
   try {
      FileOutputStream stream = new FileOutputStream(file,true);
      try {
	 stream.write(str.getBytes());
       }
      finally {
	 stream.close();
       }
    }
   catch (FileNotFoundException e) {
      PybaseMain.logE("Error appending to file",e);
    }
   catch (IOException e) {
      PybaseMain.logE("Error appending to file",e);
    }
}

/**
      * Writes the contents of the passed string to the given file.
      */
public static void writeStrToFile(String str,String file)
{
   writeStrToFile(str, new File(file));
}


/**
 * Writes the contents of the passed string to the given file.
 */
public static void writeStrToFile(String str,File file)
{
   try {
      FileOutputStream stream = new FileOutputStream(file);
      try {
	 stream.write(str.getBytes());
       }
      finally {
	 stream.close();
       }
    }
   catch (FileNotFoundException e) {
      PybaseMain.logE("Error writing to file",e);
    }
   catch (IOException e) {
      PybaseMain.logE("Error writing to file",e);
    }
}




/********************************************************************************/
/*										*/
/*	Path methods								*/
/*										*/
/********************************************************************************/

/**
 * Get the absolute path in the filesystem for the given file.
 *
 * @param f the file we're interested in
 *
 * @return the absolute (canonical) path to the file
 */
public static String getFileAbsolutePath(String f)
{
   return getFileAbsolutePath(new File(f));
}

/**
 * @see #getFileAbsolutePath(String)
 */
public static String getFileAbsolutePath(File f)
{
   try {
      return f.getCanonicalPath();
    }
   catch (IOException e) {
      return f.getAbsolutePath();
    }
}




/********************************************************************************/
/*										*/
/*	File reading methods							*/
/*										*/
/********************************************************************************/

public static IDocument getDocFromFile(java.io.File f) throws IOException
{
   return getDocFromFile(f, true);
}

/**
 * @return a string with the contents from a path within a zip file.
 */

public static String getStringFromZip(File f,String pathInZip) throws Exception
{
   return (String) getCustomReturnFromZip(f, pathInZip, String.class);
}

/**
 * @return a document with the contents from a path within a zip file.
 */

public static IDocument getDocFromZip(File f,String pathInZip) throws Exception
{
   return (IDocument) getCustomReturnFromZip(f, pathInZip, IDocument.class);
}

/**
 * @param f the zip file that should be opened
 * @param pathInZip the path within the zip file that should be gotten
 * @param returnType the class that specifies the return type of this method.
 * If null, it'll return in the fastest possible way available.
 * Valid options are:
 *	String.class
 *	IDocument.class
 *	StringBuilder.class
 *
 * @return an object with the contents from a path within a zip file, having the return type
 * of the object specified by the parameter returnType.
 */

public static Object getCustomReturnFromZip(File f,String pathInZip,
					       Class<? extends Object> returnType)
	throws Exception
{

   ZipFile zipFile = new ZipFile(f,ZipFile.OPEN_READ);
   try {
      InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(pathInZip));
      try {
	 return getStreamContents(inputStream, null, returnType);
       }
      finally {
	 inputStream.close();
       }
    }
   finally {
      zipFile.close();
    }
}


/**
 * @return a string with the contents of the passed file
 */
public static String getStringFromFile(java.io.File f,boolean loadIfNotInWorkspace)
throws IOException
{
   return (String) getCustomReturnFromFile(f, loadIfNotInWorkspace, String.class);
}

/**
 * @return the document given its 'filesystem' file
 */
public static IDocument getDocFromFile(java.io.File f,boolean loadIfNotInWorkspace)
	throws IOException
{
   return (IDocument) getCustomReturnFromFile(f, loadIfNotInWorkspace, IDocument.class);
}

/**
 * @param f the file from where we want to get the contents
 * @param returnType the class that specifies the return type of this method.
 * If null, it'll return in the fastest possible way available.
 * Valid options are:
 *	String.class
 *	IDocument.class
 *	StringBuilder.class
 *
 *
 * @return an object with the contents from the file, having the return type
 * of the object specified by the parameter returnType.
 */
public static Object getCustomReturnFromFile(java.io.File f,boolean loadIfNotInWorkspace,
						Class<? extends Object> returnType) throws IOException
{
   IFileData fd = PybaseFileManager.getFileManager().getFileData(f);
   IDocument doc = (fd == null ? null : fd.getDocument());

   if (doc != null) {
      if (returnType == null || returnType == IDocument.class) {
	 return doc;

       }
      else if (returnType == String.class) {
	 return doc.get();

       }
      else if (returnType == StringBuilder.class) {
	 return new StringBuilder(doc.get());

       }
      else {
	 throw new RuntimeException("Don't know how to treat requested return type: "
				       + returnType);
       }
    }

   if (doc == null && loadIfNotInWorkspace) {
      FileInputStream stream = new FileInputStream(f);
      try {
	 String encoding = getPythonFileEncoding(f);
	 return getStreamContents(stream, encoding, returnType);
       }
      finally {
	 try {
	    if (stream != null) stream.close();
	  }
	 catch (Exception e) {
	    PybaseMain.logE("Error reading from file",e);
	  }
       }
    }
   return doc;
}



/**
 * The encoding declared in the document is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
 */
public static String getPythonFileEncoding(IDocument doc,String fileLocation)
	throws IllegalCharsetNameException
{
   Reader inputStreamReader = new StringReader(doc.get());
   return getPythonFileEncoding(inputStreamReader, fileLocation);
}


/**
 * The encoding declared in the file is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
 */
public static String getPythonFileEncoding(File f) throws IllegalCharsetNameException
{
   try {
      final FileInputStream fileInputStream = new FileInputStream(f);
      try {
	 Reader inputStreamReader = new InputStreamReader(new BufferedInputStream(
							     fileInputStream));
	 String pythonFileEncoding = getPythonFileEncoding(inputStreamReader,
							      f.getAbsolutePath());
	 return pythonFileEncoding;
       }
      finally {
	 // NOTE: the reader will be closed at 'getPythonFileEncoding'.
	 try {
	    fileInputStream.close();
	  }
	 catch (Exception e) {
	    PybaseMain.logE("Error getin python file encoding",e);
	  }
       }
    }
   catch (FileNotFoundException e) {
      return null;
    }
}



/**
 * When we convert a string from the disk to a java string, if it had an UTF-8 BOM, it'll have that BOM converted
 * to this BOM. See: org.python.pydev.parser.PyParser27Test.testBom()
 */
public static String BOM_UNICODE = new String(new char[] { 0xFEFF });

/**
 * The encoding declared in the reader is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
 * -- may return null
 *
 * Will close the reader.
 * @param fileLocation the file we want to get the encoding from (just passed for giving a better message
 * if it fails -- may be null).
 */
public static String getPythonFileEncoding(Reader inputStreamReader,String fileLocation)
	throws IllegalCharsetNameException
{
   String ret = null;
   BufferedReader reader = new BufferedReader(inputStreamReader);
   try {
      String lEnc = null;

      // pep defines that coding must be at 1st or second line:
      // http://www.python.org/doc/peps/pep-0263/
      String l1 = reader.readLine();
      if (l1 != null) {
	 // Special case -- determined from the python docs:
	 // http://docs.python.org/reference/lexical_analysis.html#encoding-declarations
	 // We can return promptly in this case as utf-8 should be always valid.
	 if (l1.startsWith(BOM_UTF8)) {
	    return "utf-8";
	  }

	 if (l1.indexOf("coding") != -1) {
	    lEnc = l1;
	  }
       }

      if (lEnc == null) {
	 String l2 = reader.readLine();

	 // encoding must be specified in first or second line...
	 if (l2 != null && l2.indexOf("coding") != -1) {
	    lEnc = l2;
	  }
	 else {
	    ret = null;
	  }
       }

      if (lEnc != null) {
	 lEnc = lEnc.trim();
	 if (lEnc.length() == 0) {
	    ret = null;
	  }
	 else if (lEnc.charAt(0) == '#') { // it must be a comment line
	    Matcher matcher = ENCODING_PATTERN.matcher(lEnc);
	    if (matcher.find()) {
	       ret = matcher.group(1).trim();
	     }
	  }
       }
    }
   catch (IOException e) {
      PybaseMain.logE("I/O Error getting python file encoding",e);
    }
   finally {
      try {
	 reader.close();
       }
      catch (IOException e1) {}
    }
   ret = getValidEncoding(ret, fileLocation);
   return ret;
}


/**
 * @param fileLocation may be null
 */
static String getValidEncoding(String ret,String fileLocation)
{
   if (ret == null) {
      return ret;
    }
   final String lower = ret.trim().toLowerCase();
   if (lower.startsWith("latin")) {
      if (lower.indexOf("1") != -1) {
	 return "latin1"; // latin1
       }
    }
   if (lower.equals("iso-latin-1-unix")) {
      return "latin1"; // handle case from python libraries
    }
   try {
      if (!Charset.isSupported(ret)) {
	 if (LOG_ENCODING_ERROR) {
	    if (fileLocation != null) {
	       if ("uft-8".equals(ret) && fileLocation.endsWith("bad_coding.py")) {
		  return null; // this is an expected error in the python library.
		}
	     }
	    String msg = "The encoding found: >>" + ret + "<< on " + fileLocation
	       + " is not a valid encoding.";
	    PybaseMain.logE(msg,new UnsupportedEncodingException(msg));
	  }
	 return null; // ok, we've been unable to make it supported (better return null
	 // than an unsupported encoding).
       }
      return ret;
    }
   catch (IllegalCharsetNameException ex) {
      if (LOG_ENCODING_ERROR) {
	 String msg = "The encoding found: >>" + ret + "<< on " + fileLocation
	    + " is not a valid encoding.";
	 PybaseMain.logE(msg,ex);
       }
    }
   return null;
}


/**
 * Returns if the given file has a python shebang (i.e.: starts with #!... python)
 *
 * Will close the reader.
 */
public static boolean hasPythonShebang(Reader inputStreamReader)
	throws IllegalCharsetNameException
{
   BufferedReader reader = new BufferedReader(inputStreamReader);
   try {
      String l1 = reader.readLine();
      if (l1 != null) {
	 // Special case to skip bom.
	 if (l1.startsWith(BOM_UTF8)) {
	    l1 = l1.substring(BOM_UTF8.length());
	  }

	 if (l1.startsWith("#!") && l1.indexOf("python") != -1) {
	    return true;
	  }
       }

    }
   catch (IOException e) {
      PybaseMain.logE("Problem getting python shebang",e);
    }
   finally {
      try {
	 reader.close();
       }
      catch (IOException e1) { }
    }
   return false;
}


/**
		 * Useful to silent it on tests
		 */
public static boolean LOG_ENCODING_ERROR = true;

/**
		 * Start null... filled on 1st request.
		 *
		 * Currently we only care for: windows, mac os or linux (if we need some other special support,
									    * this could be improved).
	      */
public static Integer platform;









/**
		 * Copy a file from one place to another.
		 *
		 * Example from: http://www.exampledepot.com/egs/java.nio/File2File.html
		 *
		 * @param srcFilename the source file
		 * @param dstFilename the destination
		 */
public static void copyFile(String srcFilename,String dstFilename)
{
   try {
      IvyFile.copyFile(new File(srcFilename),new File(dstFilename));
   }
   catch (IOException e) { }
}





/**
		 * This method will try to create a backup file of the passed file.
		 * @param file this is the file we want to copy as the backup.
		 * @return true if it was properly copied and false otherwise.
		 */
public static boolean createBackupFile(File file)
{
   if (file != null && file.isFile()) {
      File parent = file.getParentFile();
      if (parent.isDirectory()) {
	 String[] list = parent.list();
	 HashSet<String> set = new HashSet<String>();
	 set.addAll(Arrays.asList(list));
	 String initialName = file.getName();
	 initialName += ".bak";
	 String name = initialName;
	 int i = 0;
	 while (set.contains(name)) {
	    name = initialName + i;
	    i++;
	  }
	 copyFile(file.getAbsolutePath(), new File(parent,name).getAbsolutePath());
	 return true;
       }
    }
   return false;
}


/**
 * Log with base is missing in java!
 */
public static double log(double a,double base)
{
   return Math.log(a) / Math.log(base);
}


public static void print(Object... objects)
{
   System.out.println(StringUtils.join(" ", objects));
}


private static final Map<File, Set<String>> alreadyReturned = new HashMap<File, Set<String>>();
private static Object		       lockTempFiles   = new Object();

public static File getTempFileAt(File parentDir,String prefix)
{
   return getTempFileAt(parentDir, prefix, "");
}

/**
		 * @param extension the extension (i.e.: ".py")
		 * @return
		 */
public static File getTempFileAt(File parentDir,String prefix,String extension)
{
   synchronized (lockTempFiles) {
      assert(parentDir.isDirectory());
      Set<String> current = alreadyReturned.get(parentDir);
      if (current == null) {
	 current = new HashSet<String>();
	 alreadyReturned.put(parentDir, current);
       }
      current.addAll(getFilesStartingWith(parentDir, prefix));

      StringBuilder buf = new StringBuilder();

      for (long i = 0; i < Long.MAX_VALUE; i++) {
	 buf.setLength(0);
	 String v = buf.append(prefix).append(i).append(extension).toString();
	 if (current.contains(v)) {
	    continue;
	  }
	 File file = new File(parentDir,v);
	 if (!file.exists()) {
	    current.add(file.getName());
	    return file;
	  }
       }
      return null;
    }
}


public static HashSet<String> getFilesStartingWith(File parentDir,String prefix)
{
   String[] list = parentDir.list();
   HashSet<String> hashSet = new HashSet<String>();
   for (String string : list) {
      if (string.startsWith(prefix)) {
	 hashSet.add(string);
       }
    }
   return hashSet;
}


public static void clearTempFilesAt(File parentDir,String prefix)
{
   synchronized (lockTempFiles) {
      try {
	 assert(parentDir.isDirectory());
	 String[] list = parentDir.list();
	 for (String string : list) {
	    if (string.startsWith(prefix)) {
	       String integer = string.substring(prefix.length());
	       try {
		  Integer.parseInt(integer);
		  try {
		     new File(parentDir,string).delete();
		   }
		  catch (Exception e) {
		     // ignore
		   }
		}
	       catch (NumberFormatException e) {
		  // ignore (not a file we generated)
		}
	     }
	  }
	 alreadyReturned.remove(parentDir);
       }
      catch (Throwable e) {
	 PybaseMain.logE("Problem clearing temp files",e);
       }
    }
}


/**
 * Yes, this will delete everything under a directory. Use with care!
 */
public static void deleteDirectoryTree(File directory) throws IOException
{
   if (!directory.exists()) {
      return;
    }
   File[] files = directory.listFiles();
   if (files != null) {

      for (int i = 0; i < files.length; ++i) {
	 File f = files[i];

	 if (f.isDirectory()) {
	    deleteDirectoryTree(f);
	  }
	 else {
	    deleteFile(f);
	  }
       }
    }
   if (!directory.delete()) {
      throw new IOException("Delete operation failed when deleting: " + directory);
    }
}

public static void deleteFile(File file) throws IOException
{
   if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());

   if (!file.delete()) {
      throw new IOException("Delete operation failed when deleting: " + file);
    }
}


/********************************************************************************/
/*										*/
/*	File name methods							*/
/*										*/
/********************************************************************************/

public static boolean isValidDll(String path)
{
   if (path.endsWith(".pyd") || path.endsWith(".so") || path.endsWith(".dll") || path.endsWith(".a")) {
      return true;
    }
   return false;
}


public static boolean isValidZipFile(String fileName)
{
   return fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".egg");
}


public final static String[] getDottedValidSourceFiles()
{
   return new String[]{".py", ".pyw"};
}


public static boolean isValidDllExtension(String extension)
{
   if (extension.equals("pyd") || extension.equals("so") || extension.equals("dll") || extension.equals("a")) {
      return true;
    }
   return false;
}



}	// end of class PybaseFileSystem


/* end of PybaseFileSystem.java */
