package com.ismartcoding.lib.androidsvg.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Keeps a list of font feature settings and their values.
 */
public class CSSFontFeatureSettings
{
   public static final CSSFontFeatureSettings  FONT_FEATURE_SETTINGS_NORMAL = makeDefaultSettings();
   public static final CSSFontFeatureSettings  ERROR = new CSSFontFeatureSettings((HashMap<String, Integer>) null);

   static final String  FONT_VARIANT_NORMAL = "normal";
   private static final String  FONT_VARIANT_AUTO = "auto";
   private static final String  FONT_VARIANT_NONE = "none";

   private static final String  FEATURE_ON  = "on";
   private static final String  FEATURE_OFF = "off";

   private static final int  VALUE_ON  = 1;
   private static final int  VALUE_OFF = 0;

   private static final String  TOKEN_ERROR = "ERR";

   // For font-kerning
   public static final String FEATURE_KERN = "kern";

   // For font-variant-ligatures
   static CSSFontFeatureSettings  LIGATURES_NORMAL = null;
   private static CSSFontFeatureSettings LIGATURES_ALL_OFF = null;

   private static final String  FONT_VARIANT_COMMON_LIGATURES = "common-ligatures";
   private static final String  FONT_VARIANT_NO_COMMON_LIGATURES = "no-common-ligatures";
   private static final String  FONT_VARIANT_DISCRETIONARY_LIGATURES = "discretionary-ligatures";
   private static final String  FONT_VARIANT_NO_DISCRETIONARY_LIGATURES = "no-discretionary-ligatures";
   private static final String  FONT_VARIANT_HISTORICAL_LIGATURES = "historical-ligatures";
   private static final String  FONT_VARIANT_NO_HISTORICAL_LIGATURES = "no-historical-ligatures";
   private static final String  FONT_VARIANT_CONTEXTUAL_LIGATURES = "contextual";
   private static final String  FONT_VARIANT_NO_CONTEXTUAL_LIGATURES = "no-contextual";

   public static final String FEATURE_CLIG = "clig";
   public static final String FEATURE_LIGA = "liga";
   public static final String FEATURE_DLIG = "dlig";
   public static final String FEATURE_HLIG = "hlig";
   public static final String FEATURE_CALT = "calt";

   // For font-variant-position
   static CSSFontFeatureSettings  POSITION_ALL_OFF = null;

   private static final String  FONT_VARIANT_SUB = "sub";
   private static final String  FONT_VARIANT_SUPER = "super";

   private static final String  FEATURE_SUBS = "subs";
   private static final String  FEATURE_SUPS = "sups";

   // For font-variant-caps
   static CSSFontFeatureSettings          CAPS_ALL_OFF = null;
   private static CSSFontFeatureSettings  CAPS_SMALL_CAPS = null;

   static final String  FONT_VARIANT_SMALL_CAPS = "small-caps";
   private static final String  FONT_VARIANT_ALL_SMALL_CAPS = "all-small-caps";
   private static final String  FONT_VARIANT_PETITE_CAPS = "petite-caps";
   private static final String  FONT_VARIANT_ALL_PETITE_CAPS = "all-petite-caps";
   private static final String  FONT_VARIANT_UNICASE = "unicase";
   private static final String  FONT_VARIANT_TITLING_CAPS = "titling-caps";

   private static final String  FEATURE_SMCP = "smcp";
   private static final String  FEATURE_C2SC = "c2sc";
   private static final String  FEATURE_PCAP = "pcap";
   private static final String  FEATURE_C2PC = "c2pc";
   private static final String  FEATURE_UNIC = "unic";
   private static final String  FEATURE_TITL = "titl";

   // For font-variant-numeric
   static CSSFontFeatureSettings  NUMERIC_ALL_OFF = null;

