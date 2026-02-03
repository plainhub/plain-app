package com.ismartcoding.lib.androidsvg.utils;

import android.graphics.Matrix;
import android.util.Log;
import android.util.Xml;

import com.ismartcoding.lib.androidsvg.PreserveAspectRatio;
import com.ismartcoding.lib.androidsvg.SVGExternalFileResolver;
import com.ismartcoding.lib.androidsvg.SVGParseException;
import com.ismartcoding.lib.androidsvg.utils.CSSParser.MediaType;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Box;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.CSSClipRect;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Colour;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.CurrentColor;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.GradientSpread;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Length;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.PaintReference;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgElementBase;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgObject;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgPaint;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TextChild;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TextPositionedContainer;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TextRoot;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Unit;
import com.ismartcoding.lib.androidsvg.utils.Style.Isolation;
import com.ismartcoding.lib.androidsvg.utils.Style.RenderQuality;
import com.ismartcoding.lib.androidsvg.utils.Style.TextDecoration;
import com.ismartcoding.lib.androidsvg.utils.Style.TextDirection;
import com.ismartcoding.lib.androidsvg.utils.Style.VectorEffect;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/*
 * SVG parser code. Used by SVG class. Should not be called directly.
 */

class SVGParserImpl implements SVGParser
{
   private static final String  TAG = "SVGParser";

   private static final String  SVG_NAMESPACE = "http://www.w3.org/2000/svg";
   private static final String  XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
   private static final String  FEATURE_STRING_PREFIX = "http://www.w3.org/TR/SVG11/feature#";

   private static final String  XML_STYLESHEET_PROCESSING_INSTRUCTION = "xml-stylesheet";

   // Versions of Android earlier than 15 (ICS) have an XmlPullParser that doesn't support the
   // nextToken() method. Also, they throw an exception when calling setFeature().
   // So for simplicity, we'll just force the use of the SAX parser on Androids < 15.
   private static final boolean FORCE_SAX_ON_EARLY_ANDROIDS = (android.os.Build.VERSION.SDK_INT < 15);

   private static final Pattern PATTERN_BLOCK_COMMENTS = Pattern.compile("/\\*.*?\\*/");

   // <?xml-stylesheet> attribute names and values
   public static final String  XML_STYLESHEET_ATTR_TYPE = "type";
   public static final String  XML_STYLESHEET_ATTR_ALTERNATE = "alternate";
   public static final String  XML_STYLESHEET_ATTR_HREF = "href";
   public static final String  XML_STYLESHEET_ATTR_MEDIA = "media";
   public static final String  XML_STYLESHEET_ATTR_MEDIA_ALL = "all";
   public static final String  XML_STYLESHEET_ATTR_ALTERNATE_NO = "no";

   // Used by the automatic XML parser switching code.
   // This value defines how much of the SVG file preamble will we keep in order to check for
   // a doctype definition that has internal entities defined.
   public static final int  ENTITY_WATCH_BUFFER_SIZE = 4096;


   // SVG parser
   private SVGBase                  svgDocument = null;
   private SVGBase.SvgContainer     currentElement = null;
   private boolean                  enableInternalEntities = true;
   private SVGExternalFileResolver  externalFileResolver = null;

   // For handling elements we don't support
   private boolean   ignoring = false;
   private int       ignoreDepth;

   // For handling <title> and <desc>
   private boolean        inMetadataElement = false;
   private SVGElem        metadataTag = null;
   private StringBuilder  metadataElementContents = null;

   // For handling <style>
   private boolean        inStyleElement = false;
   private StringBuilder  styleElementContents = null;


   // Define SVG tags
   private enum  SVGElem
   {
      svg,
      a,
      circle,
      clipPath,
      defs,
      desc,
      ellipse,
      g,
      image,
      line,
      linearGradient,
      marker,
      mask,
      path,
      pattern,
      polygon,
      polyline,
      radialGradient,
      rect,
      solidColor,
      stop,
      style,
      SWITCH,
      symbol,
      text,
      textPath,
      title,
      tref,
      tspan,
      use,
      view,
      UNSUPPORTED;

      private static final Map<String, SVGElem> cache = new HashMap<>();

      static {
         for (SVGElem elem : values()) {
            if (elem == SWITCH) {
               cache.put("switch", elem);
            } else if (elem != UNSUPPORTED) {
               final String key = elem.name();
               cache.put(key, elem);
            }
         }
      }

      public static SVGElem fromString(String str) {
         // First check cache to see if it is there
         SVGElem elem = cache.get(str);
         if (elem != null) {
            return elem;
         }

         return UNSUPPORTED;
      }
   }

   // Element types that we don't support. Those that are containers have their
   // contents ignored.
   //private static final String  TAG_ANIMATECOLOR        = "animateColor";
   //private static final String  TAG_ANIMATEMOTION       = "animateMotion";
   //private static final String  TAG_ANIMATETRANSFORM    = "animateTransform";
   //private static final String  TAG_ALTGLYPH            = "altGlyph";
   //private static final String  TAG_ALTGLYPHDEF         = "altGlyphDef";
   //private static final String  TAG_ALTGLYPHITEM        = "altGlyphItem";
   //private static final String  TAG_ANIMATE             = "animate";
   //private static final String  TAG_COLORPROFILE        = "color-profile";
   //private static final String  TAG_CURSOR              = "cursor";
   //private static final String  TAG_FEBLEND             = "feBlend";
   //private static final String  TAG_FECOLORMATRIX       = "feColorMatrix";
   //private static final String  TAG_FECOMPONENTTRANSFER = "feComponentTransfer";
   //private static final String  TAG_FECOMPOSITE         = "feComposite";
   //private static final String  TAG_FECONVOLVEMATRIX    = "feConvolveMatrix";
   //private static final String  TAG_FEDIFFUSELIGHTING   = "feDiffuseLighting";
   //private static final String  TAG_FEDISPLACEMENTMAP   = "feDisplacementMap";
   //private static final String  TAG_FEDISTANTLIGHT      = "feDistantLight";
   //private static final String  TAG_FEFLOOD             = "feFlood";
   //private static final String  TAG_FEFUNCA             = "feFuncA";
   //private static final String  TAG_FEFUNCB             = "feFuncB";
   //private static final String  TAG_FEFUNCG             = "feFuncG";
   //private static final String  TAG_FEFUNCR             = "feFuncR";
   //private static final String  TAG_FEGAUSSIANBLUR      = "feGaussianBlur";
   //private static final String  TAG_FEIMAGE             = "feImage";
   //private static final String  TAG_FEMERGE             = "feMerge";
   //private static final String  TAG_FEMERGENODE         = "feMergeNode";
   //private static final String  TAG_FEMORPHOLOGY        = "feMorphology";
   //private static final String  TAG_FEOFFSET            = "feOffset";
   //private static final String  TAG_FEPOINTLIGHT        = "fePointLight";
   //private static final String  TAG_FESPECULARLIGHTING  = "feSpecularLighting";
   //private static final String  TAG_FESPOTLIGHT         = "feSpotLight";
   //private static final String  TAG_FETILE              = "feTile";
   //private static final String  TAG_FETURBULENCE        = "feTurbulence";
   //private static final String  TAG_FILTER              = "filter";
   //private static final String  TAG_FONT                = "font";
   //private static final String  TAG_FONTFACE            = "font-face";
   //private static final String  TAG_FONTFACEFORMAT      = "font-face-format";
   //private static final String  TAG_FONTFACENAME        = "font-face-name";
   //private static final String  TAG_FONTFACESRC         = "font-face-src";
   //private static final String  TAG_FONTFACEURI         = "font-face-uri";
   //private static final String  TAG_FOREIGNOBJECT       = "foreignObject";
   //private static final String  TAG_GLYPH               = "glyph";
   //private static final String  TAG_GLYPHREF            = "glyphRef";
   //private static final String  TAG_HKERN               = "hkern";
   //private static final String  TAG_MASK                = "mask";
   //private static final String  TAG_METADATA            = "metadata";
   //private static final String  TAG_MISSINGGLYPH        = "missing-glyph";
   //private static final String  TAG_MPATH               = "mpath";
   //private static final String  TAG_SCRIPT              = "script";
   //private static final String  TAG_SET                 = "set";
   //private static final String  TAG_STYLE               = "style";
   //private static final String  TAG_VKERN               = "vkern";


   // Supported SVG attributes
   enum  SVGAttr
   {
      CLASS,    // Upper case because 'class' is a reserved word. Handled as a special case.
      clip,
      clip_path,
      clipPathUnits,
      clip_rule,
      color,
      cx, cy,
      direction,
      dx, dy,
      fx, fy, fr,
      d,
      display,
      fill,
      fill_rule,
      fill_opacity,
      font,
      font_family,
      font_feature_settings,
      font_size,
      font_stretch,                // @since 1.5
      font_style,
      font_weight,
      // font_size_adjust, font_stretch,
      font_kerning,                // @since 1.5
      font_variant,                // @since 1.5
      font_variant_ligatures,      // @since 1.5
      font_variant_position,       // @since 1.5
      font_variant_caps,           // @since 1.5
      font_variant_numeric,        // @since 1.5
      font_variant_east_asian,     // @since 1.5
      font_variation_settings,     // @since 1.5
      glyph_orientation_vertical,  // @since 1.5
      gradientTransform,
      gradientUnits,
      height,
      href,
      // id,
      image_rendering,
      isolation,       // @since 1.5
      letter_spacing,  // @since 1.5
      marker,
      marker_start, marker_mid, marker_end,
      markerHeight, markerUnits, markerWidth,
      mask,
      maskContentUnits, maskUnits,
      media,
      mix_blend_mode,  // @since 1.5
      offset,
      opacity,
      orient,
      overflow,
      pathLength,
      patternContentUnits, patternTransform, patternUnits,
      points,
      preserveAspectRatio,
      r,
      refX,
      refY,
      requiredFeatures, requiredExtensions, requiredFormats, requiredFonts,
      rx, ry,
      solid_color, solid_opacity,
      spreadMethod,
      startOffset,
      stop_color, stop_opacity,
      stroke,
      stroke_dasharray,
      stroke_dashoffset,
      stroke_linecap,
      stroke_linejoin,
      stroke_miterlimit,
      stroke_opacity,
      stroke_width,
      style,
      systemLanguage,
      text_anchor,
      text_decoration,
      text_orientation,  // @since 1.5
      transform,
      type,
      vector_effect,
      version,
      viewBox,
      width,
      word_spacing,  // @since 1.5
      writing_mode,  // @since 1.5
      x, y,
      x1, y1,
      x2, y2,
      viewport_fill, viewport_fill_opacity,
      visibility,
      UNSUPPORTED;

      private static final Map<String, SVGAttr> cache = new HashMap<>();

      static {
         for (SVGAttr attr : values()) {
            if (attr == CLASS) {
               cache.put("class", attr);
            } else if (attr != UNSUPPORTED) {
               final String key = attr.name().replace('_', '-');
               cache.put(key, attr);
            }
         }
      }

      public static SVGAttr fromString(String str)
      {
         // First check cache to see if it is there
         SVGAttr attr = cache.get(str);
         if (attr != null) {
            return attr;
         }

         return UNSUPPORTED;
      }
   }


   // Special attribute keywords
   static final String  NONE = "none";
   static final String  CURRENTCOLOR = "currentColor";

   static final String VALID_DISPLAY_VALUES = "|inline|block|list-item|run-in|compact|marker|table|inline-table"+
                                              "|table-row-group|table-header-group|table-footer-group|table-row"+
                                              "|table-column-group|table-column|table-cell|table-caption|none|";
   static final String VALID_VISIBILITY_VALUES = "|visible|hidden|collapse|";

