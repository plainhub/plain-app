package com.ismartcoding.lib.androidsvg;

import com.ismartcoding.lib.androidsvg.utils.TextScanner;

import java.util.HashMap;
import java.util.Map;

public class PreserveAspectRatio
{
   private final Alignment  alignment;
   private final Scale      scale;

   private static final Map<String, Alignment> aspectRatioKeywords = new HashMap<>(10);


   /**
    * Draw document at its natural position and scale.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  UNSCALED = new PreserveAspectRatio(null, null);

   /**
    * Stretch horizontally and vertically to fill the viewport.
    * <p>
    * Equivalent to <code>preserveAspectRatio="none"</code> in an SVG.
    */
   @SuppressWarnings("WeakerAccess")
   public static final PreserveAspectRatio  STRETCH = new PreserveAspectRatio(Alignment.none, null);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be centred in the viewport and may have blank strips at either the top and
    * bottom of the viewport or at the sides.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMid meet"</code> in an SVG.
    */
   @SuppressWarnings("WeakerAccess")
   public static final PreserveAspectRatio  LETTERBOX = new PreserveAspectRatio(Alignment.xMidYMid, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the top of tall and narrow viewports, and at the left of short
    * and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMinYMin meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  START = new PreserveAspectRatio(Alignment.xMinYMin, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the bottom of tall and narrow viewports, and at the right of short
    * and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMaxYMax meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  END = new PreserveAspectRatio(Alignment.xMaxYMax, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the top of tall and narrow viewports, and at the centre of
    * short and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMin meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  TOP = new PreserveAspectRatio(Alignment.xMidYMin, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the bottom of tall and narrow viewports, and at the centre of
    * short and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMax meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  BOTTOM = new PreserveAspectRatio(Alignment.xMidYMax, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fills the entire viewport.
    * This may result in some of the document falling outside the viewport.
    * <p>
    * The document will be positioned so that the centre of the document will always be visible,
    * but the edges of the document may not.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMid slice"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  FULLSCREEN = new PreserveAspectRatio(Alignment.xMidYMid, Scale.slice);

   /**
    * Keep the document's aspect ratio, but scale it so that it fills the entire viewport.
    * This may result in some of the document falling outside the viewport.
    * <p>
    * The document will be positioned so that the top left of the document will always be visible,
    * but the right hand or bottom edge may not.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMinYMin slice"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  FULLSCREEN_START = new PreserveAspectRatio(Alignment.xMinYMin, Scale.slice);



   /**
    * Determines how the document is to me positioned relative to the viewport (normally the canvas).
    * <p>
    * For the value {@code none}, the document is stretched to fit the viewport dimensions. For all
    * other values, the aspect ratio of the document is kept the same but the document is scaled to
    * fit the viewport. 
    * @since 1.2.0
    */
   public enum Alignment
   {
      /** Document is stretched to fit both the width and height of the viewport. When using this Alignment value, the value of Scale is not used and will be ignored. */
      none,
      /** Document is positioned at the top left of the viewport. */
      xMinYMin,
      /** Document is positioned at the centre top of the viewport. */
      xMidYMin,
      /** Document is positioned at the top right of the viewport. */
      xMaxYMin,
      /** Document is positioned at the middle left of the viewport. */
      xMinYMid,
      /** Document is centred in the viewport both vertically and horizontally. */
      xMidYMid,
      /** Document is positioned at the middle right of the viewport. */
      xMaxYMid,
      /** Document is positioned at the bottom left of the viewport. */
      xMinYMax,
      /** Document is positioned at the bottom centre of the viewport. */
      xMidYMax,
      /** Document is positioned at the bottom right of the viewport. */
      xMaxYMax
   }


   /**
    * Determine whether the scaled document fills the viewport entirely or is scaled to
    * fill the viewport without overflowing.
    * @since 1.2.0
    */
   public enum Scale
   {
      /**
       * The document is scaled so that it is as large as possible without overflowing the viewport.
       * There may be blank areas on one or more sides of the document.
       */
      meet,
      /**
       * The document is scaled so that entirely fills the viewport. That means that some of the
       * document may fall outside the viewport and will not be rendered.
       */
      slice
   }


   static {
      aspectRatioKeywords.put("none", PreserveAspectRatio.Alignment.none);
      aspectRatioKeywords.put("xMinYMin", PreserveAspectRatio.Alignment.xMinYMin);
      aspectRatioKeywords.put("xMidYMin", PreserveAspectRatio.Alignment.xMidYMin);
      aspectRatioKeywords.put("xMaxYMin", PreserveAspectRatio.Alignment.xMaxYMin);
      aspectRatioKeywords.put("xMinYMid", PreserveAspectRatio.Alignment.xMinYMid);
      aspectRatioKeywords.put("xMidYMid", PreserveAspectRatio.Alignment.xMidYMid);
      aspectRatioKeywords.put("xMaxYMid", PreserveAspectRatio.Alignment.xMaxYMid);
      aspectRatioKeywords.put("xMinYMax", PreserveAspectRatio.Alignment.xMinYMax);
      aspectRatioKeywords.put("xMidYMax", PreserveAspectRatio.Alignment.xMidYMax);
      aspectRatioKeywords.put("xMaxYMax", PreserveAspectRatio.Alignment.xMaxYMax);
   }


   /*
    * Private constructor
    */
   PreserveAspectRatio(Alignment alignment, Scale scale)
   {
      this.alignment = alignment;
      this.scale = scale;
   }


   /**
    * Parse the given SVG <code>preserveAspectRation</code> attribute value and return an equivalent
    * instance of this class.
    * @param value a string in the same format as an SVG {@code preserveAspectRatio} attribute
    * @return a instance of this class
    */
   public static PreserveAspectRatio  of(String value)
   {
      try {
         return parsePreserveAspectRatio(value);
      } catch (SVGParseException e) {
         throw new IllegalArgumentException(e.getMessage());
      }
   }


   /**
    * Returns the alignment value of this instance.
    * @return the alignment
    */
   @SuppressWarnings("WeakerAccess")
   public Alignment  getAlignment()
   {
      return alignment;
   }


   /**
    * Returns the scale value of this instance.
    * @return the scale
    */
   @SuppressWarnings("WeakerAccess")
   public Scale  getScale()
   {
      return scale;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      PreserveAspectRatio other = (PreserveAspectRatio) obj;
      return (alignment == other.alignment && scale == other.scale);
   }


   @Override
   public String toString()
   {
      return alignment + " " + scale;
   }




   private static PreserveAspectRatio  parsePreserveAspectRatio(String val) throws SVGParseException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      String  word = scan.nextToken();
      if ("defer".equals(word)) {    // Ignore defer keyword
         scan.skipWhitespace();
         word = scan.nextToken();
      }

      PreserveAspectRatio.Alignment  align = aspectRatioKeywords.get(word);
      PreserveAspectRatio.Scale      scale = null;

      scan.skipWhitespace();

      if (!scan.empty()) {
         String meetOrSlice = scan.nextToken();
         switch (meetOrSlice) {
            case "meet":
               scale = PreserveAspectRatio.Scale.meet; break;
            case "slice":
               scale = PreserveAspectRatio.Scale.slice; break;
            default:
               throw new SVGParseException("Invalid preserveAspectRatio definition: " + val);
         }
      }
      return new PreserveAspectRatio(align, scale);
   }

}