   private static final String  FONT_VARIANT_LINING_NUMS = "lining-nums";
   private static final String  FONT_VARIANT_OLDSTYLE_NUMS = "oldstyle-nums";
   private static final String  FONT_VARIANT_PROPORTIONAL_NUMS = "proportional-nums";
   private static final String  FONT_VARIANT_TABULAR_NUMS = "tabular-nums";
   private static final String  FONT_VARIANT_DIAGONAL_FRACTIONS = "diagonal-fractions";
   private static final String  FONT_VARIANT_STACKED_FRACTIONS = "stacked-fractions";
   private static final String  FONT_VARIANT_ORDINAL = "ordinal";
   private static final String  FONT_VARIANT_SLASHED_ZERO = "slashed-zero";

   public static final String FEATURE_LNUM = "lnum";
   public static final String FEATURE_ONUM = "onum";
   public static final String FEATURE_PNUM = "pnum";
   public static final String FEATURE_TNUM = "tnum";
   public static final String FEATURE_FRAC = "frac";
   public static final String FEATURE_AFRC = "afrc";
   public static final String FEATURE_ORDN = "ordn";
   public static final String FEATURE_ZERO = "zero";

   // For font-variant-east-asian
   static CSSFontFeatureSettings  EAST_ASIAN_ALL_OFF = null;

   private static final String  FONT_VARIANT_JIS78 = "jis78";
   private static final String  FONT_VARIANT_JIS83 = "jis83";
   private static final String  FONT_VARIANT_JIS90 = "jis90";
   private static final String  FONT_VARIANT_JIS04 = "jis04";
   private static final String  FONT_VARIANT_SIMPLIFIED = "simplified";
   private static final String  FONT_VARIANT_TRADITIONAL = "traditional";
   private static final String  FONT_VARIANT_FULL_WIDTH = "full-width";
   private static final String  FONT_VARIANT_PROPORTIONAL_WIDTH = "proportional-width";
   private static final String  FONT_VARIANT_RUBY = "ruby";

   public static final String FEATURE_JP78 = "jp78";
   public static final String FEATURE_JP83 = "jp83";
   public static final String FEATURE_JP90 = "jp90";
   public static final String FEATURE_JP04 = "jp04";
   public static final String FEATURE_SMPL = "smpl";
   public static final String FEATURE_TRAD = "trad";
   public static final String FEATURE_FWID = "fwid";
   public static final String FEATURE_PWID = "pwid";
   public static final String FEATURE_RUBY = "ruby";


   private final HashMap<String, Integer>  settings;


   private static class FontFeatureEntry {
      String  name;
      int     val;

      public FontFeatureEntry(String name, int val) {
         this.name = name;
         this.val = val;
      }
   }


