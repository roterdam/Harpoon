package harpoon.Temp;

/**
 * A Label represents an address in assembly language.
 */

public class Label  {
   private String name;
   private static int count;

  /**
   * a printable representation of the label, for use in assembly 
   * language output.
   */
   public String toString() {return name;}

  /**
   * Makes a new label that prints as "name".
   * Warning: avoid repeated calls to <tt>new Label(s)</tt> with
   * the same name <tt>s</tt>.
   */
   public Label(String n) {
	name=n;
   }

  /**
   * Makes a new label with an arbitrary name.
   */
   public Label() {
	this("L" + count++);
   }

   public boolean equals(Object o) {
       Label l;
       if (this==o) return true;
       if (null==o) return false;
       try { l=(Label) o; } catch (ClassCastException e) { return false; }
       return name.equals(l.name);
   }
}
