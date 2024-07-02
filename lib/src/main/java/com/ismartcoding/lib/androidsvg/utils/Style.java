package com.ismartcoding.lib.androidsvg.utils;

import com.ismartcoding.lib.androidsvg.utils.SVGBase.Colour;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.CSSClipRect;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.CurrentColor;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Length;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgPaint;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Unit;
import com.ismartcoding.lib.androidsvg.SVGParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  Style implements Cloneable
{
   // Which properties have been explicitly specified by this element
   long       specifiedFlags = 0;

   SvgPaint   fill;
   FillRule   fillRule;
   Float      fillOpacity;

   SvgPaint   stroke;
   Float      strokeOpacity;
   Length     strokeWidth;
   LineCap    strokeLineCap;
   LineJoin   strokeLineJoin;
   Float      strokeMiterLimit;
   Length[]   strokeDashArray;
   Length     strokeDashOffset;

   Float      opacity; // master opacity of both stroke and fill

   Colour     color;

   List<String>    fontFamily;
   Length          fontSize;
   Float           fontWeight;
   FontStyle       fontStyle;
   Float           fontStretch;
   TextDecoration  textDecoration;
   TextDirection   direction;

   TextAnchor   textAnchor;

   Boolean      overflow;  // true if overflow visible
   CSSClipRect  clip;

   String     markerStart;
   String     markerMid;
   String     markerEnd;

   Boolean    display;    // true if we should display
   Boolean    visibility; // true if visible

   SvgPaint   stopColor;
   Float      stopOpacity;

   String     clipPath;
   FillRule   clipRule;

   String     mask;

   SvgPaint   solidColor;
   Float      solidOpacity;

   SvgPaint   viewportFill;
   Float      viewportFillOpacity;

   VectorEffect  vectorEffect;

   RenderQuality  imageRendering;

   Isolation     isolation;
   CSSBlendMode  mixBlendMode;

   FontKerning               fontKerning;
   CSSFontFeatureSettings    fontVariantLigatures;
   CSSFontFeatureSettings    fontVariantPosition;
   CSSFontFeatureSettings    fontVariantCaps;
   CSSFontFeatureSettings    fontVariantNumeric;
   CSSFontFeatureSettings    fontVariantEastAsian;
   CSSFontFeatureSettings    fontFeatureSettings;
   CSSFontVariationSettings  fontVariationSettings;
   WritingMode               writingMode;
   GlypOrientationVertical   glyphOrientationVertical;
   TextOrientation           textOrientation;

   Length     letterSpacing;
   Length     wordSpacing;


   static final float  FONT_WEIGHT_MIN = 1f;
   static final float  FONT_WEIGHT_NORMAL = 400f;
   static final float  FONT_WEIGHT_BOLD = 700f;
   static final float  FONT_WEIGHT_MAX = 1000f;
   static final float  FONT_WEIGHT_LIGHTER = Float.MIN_VALUE;
   static final float  FONT_WEIGHT_BOLDER = Float.MAX_VALUE;

   static final float  FONT_STRETCH_MIN = 0f;
   static final float  FONT_STRETCH_NORMAL = 100f;


   static final long SPECIFIED_FILL                       = (1<<0);
   static final long SPECIFIED_FILL_RULE                  = (1<<1);
   static final long SPECIFIED_FILL_OPACITY               = (1<<2);
   static final long SPECIFIED_STROKE                     = (1<<3);
   static final long SPECIFIED_STROKE_OPACITY             = (1<<4);
   static final long SPECIFIED_STROKE_WIDTH               = (1<<5);
   static final long SPECIFIED_STROKE_LINECAP             = (1<<6);
   static final long SPECIFIED_STROKE_LINEJOIN            = (1<<7);
   static final long SPECIFIED_STROKE_MITERLIMIT          = (1<<8);
   static final long SPECIFIED_STROKE_DASHARRAY           = (1<<9);
   static final long SPECIFIED_STROKE_DASHOFFSET          = (1<<10);
   static final long SPECIFIED_OPACITY                    = (1<<11);
   static final long SPECIFIED_COLOR                      = (1<<12);
   static final long SPECIFIED_FONT_FAMILY                = (1<<13);
   static final long SPECIFIED_FONT_SIZE                  = (1<<14);
   static final long SPECIFIED_FONT_WEIGHT                = (1<<15);
   static final long SPECIFIED_FONT_STYLE                 = (1<<16);
   static final long SPECIFIED_TEXT_DECORATION            = (1<<17);
   static final long SPECIFIED_TEXT_ANCHOR                = (1<<18);
   static final long SPECIFIED_OVERFLOW                   = (1<<19);
   static final long SPECIFIED_CLIP                       = (1<<20);
   static final long SPECIFIED_MARKER_START               = (1<<21);
   static final long SPECIFIED_MARKER_MID                 = (1<<22);
   static final long SPECIFIED_MARKER_END                 = (1<<23);
   static final long SPECIFIED_DISPLAY                    = (1<<24);
   static final long SPECIFIED_VISIBILITY                 = (1<<25);
   static final long SPECIFIED_STOP_COLOR                 = (1<<26);
   static final long SPECIFIED_STOP_OPACITY               = (1<<27);
   static final long SPECIFIED_CLIP_PATH                  = (1<<28);
   static final long SPECIFIED_CLIP_RULE                  = (1<<29);
   static final long SPECIFIED_MASK                       = (1<<30);
   static final long SPECIFIED_SOLID_COLOR                = (1L<<31);
   static final long SPECIFIED_SOLID_OPACITY              = (1L<<32);
   static final long SPECIFIED_VIEWPORT_FILL              = (1L<<33);
   static final long SPECIFIED_VIEWPORT_FILL_OPACITY      = (1L<<34);
   static final long SPECIFIED_VECTOR_EFFECT              = (1L<<35);
   static final long SPECIFIED_DIRECTION                  = (1L<<36);
   static final long SPECIFIED_IMAGE_RENDERING            = (1L<<37);
   static final long SPECIFIED_ISOLATION                  = (1L<<38);
   static final long SPECIFIED_MIX_BLEND_MODE             = (1L<<39);
   static final long SPECIFIED_FONT_VARIANT_LIGATURES     = (1L<<40);
   static final long SPECIFIED_FONT_VARIANT_POSITION      = (1L<<41);
   static final long SPECIFIED_FONT_VARIANT_CAPS          = (1L<<42);
   static final long SPECIFIED_FONT_VARIANT_NUMERIC       = (1L<<43);
   static final long SPECIFIED_FONT_VARIANT_EAST_ASIAN    = (1L<<44);
   static final long SPECIFIED_FONT_FEATURE_SETTINGS      = (1L<<45);
   static final long SPECIFIED_WRITING_MODE               = (1L<<46);
   static final long SPECIFIED_GLYPH_ORIENTATION_VERTICAL = (1L<<47);
   static final long SPECIFIED_TEXT_ORIENTATION           = (1L<<48);
   static final long SPECIFIED_FONT_KERNING               = (1L<<49);
   static final long SPECIFIED_FONT_VARIATION_SETTINGS    = (1L<<50);
   static final long SPECIFIED_FONT_STRETCH               = (1L<<51);
   static final long SPECIFIED_LETTER_SPACING             = (1L<<52);
   static final long SPECIFIED_WORD_SPACING               = (1L<<53);

   // Flags for the settings that are applied to reset the root style
   private static final long SPECIFIED_RESET = 0xffffffffffffffffL &
                           ~(SPECIFIED_FONT_VARIANT_LIGATURES  |
                             SPECIFIED_FONT_VARIANT_POSITION   |
                             SPECIFIED_FONT_VARIANT_CAPS       |
                             SPECIFIED_FONT_VARIANT_NUMERIC    |
                             SPECIFIED_FONT_VARIANT_EAST_ASIAN |
                             SPECIFIED_FONT_VARIATION_SETTINGS);


   public enum FillRule
   {
      NonZero,
      EvenOdd
   }

   public enum LineCap
   {
      Butt,
      Round,
      Square
   }

   public enum LineJoin
   {
      Miter,
      Round,
      Bevel
   }

   public enum FontStyle
   {
      normal,
      italic,
      oblique
   }

   public enum TextAnchor
   {
      Start,
      Middle,
      End
   }

   public enum TextDecoration
   {
      None,
      Underline,
      Overline,
      LineThrough,
      Blink
   }

   public enum TextDirection
   {
      LTR,
      RTL
   }

   public enum VectorEffect
   {
      None,
      NonScalingStroke
   }

   public enum RenderQuality
   {
      auto,
      optimizeQuality,
      optimizeSpeed
   }

   public enum Isolation
   {
      auto,
      isolate
   }

   public enum CSSBlendMode
   {
      normal,
      multiply,
      screen,
      overlay,
      darken,
      lighten,
      color_dodge,
      color_burn,
      hard_light,
      soft_light,
      difference,
      exclusion,
      hue,
      saturation,
      color,
      luminosity,
      UNSUPPORTED;

      private static final Map<String, CSSBlendMode> cache = new HashMap<>();

      static {
         for (CSSBlendMode mode : values()) {
            if (mode != UNSUPPORTED) {
               final String key = mode.name().replace('_', '-');
               cache.put(key, mode);
            }
         }
      }

      public static CSSBlendMode fromString(String str)
      {
         // First check cache to see if it is there
         CSSBlendMode mode = cache.get(str);
         if (mode != null) {
            return mode;
         }

         return UNSUPPORTED;
      }
   }


   public enum  FontKerning
   {
      auto,
      normal,
      none
   }

   public enum  WritingMode
   {
      // Old SVG 1.1 values
      lr_tb,
      rl_tb,
      tb_rl,
      lr,
      rl,
      tb,
      // New CSS3 values
      horizontal_tb,
      vertical_rl,
      vertical_lr
   }


   public enum  GlypOrientationVertical
   {
      auto,
      angle0,
      angle90,
      angle180,
      angle270
   }


   public enum  TextOrientation
   {
      mixed,
      upright,
      sideways
   }


   static Style  getDefaultStyle()
   {
      Style  def = new Style();

      def.fill = Colour.BLACK;
      def.fillRule = FillRule.NonZero;
      def.fillOpacity = 1f;
      def.stroke = null;         // none
      def.strokeOpacity = 1f;
      def.strokeWidth = new Length(1f);
      def.strokeLineCap = LineCap.Butt;
      def.strokeLineJoin = LineJoin.Miter;
      def.strokeMiterLimit = 4f;
      def.strokeDashArray = null;
      def.strokeDashOffset = Length.ZERO;
      def.opacity = 1f;
      def.color = Colour.BLACK; // currentColor defaults to black
      def.fontFamily = null;
      def.fontSize = new Length(12, Unit.pt);
      def.fontWeight = FONT_WEIGHT_NORMAL;
      def.fontStyle = FontStyle.normal;
      def.fontStretch = FONT_STRETCH_NORMAL;
      def.textDecoration = TextDecoration.None;
      def.direction = TextDirection.LTR;
      def.textAnchor = TextAnchor.Start;
      def.overflow = true;  // Overflow shown/visible for root, but not for other elements (see section 14.3.3).
      def.clip = null;
      def.markerStart = null;
      def.markerMid = null;
      def.markerEnd = null;
      def.display = Boolean.TRUE;
      def.visibility = Boolean.TRUE;
      def.stopColor = Colour.BLACK;
      def.stopOpacity = 1f;
      def.clipPath = null;
      def.clipRule = FillRule.NonZero;
      def.mask = null;
      def.solidColor = null;
      def.solidOpacity = 1f;
      def.viewportFill = null;
      def.viewportFillOpacity = 1f;
      def.vectorEffect = VectorEffect.None;
      def.imageRendering = RenderQuality.auto;
      def.isolation = Isolation.auto;
      def.mixBlendMode = CSSBlendMode.normal;
      def.fontKerning = FontKerning.auto;
      def.fontVariantLigatures = CSSFontFeatureSettings.LIGATURES_NORMAL;
      def.fontVariantPosition = CSSFontFeatureSettings.POSITION_ALL_OFF;
      def.fontVariantCaps =  CSSFontFeatureSettings.CAPS_ALL_OFF;
      def.fontVariantNumeric =  CSSFontFeatureSettings.NUMERIC_ALL_OFF;
      def.fontVariantEastAsian =  CSSFontFeatureSettings.EAST_ASIAN_ALL_OFF;
      def.fontFeatureSettings = CSSFontFeatureSettings.FONT_FEATURE_SETTINGS_NORMAL;
      def.fontVariationSettings = null;
      def.letterSpacing = Length.ZERO;
      def.wordSpacing = Length.ZERO;
      def.writingMode = WritingMode.horizontal_tb;
      def.glyphOrientationVertical = GlypOrientationVertical.auto;
      def.textOrientation = TextOrientation.mixed;

      def.specifiedFlags = SPECIFIED_RESET;
      //def.inheritFlags = 0;

      return def;
   }


   // Called on the state.style object to reset the properties that don't inherit
   // from the parent style.
   void  resetNonInheritingProperties(boolean isRootSVG)
   {
      this.display = Boolean.TRUE;
      this.overflow = isRootSVG ? Boolean.TRUE : Boolean.FALSE;
      this.clip = null;
      this.clipPath = null;
      this.opacity = 1f;
      this.stopColor = Colour.BLACK;
      this.stopOpacity = 1f;
      this.mask = null;
      this.solidColor = null;
      this.solidOpacity = 1f;
      this.viewportFill = null;
      this.viewportFillOpacity = 1f;
      this.vectorEffect = VectorEffect.None;
      this.isolation = Isolation.auto;
      this.mixBlendMode = CSSBlendMode.normal;
   }


   @Override
   protected Object  clone() throws CloneNotSupportedException
   {
      Style obj = (Style) super.clone();
      if (strokeDashArray != null) {
         obj.strokeDashArray = strokeDashArray.clone();
      }
      return obj;
   }


   static void  processStyleProperty(Style style, String localName, String val, boolean isFromAttribute)
   {
      if (val.length() == 0) { // The spec doesn't say how to handle empty style attributes.
         return;               // Our strategy is just to ignore them.
      }
      if (val.equals("inherit"))
         return;

      switch (SVGParserImpl.SVGAttr.fromString(localName))
      {
         case fill:
            style.fill = SVGParserImpl.parsePaintSpecifier(val);
            if (style.fill != null)
               style.specifiedFlags |= SPECIFIED_FILL;
            break;

         case fill_rule:
            style.fillRule = SVGParserImpl.parseFillRule(val);
            if (style.fillRule != null)
               style.specifiedFlags |= SPECIFIED_FILL_RULE;
            break;

         case fill_opacity:
            style.fillOpacity = SVGParserImpl.parseOpacity(val);
            if (style.fillOpacity != null)
               style.specifiedFlags |= SPECIFIED_FILL_OPACITY;
            break;

         case stroke:
            style.stroke = SVGParserImpl.parsePaintSpecifier(val);
            if (style.stroke != null)
               style.specifiedFlags |= SPECIFIED_STROKE;
            break;

         case stroke_opacity:
            style.strokeOpacity = SVGParserImpl.parseOpacity(val);
            if (style.strokeOpacity != null)
               style.specifiedFlags |= SPECIFIED_STROKE_OPACITY;
            break;

         case stroke_width:
            try {
               style.strokeWidth = SVGParserImpl.parseLength(val);
               style.specifiedFlags |= SPECIFIED_STROKE_WIDTH;
            } catch (SVGParseException e) {
               // Do nothing
            }
            break;

         case stroke_linecap:
            style.strokeLineCap = SVGParserImpl.parseStrokeLineCap(val);
            if (style.strokeLineCap != null)
               style.specifiedFlags |= SPECIFIED_STROKE_LINECAP;
            break;

         case stroke_linejoin:
            style.strokeLineJoin = SVGParserImpl.parseStrokeLineJoin(val);
            if (style.strokeLineJoin != null)
               style.specifiedFlags |= SPECIFIED_STROKE_LINEJOIN;
            break;

         case stroke_miterlimit:
            try {
               style.strokeMiterLimit = SVGParserImpl.parseFloat(val);
               style.specifiedFlags |= SPECIFIED_STROKE_MITERLIMIT;
            } catch (SVGParseException e) {
               // Do nothing
            }
            break;

         case stroke_dasharray:
            if (SVGParserImpl.NONE.equals(val)) {
               style.strokeDashArray = null;
               style.specifiedFlags |= SPECIFIED_STROKE_DASHARRAY;
               break;
            }
            style.strokeDashArray = SVGParserImpl.parseStrokeDashArray(val);
            if (style.strokeDashArray != null)
               style.specifiedFlags |= SPECIFIED_STROKE_DASHARRAY;
            break;

         case stroke_dashoffset:
            try {
               style.strokeDashOffset = SVGParserImpl.parseLength(val);
               style.specifiedFlags |= SPECIFIED_STROKE_DASHOFFSET;
            } catch (SVGParseException e) {
               // Do nothing
            }
            break;

         case opacity:
            style.opacity = SVGParserImpl.parseOpacity(val);
            style.specifiedFlags |= SPECIFIED_OPACITY;
            break;

         case color:
            style.color = SVGParserImpl.parseColour(val);
            style.specifiedFlags |= SPECIFIED_COLOR;
            break;

         case font:
            if (isFromAttribute)
               break;
            SVGParserImpl.parseFont(style, val);
            break;

         case font_family:
            style.fontFamily = SVGParserImpl.parseFontFamily(val);
            if (style.fontFamily != null)
               style.specifiedFlags |= SPECIFIED_FONT_FAMILY;
            break;

         case font_size:
            style.fontSize = SVGParserImpl.parseFontSize(val);
            if (style.fontSize != null)
               style.specifiedFlags |= SPECIFIED_FONT_SIZE;
            break;

         case font_weight:
            style.fontWeight = SVGParserImpl.parseFontWeight(val);
            if (style.fontWeight != null)
               style.specifiedFlags |= SPECIFIED_FONT_WEIGHT;
            break;

         case font_style:
            style.fontStyle = SVGParserImpl.parseFontStyle(val);
            if (style.fontStyle != null)
               style.specifiedFlags |= SPECIFIED_FONT_STYLE;
            break;

         case font_stretch:
            style.fontStretch = SVGParserImpl.parseFontStretch(val);
            if (style.fontStretch != null)
               style.specifiedFlags |= SPECIFIED_FONT_STRETCH;
            break;

         case text_decoration:
            style.textDecoration = SVGParserImpl.parseTextDecoration(val);
            if (style.textDecoration != null)
               style.specifiedFlags |= SPECIFIED_TEXT_DECORATION;
            break;

         case direction:
            style.direction = SVGParserImpl.parseTextDirection(val);
            if (style.direction != null)
               style.specifiedFlags |= SPECIFIED_DIRECTION;
            break;

         case text_anchor:
            style.textAnchor = SVGParserImpl.parseTextAnchor(val);
            if (style.textAnchor != null)
               style.specifiedFlags |= SPECIFIED_TEXT_ANCHOR;
            break;

         case overflow:
            style.overflow = SVGParserImpl.parseOverflow(val);
            if (style.overflow != null)
               style.specifiedFlags |= SPECIFIED_OVERFLOW;
            break;

         case marker:
            style.markerStart = SVGParserImpl.parseFunctionalIRI(val, localName);
            style.markerMid = style.markerStart;
            style.markerEnd = style.markerStart;
            style.specifiedFlags |= (SPECIFIED_MARKER_START | SPECIFIED_MARKER_MID | SPECIFIED_MARKER_END);
            break;

         case marker_start:
            style.markerStart = SVGParserImpl.parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SPECIFIED_MARKER_START;
            break;

         case marker_mid:
            style.markerMid = SVGParserImpl.parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SPECIFIED_MARKER_MID;
            break;

         case marker_end:
            style.markerEnd = SVGParserImpl.parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SPECIFIED_MARKER_END;
            break;

         case display:
            if (val.indexOf('|') >= 0 || !SVGParserImpl.VALID_DISPLAY_VALUES.contains('|'+val+'|'))
               break;
            style.display = !val.equals(SVGParserImpl.NONE);
            style.specifiedFlags |= SPECIFIED_DISPLAY;
            break;

         case visibility:
            if (val.indexOf('|') >= 0 || !SVGParserImpl.VALID_VISIBILITY_VALUES.contains('|'+val+'|'))
               break;
            style.visibility = val.equals("visible");
            style.specifiedFlags |= SPECIFIED_VISIBILITY;
            break;

         case stop_color:
            if (val.equals(SVGParserImpl.CURRENTCOLOR)) {
               style.stopColor = CurrentColor.getInstance();
            } else {
               style.stopColor = SVGParserImpl.parseColour(val);
            }
            style.specifiedFlags |= SPECIFIED_STOP_COLOR;
            break;

         case stop_opacity:
            style.stopOpacity = SVGParserImpl.parseOpacity(val);
            style.specifiedFlags |= SPECIFIED_STOP_OPACITY;
            break;

         case clip:
            style.clip = SVGParserImpl.parseClip(val);
            if (style.clip != null)
               style.specifiedFlags |= SPECIFIED_CLIP;
            break;

         case clip_path:
            style.clipPath = SVGParserImpl.parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SPECIFIED_CLIP_PATH;
            break;

         case clip_rule:
            style.clipRule = SVGParserImpl.parseFillRule(val);
            style.specifiedFlags |= SPECIFIED_CLIP_RULE;
            break;

         case mask:
            style.mask = SVGParserImpl.parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SPECIFIED_MASK;
            break;

         case solid_color:
            // SVG 1.2 Tiny
            if (!isFromAttribute)
               break;
            if (val.equals(SVGParserImpl.CURRENTCOLOR)) {
               style.solidColor = CurrentColor.getInstance();
            } else {
               style.solidColor = SVGParserImpl.parseColour(val);
            }
            style.specifiedFlags |= SPECIFIED_SOLID_COLOR;
            break;

         case solid_opacity:
            // SVG 1.2 Tiny
            if (!isFromAttribute)
               break;
            style.solidOpacity = SVGParserImpl.parseOpacity(val);
            style.specifiedFlags |= SPECIFIED_SOLID_OPACITY;
            break;

         case viewport_fill:
            // SVG 1.2 Tiny
            if (val.equals(SVGParserImpl.CURRENTCOLOR)) {
               style.viewportFill = CurrentColor.getInstance();
            } else {
               style.viewportFill = SVGParserImpl.parseColour(val);
            }
            style.specifiedFlags |= SPECIFIED_VIEWPORT_FILL;
            break;

         case viewport_fill_opacity:
            // SVG 1.2 Tiny
            style.viewportFillOpacity = SVGParserImpl.parseOpacity(val);
            style.specifiedFlags |= SPECIFIED_VIEWPORT_FILL_OPACITY;
            break;

         case vector_effect:
            style.vectorEffect = SVGParserImpl.parseVectorEffect(val);
            if (style.vectorEffect != null)
               style.specifiedFlags |= SPECIFIED_VECTOR_EFFECT;
            break;

         case image_rendering:
            style.imageRendering = SVGParserImpl.parseRenderQuality(val);
            if (style.imageRendering != null)
               style.specifiedFlags |= SPECIFIED_IMAGE_RENDERING;
            break;

         case isolation:
            if (isFromAttribute)
               break;
            style.isolation = SVGParserImpl.parseIsolation(val);
            if (style.isolation != null)
               style.specifiedFlags |= SPECIFIED_ISOLATION;
            break;

         case mix_blend_mode:
            if (isFromAttribute)
               break;
            style.mixBlendMode = CSSBlendMode.fromString(val);
            if (style.mixBlendMode != null)
               style.specifiedFlags |= SPECIFIED_MIX_BLEND_MODE;
            break;

         case font_kerning:
            if (isFromAttribute)
               break;
            style.fontKerning = CSSFontFeatureSettings.parseFontKerning(val);
            if (style.fontKerning != null)
               style.specifiedFlags |= SPECIFIED_FONT_KERNING;
            break;

         case font_variant:
            if (isFromAttribute)
               break;
            CSSFontFeatureSettings.parseFontVariant(style, val);
            break;

         case font_variant_ligatures:
            if (isFromAttribute)
               break;
            style.fontVariantLigatures = CSSFontFeatureSettings.parseVariantLigatures(val);
            if (style.fontVariantLigatures != null)
               style.specifiedFlags |= SPECIFIED_FONT_VARIANT_LIGATURES;
            break;

         case font_variant_position:
            if (isFromAttribute)
               break;
            style.fontVariantPosition = CSSFontFeatureSettings.parseVariantPosition(val);
            if (style.fontVariantPosition != null)
               style.specifiedFlags |= SPECIFIED_FONT_VARIANT_POSITION;
            break;

         case font_variant_caps:
            if (isFromAttribute)
               break;
            style.fontVariantCaps = CSSFontFeatureSettings.parseVariantCaps(val);
            if (style.fontVariantCaps != null)
               style.specifiedFlags |= SPECIFIED_FONT_VARIANT_CAPS;
            break;

         case font_variant_numeric:
            if (isFromAttribute)
               break;
            style.fontVariantNumeric = CSSFontFeatureSettings.parseVariantNumeric(val);
            if (style.fontVariantNumeric != null)
               style.specifiedFlags |= SPECIFIED_FONT_VARIANT_NUMERIC;
            break;

         case font_variant_east_asian:
            if (isFromAttribute)
               break;
            style.fontVariantEastAsian = CSSFontFeatureSettings.parseEastAsian(val);
            if (style.fontVariantEastAsian != null)
               style.specifiedFlags |= SPECIFIED_FONT_VARIANT_EAST_ASIAN;
            break;

         case font_feature_settings:
            if (isFromAttribute)
               break;
            style.fontFeatureSettings = CSSFontFeatureSettings.parseFontFeatureSettings(val);
            if (style.fontFeatureSettings != null)
               style.specifiedFlags |= SPECIFIED_FONT_FEATURE_SETTINGS;
            break;

         case font_variation_settings:
            if (isFromAttribute)
               break;
            style.fontVariationSettings = CSSFontVariationSettings.parseFontVariationSettings(val);
            if (style.fontVariationSettings != null)
               style.specifiedFlags |= SPECIFIED_FONT_VARIATION_SETTINGS;
            break;

         case letter_spacing:
            style.letterSpacing = SVGParserImpl.parseLetterOrWordSpacing(val);
            if (style.letterSpacing != null)
               style.specifiedFlags |= SPECIFIED_LETTER_SPACING;
            break;

         case word_spacing:
            style.wordSpacing = SVGParserImpl.parseLetterOrWordSpacing(val);
            if (style.wordSpacing != null)
               style.specifiedFlags |= SPECIFIED_WORD_SPACING;
            break;

         /*
         case writing_mode:
            style.writingMode = WritingMode.fromString(val);
            if (style.writingMode != null)
               style.specifiedFlags |= SPECIFIED_WRITING_MODE;
            break;

         case glyph_orientation_vertical:
            style.glyphOrientationVertical = GlypOrientationVertical.fromString(val);
            if (style.glyphOrientationVertical != null)
               style.specifiedFlags |= SPECIFIED_GLYPH_ORIENTATION_VERTICAL;
            break;

         case text_orientation:
            if (isFromAttribute)
               break;
            style.textOrientation = TextOrientation.fromString(val);
            if (style.textOrientation != null)
               style.specifiedFlags |= SPECIFIED_TEXT_ORIENTATION;
            break;
         */

         default:
            break;
      }
   }

}
