package harpoon.Tools.Annotation.Lex;

import java.io.Reader;
import java.io.LineNumberReader;

import java.util.Arrays;
/* Java lexer.
 * Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
 * This program is released under the terms of the GPL; see the file
 * COPYING for more details.  There is NO WARRANTY on this code.
 */

public class Lexer implements harpoon.Tools.Annotation.Lexer {
  LineNumberReader reader;
  boolean isJava12;
  boolean isJava14;
  String line = null;
  int line_pos = 1;
  int line_num = 0;
  LineList lineL = new LineList(-line_pos, null); // sentinel for line #0
  
  public Lexer(Reader reader) {
    this(reader, 4); // by default, use a Java 1.4-compatible lexer.
  }
  public Lexer(Reader reader, int java_minor_version) {
    this.reader = new LineNumberReader(new EscapedUnicodeReader(reader));
    this.isJava12 = java_minor_version >= 2;
    this.isJava14 = java_minor_version >= 4;
  }
  
  public java_cup.runtime.Symbol nextToken() throws java.io.IOException {
    /* tokens are:
     *  Identifiers/Keywords/true/false/null (start with java letter)
     *  numeric literal (start with number)
     *  character literal (start with single quote)
     *  string (start with double quote)
     *  separator (parens, braces, brackets, semicolon, comma, period)
     *  operator (equals, plus, minus, etc)
     *  whitespace
     *  comment (start with slash)
     */
    InputElement ie;
    int startpos, endpos;
    do {
      startpos = lineL.head + line_pos;
      ie = getInputElement();
      // annotate this input element
      ie.annotate(linepos(startpos), linepos(lineL.head+line_pos-1));
      // set 'last comment' field, if appropriate.
      if (ie instanceof DocumentationComment) {
	comment = ((Comment)ie).getComment();
	comment_start = startpos;
      }
    } while (!(ie instanceof Token));
    endpos = lineL.head + line_pos - 1;

    //System.out.println(ie.toString()); // uncomment to debug lexer.
    java_cup.runtime.Symbol sym = ((Token)ie).token();
    // fix up left/right positions.
    sym.left = startpos; sym.right = endpos;
    // attach comment information to keywords.
    if (ie instanceof Keyword && comment!=null)
	sym.value = Arrays.asList(new Object[] {
	    comment, new Integer(comment_start)
	});
    // return token.
    return sym;
  }
  public boolean debug_lex() throws java.io.IOException {
    InputElement ie = getInputElement();
    System.out.println(ie);
    return !(ie instanceof EOF);
  }

  String comment; int comment_start;
  public String lastComment() { return comment; }
  public void clearComment() { comment=""; }
  
  InputElement getInputElement() throws java.io.IOException {
    if (line_num == 0)
      nextLine();
    if (line==null)
      return new EOF();
    if (line.length()<=line_pos) {      // end of line.
      nextLine();
      if (line==null)
	return new EOF();
    }
    
    switch (line.charAt(line_pos)) {

      // White space:
    case ' ':	// ASCII SP
    case '\t':	// ASCII HT
    case '\f':	// ASCII FF
    case '\n':	// LineTerminator
      return new WhiteSpace(consume());

      // EOF character:
    case '\020': // ASCII SUB
      consume();
      return new EOF();

      // Comment prefix:
    case '/':
      return getComment();

      // else, a Token
    default:
      return getToken();
    }
  }
  // May get Token instead of Comment.
  InputElement getComment() throws java.io.IOException {
    String comment;
    // line.charAt(line_pos+0) is '/'
    switch (line.charAt(line_pos+1)) {
    case '/': // EndOfLineComment
      comment = line.substring(line_pos+2);
      line_pos = line.length();
      return new EndOfLineComment(comment);
    case '*': // TraditionalComment or DocumentationComment
      line_pos += 2;
      if (line.charAt(line_pos)=='*') { // DocumentationComment
	return snarfComment(new DocumentationComment());
      } else { // TraditionalComment
	return snarfComment(new TraditionalComment());
      }
    default: // it's a token, not a comment.
      return getToken();
    }
  }

  Comment snarfComment(Comment c) throws java.io.IOException {
    StringBuffer text=new StringBuffer();
    while(true) { // Grab CommentTail
      while (line.charAt(line_pos)!='*') { // Add NotStar to comment.
	int star_pos = line.indexOf('*', line_pos);
	if (star_pos<0) {
	  text.append(line.substring(line_pos));
	  c.appendLine(text.toString()); text.setLength(0);
	  nextLine();
	  if (line==null) 
	    throw new Error("Unterminated comment at end of file.");
	} else {
	  text.append(line.substring(line_pos, star_pos));
	  line_pos=star_pos;
	}
      }
      // At this point, line.charAt(line_pos)=='*'
      // Grab CommentTailStar starting at line_pos+1.
      if (line.charAt(line_pos+1)=='/') { // safe because line ends with '\n'
	c.appendLine(text.toString()); line_pos+=2; return c;
      }
      text.append(line.charAt(line_pos++)); // add the '*'
    }
  }