   static {
      LIGATURES_NORMAL = new CSSFontFeatureSettings();
      LIGATURES_NORMAL.settings.put(FEATURE_LIGA, VALUE_ON);
      LIGATURES_NORMAL.settings.put(FEATURE_CLIG, VALUE_ON);
      LIGATURES_NORMAL.settings.put(FEATURE_DLIG, VALUE_OFF);
      LIGATURES_NORMAL.settings.put(FEATURE_HLIG, VALUE_OFF);
      LIGATURES_NORMAL.settings.put(FEATURE_CALT, VALUE_ON);

      POSITION_ALL_OFF = new CSSFontFeatureSettings();
      POSITION_ALL_OFF.settings.put(FEATURE_SUBS, VALUE_OFF);
      POSITION_ALL_OFF.settings.put(FEATURE_SUPS, VALUE_OFF);

      CAPS_ALL_OFF = new CSSFontFeatureSettings();
      CAPS_ALL_OFF.settings.put(FEATURE_SMCP, VALUE_OFF);
      CAPS_ALL_OFF.settings.put(FEATURE_C2SC, VALUE_OFF);
      CAPS_ALL_OFF.settings.put(FEATURE_PCAP, VALUE_OFF);
      CAPS_ALL_OFF.settings.put(FEATURE_C2PC, VALUE_OFF);
      CAPS_ALL_OFF.settings.put(FEATURE_UNIC, VALUE_OFF);
      CAPS_ALL_OFF.settings.put(FEATURE_TITL, VALUE_OFF);

      NUMERIC_ALL_OFF = new CSSFontFeatureSettings();
      NUMERIC_ALL_OFF.settings.put(FEATURE_LNUM, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_ONUM, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_PNUM, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_TNUM, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_FRAC, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_AFRC, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_ORDN, VALUE_OFF);
      NUMERIC_ALL_OFF.settings.put(FEATURE_ZERO, VALUE_OFF);

      EAST_ASIAN_ALL_OFF = new CSSFontFeatureSettings();
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_JP78, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_JP83, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_JP90, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_JP04, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_SMPL, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_TRAD, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_FWID, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_PWID, VALUE_OFF);
      EAST_ASIAN_ALL_OFF.settings.put(FEATURE_RUBY, VALUE_OFF);
   }


   public CSSFontFeatureSettings()
   {
      this.settings = new HashMap<>();
   }

   private CSSFontFeatureSettings(HashMap<String, Integer> initialMap)
   {
      this.settings = initialMap;
   }

   public CSSFontFeatureSettings(CSSFontFeatureSettings other)
   {
      this.settings = new HashMap<>(other.settings);
   }


   public void  applySettings(CSSFontFeatureSettings featureSettings)
   {
      if (featureSettings == null)
         return;
      this.settings.putAll(featureSettings.settings);
   }


   public void  applyKerning(Style.FontKerning kern)
   {
      if (kern == Style.FontKerning.none )
         this.settings.put(FEATURE_KERN, VALUE_OFF);
      else
         this.settings.put(FEATURE_KERN, VALUE_ON);
   }


   public boolean  hasSettings()
   {
      return this.settings.size() > 0;
   }


   @Override
   public String toString()
   {
      StringBuilder  sb = new StringBuilder();
      for (Map.Entry<String, Integer> entry: this.settings.entrySet()) {
         if (sb.length() > 0)
            sb.append(',');
         sb.append("'");
         sb.append(entry.getKey());
         sb.append("' ");
         sb.append(entry.getValue());
      }
      return sb.toString();
   }


   //-----------------------------------------------------------------------------------------------
   // Parsing font-feature-settings property value


   /*
    * Parse the value of the CSS property "font-feature-settings".
    *
    * Format is: <feature-tag-value>[comma-wsp <feature-tag-value>]*
    *            <feature-tag-value> = <string> [ <integer> | on | off ]?
    */
   static CSSFontFeatureSettings  parseFontFeatureSettings(String val)
   {
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings();

      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();

      while (true) {
         if (scan.empty())
            break;
         FontFeatureEntry  entry = nextFeatureEntry(scan);
         if (entry == null)
            return null;
         result.settings.put(entry.name, entry.val);
         scan.skipCommaWhitespace();
      }
      return result;
   }


   private static FontFeatureEntry  nextFeatureEntry(TextScanner scan)
   {
      scan.skipWhitespace();
      String name = scan.nextQuotedString();
      if (name == null || name.length() != 4)
         return null;
      scan.skipWhitespace();
      int  val = 1;
      if (!scan.empty()) {
         Integer  num = scan.nextInteger(false);
         if (num == null) {
            if (scan.consume(FEATURE_OFF))
               val = 0;
            else
               scan.consume(FEATURE_ON);  // "on" == 1 == default, so consume quietly if it is present
         } else if (val > 99) {
            return null;
         } else {
            val = num;
         }
      }
      return new FontFeatureEntry(name, val);
   }


   //-----------------------------------------------------------------------------------------------


   // Parse a font-kerning keyword
   static Style.FontKerning  parseFontKerning(String val)
   {
      switch (val)
      {
         case FONT_VARIANT_AUTO:   return Style.FontKerning.auto;
         case FONT_VARIANT_NORMAL: return Style.FontKerning.normal;
         case FONT_VARIANT_NONE:   return Style.FontKerning.none;
         default:                  return null;
      }
   }


   private static List<String>  extractTokensAsList(String val)
   {
      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();
      if (scan.empty())
         return null;
      ArrayList<String>  result = new ArrayList<>();
      while (!scan.empty())
      {
         result.add(scan.nextToken());
         scan.skipWhitespace();
      }
      return result;
   }


   /*
    * Returns:
    *   1 if token list contains token1,
    *   2 if it contains token2,
    *   3 if it contains both, or more than one of either,
    *   0 if it contains neither.
    */
   private static int  containsWhich(List<String> tokens, String token1, String token2)
   {
      if (tokens.remove(token1)) {
         return tokens.contains(token1) || tokens.contains(token2) ? 3 : 1;
      } else if (tokens.remove(token2)) {
         return tokens.contains(token2) ? 3 : 2;
      }
      return 0;
   }


   /*
    * Returns:
    *   1 if token list contains token1,
    *   2 if it contains more than one token1,
    *   0 if it doesn't contains token1.
    */
   private static int  containsOnce(List<String> tokens, String token1)
   {
      if (tokens.remove(token1)) {
         return tokens.contains(token1) ? 2 : 1;
      }
      return 0;
   }


   /*
    * Checks haystack to see which needle is present (if any).  Returns the needle.
    * If there is more than one of the needles present, then returns null.
    */
   private static String  containsOneOf(List<String> haystack, String... needles)
   {
      String found = null;
      for (String needle: needles)
      {
         if (found == null && haystack.remove(needle)) {
            found = needle;
         }
         if (haystack.contains(needle))
            return TOKEN_ERROR;
      }
      return found;
   }


   /*
    * Parse a font-variant-ligatures property
    * Format:
    *   normal | none | [ <common-lig-values> || <discretionary-lig-values> || <historical-lig-values> || <contextual-alt-values> ]
    *   <common-lig-values>        = [ common-ligatures | no-common-ligatures ]
    *   <discretionary-lig-values> = [ discretionary-ligatures | no-discretionary-ligatures ]
    *   <historical-lig-values>    = [ historical-ligatures | no-historical-ligatures ]
    *   <contextual-alt-values>    = [ contextual | no-contextual ]
    */
   static CSSFontFeatureSettings  parseVariantLigatures(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return LIGATURES_NORMAL;
      else if (val.equals(FONT_VARIANT_NONE)) {
         ensureLigaturesNone();
         return LIGATURES_ALL_OFF;
      }

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)  // No tokens found
         return null;

      ensureLigaturesNone();
      CSSFontFeatureSettings  result = parseVariantLigaturesSpecial(tokens);

      // If nothing found, or duplicate keywords found, or tokens left over, then we have an error
      if (result == null || result == ERROR || tokens.size() > 0)
         return null;

      return result;
   }


   private static CSSFontFeatureSettings  parseVariantLigaturesSpecial(List<String> tokens)
   {
      ensureLigaturesNone();
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(LIGATURES_ALL_OFF);
      boolean                 found = false;

      switch (containsWhich(tokens, FONT_VARIANT_COMMON_LIGATURES, FONT_VARIANT_NO_COMMON_LIGATURES))
      {
         case 1: result.addSettings(FEATURE_CLIG, FEATURE_LIGA, VALUE_ON); found = true; break;
         case 2: result.addSettings(FEATURE_CLIG, FEATURE_LIGA, VALUE_OFF); found = true; break;
         case 3: return ERROR;
      }

      switch (containsWhich(tokens, FONT_VARIANT_DISCRETIONARY_LIGATURES, FONT_VARIANT_NO_DISCRETIONARY_LIGATURES))
      {
         case 1: result.settings.put(FEATURE_DLIG, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_DLIG, VALUE_OFF); found = true; break;
         case 3: return ERROR;
      }

      switch (containsWhich(tokens, FONT_VARIANT_HISTORICAL_LIGATURES, FONT_VARIANT_NO_HISTORICAL_LIGATURES))
      {
         case 1: result.settings.put(FEATURE_HLIG, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_HLIG, VALUE_OFF); found = true; break;
         case 3: return ERROR;
      }

      switch (containsWhich(tokens, FONT_VARIANT_CONTEXTUAL_LIGATURES, FONT_VARIANT_NO_CONTEXTUAL_LIGATURES))
      {
         case 1: result.settings.put(FEATURE_CALT, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_CALT, VALUE_OFF); found = true; break;
         case 3: return ERROR;
      }
      return found ? result : null;
   }


   private void  addSettings(String feature1, String feature2, int onOrOff)
   {
      this.settings.put(feature1, onOrOff);
      this.settings.put(feature2, onOrOff);
   }


   // Parse a font-kerning property
   static CSSFontFeatureSettings  parseVariantPosition(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return POSITION_ALL_OFF;

      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(POSITION_ALL_OFF);
      switch (val)
      {
         case FONT_VARIANT_SUB:    result.settings.put(FEATURE_SUBS, VALUE_ON); break;
         case FONT_VARIANT_SUPER:  result.settings.put(FEATURE_SUPS, VALUE_ON); break;
         default:                  return null;
      }
      return result;
   }


   // Used only by parseFontVariant()
   // Only looks for the values unique to this property
   private static CSSFontFeatureSettings  parseVariantPositionSpecial(List<String> tokens)
   {
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(POSITION_ALL_OFF);
      boolean                 found = false;

      switch (containsWhich(tokens, FONT_VARIANT_SUB, FONT_VARIANT_SUPER))
      {
         case 1: result.settings.put(FEATURE_SUBS, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_SUPS, VALUE_ON); found = true; break;
         case 3: return ERROR;
      }
      return found ? result : null;
   }


   // Parse a font-variant-caps property
   static CSSFontFeatureSettings  parseVariantCaps(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return CAPS_ALL_OFF;

      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(CAPS_ALL_OFF);
      return setCapsFeature(result, val) ? result : null;
   }

   private static boolean  setCapsFeature(CSSFontFeatureSettings result, String val)
   {
      switch (val)
      {
         case FONT_VARIANT_SMALL_CAPS:      result.settings.put(FEATURE_SMCP, VALUE_ON); break;
         case FONT_VARIANT_ALL_SMALL_CAPS:  result.addSettings(FEATURE_SMCP, FEATURE_C2SC, VALUE_ON); break;
         case FONT_VARIANT_PETITE_CAPS:     result.settings.put(FEATURE_PCAP, VALUE_ON); break;
         case FONT_VARIANT_ALL_PETITE_CAPS: result.addSettings(FEATURE_PCAP, FEATURE_C2PC, VALUE_ON); break;
         case FONT_VARIANT_UNICASE:         result.settings.put(FEATURE_UNIC, VALUE_ON); break;
         case FONT_VARIANT_TITLING_CAPS:    result.settings.put(FEATURE_TITL, VALUE_ON); break;
         default:                           return false;
      }
      return true;
   }


   // Used only by parseFontVariant()
   // Only looks for the values unique to this property
   private static CSSFontFeatureSettings  parseVariantCapsSpecial(List<String> tokens)
   {
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(CAPS_ALL_OFF);

      String which = containsOneOf(tokens, FONT_VARIANT_SMALL_CAPS, FONT_VARIANT_ALL_SMALL_CAPS, FONT_VARIANT_PETITE_CAPS,
                                           FONT_VARIANT_ALL_PETITE_CAPS, FONT_VARIANT_UNICASE, FONT_VARIANT_TITLING_CAPS);
      if (which == TOKEN_ERROR)
         return ERROR;
      if (which == null)
         return null;

      setCapsFeature(result, which);
      return result;
   }


   /*
    * Parse a font-variant-numeric property
    * Format:
    *   normal | [ <numeric-figure-values> || <numeric-spacing-values> || <numeric-fraction-values> || ordinal || slashed-zero ]
    *   <numeric-figure-values>   = [ lining-nums | oldstyle-nums ]
    *   <numeric-spacing-values>  = [ proportional-nums | tabular-nums ]
    *   <numeric-fraction-values> = [ diagonal-fractions | stacked-fractions ]
    */
   static CSSFontFeatureSettings  parseVariantNumeric(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return NUMERIC_ALL_OFF;

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)
         return null;

      CSSFontFeatureSettings  result = parseVariantNumericSpecial(tokens);

      // If nothing found, or duplicate keywords found, or tokens left over, then we have an error
      if (result == null || result == ERROR || tokens.size() > 0)
         return null;

      return result;
   }


   private static CSSFontFeatureSettings  parseVariantNumericSpecial(List<String> tokens)
   {
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(NUMERIC_ALL_OFF);
      boolean                 found = false;

      switch (containsWhich(tokens, FONT_VARIANT_LINING_NUMS, FONT_VARIANT_OLDSTYLE_NUMS))
      {
         case 1: result.settings.put(FEATURE_LNUM, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_ONUM, VALUE_ON); found = true; break;
         case 3: return ERROR;
      }

      switch (containsWhich(tokens, FONT_VARIANT_PROPORTIONAL_NUMS, FONT_VARIANT_TABULAR_NUMS))
      {
         case 1: result.settings.put(FEATURE_PNUM, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_TNUM, VALUE_ON); found = true; break;
         case 3: return ERROR;
      }

      switch (containsWhich(tokens, FONT_VARIANT_DIAGONAL_FRACTIONS, FONT_VARIANT_STACKED_FRACTIONS))
      {
         case 1: result.settings.put(FEATURE_FRAC, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_AFRC, VALUE_ON); found = true; break;
         case 3: return ERROR;
      }

      switch (containsOnce(tokens, FONT_VARIANT_ORDINAL))
      {
         case 1: result.settings.put(FEATURE_ORDN, VALUE_ON); found = true; break;
         case 2: return ERROR;
      }

      switch (containsOnce(tokens, FONT_VARIANT_SLASHED_ZERO))
      {
         case 1: result.settings.put(FEATURE_ZERO, VALUE_ON); found = true; break;
         case 2: return ERROR;
      }

      return found ? result : null;
   }


   /*
    * Parse a font-variant-east-asian property
    * Format:
    *   normal | [ <east-asian-variant-values> || <east-asian-width-values> || ruby ]
    *   <east-asian-variant-values> = [ jis78 | jis83 | jis90 | jis04 | simplified | traditional ]
    *   <east-asian-width-values>   = [ full-width | proportional-width ]
    */
   static CSSFontFeatureSettings  parseEastAsian(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
          return EAST_ASIAN_ALL_OFF;

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)
         return null;

      CSSFontFeatureSettings  result = parseVariantEastAsianSpecial(tokens);

      // If nothing found, or duplicate keywords found, or tokens left over, then we have an error
      if (result == null || result == ERROR || tokens.size() > 0)
         return null;

      return result;
   }


   private static CSSFontFeatureSettings  parseVariantEastAsianSpecial(List<String> tokens)
   {
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(EAST_ASIAN_ALL_OFF);
      boolean                 found = false;

      String which = containsOneOf(tokens, FONT_VARIANT_JIS78, FONT_VARIANT_JIS83, FONT_VARIANT_JIS90,
                                           FONT_VARIANT_JIS04, FONT_VARIANT_SIMPLIFIED, FONT_VARIANT_TRADITIONAL);
      if (which != null)
      {
         switch (which)
         {
            case FONT_VARIANT_JIS78:       result.settings.put(FEATURE_JP78, VALUE_ON); break;
            case FONT_VARIANT_JIS83:       result.settings.put(FEATURE_JP83, VALUE_ON); break;
            case FONT_VARIANT_JIS90:       result.settings.put(FEATURE_JP90, VALUE_ON); break;
            case FONT_VARIANT_JIS04:       result.settings.put(FEATURE_JP04, VALUE_ON); break;
            case FONT_VARIANT_SIMPLIFIED:  result.settings.put(FEATURE_SMPL, VALUE_ON); break;
            case FONT_VARIANT_TRADITIONAL: result.settings.put(FEATURE_TRAD, VALUE_ON); break;
            case TOKEN_ERROR:              return ERROR;  // more than one, or duplicate, found
         }
         found = true;
      }

      switch (containsWhich(tokens, FONT_VARIANT_FULL_WIDTH, FONT_VARIANT_PROPORTIONAL_WIDTH))
      {
         case 1: result.settings.put(FEATURE_FWID, VALUE_ON); found = true; break;
         case 2: result.settings.put(FEATURE_PWID, VALUE_ON); found = true; break;
         case 3: return ERROR;
      }

      switch (containsOnce(tokens, FONT_VARIANT_RUBY))
      {
         case 1: result.settings.put(FEATURE_RUBY, VALUE_ON); found = true; break;
         case 2: return ERROR;
      }

      return found ? result : null;
   }


   //-----------------------------------------------------------------------------------------------


   static void parseFontVariant(Style style, String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
      {
         style.fontVariantLigatures = LIGATURES_NORMAL;
         style.fontVariantPosition = POSITION_ALL_OFF;
         style.fontVariantCaps = CAPS_ALL_OFF;
         style.fontVariantNumeric = NUMERIC_ALL_OFF;
         style.fontVariantEastAsian = EAST_ASIAN_ALL_OFF;
         style.specifiedFlags |= (Style.SPECIFIED_FONT_VARIANT_LIGATURES | Style.SPECIFIED_FONT_VARIANT_POSITION |
                                  Style.SPECIFIED_FONT_VARIANT_CAPS | Style.SPECIFIED_FONT_VARIANT_NUMERIC |
                                  Style.SPECIFIED_FONT_VARIANT_EAST_ASIAN);
         return;
      }
      else if (val.equals(FONT_VARIANT_NONE))
      {
         ensureLigaturesNone();
         style.fontVariantLigatures = LIGATURES_ALL_OFF;
         style.fontVariantPosition = POSITION_ALL_OFF;
         style.fontVariantCaps = CAPS_ALL_OFF;
         style.fontVariantNumeric = NUMERIC_ALL_OFF;
         style.fontVariantEastAsian = EAST_ASIAN_ALL_OFF;
         style.specifiedFlags |= (Style.SPECIFIED_FONT_VARIANT_LIGATURES | Style.SPECIFIED_FONT_VARIANT_POSITION |
                                  Style.SPECIFIED_FONT_VARIANT_CAPS | Style.SPECIFIED_FONT_VARIANT_NUMERIC |
                                  Style.SPECIFIED_FONT_VARIANT_EAST_ASIAN);
         return;
      }

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)
         return;

      CSSFontFeatureSettings  ligatures = parseVariantLigaturesSpecial(tokens);
      if (ligatures == ERROR)
         return;

      CSSFontFeatureSettings  position = null;
      if (tokens.size() > 0) {
         position = parseVariantPositionSpecial(tokens);
         if (position == ERROR)
            return;
      }

      CSSFontFeatureSettings  caps = null;
      if (tokens.size() > 0) {
         caps = parseVariantCapsSpecial(tokens);
         if (caps == ERROR)
            return;
      }

      CSSFontFeatureSettings  numeric = null;
      if (tokens.size() > 0) {
         numeric = parseVariantNumericSpecial(tokens);
         if (numeric == ERROR)
            return;
      }

      CSSFontFeatureSettings  eastAsian = null;
      if (tokens.size() > 0) {
         eastAsian = parseVariantEastAsianSpecial(tokens);
         if (eastAsian == ERROR)
            return;
      }

      //if (tokens.size() > 0)  // Tokens left over in line?
      // Ignore them, as they may be CSS Fonts 4 keywords, for example.

      // We found some good keywords in this value
      if (ligatures != null) {
         style.fontVariantLigatures = ligatures;
         style.specifiedFlags |= Style.SPECIFIED_FONT_VARIANT_LIGATURES;
      }

      if (position != null) {
         style.fontVariantPosition = position;
         style.specifiedFlags |= Style.SPECIFIED_FONT_VARIANT_POSITION;
      }

      if (caps != null) {
         style.fontVariantCaps = caps;
         style.specifiedFlags |= Style.SPECIFIED_FONT_VARIANT_CAPS;
      }

      if (numeric != null) {
         style.fontVariantNumeric = numeric;
         style.specifiedFlags |= Style.SPECIFIED_FONT_VARIANT_NUMERIC;
      }

      if (eastAsian != null) {
         style.fontVariantEastAsian = eastAsian;
         style.specifiedFlags |= Style.SPECIFIED_FONT_VARIANT_EAST_ASIAN;
      }
   }


   //-----------------------------------------------------------------------------------------------


   private static final CSSFontFeatureSettings  makeDefaultSettings()
   {
      // See: https://www.w3.org/TR/css-fonts-3/#default-features
      CSSFontFeatureSettings result = new CSSFontFeatureSettings();
      result.settings.put("rlig", VALUE_ON);
      result.settings.put("liga", VALUE_ON);
      result.settings.put("clig", VALUE_ON);
      result.settings.put("calt", VALUE_ON);
      result.settings.put("locl", VALUE_ON);
      result.settings.put("ccmp", VALUE_ON);
      result.settings.put("mark", VALUE_ON);
      result.settings.put("mkmk", VALUE_ON);
      // TODO FIXME  also enable "vert" for vertical runs in complex scripts
      return result;
   }


   private static void  ensureLigaturesNone()
   {
      // all ligatures OFF
      if (LIGATURES_ALL_OFF != null)
         return;
      CSSFontFeatureSettings result = new CSSFontFeatureSettings();
      result.settings.put("liga", VALUE_OFF);
      result.settings.put("clig", VALUE_OFF);
      result.settings.put("dlig", VALUE_OFF);
      result.settings.put("hlig", VALUE_OFF);
      result.settings.put("calt", VALUE_OFF);
      LIGATURES_ALL_OFF = result;
   }


   private void  ensurePositionNormal()
   {
      // common and contextual ligatures ON; discretionary  and historical ligatures OFF
      if (POSITION_ALL_OFF == null) {
         CSSFontFeatureSettings result = new CSSFontFeatureSettings();
         result.settings.put(FEATURE_SUBS, VALUE_OFF);
         result.settings.put(FEATURE_SUPS, VALUE_OFF);
         this.POSITION_ALL_OFF = result;
      }
   }


   // Used when parsing the CSS "font" shortcut property
   static CSSFontFeatureSettings  makeSmallCaps()
   {
      if (CAPS_SMALL_CAPS == null) {
         CAPS_SMALL_CAPS = new CSSFontFeatureSettings();
         CAPS_SMALL_CAPS.settings.put(FEATURE_SMCP, VALUE_ON);
         CAPS_SMALL_CAPS.settings.put(FEATURE_C2SC, VALUE_OFF);
         CAPS_SMALL_CAPS.settings.put(FEATURE_PCAP, VALUE_OFF);
         CAPS_SMALL_CAPS.settings.put(FEATURE_C2PC, VALUE_OFF);
         CAPS_SMALL_CAPS.settings.put(FEATURE_UNIC, VALUE_OFF);
         CAPS_SMALL_CAPS.settings.put(FEATURE_TITL, VALUE_OFF);
      }
      return CAPS_SMALL_CAPS;
   }
}