   // These static inner classes are only loaded/initialized when first used and are thread safe
   private static class ColourKeywords {
      private static final Map<String, Integer> colourKeywords = new HashMap<>(47);
      static {
         colourKeywords.put("aliceblue", 0xfff0f8ff);
         colourKeywords.put("antiquewhite", 0xfffaebd7);
         colourKeywords.put("aqua", 0xff00ffff);
         colourKeywords.put("aquamarine", 0xff7fffd4);
         colourKeywords.put("azure", 0xfff0ffff);
         colourKeywords.put("beige", 0xfff5f5dc);
         colourKeywords.put("bisque", 0xffffe4c4);
         colourKeywords.put("black", 0xff000000);
         colourKeywords.put("blanchedalmond", 0xffffebcd);
         colourKeywords.put("blue", 0xff0000ff);
         colourKeywords.put("blueviolet", 0xff8a2be2);
         colourKeywords.put("brown", 0xffa52a2a);
         colourKeywords.put("burlywood", 0xffdeb887);
         colourKeywords.put("cadetblue", 0xff5f9ea0);
         colourKeywords.put("chartreuse", 0xff7fff00);
         colourKeywords.put("chocolate", 0xffd2691e);
         colourKeywords.put("coral", 0xffff7f50);
         colourKeywords.put("cornflowerblue", 0xff6495ed);
         colourKeywords.put("cornsilk", 0xfffff8dc);
         colourKeywords.put("crimson", 0xffdc143c);
         colourKeywords.put("cyan", 0xff00ffff);
         colourKeywords.put("darkblue", 0xff00008b);
         colourKeywords.put("darkcyan", 0xff008b8b);
         colourKeywords.put("darkgoldenrod", 0xffb8860b);
         colourKeywords.put("darkgray", 0xffa9a9a9);
         colourKeywords.put("darkgreen", 0xff006400);
         colourKeywords.put("darkgrey", 0xffa9a9a9);
         colourKeywords.put("darkkhaki", 0xffbdb76b);
         colourKeywords.put("darkmagenta", 0xff8b008b);
         colourKeywords.put("darkolivegreen", 0xff556b2f);
         colourKeywords.put("darkorange", 0xffff8c00);
         colourKeywords.put("darkorchid", 0xff9932cc);
         colourKeywords.put("darkred", 0xff8b0000);
         colourKeywords.put("darksalmon", 0xffe9967a);
         colourKeywords.put("darkseagreen", 0xff8fbc8f);
         colourKeywords.put("darkslateblue", 0xff483d8b);
         colourKeywords.put("darkslategray", 0xff2f4f4f);
         colourKeywords.put("darkslategrey", 0xff2f4f4f);
         colourKeywords.put("darkturquoise", 0xff00ced1);
         colourKeywords.put("darkviolet", 0xff9400d3);
         colourKeywords.put("deeppink", 0xffff1493);
         colourKeywords.put("deepskyblue", 0xff00bfff);
         colourKeywords.put("dimgray", 0xff696969);
         colourKeywords.put("dimgrey", 0xff696969);
         colourKeywords.put("dodgerblue", 0xff1e90ff);
         colourKeywords.put("firebrick", 0xffb22222);
         colourKeywords.put("floralwhite", 0xfffffaf0);
         colourKeywords.put("forestgreen", 0xff228b22);
         colourKeywords.put("fuchsia", 0xffff00ff);
         colourKeywords.put("gainsboro", 0xffdcdcdc);
         colourKeywords.put("ghostwhite", 0xfff8f8ff);
         colourKeywords.put("gold", 0xffffd700);
         colourKeywords.put("goldenrod", 0xffdaa520);
         colourKeywords.put("gray", 0xff808080);
         colourKeywords.put("green", 0xff008000);
         colourKeywords.put("greenyellow", 0xffadff2f);
         colourKeywords.put("grey", 0xff808080);
         colourKeywords.put("honeydew", 0xfff0fff0);
         colourKeywords.put("hotpink", 0xffff69b4);
         colourKeywords.put("indianred", 0xffcd5c5c);
         colourKeywords.put("indigo", 0xff4b0082);
         colourKeywords.put("ivory", 0xfffffff0);
         colourKeywords.put("khaki", 0xfff0e68c);
         colourKeywords.put("lavender", 0xffe6e6fa);
         colourKeywords.put("lavenderblush", 0xfffff0f5);
         colourKeywords.put("lawngreen", 0xff7cfc00);
         colourKeywords.put("lemonchiffon", 0xfffffacd);
         colourKeywords.put("lightblue", 0xffadd8e6);
         colourKeywords.put("lightcoral", 0xfff08080);
         colourKeywords.put("lightcyan", 0xffe0ffff);
         colourKeywords.put("lightgoldenrodyellow", 0xfffafad2);
         colourKeywords.put("lightgray", 0xffd3d3d3);
         colourKeywords.put("lightgreen", 0xff90ee90);
         colourKeywords.put("lightgrey", 0xffd3d3d3);
         colourKeywords.put("lightpink", 0xffffb6c1);
         colourKeywords.put("lightsalmon", 0xffffa07a);
         colourKeywords.put("lightseagreen", 0xff20b2aa);
         colourKeywords.put("lightskyblue", 0xff87cefa);
         colourKeywords.put("lightslategray", 0xff778899);
         colourKeywords.put("lightslategrey", 0xff778899);
         colourKeywords.put("lightsteelblue", 0xffb0c4de);
         colourKeywords.put("lightyellow", 0xffffffe0);
         colourKeywords.put("lime", 0xff00ff00);
         colourKeywords.put("limegreen", 0xff32cd32);
         colourKeywords.put("linen", 0xfffaf0e6);
         colourKeywords.put("magenta", 0xffff00ff);
         colourKeywords.put("maroon", 0xff800000);
         colourKeywords.put("mediumaquamarine", 0xff66cdaa);
         colourKeywords.put("mediumblue", 0xff0000cd);
         colourKeywords.put("mediumorchid", 0xffba55d3);
         colourKeywords.put("mediumpurple", 0xff9370db);
         colourKeywords.put("mediumseagreen", 0xff3cb371);
         colourKeywords.put("mediumslateblue", 0xff7b68ee);
         colourKeywords.put("mediumspringgreen", 0xff00fa9a);
         colourKeywords.put("mediumturquoise", 0xff48d1cc);
         colourKeywords.put("mediumvioletred", 0xffc71585);
         colourKeywords.put("midnightblue", 0xff191970);
         colourKeywords.put("mintcream", 0xfff5fffa);
         colourKeywords.put("mistyrose", 0xffffe4e1);
         colourKeywords.put("moccasin", 0xffffe4b5);
         colourKeywords.put("navajowhite", 0xffffdead);
         colourKeywords.put("navy", 0xff000080);
         colourKeywords.put("oldlace", 0xfffdf5e6);
         colourKeywords.put("olive", 0xff808000);
         colourKeywords.put("olivedrab", 0xff6b8e23);
         colourKeywords.put("orange", 0xffffa500);
         colourKeywords.put("orangered", 0xffff4500);
         colourKeywords.put("orchid", 0xffda70d6);
         colourKeywords.put("palegoldenrod", 0xffeee8aa);
         colourKeywords.put("palegreen", 0xff98fb98);
         colourKeywords.put("paleturquoise", 0xffafeeee);
         colourKeywords.put("palevioletred", 0xffdb7093);
         colourKeywords.put("papayawhip", 0xffffefd5);
         colourKeywords.put("peachpuff", 0xffffdab9);
         colourKeywords.put("peru", 0xffcd853f);
         colourKeywords.put("pink", 0xffffc0cb);
         colourKeywords.put("plum", 0xffdda0dd);
         colourKeywords.put("powderblue", 0xffb0e0e6);
         colourKeywords.put("purple", 0xff800080);
         colourKeywords.put("rebeccapurple", 0xff663399);
         colourKeywords.put("red", 0xffff0000);
         colourKeywords.put("rosybrown", 0xffbc8f8f);
         colourKeywords.put("royalblue", 0xff4169e1);
         colourKeywords.put("saddlebrown", 0xff8b4513);
         colourKeywords.put("salmon", 0xfffa8072);
         colourKeywords.put("sandybrown", 0xfff4a460);
         colourKeywords.put("seagreen", 0xff2e8b57);
         colourKeywords.put("seashell", 0xfffff5ee);
         colourKeywords.put("sienna", 0xffa0522d);
         colourKeywords.put("silver", 0xffc0c0c0);
         colourKeywords.put("skyblue", 0xff87ceeb);
         colourKeywords.put("slateblue", 0xff6a5acd);
         colourKeywords.put("slategray", 0xff708090);
         colourKeywords.put("slategrey", 0xff708090);
         colourKeywords.put("snow", 0xfffffafa);
         colourKeywords.put("springgreen", 0xff00ff7f);
         colourKeywords.put("steelblue", 0xff4682b4);
         colourKeywords.put("tan", 0xffd2b48c);
         colourKeywords.put("teal", 0xff008080);
         colourKeywords.put("thistle", 0xffd8bfd8);
         colourKeywords.put("tomato", 0xffff6347);
         colourKeywords.put("turquoise", 0xff40e0d0);
         colourKeywords.put("violet", 0xffee82ee);
         colourKeywords.put("wheat", 0xfff5deb3);
         colourKeywords.put("white", 0xffffffff);
         colourKeywords.put("whitesmoke", 0xfff5f5f5);
         colourKeywords.put("yellow", 0xffffff00);
         colourKeywords.put("yellowgreen", 0xff9acd32);
         colourKeywords.put("transparent", 0x00000000);
      }

      static Integer get(String colourName) {
         return colourKeywords.get(colourName);
      }
   }

   private static class FontSizeKeywords {
      private static final Map<String, Length> fontSizeKeywords = new HashMap<>(9);
      static {
         fontSizeKeywords.put("xx-small", new Length(0.694f, Unit.pt));
         fontSizeKeywords.put("x-small", new Length(0.833f, Unit.pt));
         fontSizeKeywords.put("small", new Length(10.0f, Unit.pt));
         fontSizeKeywords.put("medium", new Length(12.0f, Unit.pt));
         fontSizeKeywords.put("large", new Length(14.4f, Unit.pt));
         fontSizeKeywords.put("x-large", new Length(17.3f, Unit.pt));
         fontSizeKeywords.put("xx-large", new Length(20.7f, Unit.pt));
         fontSizeKeywords.put("smaller", new Length(83.33f, Unit.percent));
         fontSizeKeywords.put("larger", new Length(120f, Unit.percent));
      }

      static Length get(String fontSize) {
         return fontSizeKeywords.get(fontSize);
      }
   }

   private static class FontWeightKeywords {
      private static final Map<String, Float> fontWeightKeywords = new HashMap<>(4);
      static {
         fontWeightKeywords.put("normal", Style.FONT_WEIGHT_NORMAL);
         fontWeightKeywords.put("bold", Style.FONT_WEIGHT_BOLD);
         fontWeightKeywords.put("bolder", Style.FONT_WEIGHT_BOLDER);
         fontWeightKeywords.put("lighter", Style.FONT_WEIGHT_LIGHTER);
      }

      static Float get(String fontWeight) {
         return fontWeightKeywords.get(fontWeight);
      }

      static boolean contains(String fontStretch) {
         return fontWeightKeywords.containsKey(fontStretch);
      }
   }

