package com.ismartcoding.lib.androidsvg.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CSSTextScanner extends TextScanner
{
   static final Pattern PATTERN_BLOCK_COMMENTS = Pattern.compile("(?s)/\\*.*?\\*/");

   public CSSTextScanner(String input)
   {
      super(PATTERN_BLOCK_COMMENTS.matcher(input).replaceAll(""));  // strip all block comments
   }

   /*
    * Scans for a CSS 'ident' identifier.
    */
   public String  nextIdentifier()
   {
      int  end = scanForIdentifier();
      if (end == position)
         return null;
      String result = input.substring(position, end);
      position = end;
      return result;
   }


   // ident-token:
   //   start-char rest-char*
   //   - start-char rest-char*
   //   -- rest-char*
   //
   // Where:
   //   start-char: a-z A-Z _ or escape or non-ASCII
   //   rest-char: a-z A-Z 0-9 _ - or escape non-ASCII
   //   escape:  (not yet implemented)
   //     \ char
   //     \ hexdigit{1-6}
   //     \ hexdigit{1-6} whitespace
   //   non-ASCII: >= U+0080
   //   whitespace: (space or \t or newline)+
   //   newline: \n or \r\n or \r or \f

   private int  scanForIdentifier()
   {
      if (empty())
         return position;
      int  start = position;
      int  lastValidPos = position;

      int  ch = input.charAt(position);
      if (ch == '-')
         ch = advanceChar();
      // start-char
      if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch == '-') || (ch == '_') || (ch >= 0x80))
      {
         ch = advanceChar();
         // rest-char
         while ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch == '-') || (ch == '_') || (ch >= 0x80)) {
            ch = advanceChar();
         }
         lastValidPos = position;
      }
      position = start;
      return lastValidPos;
   }


   /*
    * Parse a simpleSelectors group (eg. E, F, G). In many/most cases there will be only one entry.
    */
   public List<CSSParser.Selector> nextSelectorGroup() throws CSSParseException
   {
      if (empty())
         return null;

      ArrayList<CSSParser.Selector> selectorGroup = new ArrayList<>(1);
      CSSParser.Selector selector = new CSSParser.Selector();

      while (!empty())
      {
         if (nextSimpleSelector(selector))
         {
            // If there is a comma, keep looping, otherwise break
            if (!skipCommaWhitespace())
               continue;  // if not a comma, go back and check for next part of simpleSelectors
            selectorGroup.add(selector);
            selector = new CSSParser.Selector();
         }
         else
            break;
      }
      if (!selector.isEmpty())
         selectorGroup.add(selector);
      return selectorGroup;
   }


   /*
    * Scans for a CSS 'simple simpleSelectors'.
    * Returns true if it found one.
    * Returns false if there was an error or the input is empty.
    */
   boolean  nextSimpleSelector(CSSParser.Selector selector) throws CSSParseException
   {
      if (empty())
         return false;

      int             start = position;
      CSSParser.Combinator combinator = null;
      CSSParser.SimpleSelector selectorPart = null;

      if (!selector.isEmpty())
      {
         if (consume('>')) {
            combinator = CSSParser.Combinator.CHILD;
            skipWhitespace();
         } else if (consume('+')) {
            combinator = CSSParser.Combinator.FOLLOWS;
            skipWhitespace();
         }
      }

      if (consume('*')) {
         selectorPart = new CSSParser.SimpleSelector(combinator, null);
      } else {
         String tag = nextIdentifier();
         if (tag != null) {
            selectorPart = new CSSParser.SimpleSelector(combinator, tag);
            selector.addedElement();
         }
      }

      while (!empty())
      {
         if (consume('.'))
         {
            // ".foo" is equivalent to *[class="foo"]
            if (selectorPart == null)
               selectorPart = new CSSParser.SimpleSelector(combinator, null);
            String  value = nextIdentifier();
            if (value == null)
               throw new CSSParseException("Invalid \".class\" simpleSelectors");
            selectorPart.addAttrib(CSSParser.CLASS, CSSParser.AttribOp.EQUALS, value);
            selector.addedAttributeOrPseudo();
            continue;
         }

         if (consume('#'))
         {
            // "#foo" is equivalent to *[id="foo"]
            if (selectorPart == null)
               selectorPart = new CSSParser.SimpleSelector(combinator, null);
            String  value = nextIdentifier();
            if (value == null)
               throw new CSSParseException("Invalid \"#id\" simpleSelectors");
            selectorPart.addAttrib(CSSParser.ID, CSSParser.AttribOp.EQUALS, value);
            selector.addedIdAttribute();
            continue;
         }

         // Now check for attribute selection and pseudo selectors
         if (consume('['))
         {
            if (selectorPart == null)
               selectorPart = new CSSParser.SimpleSelector(combinator, null);
            skipWhitespace();
            String  attrName = nextIdentifier();
            String  attrValue = null;
            if (attrName == null)
               throw new CSSParseException("Invalid attribute simpleSelectors");
            skipWhitespace();
            CSSParser.AttribOp op = null;
            if (consume('='))
               op = CSSParser.AttribOp.EQUALS;
            else if (consume("~="))
               op = CSSParser.AttribOp.INCLUDES;
            else if (consume("|="))
               op = CSSParser.AttribOp.DASHMATCH;
            if (op != null) {
               skipWhitespace();
               attrValue = nextAttribValue();
               if (attrValue == null)
                  throw new CSSParseException("Invalid attribute simpleSelectors");
               skipWhitespace();
            }
            if (!consume(']'))
               throw new CSSParseException("Invalid attribute simpleSelectors");
            selectorPart.addAttrib(attrName, (op == null) ? CSSParser.AttribOp.EXISTS : op, attrValue);
            selector.addedAttributeOrPseudo();
            continue;
         }

         if (consume(':'))
         {
            if (selectorPart == null)
               selectorPart = new CSSParser.SimpleSelector(combinator, null);
            parsePseudoClass(selector, selectorPart);
            continue;
         }

         break;
      }

      if (selectorPart != null)
      {
         selector.add(selectorPart);
         return true;
      }

      // Otherwise 'fail'
      position = start;
      return false;
   }


   private static class  AnPlusB
   {
      final public int a;
      final public int b;

      AnPlusB(int a, int b) {
         this.a = a;
         this.b = b;
      }
   }


   private AnPlusB  nextAnPlusB()
   {
      if (empty())
         return null;

      int  start = position;

      if (!consume('('))
         return null;
      skipWhitespace();

      AnPlusB  result;
      if (consume("odd"))
         result = new AnPlusB(2, 1);
      else if (consume("even"))
         result = new AnPlusB(2, 0);
      else
      {
         // Parse an expression of the form +An+B
         // First check for an optional leading sign
         int  aSign = 1,
               bSign = 1;
         if (consume('+')) {
            // do nothing
         } else if (consume('-')) {
            bSign = -1;
         }
         // Then an integer
         IntegerParser a = null,
               b = IntegerParser.parseInt(input, position, inputLength, false);
         if (b != null)
            position = b.getEndPos();
         // If an 'n' is next then that last part was the 'a' part. Now check for the 'b' part.
         if (consume('n') || consume('N')) {
            a = (b != null) ? b : new IntegerParser(1, position);
            aSign = bSign;
            b = null;
            bSign = 1;
            skipWhitespace();
            // Check for the sign for the b part
            boolean  hasB = consume('+');
            if (!hasB) {
               hasB = consume('-');
               if (hasB)
                  bSign = -1;
            }
            // If there was a sign, then the b integer should follow next
            if (hasB) {
               skipWhitespace();
               b = IntegerParser.parseInt(input, position, inputLength, false);
               if (b != null) {
                  position = b.getEndPos();
               } else {
                  position = start;
                  return null;
               }
            }
         }
         // Construct the result in anticipation that we will get the end bracket next
         result = new AnPlusB((a == null) ? 0 : aSign * a.value(),
               (b == null) ? 0 : bSign * b.value());
      }

      skipWhitespace();
      if (consume(')'))
         return result;

      position = start;
      return null;
   }


   /*
    * Parse a list of identifiers from a pseudo class parameter set.
    * Eg. for :lang(en)
    */
   private List<String>  nextIdentListParam()
   {
      if (empty())
         return null;

      int                start = position;
      ArrayList<String>  result = null;

      if (!consume('('))
         return null;
      skipWhitespace();

      do {
         String ident = nextIdentifier();
         if (ident == null) {
            position = start;
            return null;
         }
         if (result == null)
            result = new ArrayList<>();
         result.add(ident);
         skipWhitespace();
      } while (skipCommaWhitespace());

      if (consume(')'))
         return result;

      position = start;
      return null;
   }


   /*
    * Parse a simpleSelectors group inside a pair of brackets.  For the :not pseudo class.
    */
   private List<CSSParser.Selector>  nextPseudoNotParam() throws CSSParseException
   {
      if (empty())
         return null;

      int  start = position;

      if (!consume('('))
         return null;
      skipWhitespace();

      // Parse the parameter contents
      List<CSSParser.Selector>  result = nextSelectorGroup();

      if (result == null) {
         position = start;
         return null;
      }

      if (!consume(')')) {
         position = start;
         return null;
      }

      // Nesting a :not() pseudo class within a :not() is not allowed.
      for (CSSParser.Selector selector: result) {
         if (selector.simpleSelectors == null)
            break;
         for (CSSParser.SimpleSelector simpleSelector: selector.simpleSelectors) {
            if (simpleSelector.pseudos == null)
               break;
            for (CSSParser.PseudoClass pseudo: simpleSelector.pseudos) {
               if (pseudo instanceof CSSParser.PseudoClassNot)
                  return null;
            }
         }
      }

      return result;
   }


   /*
    * Parse a pseudo class (such as ":first-child")
    */
   private void  parsePseudoClass(CSSParser.Selector selector, CSSParser.SimpleSelector selectorPart) throws CSSParseException
   {
      // skip pseudo
//         int     pseudoStart = position;
      String  ident = nextIdentifier();
      if (ident == null)
         throw new CSSParseException("Invalid pseudo class");

      CSSParser.PseudoClass pseudo;
      CSSParser.PseudoClassIdents identEnum = CSSParser.PseudoClassIdents.fromString(ident);
      switch (identEnum)
      {
         case first_child:
            pseudo = new CSSParser.PseudoClassAnPlusB(0, 1, true, false, null);
            selector.addedAttributeOrPseudo();
            break;

         case last_child:
            pseudo = new CSSParser.PseudoClassAnPlusB(0, 1, false, false, null);
            selector.addedAttributeOrPseudo();
            break;

         case only_child:
            pseudo = new CSSParser.PseudoClassOnlyChild(false, null);
            selector.addedAttributeOrPseudo();
            break;

         case first_of_type:
            pseudo = new CSSParser.PseudoClassAnPlusB(0, 1, true, true, selectorPart.tag);
            selector.addedAttributeOrPseudo();
            break;

         case last_of_type:
            pseudo = new CSSParser.PseudoClassAnPlusB(0, 1, false, true, selectorPart.tag);
            selector.addedAttributeOrPseudo();
            break;

         case only_of_type:
            pseudo = new CSSParser.PseudoClassOnlyChild(true, selectorPart.tag);
            selector.addedAttributeOrPseudo();
            break;

         case root:
            pseudo = new CSSParser.PseudoClassRoot();
            selector.addedAttributeOrPseudo();
            break;

         case empty:
            pseudo = new CSSParser.PseudoClassEmpty();
            selector.addedAttributeOrPseudo();
            break;

         case nth_child:
         case nth_last_child:
         case nth_of_type:
         case nth_last_of_type:
            boolean fromStart = identEnum == CSSParser.PseudoClassIdents.nth_child || identEnum == CSSParser.PseudoClassIdents.nth_of_type;
            boolean ofType    = identEnum == CSSParser.PseudoClassIdents.nth_of_type || identEnum == CSSParser.PseudoClassIdents.nth_last_of_type;
            AnPlusB  ab = nextAnPlusB();
            if (ab == null)
               throw new CSSParseException("Invalid or missing parameter section for pseudo class: " + ident);
            pseudo = new CSSParser.PseudoClassAnPlusB(ab.a, ab.b, fromStart, ofType, selectorPart.tag);
            selector.addedAttributeOrPseudo();
            break;

         case not:
            List<CSSParser.Selector>  notSelectorGroup = nextPseudoNotParam();
            if (notSelectorGroup == null)
               throw new CSSParseException("Invalid or missing parameter section for pseudo class: " + ident);
            pseudo = new CSSParser.PseudoClassNot(notSelectorGroup);
            selector.specificity = ((CSSParser.PseudoClassNot) pseudo).getSpecificity();
            break;

         case target:
            //TODO
            pseudo = new CSSParser.PseudoClassTarget();
            selector.addedAttributeOrPseudo();
            break;

         case lang:
            List<String>  langs = nextIdentListParam();
            pseudo = new CSSParser.PseudoClassNotSupported(ident);
            selector.addedAttributeOrPseudo();
            break;

         case link:
         case visited:
         case hover:
         case active:
         case focus:
         case enabled:
         case disabled:
         case checked:
         case indeterminate:
            pseudo = new CSSParser.PseudoClassNotSupported(ident);
            selector.addedAttributeOrPseudo();
            break;

         default:
            throw new CSSParseException("Unsupported pseudo class: " + ident);
      }

//         selectorPart.addPseudo(input.substring(pseudoStart, position));
      selectorPart.addPseudo(pseudo);
//         simpleSelectors.addedAttributeOrPseudo();
   }


   /*
    * The value (bar) part of "[foo="bar"]".
    */
   private String  nextAttribValue()
   {
      if (empty())
         return null;

      String  result = nextQuotedString();
      if (result != null)
         return result;
      return nextIdentifier();
   }

   /*
    * Scans for a CSS property value.
    */
   public String  nextPropertyValue()
   {
      if (empty())
         return null;
      int  start = position;
      int  lastValidPos = position;

      int  ch = input.charAt(position);
      while (ch != -1 && ch != ';' && ch != '}' && ch != '!' && !isEOL(ch)) {
         if (!isWhitespace(ch))  // don't include an spaces at the end
            lastValidPos = position + 1;
         ch = advanceChar();
      }
      if (position > start)
         return input.substring(start, lastValidPos);
      position = start;
      return null;
   }

   /*
    * Scans for a string token
    */
   public String  nextCSSString()
   {
      if (empty())
         return null;
      int  ch = input.charAt(position);
      int  endQuote = ch;
      if (ch != '\'' && ch != '"')
         return null;

      StringBuilder  sb = new StringBuilder();
      position++;
      ch = nextChar();
      while (ch != -1 && ch != endQuote)
      {
         if (ch == '\\') {
            // Escaped char sequence
            ch = nextChar();
            if (ch == -1)    // EOF: do nothing
               continue;
            if (ch == '\n' || ch == '\r' || ch == '\f') {  // a CSS newline
               ch = nextChar();
               continue;     // Newline: consume it
            }
            int  hc = hexChar(ch);
            if (hc != -1) {
               int  codepoint = hc;
               for (int i=1; i<=5; i++) {
                  ch = nextChar();
                  hc = hexChar(ch);
                  if (hc == -1)
                     break;
                  codepoint = codepoint * 16 + hc;
               }
               sb.append((char) codepoint);
               continue;
            }
            // Other chars just unescape to themselves
            // Fall through to append
         }
         sb.append((char) ch);
         ch = nextChar();
      }
      return sb.toString();
   }


   private int  hexChar(int ch)
   {
      if (ch >= '0' && ch <= '9')
         return (ch - (int)'0');
      if (ch >= 'A' && ch <= 'F')
         return (ch - (int)'A') + 10;
      if (ch >= 'a' && ch <= 'f')
         return (ch - (int)'a') + 10;
      return -1;
   }


   /*
    * Scans for a url("...")
    * Called a <url> in the CSS spec.
    */
   public String  nextURL()
   {
      if (empty())
         return null;
      int  start = position;
      if (!consume("url("))
         return null;

      skipWhitespace();

      String url = nextCSSString();
      if (url == null)
         url = nextLegacyURL();  // legacy quote-less url(...).  Called a <url-token> in the CSS3 spec.

      if (url == null) {
         position = start;
         return null;
      }

      skipWhitespace();

      if (empty() || consume(")"))
         return url;

      position = start;
      return null;
   }


   /*
    * Scans for a legacy URL string
    * See nextURLToken().
    */
   String  nextLegacyURL()
   {
      StringBuilder  sb = new StringBuilder();

      while (!empty())
      {
         int  ch = input.charAt(position);

         if (ch == '\'' || ch == '"' || ch == '(' || ch == ')' || isWhitespace(ch) || Character.isISOControl(ch))
            break;

         position++;
         if (ch == '\\')
         {
            if (empty())    // EOF: do nothing
               continue;
            // Escaped char sequence
            ch = input.charAt(position++);
            if (ch == '\n' || ch == '\r' || ch == '\f') {  // a CSS newline
               continue;     // Newline: consume it
            }
            int  hc = hexChar(ch);
            if (hc != -1) {
               int  codepoint = hc;
               for (int i=1; i<=5; i++) {
                  if (empty())
                     break;
                  hc = hexChar( input.charAt(position) );
                  if (hc == -1)  // Not a hex char
                     break;
                  position++;
                  codepoint = codepoint * 16 + hc;
               }
               sb.append((char) codepoint);
               continue;
            }
            // Other chars just unescape to themselves
            // Fall through to append
         }
         sb.append((char) ch);
      }
      if (sb.length() == 0)
         return null;
      return sb.toString();
   }
}
