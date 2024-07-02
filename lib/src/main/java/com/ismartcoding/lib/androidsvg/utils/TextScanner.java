package com.ismartcoding.lib.androidsvg.utils;

import com.ismartcoding.lib.androidsvg.utils.SVGBase.Length;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Unit;

import java.util.Locale;


public class TextScanner
{
   final String  input;
   int           position = 0;
   int           inputLength;

   private  final NumberParser numberParser = new NumberParser();


   public TextScanner(String input)
   {
      this.input = input.trim();
      this.inputLength = this.input.length();
   }

   /**
    * Returns true if we have reached the end of the input.
    */
   public boolean  empty()
   {
      return (position == inputLength);
   }

   boolean  isWhitespace(int c)
   {
      return (c==' ' || c=='\n' || c=='\r' || c =='\t');
   }

   public void  skipWhitespace()
   {
      while (position < inputLength) {
         if (!isWhitespace(input.charAt(position)))
            break;
         position++;
      }
   }

   boolean  isEOL(int c)
   {
      return (c=='\n' || c=='\r');
   }

   // Skip the sequence: <space>*(<comma><space>)?
   // Returns true if we found a comma in there.
   public boolean  skipCommaWhitespace()
   {
      skipWhitespace();
      if (position == inputLength)
         return false;
      if (!(input.charAt(position) == ','))
         return false;
      position++;
      skipWhitespace();
      return true;
   }


   public float  nextFloat()
   {
      float  val = numberParser.parseNumber(input, position, inputLength);
      if (!Float.isNaN(val))
         position = numberParser.getEndPos();
      return val;
   }

   /*
    * Scans for a comma-whitespace sequence with a float following it.
    * If found, the float is returned. Otherwise null is returned and
    * the scan position left as it was.
    */
   float  possibleNextFloat()
   {
      skipCommaWhitespace();
      float  val = numberParser.parseNumber(input, position, inputLength);
      if (!Float.isNaN(val))
         position = numberParser.getEndPos();
      return val;
   }

   /*
    * Scans for comma-whitespace sequence with a float following it.
    * But only if the provided 'lastFloat' (representing the last coord
    * scanned was non-null (ie parsed correctly).
    */
   float  checkedNextFloat(float lastRead)
   {
      if (Float.isNaN(lastRead)) {
         return Float.NaN;
      }
      skipCommaWhitespace();
      return nextFloat();
   }

   float  checkedNextFloat(Boolean lastRead)
   {
      if (lastRead == null) {
         return Float.NaN;
      }
      skipCommaWhitespace();
      return nextFloat();
   }

   Integer  nextInteger(boolean withSign)
   {
      IntegerParser  ip = IntegerParser.parseInt(input, position, inputLength, withSign);
      if (ip == null)
         return null;
      position = ip.getEndPos();
      return ip.value();
   }

   /*
    * Returns the char at the current position and advances the pointer.
    */
   Integer  nextChar()
   {
      if (position == inputLength)
         return null;
      return (int) input.charAt(position++);
   }

   Length nextLength()
   {
      float  scalar = nextFloat();
      if (Float.isNaN(scalar))
         return null;
      Unit unit = nextUnit();
      if (unit == null)
         return new Length(scalar, Unit.px);
      else
         return new Length(scalar, unit);
   }

   /*
    * Scan for a 'flag'. A flag is a '0' or '1' digit character.
    */
   Boolean  nextFlag()
   {
      if (position == inputLength)
         return null;
      char  ch = input.charAt(position);
      if (ch == '0' || ch == '1') {
         position++;
         return (ch == '1');
      }
      return null;
   }

   /*
    * Like checkedNextFloat, but reads a flag (see path definition parser)
    */
   Boolean  checkedNextFlag(Object lastRead)
   {
      if (lastRead == null) {
         return null;
      }
      skipCommaWhitespace();
      return nextFlag();
   }

   public boolean  consume(char ch)
   {
      boolean  found = (position < inputLength && input.charAt(position) == ch);
      if (found)
         position++;
      return found;
   }


   public boolean  consume(String str)
   {
      int  len = str.length();
      boolean  found = (position <= (inputLength - len) && input.substring(position,position+len).equals(str));
      if (found)
         position += len;
      return found;
   }


