// Loader.java, created by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import java.io.IOException;
import java.io.FileNotFoundException;

/** 
 * Quick and dirty class file loader.
 * Looks through CLASSPATH to find the class.  Understands .jar and .zip
 * files.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Loader.java,v 1.7 1998-10-16 08:54:10 cananian Exp $
 */
class Loader {
  /** Return an enumeration of zipfiles in the CLASSPATH that may be
   *  candidates for preloading. */
  public static Enumeration preloadable() {
    final Enumeration e = classpaths();
    return new Enumeration() {
      String path = null;
      private void adv() { 
	while (path==null && e.hasMoreElements()) {
	  String p = (String) e.nextElement();
	  if (p.toLowerCase().endsWith(".zip") ||
	      p.toLowerCase().endsWith(".jar")) 
	    path = p;
	}
      }
      public boolean hasMoreElements() { adv(); return (path!=null); }
      public Object  nextElement() { 
	adv(); String r=path; path=null; return r;
      }
    };
  }
  /** Enumerate the components of the system CLASSPATH. */
  public static Enumeration classpaths() {
    String classpath = System.getProperty("java.class.path");
    final String pathsep   = System.getProperty("path.separator");

    // For convenience, make sure classpath begins with and ends with pathsep.
    if (!classpath.startsWith(pathsep)) classpath = pathsep + classpath;
    if (!classpath.endsWith(pathsep)) classpath = classpath + pathsep;
    final String cp = classpath;

    return new Enumeration() {
      int i=0;
      public boolean hasMoreElements() { 
	return (cp.length() > (i+pathsep.length()));
      }
      public Object nextElement() {
	i+=pathsep.length(); // cp begins with pathsep.
	String path = cp.substring(i, cp.indexOf(pathsep, i));
	i+=path.length(); // skip over path.
	return path;
      }
    };
  }


  public static String classToResource(String classname) {
    String filesep   = System.getProperty("file.separator");
    // Swap all '.' for '/' & append ".class"
    return classname.replace('.', filesep.charAt(0)) + ".class";
  }

  /** Open an InputStream on a resource found somewhere in the CLASSPATH.
   * @param name The filename of the resource to locate.
   */
  public static InputStream getResourceAsStream(String name) {
    for (Enumeration e = classpaths(); e.hasMoreElements(); ) {
      String path = (String) e.nextElement();
      
      InputStream is = getResourceAsStream(path, name);
      if (is!=null) return is; // return stream if found.
    }
    // Couldn't find resource.
    return null;
  }

  /** Attempt to open resource given section of CLASSPATH and resource name. */
  static InputStream getResourceAsStream(String path, String name) {
    // special case .zip and .jar files.
    if (path.toLowerCase().endsWith(".zip") ||
	path.toLowerCase().endsWith(".jar")) {
      try {
	return getResourceAsStream(new ZipFile(path), name);
      } catch (IOException e) {
	return null;
      }
    } else return getResourceAsStream(new File(path, name));
  }

  /** Open a resource in a zipfile. */
  static InputStream getResourceAsStream(ZipFile zf, String name) {
    try {
      ZipEntry ze = zf.getEntry(name);
      if (ze==null) return null;
      return zf.getInputStream(ze);
    } catch (IOException e) {
      return null;
    }
  }
  
  /** Open a resource in a file. */
  static InputStream getResourceAsStream(File name) {
    try {
      return new FileInputStream(name);
    } catch (FileNotFoundException e) {
      return null;
    }
  }
}