  Token getToken() {
    // Tokens are: Identifiers, Keywords, Literals, Separators, Operators.
    switch (line.charAt(line_pos)) {
      // Separators: (period is a special case)
    case '(':
    case ')':
    case '{':
    case '}':
    case '[':
    case ']':
    case ';':
    case ',':
      return new Separator(consume());

      // Operators:
    case '=':
    case '>':
    case '<':
    case '!':
    case '~':
    case '?':
    case ':':
    case '&':
    case '|':
    case '+':
    case '-':
    case '*':
    case '/':
    case '^':
    case '%':
      return getOperator();
    case '\'':
      return getCharLiteral();
    case '\"':
      return getStringLiteral();

      // a period is a special case:
    case '.':
      if (Character.digit(line.charAt(line_pos+1),10)!=-1)
	return getNumericLiteral();
      else return new Separator(consume());

    default: 
      break;
    }
    if (Character.isJavaIdentifierStart(line.charAt(line_pos)))
      return getIdentifier();
    if (Character.isDigit(line.charAt(line_pos)))
      return getNumericLiteral();
    throw new Error("Illegal character on line "+line_num);
  }

  static final String[] keywords = new String[] {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", 
    "extends", "final", "finally", "float", "for", "goto", "if", 
    "implements", "import", "instanceof", "int", "interface", "long", 
    "native", "new", "package", "private", "protected", "public", 
    "return", "short", "static", "strictfp", "super", "switch",
    "synchronized", "this", "throw", "throws", "transient", "try", "void",
    "volatile", "while" };
  Token getIdentifier() {
    // Get id string.
    StringBuffer sb = new StringBuffer().append(consume());

    if (!Character.isJavaIdentifierStart(sb.charAt(0)))
      throw new Error("Invalid Java Identifier on line "+line_num);
    while (Character.isJavaIdentifierPart(line.charAt(line_pos)))
      sb.append(consume());
    String s = sb.toString();
    // Now check against boolean literals and null literal.
    if (s.equals("null")) return new NullLiteral();
    if (s.equals("true")) return new BooleanLiteral(true);
    if (s.equals("false")) return new BooleanLiteral(false);
    // Check against keywords.
    //  pre-java 1.4 compatibility:
    if (!isJava14 && s.equals("assert")) return new Identifier(s);
    //  pre-java 1.2 compatibility:
    if (!isJava12 && s.equals("strictfp")) return new Identifier(s);
    // use binary search.
    for (int l=0, r=keywords.length; r > l; ) {
      int x = (l+r)/2, cmp = s.compareTo(keywords[x]);
      if (cmp < 0) r=x; else l=x+1;
      if (cmp== 0) return new Keyword(s);
    }
    // not a keyword.
    return new Identifier(s);
  }
  NumericLiteral getNumericLiteral() {
    int i;
    // leading decimal indicates float.
    if (line.charAt(line_pos)=='.')
      return getFloatingPointLiteral();
    // 0x indicates Hex.
    if (line.charAt(line_pos)=='0' &&
	(line.charAt(line_pos+1)=='x' ||
	 line.charAt(line_pos+1)=='X')) {
      line_pos+=2; return getIntegerLiteral(/*base*/16);
    }
    // otherwise scan to first non-numeric
    for (i=line_pos; Character.digit(line.charAt(i),10)!=-1; )
      i++;
    switch(line.charAt(i)) { // discriminate based on first non-numeric
    case '.':
    case 'f':
    case 'F':
    case 'd':
    case 'D':
    case 'e':
    case 'E':
      return getFloatingPointLiteral();
    case 'L':
    case 'l':
    default:
      if (line.charAt(line_pos)=='0')
	return getIntegerLiteral(/*base*/8);
      return getIntegerLiteral(/*base*/10);
    }
  }
  NumericLiteral getIntegerLiteral(int radix) {
    long val=0;
    while (Character.digit(line.charAt(line_pos),radix)!=-1)
      val = (val*radix) + Character.digit(consume(),radix);
    if (line.charAt(line_pos) == 'l' ||
	line.charAt(line_pos) == 'L') {
      consume();
      return new LongLiteral(val);
    } 
    // we compare MAX_VALUE against val/2 to allow constants like
    // 0xFFFF0000 to get past the test. (unsigned long->signed int)
    if ((val/2) > Integer.MAX_VALUE ||
	 val    < Integer.MIN_VALUE)
      throw new Error("Constant does not fit in integer on line "+line_num);
    return new IntegerLiteral((int)val);
  }
  NumericLiteral getFloatingPointLiteral() {
    String rep = getDigits();
    if (line.charAt(line_pos)=='.')
      rep+=consume() + getDigits();
    if (line.charAt(line_pos)=='e' ||
	line.charAt(line_pos)=='E') {
      rep+=consume();
      if (line.charAt(line_pos)=='+' ||
	  line.charAt(line_pos)=='-')
	rep+=consume();
      rep+=getDigits();
    }
    try {
      switch (line.charAt(line_pos)) {
      case 'f':
      case 'F':
	consume();
	return new FloatLiteral(Float.valueOf(rep).floatValue());
      case 'd':
      case 'D':
	consume();
	/* falls through */
      default:
	return new DoubleLiteral(Double.valueOf(rep).doubleValue());
      }
    } catch (NumberFormatException e) {
      throw new Error("Illegal floating-point on line "+line_num+": "+e);
    }
  }
  String getDigits() {
    StringBuffer sb = new StringBuffer();
    while (Character.digit(line.charAt(line_pos),10)!=-1)
      sb.append(consume());
    return sb.toString();
  }

