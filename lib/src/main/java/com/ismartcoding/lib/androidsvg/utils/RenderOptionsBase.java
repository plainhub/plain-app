package com.ismartcoding.lib.androidsvg.utils;

import com.ismartcoding.lib.androidsvg.CSS;
import com.ismartcoding.lib.androidsvg.PreserveAspectRatio;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Box;

public class RenderOptionsBase
{
   String               css = null;
   CSSParser.Ruleset    cssRuleset = null;
   //String             id = null;
   PreserveAspectRatio  preserveAspectRatio = null;
   String               targetId = null;
   SVGBase.Box              viewBox = null;
   String               viewId = null;
   SVGBase.Box              viewPort = null;


   /**
    * Create a new <code>RenderOptions</code> instance.  You can choose to use either this constructor,
    * or <code>new RenderOptions.create()</code>.  Both are equivalent.
    */
   public RenderOptionsBase()
   {
   }


   /**
    * Create a new <code>RenderOptions</code> instance.  This is just an alternative to <code>new RenderOptions()</code>.
    * @return new instance of this class.
    */
   public static RenderOptionsBase create()
   {
      return new RenderOptionsBase();
   }


   /**
    * Creates a copy of the given <code>RenderOptions</code> object.
    * @param other the object to copy
    */
   public RenderOptionsBase(RenderOptionsBase other)
   {
      if (other == null)
         return;
      this.css = other.css;
      this.cssRuleset = other.cssRuleset;
      //this.id = other.id;
      this.preserveAspectRatio = other.preserveAspectRatio;
      this.viewBox = other.viewBox;
      this.viewId = other.viewId;
      this.viewPort = other.viewPort;
      this.targetId = other.targetId;
   }

   /**
    * Specifies some already parsed CSS that will be applied during render in
    * addition to any specified in the file itself.
    * @param css CSS rules to apply
    * @return this same <code>RenderOptions</code> instance
    */
   public RenderOptionsBase css(CSS css)
   {
      this.cssRuleset = css.cssRuleset;
      this.css = null;
      return this;
   }

   public RenderOptionsBase css(String css)
   {
      this.css = css;
      this.cssRuleset = null;
      return this;
   }

   /**
    * Returns true if this RenderOptions instance has had CSS set with {@code css()}.
    * @return true if this RenderOptions instance has had CSS set
    */
   public boolean hasCss()
   {
      return this.css != null && this.css.trim().length() > 0 || this.cssRuleset != null;
   }


   /**
    * Specifies how the renderer should handle aspect ratio when rendering the SVG.
    * If not specified, the default will be <code>PreserveAspectRatio.LETTERBOX</code>. This is
    * equivalent to the SVG default of <code>xMidYMid meet</code>.
    * @param preserveAspectRatio the new aspect ration value
    * @return this same <code>RenderOptions</code> instance
    */
   @SuppressWarnings("UnusedReturnValue")
   public RenderOptionsBase preserveAspectRatio(PreserveAspectRatio preserveAspectRatio)
   {
      this.preserveAspectRatio = preserveAspectRatio;
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a preserveAspectRatio value set with {@code preserveAspectRatio()}.
    * @return true if this RenderOptions instance has had a preserveAspectRatio value set
    */
   public boolean hasPreserveAspectRatio()
   {
      return this.preserveAspectRatio != null;
   }


   /**
    * Specifies the {@code id} of a {@code <view>} element in the SVG.  A {@code <view>}
    * element is a way to specify a predetermined view of the document, that differs from the default view.
    * For example it can allow you to focus in on a small detail of the document.
    *
    * Note: setting this option will override any {@link #viewBox(float,float,float,float)} or {@link #preserveAspectRatio(PreserveAspectRatio)} settings.
    *
    * @param viewId the id attribute of the view that should be used for rendering
    * @return this same <code>RenderOptions</code> instance
    */
   public RenderOptionsBase view(String viewId)
   {
      this.viewId = viewId;
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a view set with {@code view()}.
    * @return true if this RenderOptions instance has had a view set
    */
   public boolean hasView()
   {
      return this.viewId != null;
   }


   /**
    * Specifies alternative values to use for the root element {@code viewBox}. Any existing {@code viewBox}
    * attribute value will be ignored.
    *
    * Note: will be overridden if a {@link #view(String)} is set.
    *
    * @param minX The left X coordinate of the viewBox
    * @param minY The top Y coordinate of the viewBox
    * @param width The width of the viewBox
    * @param height The height of the viewBox
    * @return this same <code>RenderOptions</code> instance
    */
   public RenderOptionsBase viewBox(float minX, float minY, float width, float height)
   {
      this.viewBox = new Box(minX, minY, width, height);
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a viewBox set with {@code viewBox()}.
    * @return true if this RenderOptions instance has had a viewBox set
    */
   public boolean hasViewBox()
   {
      return this.viewBox != null;
   }


   /**
    * Describes the viewport into which the SVG should be rendered.  If this is not specified,
    * then the whole of the canvas will be used as the viewport.  If rendering to a <code>Picture</code>
    * then a default viewport width and height will be used.

    * @param minX The left X coordinate of the viewport
    * @param minY The top Y coordinate of the viewport
    * @param width The width of the viewport
    * @param height The height of the viewport
    * @return this same <code>RenderOptions</code> instance
    */
   public RenderOptionsBase viewPort(float minX, float minY, float width, float height)
   {
      this.viewPort = new Box(minX, minY, width, height);
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a viewPort set with {@code viewPort()}.
    * @return true if this RenderOptions instance has had a viewPort set
    */
   public boolean hasViewPort()
   {
      return this.viewPort != null;
   }


   /**
    * Specifies the {@code id} of an element, in the SVG, to treat as the target element when
    * using the {@code :target} CSS pseudo class.
    *
    * @param targetId the id attribute of an element
    * @return this same <code>RenderOptions</code> instance
    */
   public RenderOptionsBase target(String targetId)
   {
      this.targetId = targetId;
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a target set with {@code target()}.
    * @return true if this RenderOptions instance has had a target set
    */
   public boolean hasTarget()
   {
      return this.targetId != null;
   }


}