   /*
    * Skip the current char and peek at the char in the following position.
    */
   int  advanceChar()
   {
      if (position == inputLength)
         return -1;
      position++;
      if (position < inputLength)
         return input.charAt(position);
      else
         return -1;
   }


   /*
    * Scans the input starting immediately at 'position' for the next token.
    * A token is a sequence of characters terminating at a whitespace character.
    * Note that this routine only checks for whitespace characters.  Use nextToken(char)
    * if token might end with another character.
    */
   public String  nextToken()
   {
      return nextToken(' ', false);
   }

   /*
    * Scans the input starting immediately at 'position' for the next token.
    * A token is a sequence of characters terminating at either a whitespace character
    * or the supplied terminating character.
    */
   public String  nextToken(char terminator)
   {
      return nextToken(terminator, false);
   }

   /*
    * Scans the input starting immediately at 'position' for the next token.
    * A token is a sequence of characters terminating at either a the supplied terminating
    * character.  Whitespaces are allowed.
    */
   String  nextTokenWithWhitespace(char terminator)
   {
      return nextToken(terminator, true);
   }

   /*
    * Scans the input starting immediately at 'position' for the next token.
    * A token is a sequence of characters terminating at either the supplied terminating
    * character, or (optionally) a whitespace character.
    */
   String  nextToken(char terminator, boolean allowWhitespace)
   {
      if (empty())
         return null;

      int  ch = input.charAt(position);
      if ((!allowWhitespace && isWhitespace(ch)) || ch == terminator)
         return null;

      int  start = position;
      ch = advanceChar();
      while (ch != -1) {
         if (ch == terminator)
            break;
         if (!allowWhitespace && isWhitespace(ch))
            break;
         ch = advanceChar();
      }
      return input.substring(start, position);
   }


   /*
    * Scans the input starting immediately at 'position' looking for a continuous
    * sequence of ASCII letters. Terminates at any non-letter.
    */
   public String  nextWord()
   {
      if (empty())
         return null;
      int  start = position;

      int  ch = input.charAt(position);
      if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))
      {
         ch = advanceChar();
         while ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))
            ch = advanceChar();
         return input.substring(start, position);
      }
      position = start;
      return null;
   }


   /*
    * Scans the input starting immediately at 'position' for the a sequence
    * of letter characters terminated by an open bracket.  The function
    * name is returned.
    */
   String  nextFunction()
   {
      if (empty())
         return null;
      int  start = position;

      int  ch = input.charAt(position);
      while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))
         ch = advanceChar();
      int end = position;
      while (isWhitespace(ch))
         ch = advanceChar();
      if (ch == '(') {
         position++;
         return input.substring(start, end);
      }
      position = start;
      return null;
   }

   /*
    * Get the next few chars. Mainly used for error messages.
    */
   String  ahead()
   {
      int start = position;
      while (!empty() && !isWhitespace(input.charAt(position)))
         position++;
      String  str = input.substring(start, position);
      position = start;
      return str;
   }

   Unit nextUnit()
   {
      if (empty())
         return null;
      int  ch = input.charAt(position);
      if (ch == '%') {
         position++;
         return Unit.percent;
      }
      if (position > (inputLength - 2))
         return null;
      try {
         Unit result = Unit.valueOf(input.substring(position, position + 2).toLowerCase(Locale.US));
         position +=2;
         return result;
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   /*
    * Check whether the next character is a letter.
    */
   boolean  hasLetter()
   {
      if (position == inputLength)
         return false;
      char  ch = input.charAt(position);
      return ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'));
   }

   /*
    * Extract a quoted string from the input.
    */
   public String  nextQuotedString()
   {
      if (empty())
         return null;
      int  start = position;
      int  ch = input.charAt(position);
      int  endQuote = ch;
      if (ch != '\'' && ch!='"')
         return null;
      ch = advanceChar();
      while (ch != -1 && ch != endQuote)
         ch = advanceChar();
      if (ch == -1) {
         position = start;
         return null;
      }
      position++;
      return input.substring(start+1, position-1);
   }

   /*
    * Return the remaining input as a string.
    */
   String  restOfText()
   {
      if (empty())
         return null;

      int  start = position;
      position = inputLength;
      return input.substring(start);
   }

}