  Operator getOperator() {
    char first = consume();
    char second= line.charAt(line_pos);

    switch(first) {
      // single-character operators.
    case '~':
    case '?':
    case ':':
      return new Operator(new String(new char[] {first}));
      // doubled operators
    case '+':
    case '-':
    case '&':
    case '|':
      if (first==second) 
	return new Operator(new String(new char[] {first, consume()}));
    default:
      break;
    }
    // Check for trailing '='
    if (second=='=')
	return new Operator(new String(new char[] {first, consume()}));

    // Special-case '<<', '>>' and '>>>'
    if ((first=='<' && second=='<') || // <<
	(first=='>' && second=='>')) {  // >>
      String op = new String(new char[] {first, consume()});
      if (first=='>' && line.charAt(line_pos)=='>') // >>>
	op += consume();
      if (line.charAt(line_pos)=='=') // <<=, >>=, >>>=
	op += consume();
      return new Operator(op);
    }

    // Otherwise return single operator.
    return new Operator(new String(new char[] {first}));
  }

  CharacterLiteral getCharLiteral() {
    char firstquote = consume();
    char val;
    switch (line.charAt(line_pos)) {
    case '\\':
      val = getEscapeSequence();
      break;
    case '\'':
      throw new Error("Invalid character literal on line "+line_num);
    case '\n':
      throw new Error("Invalid character literal on line "+line_num);
    default:
      val = consume();
      break;
    }
    char secondquote = consume();
    if (firstquote != '\'' || secondquote != '\'')
      throw new Error("Invalid character literal on line "+line_num);
    return new CharacterLiteral(val);
  }
  StringLiteral getStringLiteral() {
    char openquote = consume();
    StringBuffer val = new StringBuffer();
    while (line.charAt(line_pos)!='\"') {
      switch(line.charAt(line_pos)) {
      case '\\':
	val.append(getEscapeSequence());
	break;
      case '\n':
	throw new Error("Invalid string literal on line " + line_num);
      default:
	val.append(consume());
	break;
      }
    }
    char closequote = consume();
    if (openquote != '\"' || closequote != '\"')
      throw new Error("Invalid string literal on line " + line_num);
    
    return new StringLiteral(val.toString().intern());
  }

  char getEscapeSequence() {
    if (consume() != '\\')
      throw new Error("Invalid escape sequence on line " + line_num);
    switch(line.charAt(line_pos)) {
    case 'b':
      consume(); return '\b';
    case 't':
      consume(); return '\t';
    case 'n':
      consume(); return '\n';
    case 'f':
      consume(); return '\f';
    case 'r':
      consume(); return '\r';
    case '\"':
      consume(); return '\"';
    case '\'':
      consume(); return '\'';
    case '\\':
      consume(); return '\\';
    case '0':
    case '1':
    case '2':
    case '3':
      return (char) getOctal(3);
    case '4':
    case '5':
    case '6':
    case '7':
      return (char) getOctal(2);
    default:
      throw new Error("Invalid escape sequence on line " + line_num);
    }
  }
  int getOctal(int maxlength) {
    int i, val=0;
    for (i=0; i<maxlength; i++)
      if (Character.digit(line.charAt(line_pos), 8)!=-1) {
	val = (8*val) + Character.digit(consume(), 8);
      } else break;
    if ((i==0) || (val>0xFF)) // impossible.
      throw new Error("Invalid octal escape sequence in line " + line_num);
    return val;
  }

  char consume() { return line.charAt(line_pos++); }
  void nextLine() throws java.io.IOException {
    line=reader.readLine();
    if (line!=null) line=line+'\n'; 
    lineL = new LineList(lineL.head+line_pos, lineL); // for error reporting
    line_pos=0; 
    line_num++; 
  }

  // Deal with error messages.
  public void errorMsg(String msg, java_cup.runtime.Symbol info) {
    LinePos lp = linepos(info.left);
    System.err.println(msg+" at line "+lp.line);
    num_errors++;
  }
  private int num_errors = 0;
  public int numErrors() { return num_errors; }

  public LinePos linepos(int character_offset) {
    int n=line_num, c=character_offset-lineL.head;
    for (LineList p = lineL; p!=null; p=p.tail, n--)
      if (p.head<=character_offset) { c=character_offset-p.head; break; }
    return new LinePos(n, c);
  }
  
  class LineList {
    int head;
    LineList tail;
    LineList(int head, LineList tail) { this.head = head; this.tail = tail; }
  }
}