   private static class FontStretchKeywords {
      private static final Map<String, Float> fontStretchKeywords = new HashMap<>(9);
      static {
         fontStretchKeywords.put("ultra-condensed", 50f);
         fontStretchKeywords.put("extra-condensed", 62.5f);
         fontStretchKeywords.put("condensed", 75f);
         fontStretchKeywords.put("semi-condensed", 87.5f);
         fontStretchKeywords.put("normal", Style.FONT_STRETCH_NORMAL);
         fontStretchKeywords.put("semi-expanded", 112.5f);
         fontStretchKeywords.put("expanded", 125f);
         fontStretchKeywords.put("extra-expanded", 150f);
         fontStretchKeywords.put("ultra-expanded", 200f);
      }

      static Float get(String fontStretch) {
         return fontStretchKeywords.get(fontStretch);
      }

      static boolean contains(String fontStretch) {
         return fontStretchKeywords.containsKey(fontStretch);
      }
   }


   //=========================================================================
   // Main parser invocation methods
   //=========================================================================


   public SVGBase parseStream(InputStream is) throws SVGParseException
   {
      // Transparently handle zipped files (.svgz)
      if (!is.markSupported()) {
         // We need a a buffered stream so we can use mark() and reset()
         is = new BufferedInputStream(is);
      }
      try
      {
         is.mark(3);
         int  firstTwoBytes = is.read() + (is.read() << 8);
         is.reset();
         if (firstTwoBytes == GZIPInputStream.GZIP_MAGIC) {
            // Looks like a zipped file.
            is = new BufferedInputStream( new GZIPInputStream(is) );
         }
      }
      catch (IOException ioe)
      {
         // Not a zipped SVG. Fall through and try parsing it normally.
      }

      try
      {
         if (FORCE_SAX_ON_EARLY_ANDROIDS) {
            debug("Forcing SAX parser for this version of Android");
            parseUsingSAX(is);
            return svgDocument;
         }

         if (enableInternalEntities)
         {
            // We need to check for the presence of entities in the file so we can decide which parser to use.
            is.mark(ENTITY_WATCH_BUFFER_SIZE);
            // Read that number of bytes into a buffer so we
            byte[]  checkBuf = new byte[ENTITY_WATCH_BUFFER_SIZE];
            int n = is.read(checkBuf);
            // Read in the bytes as a string. We should probably use UTF-8 here, but the string
            // constructor that takes a charset requires SDK 9. We should be okay though, since we
            // are only looking for plain ASCII. And that'll be the same in any encoding.
            String preamble = new String(checkBuf, 0, n);
            // Reset the stream so that the XML parsers can do their job.
            is.reset();
            if (preamble.contains("<!ENTITY ")) {
               // Found something that looks like an entity definition.
               // So we'll use the SAX parser which supports them.
               debug("Switching to SAX parser to process entities");
               parseUsingSAX(is);
               return svgDocument;
            }
         }

         // Use the (faster) XmlPullParser
         parseUsingXmlPullParser(is);
         return svgDocument;
      }
      catch (IOException e) {
         Log.e(TAG, "Error occurred while performing check for entities.  File may not be parsed correctly if it contains entity definitions.", e);
         parseUsingXmlPullParser(is);
         return svgDocument;
      }
      finally
      {
         try {
            is.close();
         } catch (IOException e) {
            Log.e(TAG, "Exception thrown closing input stream");
         }
      }
   }

   //=========================================================================
   // Attribute setters
   //=========================================================================

   @Override
   public SVGParser setInternalEntitiesEnabled(boolean enable) {
      enableInternalEntities = enable;
      return this;
   }

   @Override
   public SVGParser setExternalFileResolver(SVGExternalFileResolver fileResolver) {
      externalFileResolver = fileResolver;
      return this;
   }

   //=========================================================================
   // XmlPullParser parsing
   //=========================================================================


   /*
    * Implements the SAX Attributes class so that our parser can share a common attributes object
    */
   private static class  XPPAttributesWrapper  implements Attributes
   {
      private final XmlPullParser  parser;

      public XPPAttributesWrapper(XmlPullParser parser)
      {
         this.parser = parser;
      }

      @Override
      public int getLength()
      {
         return parser.getAttributeCount();
      }

      @Override
      public String getURI(int index)
      {
         return parser.getAttributeNamespace(index);
      }

      @Override
      public String getLocalName(int index)
      {
         return parser.getAttributeName(index);
      }

      @Override
      public String getQName(int index)
      {
         String qName = parser.getAttributeName(index);
         if (parser.getAttributePrefix(index) != null)
            qName = parser.getAttributePrefix(index) + ':' + qName;
         return qName;
      }

      @Override
      public String getValue(int index)
      {
         return parser.getAttributeValue(index);
      }

      // Not used, and not implemented
      @Override
      public String getType(int index) { return null; }
      @Override
      public int getIndex(String uri, String localName) { return -1; }
      @Override
      public int getIndex(String qName) { return -1; }
      @Override
      public String getType(String uri, String localName) { return null; }
      @Override
      public String getType(String qName) { return null; }
      @Override
      public String getValue(String uri, String localName) { return null; }
      @Override
      public String getValue(String qName) { return null; }
   }


   private void parseUsingXmlPullParser(InputStream is) throws SVGParseException
   {
      try
      {
         XmlPullParser         parser = Xml.newPullParser();
         XPPAttributesWrapper  attributes = new XPPAttributesWrapper(parser);


         parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
         parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
         parser.setInput(is, null);

         int  eventType = parser.getEventType();
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            switch(eventType) {
               case XmlPullParser.START_DOCUMENT:
                  startDocument();
                  break;
               case XmlPullParser.START_TAG:
                  String qName = parser.getName();
                  if (parser.getPrefix() != null)
                     qName = parser.getPrefix() + ':' + qName;
                  startElement(parser.getNamespace(), parser.getName(), qName, attributes);
                  break;
               case XmlPullParser.END_TAG:
                  qName = parser.getName();
                  if (parser.getPrefix() != null)
                     qName = parser.getPrefix() + ':' + qName;
                  endElement(parser.getNamespace(), parser.getName(), qName);
                  break;
               case XmlPullParser.TEXT:
                  int[] startAndLength = new int[2];
                  char[] text = parser.getTextCharacters(startAndLength);
                  text(text, startAndLength[0], startAndLength[1]);
                  break;
               case XmlPullParser.ENTITY_REF:
                  text(parser.getText());
                  break;
               case XmlPullParser.CDSECT:
                  text(parser.getText());
                  break;
               //case XmlPullParser.COMMENT:
               //   text(parser.getText());
               //   break;
               //case XmlPullParser.DOCDECL:
               //   text(parser.getText());
               //   break;
               //case XmlPullParser.IGNORABLE_WHITESPACE:
               //   text(parser.getText());
               //   break;
               case XmlPullParser.PROCESSING_INSTRUCTION:
                  TextScanner  scan = new TextScanner(parser.getText());
                  String       instr = scan.nextToken();
                  handleProcessingInstruction(instr, parseProcessingInstructionAttributes(scan));
                  break;
            }
            eventType = parser.nextToken();
         }
         endDocument();

      }
      catch (XmlPullParserException e)
      {
         throw new SVGParseException("XML parser problem", e);
      }
      catch (IOException e)
      {
         throw new SVGParseException("Stream error", e);
      }
   }


   //=========================================================================
   // SAX parsing method and handler class
   //=========================================================================


   private void parseUsingSAX(InputStream is) throws SVGParseException
   {
      try
      {
         // Invoke the SAX XML parser on the input.
         SAXParserFactory  spf = SAXParserFactory.newInstance();

         if (!FORCE_SAX_ON_EARLY_ANDROIDS) {
            // Disable external entity resolving
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
         }

         SAXParser sp = spf.newSAXParser();
         XMLReader xr = sp.getXMLReader();

         SAXHandler  handler = new SAXHandler();
         xr.setContentHandler(handler);
         xr.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

         xr.parse(new InputSource(is));
      }
      catch (ParserConfigurationException e)
      {
         throw new SVGParseException("XML parser problem", e);
      }
      catch (SAXException e)
      {
         throw new SVGParseException("SVG parse error", e);
      }
      catch (IOException e)
      {
         throw new SVGParseException("Stream error", e);
      }
   }


   private class  SAXHandler  extends DefaultHandler2
   {
      @Override
      public void startDocument()
      {
         SVGParserImpl.this.startDocument();
      }


      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
      {
         SVGParserImpl.this.startElement(uri, localName, qName, attributes);
      }


      @Override
      public void characters(char[] ch, int start, int length) throws SAXException
      {
         SVGParserImpl.this.text(new String(ch, start, length));
      }


      /*
      @Override
      public void comment(char[] ch, int start, int length) throws SAXException
      {
         SVGParser.this.text(new String(ch, start, length));
      }
      */


      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException
      {
         SVGParserImpl.this.endElement(uri, localName, qName);
      }


      @Override
      public void endDocument()
      {
         SVGParserImpl.this.endDocument();
      }


      @Override
      public void processingInstruction(String target, String data)
      {
         TextScanner  scan = new TextScanner(data);
         Map<String, String> attributes = parseProcessingInstructionAttributes(scan);
         handleProcessingInstruction(target, attributes);
      }
   }


   //=========================================================================
   // Parser event classes used by both XML parser implementations
   //=========================================================================


   private void startDocument()
   {
      SVGParserImpl.this.svgDocument = new SVGBase(enableInternalEntities, externalFileResolver);
   }


   private void startElement(String uri, String localName, String qName, Attributes attributes) throws SVGParseException
   {
      if (ignoring) {
         ignoreDepth++;
         return;
      }
      if (!SVG_NAMESPACE.equals(uri) && !"".equals(uri)) {
         return;
      }

      String tag = (localName.length() > 0) ? localName : qName;

      SVGElem  elem = SVGElem.fromString(tag);
      switch (elem)
      {
         case svg:
            svg(attributes); break;
         case g:
            g(attributes); break;
         case defs:
            defs(attributes); break;
         case a:
            a(attributes); break;
         case use:
            use(attributes); break;
         case path:
            path(attributes); break;
         case rect:
            rect(attributes); break;
         case circle:
            circle(attributes); break;
         case ellipse:
            ellipse(attributes); break;
         case line:
            line(attributes); break;
         case polyline:
            polyline(attributes); break;
         case polygon:
            polygon(attributes); break;
         case text:
            text(attributes); break;
         case tspan:
            tspan(attributes); break;
         case tref:
            tref(attributes); break;
         case SWITCH:
            zwitch(attributes); break;
         case symbol:
            symbol(attributes); break;
         case marker:
            marker(attributes); break;
         case linearGradient:
            linearGradient(attributes); break;
         case radialGradient:
            radialGradient(attributes); break;
         case stop:
            stop(attributes); break;
         case title:
         case desc:
            inMetadataElement = true;
            metadataTag = elem;
            break;
         case clipPath:
            clipPath(attributes); break;
         case textPath:
            textPath(attributes); break;
         case pattern:
            pattern(attributes); break;
         case image:
            image(attributes); break;
         case view:
            view(attributes); break;
         case mask:
            mask(attributes); break;
         case style:
            style(attributes); break;
         case solidColor:
            solidColor(attributes); break;
         default:
            ignoring = true;
            ignoreDepth = 1;
            break;
      }
   }


   private void  text(String characters) throws SVGParseException
   {
      if (ignoring)
         return;

      if (inMetadataElement)
      {
         if (metadataElementContents == null)
            metadataElementContents = new StringBuilder(characters.length());
         metadataElementContents.append(characters);
      }
      else if (inStyleElement)
      {
         if (styleElementContents == null)
            styleElementContents = new StringBuilder(characters.length());
         styleElementContents.append(characters);
      }
      else if (currentElement instanceof SVGBase.TextContainer)
      {
         appendToTextContainer(characters);
      }
   }


   private void  text(char[] ch, int start, int length) throws SVGParseException
   {
      if (ignoring)
         return;

      if (inMetadataElement)
      {
         if (metadataElementContents == null)
            metadataElementContents = new StringBuilder(length);
         metadataElementContents.append(ch, start, length);
      }
      else if (inStyleElement)
      {
         if (styleElementContents == null)
            styleElementContents = new StringBuilder(length);
         styleElementContents.append(ch, start, length);
      }
      else if (currentElement instanceof SVGBase.TextContainer)
      {
         appendToTextContainer(new String(ch, start, length));
      }

   }


   private void  appendToTextContainer(String characters) throws SVGParseException
   {
      // The parser can pass us several text nodes in a row. If this happens, we
      // want to collapse them all into one SVGBase.TextSequence node
      SVGBase.SvgConditionalContainer  parent = (SVGBase.SvgConditionalContainer) currentElement;
      int  numOlderSiblings = parent.getChildren().size();
      SVGBase.SvgObject  previousSibling = (numOlderSiblings == 0) ? null : parent.getChildren().get(numOlderSiblings-1);
      if (previousSibling instanceof SVGBase.TextSequence) {
         // Last sibling was a TextSequence also, so merge them.
         ((SVGBase.TextSequence) previousSibling).text += characters;
      } else {
         // Add a new TextSequence to the child node list
         currentElement.addChild(new SVGBase.TextSequence( characters ));
      }
   }


   private void  endElement(String uri, String localName, String qName) throws SVGParseException
   {
      if (ignoring) {
         if (--ignoreDepth == 0) {
            ignoring = false;
         }
         return;
      }

      if (!SVG_NAMESPACE.equals(uri) && !"".equals(uri)) {
         return;
      }

      String tag = (localName.length() > 0) ? localName : qName;
      switch (SVGElem.fromString(tag))
      {
         case title:
         case desc:
            inMetadataElement = false;
            if (metadataElementContents != null)
            {
               if (metadataTag == SVGElem.title)
                  svgDocument.setTitle(metadataElementContents.toString());
               else if (metadataTag == SVGElem.desc)
                  svgDocument.setDesc(metadataElementContents.toString());
               metadataElementContents.setLength(0);
            }
            return;

         case style:
            if (styleElementContents != null) {
               inStyleElement = false;
               parseCSSStyleSheet(styleElementContents.toString());
               styleElementContents.setLength(0);
               return;
            }
            break;

         case svg:
         case g:
         case defs:
         case a:
         case use:
         case image:
         case text:
         case tspan:
         case SWITCH:
         case symbol:
         case marker:
         case linearGradient:
         case radialGradient:
         case stop:
         case clipPath:
         case textPath:
         case pattern:
         case view:
         case mask:
         case solidColor:
            if (currentElement == null) {
               // This situation has been reported by a user. But I am unable to reproduce this fault.
               // If you can get this error please add your SVG file to https://github.com/BigBadaboom/androidsvg/issues/177
               // For now we'll return a parse exception for consistency (instead of NPE).
               throw new SVGParseException(String.format("Unbalanced end element </%s> found", tag));
            }
            currentElement = ((SvgObject) currentElement).parent;
            break;

         default:
            // no action
      }

   }


   private void  endDocument()
   {
   }


   private void  handleProcessingInstruction(String instruction, Map<String, String> attributes)
   {
      if (instruction.equals(XML_STYLESHEET_PROCESSING_INSTRUCTION) && externalFileResolver != null)
      {
         // If a "type" is specified, make sure it is the CSS type
         String  attr = attributes.get(XML_STYLESHEET_ATTR_TYPE);
         if (attr != null && !CSSParser.CSS_MIME_TYPE.equals(attributes.get("type")))
            return;
         // Alternate stylesheets are not supported
         attr = attributes.get(XML_STYLESHEET_ATTR_ALTERNATE);
         if (attr != null && !XML_STYLESHEET_ATTR_ALTERNATE_NO.equals(attributes.get("alternate")))
            return;

         attr = attributes.get(XML_STYLESHEET_ATTR_HREF);
         if (attr != null)
         {
            String  css = externalFileResolver.resolveCSSStyleSheet(attr);
            if (css == null)
               return;

            String  mediaAttr = attributes.get(XML_STYLESHEET_ATTR_MEDIA);
            if (mediaAttr != null && !XML_STYLESHEET_ATTR_MEDIA_ALL.equals(mediaAttr.trim())) {
               css = "@media " + mediaAttr + " { " + css + "}";
            }

            parseCSSStyleSheet(css);
         }

      }
   }


   private Map<String,String>  parseProcessingInstructionAttributes(TextScanner scan)
   {
      HashMap<String, String>  attributes = new HashMap<>();

      scan.skipWhitespace();
      String  attrName = scan.nextToken('=');
      while (attrName != null)
      {
         scan.consume('=');
         String value = scan.nextQuotedString();
         attributes.put(attrName, value);

         scan.skipWhitespace();
         attrName = scan.nextToken('=');
      }
      return attributes;
   }


   //=========================================================================


   private void  debug(String format, Object... args)
   {
   }


   //=========================================================================
   // Handlers for each SVG element
   //=========================================================================
   // <svg> element

   private void  svg(Attributes attributes) throws SVGParseException
   {
      debug("<svg>");

      SVGBase.Svg  obj = new SVGBase.Svg();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      parseAttributesSVG(obj, attributes);
      if (currentElement == null) {
         svgDocument.setRootElement(obj);
      } else {
         currentElement.addChild(obj);
      }
      currentElement = obj;
   }

   
   private void  parseAttributesSVG(SVGBase.Svg obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <svg> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <svg> element. height cannot be negative");
               break;
            case version:
               obj.version = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <g> group element


   private void  g(Attributes attributes) throws SVGParseException
   {
      debug("<g>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Group  obj = new SVGBase.Group();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <defs> group element


   private void  defs(Attributes attributes) throws SVGParseException
   {
      debug("<defs>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Defs  obj = new SVGBase.Defs();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <a> element


   private void  a(Attributes attributes) throws SVGParseException
   {
      debug("<a>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.A  obj = new SVGBase.A();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesA(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesA(SVGBase.A obj, Attributes attributes)
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         //noinspection SwitchStatementWithTooFewBranches
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <use> element


   private void  use(Attributes attributes) throws SVGParseException
   {
      debug("<use>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Use  obj = new SVGBase.Use();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesUse(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesUse(SVGBase.Use obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <use> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <use> element. height cannot be negative");
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <image> element


   private void  image(Attributes attributes) throws SVGParseException
   {
      debug("<image>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Image  obj = new SVGBase.Image();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesImage(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesImage(SVGBase.Image obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <use> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <use> element. height cannot be negative");
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            case preserveAspectRatio:
               parsePreserveAspectRatio(obj, val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <path> element


   private void  path(Attributes attributes) throws SVGParseException
   {
      debug("<path>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Path  obj = new SVGBase.Path();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesPath(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesPath(SVGBase.Path obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case d:
               obj.d = parsePath(val);
               break;
            case pathLength:
               obj.pathLength = parseFloat(val);
               if (obj.pathLength < 0f)
                  throw new SVGParseException("Invalid <path> element. pathLength cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <rect> element


   private void  rect(Attributes attributes) throws SVGParseException
   {
      debug("<rect>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Rect  obj = new SVGBase.Rect();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesRect(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesRect(SVGBase.Rect obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <rect> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <rect> element. height cannot be negative");
               break;
            case rx:
               obj.rx = parseLength(val);
               if (obj.rx.isNegative())
                  throw new SVGParseException("Invalid <rect> element. rx cannot be negative");
               break;
            case ry:
               obj.ry = parseLength(val);
               if (obj.ry.isNegative())
                  throw new SVGParseException("Invalid <rect> element. ry cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <circle> element


   private void  circle(Attributes attributes) throws SVGParseException
   {
      debug("<circle>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Circle  obj = new SVGBase.Circle();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesCircle(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesCircle(SVGBase.Circle obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case cx:
               obj.cx = parseLength(val);
               break;
            case cy:
               obj.cy = parseLength(val);
               break;
            case r:
               obj.r = parseLength(val);
               if (obj.r.isNegative())
                  throw new SVGParseException("Invalid <circle> element. r cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <ellipse> element


   private void  ellipse(Attributes attributes) throws SVGParseException
   {
      debug("<ellipse>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Ellipse  obj = new SVGBase.Ellipse();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesEllipse(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesEllipse(SVGBase.Ellipse obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case cx:
               obj.cx = parseLength(val);
               break;
            case cy:
               obj.cy = parseLength(val);
               break;
            case rx:
               obj.rx = parseLength(val);
               if (obj.rx.isNegative())
                  throw new SVGParseException("Invalid <ellipse> element. rx cannot be negative");
               break;
            case ry:
               obj.ry = parseLength(val);
               if (obj.ry.isNegative())
                  throw new SVGParseException("Invalid <ellipse> element. ry cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <line> element


   private void  line(Attributes attributes) throws SVGParseException
   {
      debug("<line>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Line  obj = new SVGBase.Line();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesLine(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesLine(SVGBase.Line obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x1:
               obj.x1 = parseLength(val);
               break;
            case y1:
               obj.y1 = parseLength(val);
               break;
            case x2:
               obj.x2 = parseLength(val);
               break;
            case y2:
               obj.y2 = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <polyline> element


   private void  polyline(Attributes attributes) throws SVGParseException
   {
      debug("<polyline>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.PolyLine  obj = new SVGBase.PolyLine();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesPolyLine(obj, attributes, "polyline");
      currentElement.addChild(obj);     
   }


   /*
    *  Parse the "points" attribute. Used by both <polyline> and <polygon>.
    */
   private void  parseAttributesPolyLine(SVGBase.PolyLine obj, Attributes attributes, String tag) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.points)
         {
            TextScanner scan = new TextScanner(attributes.getValue(i));
            List<Float> points = new ArrayList<>();
            scan.skipWhitespace();

            while (!scan.empty()) {
               float x = scan.nextFloat();
               if (Float.isNaN(x))
                  throw new SVGParseException("Invalid <"+tag+"> points attribute. Non-coordinate content found in list.");
               scan.skipCommaWhitespace();
               float y = scan.nextFloat();
               if (Float.isNaN(y))
                  throw new SVGParseException("Invalid <"+tag+"> points attribute. There should be an even number of coordinates.");
               scan.skipCommaWhitespace();
               points.add(x);
               points.add(y);
            }
            obj.points = new float[points.size()];
            int j = 0;
            for (float f: points) {
               obj.points[j++] = f;
            }
         }
      }
   }


   //=========================================================================
   // <polygon> element


   private void  polygon(Attributes attributes) throws SVGParseException
   {
      debug("<polygon>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Polygon  obj = new SVGBase.Polygon();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesPolyLine(obj, attributes, "polygon"); // reuse of polyline "points" parser
      currentElement.addChild(obj);     
   }


   //=========================================================================
   // <text> element


   private void  text(Attributes attributes) throws SVGParseException
   {
      debug("<text>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Text  obj = new SVGBase.Text();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPosition(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesTextPosition(TextPositionedContainer obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLengthList(val);
               break;
            case y:
               obj.y = parseLengthList(val);
               break;
            case dx:
               obj.dx = parseLengthList(val);
               break;
            case dy:
               obj.dy = parseLengthList(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <tspan> element


   private void  tspan(Attributes attributes) throws SVGParseException
   {
      debug("<tspan>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVGBase.TextContainer))
         throw new SVGParseException("Invalid document. <tspan> elements are only valid inside <text> or other <tspan> elements.");
      SVGBase.TSpan  obj = new SVGBase.TSpan();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPosition(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
      if (obj.parent instanceof TextRoot)
         obj.setTextRoot((TextRoot) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   //=========================================================================
   // <tref> element


   private void  tref(Attributes attributes) throws SVGParseException
   {
      debug("<tref>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVGBase.TextContainer))
         throw new SVGParseException("Invalid document. <tref> elements are only valid inside <text> or <tspan> elements.");
      SVGBase.TRef  obj = new SVGBase.TRef();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTRef(obj, attributes);
      currentElement.addChild(obj);
      if (obj.parent instanceof TextRoot)
         obj.setTextRoot((TextRoot) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   private void  parseAttributesTRef(SVGBase.TRef obj, Attributes attributes)
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         //noinspection SwitchStatementWithTooFewBranches
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <switch> element


   private void  zwitch(Attributes attributes) throws SVGParseException
   {
      debug("<switch>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Switch  obj = new SVGBase.Switch();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesConditional(SVGBase.SvgConditional obj, Attributes attributes)
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case requiredFeatures:
               obj.setRequiredFeatures(parseRequiredFeatures(val));
               break;
            case requiredExtensions:
               obj.setRequiredExtensions(val);
               break;
            case systemLanguage:
               obj.setSystemLanguage(parseSystemLanguage(val));
               break;
            case requiredFormats:
               obj.setRequiredFormats(parseRequiredFormats(val));
               break;
            case requiredFonts:
               List<String>  fonts = parseFontFamily(val);
               Set<String>  fontSet = (fonts != null) ? new HashSet<>(fonts) : new HashSet<String>(0);
               obj.setRequiredFonts(fontSet);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <symbol> element


   private void  symbol(Attributes attributes) throws SVGParseException
   {
      debug("<symbol>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Symbol  obj = new SVGBase.Symbol();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }

   
   //=========================================================================
   // <marker> element


   private void  marker(Attributes attributes) throws SVGParseException
   {
      debug("<marker>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Marker  obj = new SVGBase.Marker();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      parseAttributesMarker(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesMarker(SVGBase.Marker obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case refX:
               obj.refX = parseLength(val);
               break;
            case refY:
               obj.refY = parseLength(val);
               break;
            case markerWidth:
               obj.markerWidth = parseLength(val);
               if (obj.markerWidth.isNegative())
                  throw new SVGParseException("Invalid <marker> element. markerWidth cannot be negative");
               break;
            case markerHeight:
               obj.markerHeight = parseLength(val);
               if (obj.markerHeight.isNegative())
                  throw new SVGParseException("Invalid <marker> element. markerHeight cannot be negative");
               break;
            case markerUnits:
               if ("strokeWidth".equals(val)) {
                  obj.markerUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.markerUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute markerUnits");
               } 
               break;
            case orient:
               if ("auto".equals(val)) {
                  obj.orient = Float.NaN;
               } else {
                  obj.orient = parseFloat(val);
               }
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <linearGradient> element


   private void  linearGradient(Attributes attributes) throws SVGParseException
   {
      debug("<linearGradient>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.SvgLinearGradient  obj = new SVGBase.SvgLinearGradient();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesGradient(obj, attributes);
      parseAttributesLinearGradient(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesGradient(SVGBase.GradientElement obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case gradientUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.gradientUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.gradientUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute gradientUnits");
               } 
               break;
            case gradientTransform:
               obj.gradientTransform = parseTransformList(val);
               break;
            case spreadMethod:
               try
               {
                  obj.spreadMethod = GradientSpread.valueOf(val);
               } 
               catch (IllegalArgumentException e)
               {
                  throw new SVGParseException("Invalid spreadMethod attribute. \""+val+"\" is not a valid value.");
               }
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   private void  parseAttributesLinearGradient(SVGBase.SvgLinearGradient obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x1:
               obj.x1 = parseLength(val);
               break;
            case y1:
               obj.y1 = parseLength(val);
               break;
            case x2:
               obj.x2 = parseLength(val);
               break;
            case y2:
               obj.y2 = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <radialGradient> element


   private void  radialGradient(Attributes attributes) throws SVGParseException
   {
      debug("<radialGradient>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.SvgRadialGradient  obj = new SVGBase.SvgRadialGradient();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesGradient(obj, attributes);
      parseAttributesRadialGradient(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesRadialGradient(SVGBase.SvgRadialGradient obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case cx:
               obj.cx = parseLength(val);
               break;
            case cy:
               obj.cy = parseLength(val);
               break;
            case r:
               obj.r = parseLength(val);
               if (obj.r.isNegative())
                  throw new SVGParseException("Invalid <radialGradient> element. r cannot be negative");
               break;
            case fx:
               obj.fx = parseLength(val);
               break;
            case fy:
               obj.fy = parseLength(val);
               break;
            case fr:
               obj.fr = parseLength(val);
               if (obj.fr.isNegative())
                  throw new SVGParseException("Invalid <radialGradient> element. fr cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // Gradient <stop> element


   private void  stop(Attributes attributes) throws SVGParseException
   {
      debug("<stop>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVGBase.GradientElement))
         throw new SVGParseException("Invalid document. <stop> elements are only valid inside <linearGradient> or <radialGradient> elements.");
      SVGBase.Stop  obj = new SVGBase.Stop();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesStop(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesStop(SVGBase.Stop obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         //noinspection SwitchStatementWithTooFewBranches
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case offset:
               obj.offset = parseGradientOffset(val);
               break;
            default:
               break;
         }
      }
   }


   private Float  parseGradientOffset(String val) throws SVGParseException
   {
      if (val.length() == 0)
         throw new SVGParseException("Invalid offset value in <stop> (empty string)");
      int      end = val.length();
      boolean  isPercent = false;

      if (val.charAt(val.length()-1) == '%') {
         end -= 1;
         isPercent = true;
      }
      try
      {
         float scalar = parseFloat(val, 0, end);
         if (isPercent)
            scalar /= 100f;
         return (scalar < 0) ? 0 : (scalar > 100) ? 100 : scalar;
      }
      catch (NumberFormatException e)
      {
         throw new SVGParseException("Invalid offset value in <stop>: "+val, e);
      }
   }


   //=========================================================================
   // <solidColor> element


   private void  solidColor(Attributes attributes) throws SVGParseException
   {
      debug("<solidColor>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.SolidColor  obj = new SVGBase.SolidColor();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <clipPath> element


   private void  clipPath(Attributes attributes) throws SVGParseException
   {
      debug("<clipPath>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.ClipPath  obj = new SVGBase.ClipPath();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesClipPath(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesClipPath(SVGBase.ClipPath obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         //noinspection SwitchStatementWithTooFewBranches
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case clipPathUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.clipPathUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.clipPathUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute clipPathUnits");
               }
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <textPath> element


   private void textPath(Attributes attributes) throws SVGParseException
   {
      debug("<textPath>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.TextPath  obj = new SVGBase.TextPath();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPath(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
      if (obj.parent instanceof TextRoot)
         obj.setTextRoot((TextRoot) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   private void  parseAttributesTextPath(SVGBase.TextPath obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            case startOffset:
               obj.startOffset = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <pattern> element


   private void pattern(Attributes attributes) throws SVGParseException
   {
      debug("<pattern>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Pattern  obj = new SVGBase.Pattern();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      parseAttributesPattern(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesPattern(SVGBase.Pattern obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case patternUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.patternUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.patternUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute patternUnits");
               } 
               break;
            case patternContentUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.patternContentUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.patternContentUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute patternContentUnits");
               } 
               break;
            case patternTransform:
               obj.patternTransform = parseTransformList(val);
               break;
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <pattern> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <pattern> element. height cannot be negative");
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <view> element


   private void  view(Attributes attributes) throws SVGParseException
   {
      debug("<view>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.View  obj = new SVGBase.View();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }

   
   //=========================================================================
   // <mask> element


   private void mask(Attributes attributes) throws SVGParseException
   {
      debug("<mask>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVGBase.Mask  obj = new SVGBase.Mask();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesMask(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesMask(SVGBase.Mask obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case maskUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.maskUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.maskUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute maskUnits");
               } 
               break;
            case maskContentUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.maskContentUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.maskContentUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute maskContentUnits");
               } 
               break;
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <mask> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <mask> element. height cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // Attribute parsing
   //=========================================================================


   private void  parseAttributesCore(SvgElementBase obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String  qname = attributes.getQName(i);
         if (qname.equals("id") || qname.equals("xml:id"))
         {
            obj.id = attributes.getValue(i).trim();
            break;
         }
         else if (qname.equals("xml:space")) {
            String  val = attributes.getValue(i).trim();
            if ("default".equals(val)) {
               obj.spacePreserve = Boolean.FALSE;
            } else if ("preserve".equals(val)) {
               obj.spacePreserve = Boolean.TRUE;
            } else {
               throw new SVGParseException("Invalid value for \"xml:space\" attribute: "+val);
            }
            break;
         }
      }
   }


   /*
    * Parse the style attributes for an element.
    */
   private void  parseAttributesStyle(SvgElementBase obj, Attributes attributes)
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String  val = attributes.getValue(i).trim();
         if (val.length() == 0) {  // Empty attribute. Ignore it.
            continue;
         }
         //boolean  inherit = val.equals("inherit");   // NYI

         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case style:
               parseStyle(obj, val);
               break;

            case CLASS:
               obj.classNames = CSSParser.parseClassAttribute(val);
               break;

            default:
               if (obj.baseStyle == null)
                  obj.baseStyle = new Style();
               Style.processStyleProperty(obj.baseStyle, attributes.getLocalName(i), attributes.getValue(i).trim(), true);
               break;
         }
      }
   }


   /*
    * Parse the 'style' attribute.
    */
   private static void  parseStyle(SvgElementBase obj, String style)
   {
      CSSTextScanner  scan = new CSSTextScanner(PATTERN_BLOCK_COMMENTS.matcher(style).replaceAll(""));  // regex strips block comments

      while (!scan.empty())
      {
         scan.skipWhitespace();
         String  propertyName = scan.nextIdentifier();
         scan.skipWhitespace();
         if (scan.consume(';'))
            continue;  // Handle stray/extra separators gracefully
         if (!scan.consume(':'))
            break;  // Unrecoverable parse error
         scan.skipWhitespace();
         String  propertyValue = scan.nextPropertyValue();
         if (propertyValue == null)
            continue;  // Empty value. Just ignore this property and keep parsing
         scan.skipWhitespace();
         if (scan.empty() || scan.consume(';'))
         {
            if (obj.style == null)
               obj.style = new Style();
            Style.processStyleProperty(obj.style, propertyName, propertyValue, false);
            scan.skipWhitespace();
         }
      }
   }


   private void  parseAttributesViewBox(SVGBase.SvgViewBoxContainer obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case viewBox:
               obj.viewBox = parseViewBox(val);
               break;
            case preserveAspectRatio:
               parsePreserveAspectRatio(obj, val);
               break;
            default:
               break;
         }
      }
   }


   private void  parseAttributesTransform(SVGBase.HasTransform obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.transform)
         {
            obj.setTransform( parseTransformList(attributes.getValue(i)) );
         }
      }
   }


   private Matrix  parseTransformList(String val) throws SVGParseException
   {
      Matrix  matrix = new Matrix();

      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         String  cmd = scan.nextFunction();

         if (cmd == null)
            throw new SVGParseException("Bad transform function encountered in transform list: "+val);

         switch (cmd) {
            case "matrix":
               scan.skipWhitespace();
               float a = scan.nextFloat();
               scan.skipCommaWhitespace();
               float b = scan.nextFloat();
               scan.skipCommaWhitespace();
               float c = scan.nextFloat();
               scan.skipCommaWhitespace();
               float d = scan.nextFloat();
               scan.skipCommaWhitespace();
               float e = scan.nextFloat();
               scan.skipCommaWhitespace();
               float f = scan.nextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(f) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               Matrix m = new Matrix();
               m.setValues(new float[]{a, c, e, b, d, f, 0, 0, 1});
               matrix.preConcat(m);
               break;

            case "translate":
               scan.skipWhitespace();
               float tx = scan.nextFloat();
               float ty = scan.possibleNextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(tx) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               if (Float.isNaN(ty))
                  matrix.preTranslate(tx, 0f);
               else
                  matrix.preTranslate(tx, ty);
               break;

            case "scale":
               scan.skipWhitespace();
               float sx = scan.nextFloat();
               float sy = scan.possibleNextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(sx) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               if (Float.isNaN(sy))
                  matrix.preScale(sx, sx);
               else
                  matrix.preScale(sx, sy);
               break;

            case "rotate": {
               scan.skipWhitespace();
               float ang = scan.nextFloat();
               float cx = scan.possibleNextFloat();
               float cy = scan.possibleNextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(ang) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               if (Float.isNaN(cx)) {
                  matrix.preRotate(ang);
               } else if (!Float.isNaN(cy)) {
                  matrix.preRotate(ang, cx, cy);
               } else {
                  throw new SVGParseException("Invalid transform list: " + val);
               }
               break;
            }

            case "skewX": {
               scan.skipWhitespace();
               float ang = scan.nextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(ang) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               matrix.preSkew((float) Math.tan(Math.toRadians(ang)), 0f);
               break;
            }

            case "skewY": {
               scan.skipWhitespace();
               float ang = scan.nextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(ang) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               matrix.preSkew(0f, (float) Math.tan(Math.toRadians(ang)));
               break;
            }

            default:
               throw new SVGParseException("Invalid transform list fn: " + cmd + ")");
         }

         if (scan.empty())
            break;
         scan.skipCommaWhitespace();
      }

      return matrix;
   }


   //=========================================================================
   // Parsing various SVG value types
   //=========================================================================


   /*
    * Parse an SVG 'Length' value (usually a coordinate).
    * Spec says: length ::= number ("em" | "ex" | "px" | "in" | "cm" | "mm" | "pt" | "pc" | "%")?
    */
   static Length  parseLength(String val) throws SVGParseException
   {
      if (val.length() == 0)
         throw new SVGParseException("Invalid length value (empty string)");
      int   end = val.length();
      Unit  unit = Unit.px;
      char  lastChar = val.charAt(end-1);

      if (lastChar == '%') {
         end -= 1;
         unit = Unit.percent;
      } else if (end > 2 && Character.isLetter(lastChar) && Character.isLetter(val.charAt(end-2))) {
         end -= 2;
         String unitStr = val.substring(end);
         try {
            unit = Unit.valueOf(unitStr.toLowerCase(Locale.US));
         } catch (IllegalArgumentException e) {
            throw new SVGParseException("Invalid length unit specifier: "+val);
         }
      }
      try
      {
         float scalar = parseFloat(val, 0, end);
         return new Length(scalar, unit);
      }
      catch (NumberFormatException e)
      {
         throw new SVGParseException("Invalid length value: "+val, e);
      }
   }


   /*
    * Parse a list of Length/Coords
    */
   private static List<Length>  parseLengthList(String val) throws SVGParseException
   {
      if (val.length() == 0)
         throw new SVGParseException("Invalid length list (empty string)");

      List<Length>  coords = new ArrayList<>(1);

      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         float scalar = scan.nextFloat();
         if (Float.isNaN(scalar))
            throw new SVGParseException("Invalid length list value: "+scan.ahead());
         Unit  unit = scan.nextUnit();
         if (unit == null)
            unit = Unit.px;
         coords.add(new Length(scalar, unit));
         scan.skipCommaWhitespace();
      }
      return coords;
   }


   /*
    * Parse a generic float value.
    */
   static float  parseFloat(String val) throws SVGParseException
   {
      int  len = val.length();
      if (len == 0)
         throw new SVGParseException("Invalid float value (empty string)");
      return parseFloat(val, 0, len);
   }

   private static float  parseFloat(String val, int offset, int len) throws SVGParseException
   {
      NumberParser np = new NumberParser();
      float  num = np.parseNumber(val, offset, len);
      if (!Float.isNaN(num)) {
         return num;
      } else {
         throw new SVGParseException("Invalid float value: "+val);
      }
   }


   /*
    * Parse an opacity value (a float clamped to the range 0..1).
    */
   static Float  parseOpacity(String val)
   {
      try {
         float  o = parseFloat(val);
         return (o < 0f) ? 0f : Math.min(o, 1f);
      } catch (SVGParseException e) {
         return null;
      }
   }


   /*
    * Parse a viewBox attribute.
    */
   private static Box  parseViewBox(String val) throws SVGParseException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      float minX = scan.nextFloat();
      scan.skipCommaWhitespace();
      float minY = scan.nextFloat();
      scan.skipCommaWhitespace();
      float width = scan.nextFloat();
      scan.skipCommaWhitespace();
      float height = scan.nextFloat();

      if (Float.isNaN(minX) || Float.isNaN(minY) || Float.isNaN(width) || Float.isNaN(height))
         throw new SVGParseException("Invalid viewBox definition - should have four numbers");
      if (width < 0)
         throw new SVGParseException("Invalid viewBox. width cannot be negative");
      if (height < 0)
         throw new SVGParseException("Invalid viewBox. height cannot be negative");

      return new SVGBase.Box(minX, minY, width, height);
   }


   /*
    * Parse a preserveAspectRation attribute
    */
   private static void  parsePreserveAspectRatio(SVGBase.SvgPreserveAspectRatioContainer obj, String val) throws SVGParseException
   {
      obj.preserveAspectRatio = PreserveAspectRatio.of(val);
   }


   /*
    * Parse a paint specifier such as in the fill and stroke attributes.
    */
   static SvgPaint parsePaintSpecifier(String val)
   {
      if (val.startsWith("url("))
      {
         int  closeBracket = val.indexOf(")"); 
         if (closeBracket != -1)
         {
            String    href = val.substring(4, closeBracket).trim();
            SvgPaint  fallback = null;

            val = val.substring(closeBracket+1).trim();
            if (val.length() > 0)
               fallback = parseColourSpecifer(val);
            return new PaintReference(href, fallback);
         }
         else
         {
            String    href = val.substring(4).trim();
            return new PaintReference(href, null);
         }
      }
      return parseColourSpecifer(val);
   }


   private static SvgPaint parseColourSpecifer(String val)
   {
      switch (val) {
         case NONE:
            return Colour.TRANSPARENT;
         case CURRENTCOLOR:
            return CurrentColor.getInstance();
         default:
            return parseColour(val);
      }
   }


   /*
    * Parse a colour definition.
    */
   static Colour  parseColour(String val)
   {
      if (val.charAt(0) == '#')
      {
         IntegerParser ip = IntegerParser.parseHex(val, 1, val.length());
         if (ip == null) {
            return Colour.BLACK;
         }
         int  pos = ip.getEndPos();
         int  h1, h2, h3, h4;
         switch (pos) {
            case 4:
               int threehex = ip.value();
               h1 = threehex & 0xf00;  // r
               h2 = threehex & 0x0f0;  // g
               h3 = threehex & 0x00f;  // b
               return new Colour(0xff000000|h1<<12|h1<<8|h2<<8|h2<<4|h3<<4|h3);
            case 5:
               int fourhex = ip.value();
               h1 = fourhex & 0xf000;  // r
               h2 = fourhex & 0x0f00;  // g
               h3 = fourhex & 0x00f0;  // b
               h4 = fourhex & 0x000f;  // alpha
               return new Colour(h4<<28|h4<<24 | h1<<8|h1<<4 | h2<<4|h2 | h3|h3>>4);
            case 7:
               return new Colour(0xff000000 | ip.value());
            case 9:
               return new Colour(ip.value() << 24 | ip.value() >>> 8);
            default:
               // Hex value had bad length for a colour
               return Colour.BLACK;
         }
      }

      // Parse an rgb() or rgba() colour.
      // In CSS Color 4, these are synonyms, and the alpha parameter is optional in both cases.
      String   valLowerCase = val.toLowerCase(Locale.US);
      boolean  isRGBA = valLowerCase.startsWith("rgba(");
      if (isRGBA || valLowerCase.startsWith("rgb("))
      {
         TextScanner  scan = new TextScanner(val.substring(isRGBA ? 5 : 4));
         scan.skipWhitespace();

         float  red = scan.nextFloat();
         if (!Float.isNaN(red)) {
            if (scan.consume('%'))
               red = (red * 256) / 100;

            // If there is a comma, then it is the "legacy" format: rgb(r, g, b, a?).
            // Otherwise we assume it is the new format: rgb[a?](r g b / a?).
            boolean isLegacyCSSColor3 = scan.skipCommaWhitespace();

            float green = scan.nextFloat();
            if (!Float.isNaN(green)) {
               if (scan.consume('%'))
                  green = (green * 256) / 100;

               if (isLegacyCSSColor3) {
                  if (!scan.skipCommaWhitespace())
                     return Colour.BLACK;   // Error
               } else {
                  scan.skipWhitespace();
               }

               float blue = scan.nextFloat();
               if (!Float.isNaN(blue)) {
                  if (scan.consume('%'))
                     blue = (blue * 256) / 100;

                  // Now look for optional alpha
                  float alpha = Float.NaN;
                  if (isLegacyCSSColor3) {
                     if (scan.skipCommaWhitespace())
                        alpha = scan.nextFloat();
                  } else {
                     scan.skipWhitespace();
                     if (scan.consume('/')) {
                        scan.skipWhitespace();
                        alpha = scan.nextFloat();
                     }
                  }
                  scan.skipWhitespace();
                  if (!scan.consume(')'))
                     return Colour.BLACK;
                  if (Float.isNaN(alpha))
                     return new Colour( 0xff000000 | clamp255(red)<<16 | clamp255(green)<<8 | clamp255(blue) );
                  else
                     return new Colour( clamp255(alpha * 256)<<24 | clamp255(red)<<16 | clamp255(green)<<8 | clamp255(blue) );
               }
            }
         }
      }
      else
      {
         // Parse an hsl() or hsla() colour.
         // In CSS Color 4, these are synonyms, and the alpha parameter is optional in both cases.
         boolean  isHSLA = valLowerCase.startsWith("hsla(");
         if (isHSLA || valLowerCase.startsWith("hsl("))
         {
            TextScanner  scan = new TextScanner(val.substring(isHSLA ? 5 : 4));
            scan.skipWhitespace();

            float  hue = scan.nextFloat();
            if (!Float.isNaN(hue)) {
               scan.consume("deg");  // Optional units

               // If there is a comma, then it is the "legacy" format: rgb(r, g, b, a?).
               // Otherwise we assume it is the new format: rgb[a?](r g b / a?).
               boolean isLegacyCSSColor3 = scan.skipCommaWhitespace();

               float saturation = scan.nextFloat();
               if (!Float.isNaN(saturation)) {
                  if (!scan.consume('%'))
                     return Colour.BLACK;

                  if (isLegacyCSSColor3) {
                     if (!scan.skipCommaWhitespace())
                        return Colour.BLACK;
                  } else {
                     scan.skipWhitespace();
                  }

                  float lightness = scan.nextFloat();
                  if (!Float.isNaN(lightness)) {
                     if (!scan.consume('%'))
                        return Colour.BLACK;

                     // Now look for optional alpha
                     float alpha = Float.NaN;
                     if (isLegacyCSSColor3) {
                        if (scan.skipCommaWhitespace())
                           alpha = scan.nextFloat();
                     } else {
                        scan.skipWhitespace();
                        if (scan.consume('/')) {
                           scan.skipWhitespace();
                           alpha = scan.nextFloat();
                        }
                     }
                     scan.skipWhitespace();
                     if (!scan.consume(')'))
                        return Colour.BLACK;
                     if (Float.isNaN(alpha))
                        return new Colour( 0xff000000 | hslToRgb(hue, saturation, lightness) );
                     else
                        return new Colour( clamp255(alpha * 256)<<24 | hslToRgb(hue, saturation, lightness) );
                  }
               }
            }
         }
      }

      // Must be a colour keyword
      return parseColourKeyword(valLowerCase);
   }


   // Clamp a float to the range 0..255
   private static int  clamp255(float val)
   {
      return (val < 0) ? 0 : (val > 255) ? 255 : Math.round(val);
   }


   // Hue (degrees), saturation [0, 100], lightness [0, 100]
   private static int  hslToRgb(float hue, float sat, float light)
   {
      hue = (hue >= 0f) ? hue % 360f : (hue % 360f) + 360f;  // positive modulo (ie. -10 => 350)
      hue /= 60f;    // [0, 360] -> [0, 6]
      sat /= 100;   // [0, 100] -> [0, 1]
      light /= 100; // [0, 100] -> [0, 1]
      sat = (sat < 0f) ? 0f : Math.min(sat, 1f);
      light = (light < 0f) ? 0f : Math.min(light, 1f);
      float  t1, t2;
      if (light <= 0.5f) {
         t2 = light * (sat + 1f);
      } else {
         t2 = light + sat - (light * sat);
      }
      t1 = light * 2f - t2;
      float  r = hueToRgb(t1, t2, hue + 2f);
      float  g = hueToRgb(t1, t2, hue);
      float  b = hueToRgb(t1, t2, hue - 2f);
      return clamp255(r * 256f)<<16 | clamp255(g * 256f)<<8 | clamp255(b * 256f);
   }

   private static float  hueToRgb(float t1, float t2, float hue) {
      if (hue < 0f) hue += 6f;
      if (hue >= 6f) hue -= 6f;

      if (hue < 1) return (t2 - t1) * hue + t1;
      else if (hue < 3f) return t2;
      else if (hue < 4f) return (t2 - t1) * (4f - hue) + t1;
      else return t1;
   }


   // Parse a colour component value (0..255 or 0%-100%)
   private static Colour  parseColourKeyword(String nameLowerCase)
   {
      Integer  col = ColourKeywords.get(nameLowerCase);
      return (col == null) ? Colour.BLACK : new Colour(col);
   }


   // Parse a font attribute
   // [ [ <'font-style'> || <'font-variant'> || <'font-weight'> ]? <'font-size'> [ / <'line-height'> ]? <'font-family'> ] | caption | icon | menu | message-box | small-caption | status-bar | inherit
   static void  parseFont(Style style, String val)
   {
      Float            fontWeight = null;
      Style.FontStyle  fontStyle = null;
      String           fontVariant = null;
      Float            fontStretch = null;
      Boolean          fontVariantSmallCaps = null;

      final String  NORMAL = "normal";

      // Start by checking for the fixed size standard system font names (which we don't support)
      if ("|caption|icon|menu|message-box|small-caption|status-bar|".contains('|'+val+'|'))
         return;
         
      // First part: style/variant/weight (opt - one or more)
      TextScanner  scan = new TextScanner(val);
      String       item;
      while (true)
      {
         item = scan.nextToken('/');
         scan.skipWhitespace();
         if (item == null)
            return;
         if (fontWeight != null && fontStyle != null)
            break;
         if (item.equals(NORMAL))  {
            // indeterminate right now which of these this refers to
            continue;
         }
         if (fontWeight == null && FontWeightKeywords.contains(item)) {
            fontWeight = FontWeightKeywords.get(item);
            continue;
         }
         if (fontStyle == null) {
            fontStyle = parseFontStyle(item);
            if (fontStyle != null)
               continue;
         }
         // Must be a font-variant keyword?
         if (fontVariantSmallCaps == null && item.equals(CSSFontFeatureSettings.FONT_VARIANT_SMALL_CAPS)) {
            fontVariantSmallCaps = true;
            continue;
         }
         if (fontStretch == null && FontStretchKeywords.contains(item)) {
            fontStretch = FontStretchKeywords.get(item);
            continue;
         }
         // Not any of these. Break and try next section
         break;
      }

      // Second part: font size (reqd) and line-height (opt)
      Length  fontSize = parseFontSize(item);

      // Check for line-height (which we don't support)
      if (scan.consume('/'))
      {
         scan.skipWhitespace();
         item = scan.nextToken();
         if (item != null) {
            try {
               parseLength(item);
            } catch (SVGParseException e) {
               return;
            }
         }
         scan.skipWhitespace();
      }
      
      // Third part: font family
      style.fontFamily = parseFontFamily(scan.restOfText());

      style.fontSize = fontSize;
      style.fontWeight = (fontWeight == null) ? Style.FONT_WEIGHT_NORMAL : fontWeight;
      style.fontStyle = (fontStyle == null) ? Style.FontStyle.normal : fontStyle;
      style.fontStretch = (fontStretch == null) ? Style.FONT_STRETCH_NORMAL : fontStretch;
      style.fontKerning = Style.FontKerning.auto;
      style.fontVariantLigatures = CSSFontFeatureSettings.LIGATURES_NORMAL;
      style.fontVariantPosition = CSSFontFeatureSettings.POSITION_ALL_OFF;
      style.fontVariantCaps = CSSFontFeatureSettings.CAPS_ALL_OFF;
      if (fontVariantSmallCaps == Boolean.TRUE)
         style.fontVariantCaps = CSSFontFeatureSettings.makeSmallCaps();
      style.fontVariantNumeric =  CSSFontFeatureSettings.NUMERIC_ALL_OFF;
      style.fontVariantEastAsian =  CSSFontFeatureSettings.EAST_ASIAN_ALL_OFF;
      style.fontFeatureSettings = CSSFontFeatureSettings.FONT_FEATURE_SETTINGS_NORMAL;
      style.fontVariationSettings = null;

      style.specifiedFlags |= (Style.SPECIFIED_FONT_FAMILY | Style.SPECIFIED_FONT_SIZE | Style.SPECIFIED_FONT_WEIGHT | Style.SPECIFIED_FONT_STYLE | Style.SPECIFIED_FONT_STRETCH |
                               Style.SPECIFIED_FONT_KERNING | Style.SPECIFIED_FONT_VARIANT_LIGATURES | Style.SPECIFIED_FONT_VARIANT_POSITION | Style.SPECIFIED_FONT_VARIANT_CAPS |
                               Style.SPECIFIED_FONT_VARIANT_NUMERIC | Style.SPECIFIED_FONT_VARIANT_EAST_ASIAN | Style.SPECIFIED_FONT_FEATURE_SETTINGS | Style.SPECIFIED_FONT_VARIATION_SETTINGS);
   }


   // Parse a font family list
   static List<String>  parseFontFamily(String val)
   {
      List<String> fonts = null;
      TextScanner  scan = new TextScanner(val);
      while (true)
      {
         String item = scan.nextQuotedString();
         if (item == null)
            item = scan.nextTokenWithWhitespace(',');
         if (item == null)
            break;
         if (fonts == null)
            fonts = new ArrayList<>();
         fonts.add(item);
         scan.skipCommaWhitespace();
         if (scan.empty())
            break;
      }
      return fonts;
   }


   // Parse a font size keyword or numerical value
   static Length  parseFontSize(String val)
   {
      try {
         Length  size = FontSizeKeywords.get(val);
         if (size == null)
            size = parseLength(val);
         return size;
      } catch (SVGParseException e) {
         return null;
      }
   }


   // Parse a font weight keyword or numerical value
   static Float  parseFontWeight(String val)
   {
      Float  result = FontWeightKeywords.get(val);
      if (result == null) {
         // Check for a number
         TextScanner  scan = new TextScanner(val);
         result = scan.nextFloat();
         scan.skipWhitespace();
         if (!scan.empty())
            return null;
         if (result < Style.FONT_WEIGHT_MIN || result > Style.FONT_WEIGHT_MAX)
            return null;   // Invalid
      }
      return result;
   }


   // Parse a font stretch keyword or numerical value
   static Float  parseFontStretch(String val)
   {
      Float  result = FontStretchKeywords.get(val);
      if (result == null) {
         // Check for a percentage value
         TextScanner  scan = new TextScanner(val);
         result = scan.nextFloat();
         if (!scan.consume('%'))
            return null;
         scan.skipWhitespace();
         if (!scan.empty())
            return null;
         if (result < Style.FONT_STRETCH_MIN)
            return null;   // Invalid
      }
      return result;
   }


   // Parse a font style keyword
   static Style.FontStyle  parseFontStyle(String val)
   {
      // Italic is probably the most common, so test that first :)
      switch (val)
      {
         case "italic":  return Style.FontStyle.italic;
         case "normal":  return Style.FontStyle.normal;
         case "oblique": return Style.FontStyle.oblique;
         default:        return null;
      }
   }


   // Parse a text decoration keyword
   static TextDecoration  parseTextDecoration(String val)
   {
      switch (val)
      {
         case NONE:           return Style.TextDecoration.None;
         case "underline":    return Style.TextDecoration.Underline;
         case "overline":     return Style.TextDecoration.Overline;
         case "line-through": return Style.TextDecoration.LineThrough;
         case "blink":        return Style.TextDecoration.Blink;
         default:             return null;
      }
   }


   // Parse a text decoration keyword
   static TextDirection  parseTextDirection(String val)
   {
      switch (val)
      {
         case "ltr": return Style.TextDirection.LTR;
         case "rtl": return Style.TextDirection.RTL;
         default:    return null;
      }
   }


   // Parse fill rule
   static Style.FillRule  parseFillRule(String val)
   {
      if ("nonzero".equals(val))
         return Style.FillRule.NonZero;
      if ("evenodd".equals(val))
         return Style.FillRule.EvenOdd;
      return null;
   }


   // Parse stroke-linecap
   static Style.LineCap  parseStrokeLineCap(String val)
   {
      if ("butt".equals(val))
         return Style.LineCap.Butt;
      if ("round".equals(val))
         return Style.LineCap.Round;
      if ("square".equals(val))
         return Style.LineCap.Square;
      return null;
   }


   // Parse stroke-linejoin
   static Style.LineJoin  parseStrokeLineJoin(String val)
   {
      if ("miter".equals(val))
         return Style.LineJoin.Miter;
      if ("round".equals(val))
         return Style.LineJoin.Round;
      if ("bevel".equals(val))
         return Style.LineJoin.Bevel;
      return null;
   }


   // Parse stroke-dasharray
   static Length[]  parseStrokeDashArray(String val)
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      if (scan.empty())
         return null;
      
      Length dash = scan.nextLength();
      if (dash == null)
         return null;
      if (dash.isNegative())
         return null;

      float sum = dash.floatValue();

      List<Length> dashes = new ArrayList<>();
      dashes.add(dash);
      while (!scan.empty())
      {
         scan.skipCommaWhitespace();
         dash = scan.nextLength();
         if (dash == null)  // must have hit something unexpected
            return null;
         if (dash.isNegative())
            return null;
         dashes.add(dash);
         sum += dash.floatValue();
      }

      // Spec (section 11.4) says if the sum of dash lengths is zero, it should
      // be treated as "none" ie a solid stroke.
      if (sum == 0f)
         return null;
      
      return dashes.toArray(new Length[0]);
   }


   // Parse a text anchor keyword
   static Style.TextAnchor  parseTextAnchor(String val)
   {
      switch (val)
      {
         case "start":  return Style.TextAnchor.Start;
         case "middle": return Style.TextAnchor.Middle;
         case "end":    return Style.TextAnchor.End;
         default:       return null;
      }
   }


   // Parse a text anchor keyword
   static Boolean  parseOverflow(String val)
   {
      switch (val)
      {
         case "visible":
         case "auto":
            return Boolean.TRUE;
         case "hidden":
         case "scroll":
            return Boolean.FALSE;
         default:
            return null;
      }
   }


   // Parse CSS clip shape (always a rect())
   static CSSClipRect  parseClip(String val)
   {
      if ("auto".equals(val))
         return null;
      if (!val.startsWith("rect("))
         return null;

      TextScanner scan = new TextScanner(val.substring(5));
      scan.skipWhitespace();

      Length top = parseLengthOrAuto(scan);
      scan.skipCommaWhitespace();
      Length right = parseLengthOrAuto(scan);
      scan.skipCommaWhitespace();
      Length bottom = parseLengthOrAuto(scan);
      scan.skipCommaWhitespace();
      Length left = parseLengthOrAuto(scan);

      scan.skipWhitespace();
      if (!scan.consume(')') && !scan.empty())   // Be forgibing. Allow missing ')'.
         return null;

      return new CSSClipRect(top, right, bottom, left);
   }


   private static Length parseLengthOrAuto(TextScanner scan)
   {
      if (scan.consume("auto"))
         return Length.ZERO;

      return scan.nextLength();
   }


   // Parse a vector effect keyword
   static VectorEffect  parseVectorEffect(String val)
   {
      switch (val)
      {
         case NONE:                 return Style.VectorEffect.None;
         case "non-scaling-stroke": return Style.VectorEffect.NonScalingStroke;
         default:                   return null;
      }
   }


   // Parse a rendering quality property
   static RenderQuality  parseRenderQuality(String val)
   {
      switch (val)
      {
         case "auto":            return RenderQuality.auto;
         case "optimizeQuality": return RenderQuality.optimizeQuality;
         case "optimizeSpeed":   return RenderQuality.optimizeSpeed;
         default:                return null;
      }
   }


   // Parse a isolation property
   static Isolation  parseIsolation(String val)
   {
      switch (val)
      {
         case "auto":    return Isolation.auto;
         case "isolate": return Style.Isolation.isolate;
         default:        return null;
      }
   }


   static Length  parseLetterOrWordSpacing(String val)
   {
      if ("normal".equals(val))
         return Length.ZERO;
      else {
         try {
            Length result = parseLength(val);
            // Percent units were removed in SVG2 and are treated as an error.
            return (result.unit == Unit.percent) ? null : result;
         } catch (SVGParseException e) {
            return null;
         }
      }
   }


   //=========================================================================


   // Parse the string that defines a path.
   protected static SVGBase.PathDefinition  parsePath(String val)
   {
      TextScanner  scan = new TextScanner(val);

      float   currentX = 0f, currentY = 0f;    // The last point visited in the subpath
      float   lastMoveX = 0f, lastMoveY = 0f;  // The initial point of current subpath
      float   lastControlX = 0f, lastControlY = 0f;  // Last control point of the just completed bezier curve.
      float   x,y, x1,y1, x2,y2;
      float   rx,ry, xAxisRotation;
      Boolean largeArcFlag, sweepFlag;

      SVGBase.PathDefinition  path = new SVGBase.PathDefinition();

      if (scan.empty())
         return path;

      int  pathCommand = scan.nextChar();

      if (pathCommand != 'M' && pathCommand != 'm')
         return path;  // Invalid path - doesn't start with a move

      while (true)
      {
         scan.skipWhitespace();

         switch (pathCommand)
         {
            // Move
            case 'M':
            case 'm':
               x = scan.nextFloat();
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               // Relative moveto at the start of a path is treated as an absolute moveto.
               if (pathCommand=='m' && !path.isEmpty()) {
                  x += currentX;
                  y += currentY;
               }
               path.moveTo(x, y);
               currentX = lastMoveX = lastControlX = x;
               currentY = lastMoveY = lastControlY = y;
               // Any subsequent coord pairs should be treated as a lineto.
               pathCommand = (pathCommand=='m') ? 'l' : 'L';
               break;

               // Line
            case 'L':
            case 'l':
               x = scan.nextFloat();
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='l') {
                  x += currentX;
                  y += currentY;
               }
               path.lineTo(x, y);
               currentX = lastControlX = x;
               currentY = lastControlY = y;
               break;

               // Cubic bezier
            case 'C':
            case 'c':
               x1 = scan.nextFloat();
               y1 = scan.checkedNextFloat(x1);
               x2 = scan.checkedNextFloat(y1);
               y2 = scan.checkedNextFloat(x2);
               x = scan.checkedNextFloat(y2);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='c') {
                  x += currentX;
                  y += currentY;
                  x1 += currentX;
                  y1 += currentY;
                  x2 += currentX;
                  y2 += currentY;
               }
               path.cubicTo(x1, y1, x2, y2, x, y);
               lastControlX = x2;
               lastControlY = y2;
               currentX = x;
               currentY = y;
               break;

               // Smooth curve (first control point calculated)
            case 'S':
            case 's':
               x1 = 2 * currentX - lastControlX;
               y1 = 2 * currentY - lastControlY;
               x2 = scan.nextFloat();
               y2 = scan.checkedNextFloat(x2);
               x = scan.checkedNextFloat(y2);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='s') {
                  x += currentX;
                  y += currentY;
                  x2 += currentX;
                  y2 += currentY;
               }
               path.cubicTo(x1, y1, x2, y2, x, y);
               lastControlX = x2;
               lastControlY = y2;
               currentX = x;
               currentY = y;
               break;

               // Close path
            case 'Z':
            case 'z':
               path.close();
               currentX = lastControlX = lastMoveX;
               currentY = lastControlY = lastMoveY;
               break;

               // Horizontal line
            case 'H':
            case 'h':
               x = scan.nextFloat();
               if (Float.isNaN(x)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='h') {
                  x += currentX;
               }
               path.lineTo(x, currentY);
               currentX = lastControlX = x;
               lastControlY = currentY;
               break;

               // Vertical line
            case 'V':
            case 'v':
               y = scan.nextFloat();
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='v') {
                  y += currentY;
               }
               path.lineTo(currentX, y);
               lastControlX = currentX;
               currentY = lastControlY = y;
               break;

               // Quadratic bezier
            case 'Q':
            case 'q':
               x1 = scan.nextFloat();
               y1 = scan.checkedNextFloat(x1);
               x = scan.checkedNextFloat(y1);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='q') {
                  x += currentX;
                  y += currentY;
                  x1 += currentX;
                  y1 += currentY;
               }
               path.quadTo(x1, y1, x, y);
               lastControlX = x1;
               lastControlY = y1;
               currentX = x;
               currentY = y;
               break;

               // Smooth quadratic bezier
            case 'T':
            case 't':
               x1 = 2 * currentX - lastControlX;
               y1 = 2 * currentY - lastControlY;
               x = scan.nextFloat();
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='t') {
                  x += currentX;
                  y += currentY;
               }
               path.quadTo(x1, y1, x, y);
               lastControlX = x1;
               lastControlY = y1;
               currentX = x;
               currentY = y;
               break;

               // Arc
            case 'A':
            case 'a':
               rx = scan.nextFloat();
               ry = scan.checkedNextFloat(rx);
               xAxisRotation = scan.checkedNextFloat(ry);
               largeArcFlag = scan.checkedNextFlag(xAxisRotation);
               sweepFlag = scan.checkedNextFlag(largeArcFlag);
               x = scan.checkedNextFloat(sweepFlag);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y) || rx < 0 || ry < 0) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='a') {
                  x += currentX;
                  y += currentY;
               }
               path.arcTo(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y);
               currentX = lastControlX = x;
               currentY = lastControlY = y;
               break;

            default:
               return path;
         }

         scan.skipCommaWhitespace();
         if (scan.empty())
            break;

         // Test to see if there is another set of coords for the current path command
         if (scan.hasLetter()) {
            // Nope, so get the new path command instead
            pathCommand = scan.nextChar();
         }
      }
      return path;
   }


   //=========================================================================
   // Conditional processing (ie for <switch> element)

   
   // Parse the attribute that declares the list of SVG features that must be
   // supported if we are to render this element
   private static Set<String>  parseRequiredFeatures(String val)
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<>();

      while (!scan.empty())
      {
         String feature = scan.nextToken();
         if (feature.startsWith(FEATURE_STRING_PREFIX)) {
            result.add(feature.substring(FEATURE_STRING_PREFIX.length()));
         } else {
            // Not a feature string we recognise or support. (In order to avoid accidentally
            // matches with our truncated feature strings, we'll replace it with a string
            // we know for sure won't match anything.
            result.add("UNSUPPORTED");
         }
         scan.skipWhitespace();
      }
      return result;
   }


   // Parse the attribute that declares the list of languages, one of which
   // must be supported if we are to render this element
   private static Set<String>  parseSystemLanguage(String val)
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<>();

      while (!scan.empty())
      {
         String language = scan.nextToken();
         int  hyphenPos = language.indexOf('-'); 
         if (hyphenPos != -1) {
            language = language.substring(0, hyphenPos);
         }
         // Get canonical version of language code in case it has changed (see the JavaDoc for Locale.getLanguage())
         language = new Locale.Builder().setLanguage(language).build().getLanguage();
         result.add(language);
         scan.skipWhitespace();
      }
      return result;
   }


   // Parse the attribute that declares the list of MIME types that must be
   // supported if we are to render this element
   private static Set<String>  parseRequiredFormats(String val)
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<>();

      while (!scan.empty())
      {
         String mimetype = scan.nextToken();
         result.add(mimetype);
         scan.skipWhitespace();
      }
      return result;
   }


   static String  parseFunctionalIRI(String val, String attrName)
   {
      if (val.equals(NONE))
         return null;
      if (!val.startsWith("url("))
         return null;
      if (val.endsWith(")"))
         return val.substring(4, val.length()-1).trim();
      else
         return val.substring(4).trim();
      // Unlike CSS, the SVG spec seems to indicate that quotes are not allowed in "url()" references
   }


   //=========================================================================
   // Parsing <style> element. Very basic CSS parser.
   //=========================================================================


   private void  style(Attributes attributes) throws SVGParseException
   {
      debug("<style>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");

      // Check style sheet is in CSS format
      boolean  isTextCSS = true;
      String   media = "all";

      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case type:
               isTextCSS = val.equals(CSSParser.CSS_MIME_TYPE);
               break;
            case media:
               media = val;
               break;
            default:
               break;
         }
      }

      if (isTextCSS && CSSParser.mediaMatches(media, MediaType.screen)) {
         inStyleElement = true;
      } else {
         ignoring = true;
         ignoreDepth = 1;
      }
   }


   private void  parseCSSStyleSheet(String sheet)
   {
      CSSParser  cssp = new CSSParser(MediaType.screen, CSSParser.Source.Document, externalFileResolver);
      svgDocument.addCSSRules(cssp.parse(sheet));
   }

}
