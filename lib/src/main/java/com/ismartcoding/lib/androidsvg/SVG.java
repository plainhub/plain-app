package com.ismartcoding.lib.androidsvg;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.RectF;

import com.ismartcoding.lib.androidsvg.utils.SVGBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * AndroidSVG is a library for reading, parsing and rendering SVG documents on Android devices.
 * <p>
 * All interaction with AndroidSVG is via this class.
 * <p>
 * Typically, you will call one of the SVG loading and parsing classes then call the renderer,
 * passing it a canvas to draw upon.
 * 
 * <h3>Usage summary</h3>
 * 
 * <ul>
 * <li>Use one of the static {@code getFromX()} methods to read and parse the SVG file.  They will
 * return an instance of this class.
 * <li>Call one of the {@code renderToX()} methods to render the document.
 * </ul>
 * 
 * <h3>Usage example</h3>
 * 
 * <pre>
 * {@code
 * SVG.registerExternalFileResolver(myResolver);
 *
 * SVG  svg = SVG.getFromAsset(getContext().getAssets(), svgPath);
 *
 * Bitmap  newBM = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
 * Canvas  bmcanvas = new Canvas(newBM);
 * bmcanvas.drawRGB(255, 255, 255);  // Clear background to white
 *
 * svg.renderToCanvas(bmcanvas);
 * }
 * </pre>
 * 
 * For more detailed information on how to use this library, see the documentation at {@code http://code.google.com/p/androidsvg/}
 */

public class SVG
{
   //static final String  TAG = "SVG";

   private static final String  VERSION = "1.5";

   private SVGBase  base;


   // Users should use one of the getFromX() methods to create an instance of SVG
   private SVG(SVGBase base)
   {
      this.base = base;
   }


   /**
    * Read and parse an SVG from the given {@code InputStream}.
    * 
    * @param is the input stream from which to read the file.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromInputStream(InputStream is) throws SVGParseException
   {
      return new SVG(SVGBase.getFromInputStream(is));
   }


   /**
    * Read and parse an SVG from the given {@code String}.
    * 
    * @param svg the String instance containing the SVG document.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public static SVG  getFromString(String svg) throws SVGParseException
   {
      return new SVG(SVGBase.getFromString(svg));
   }


   /**
    * Read and parse an SVG from the given resource location.
    * 
    * @param context the Android context of the resource.
    * @param resourceId the resource identifier of the SVG document.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromResource(Context context, int resourceId) throws SVGParseException
   {
      return getFromResource(context.getResources(), resourceId);
   }


   /**
    * Read and parse an SVG from the given resource location.
    *
    * @param resources the set of Resources in which to locate the file.
    * @param resourceId the resource identifier of the SVG document.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    * @since 1.2.1
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromResource(Resources resources, int resourceId) throws SVGParseException
   {
      return new SVG(SVGBase.getFromResource(resources, resourceId));
   }


   /**
    * Read and parse an SVG from the assets folder.
    * 
    * @param assetManager the AssetManager instance to use when reading the file.
    * @param filename the filename of the SVG document within assets.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    * @throws IOException if there is some IO error while reading the file.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public static SVG  getFromAsset(AssetManager assetManager, String filename) throws SVGParseException, IOException
   {
      return new SVG(SVGBase.getFromAsset(assetManager, filename));
   }


   /**
    * Parse an SVG path definition from the given {@code String}.
    *
    * {@code
    * Path  path = SVG.parsePath("M 0,0 L 100,100");
    * path.setFillType(Path.FillType.EVEN_ODD);
    *
    * // You could render the path to a Canvas now
    * Paint paint = new Paint();
    * paint.setStyle(Paint.Style.FILL);
    * paint.setColor(Color.RED);
    * canvas.drawPath(path, paint);
    *
    * // Or perform other operations on it
    * RectF bounds = new RectF();
    * path.computeBounds(bounds, false);
    * }
    *
    * Note that this method does not throw any exceptions or return any errors. Per the SVG
    * specification, if there are any errors in the path definition, the valid portion of the
    * path up until the first error is returned.
    *
    * @param pathDefinition an SVG path element definition string
    * @return an Android {@code Path}
    * @since 1.5
    */
   public static android.graphics.Path  parsePath(String pathDefinition)
   {
      return SVGBase.parsePath(pathDefinition);
   }


   //===============================================================================

   /**
    * Tells the parser whether to allow the expansion of internal entities.
    * An example of a document containing an internal entities is:
    *
    * {@code
    * <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.0//EN" "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd" [
    *   <!ENTITY hello "Hello World!">
    * ]>
    * <svg>
    *    <text>&hello;</text>
    * </svg>
    * }
    *
    * Entities are useful in some circumstances, but SVG files that use them are quite rare.  Note
    * also that enabling entity expansion makes you vulnerable to the
    * <a href="https://en.wikipedia.org/wiki/Billion_laughs_attack">Billion Laughs Attack</a>
    *
    * Entity expansion is enabled by default.
    *
    * @param enable Set true if you want to enable entity expansion by the parser.
    * @since 1.3
    */
   @SuppressWarnings("unused")
   public static void  setInternalEntitiesEnabled(boolean enable)
   {
      SVGBase.setInternalEntitiesEnabled(enable);
   }

   /**
    * Indicates whether internal entities were enabled when this SVG was parsed.
    *
    * <p>
    * <em>Note: prior to release 1.5, this was a static method of (@code SVG}.  In 1.5, it was
    * changed to a instance method to coincide with the change making parsing settings thread safe.</em>
    * </p>
    *
    * @return true if internal entity expansion is enabled in the parser
    * @since 1.5
    */
   @SuppressWarnings("unused")
   public boolean  isInternalEntitiesEnabled()
   {
      return base.isInternalEntitiesEnabled();
   }


   /**
    * Register an {@link SVGExternalFileResolver} instance that the renderer should use when resolving
    * external references such as images, fonts, and CSS stylesheets.
    *
    * <p>
    * <em>Note: prior to release 1.3, this was an instance method of (@code SVG}.  In 1.3, it was
    * changed to a static method so that users can resolve external references to CSS files while
    * the SVG is being parsed.</em>
    * </p>
    * 
    * @param fileResolver the resolver to use.
    * @since 1.3
    */
   @SuppressWarnings("unused")
   public static void  registerExternalFileResolver(SVGExternalFileResolver fileResolver)
   {
      SVGBase.registerExternalFileResolver(fileResolver);
   }


   /**
    * De-register the current {@link SVGExternalFileResolver} instance.
    *
    * @since 1.3
    */
   @SuppressWarnings("unused")
   public static void  deregisterExternalFileResolver()
   {
      SVGBase.deregisterExternalFileResolver();
   }




   /**
    * Get the {@link SVGExternalFileResolver} in effect when this SVG was parsed..
    *
    * @return the current external file resolver instance
    * @since 1.5
    */
   @SuppressWarnings("unused")
   public SVGExternalFileResolver  getExternalFileResolver()
   {
      return base.getExternalFileResolver();
   }


   /**
    * Set the DPI (dots-per-inch) value to use when rendering.  The DPI setting is used in the
    * conversion of "physical" units - such an "pt" or "cm" - to pixel values.  The default DPI is 96.
    * <p>
    * You should not normally need to alter the DPI from the default of 96 as recommended by the SVG
    * and CSS specifications.
    *  
    * @param dpi the DPI value that the renderer should use.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setRenderDPI(float dpi)
   {
      base.setRenderDPI(dpi);
   }


   /**
    * Get the current render DPI setting.
    * @return the DPI value
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getRenderDPI()
   {
      return base.getRenderDPI();
   }


   //===============================================================================
   // SVG document rendering to a Picture object (indirect rendering)


   /**
    * Renders this SVG document to a Picture object.
    * <p>
    * An attempt will be made to determine a suitable initial viewport from the contents of the SVG file.
    * If an appropriate viewport can't be determined, a default viewport of 512x512 will be used.
    * 
    * @return a Picture object suitable for later rendering using {@code Canvas.drawPicture()}
    */
   @SuppressWarnings("WeakerAccess")
   public Picture  renderToPicture()
   {
      return base.renderToPicture(null);
   }


   /**
    * Renders this SVG document to a {@link Picture}.
    * 
    * @param widthInPixels the width of the initial viewport
    * @param heightInPixels the height of the initial viewport
    * @return a Picture object suitable for later rendering using {@link Canvas#drawPicture(Picture)}
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Picture  renderToPicture(int widthInPixels, int heightInPixels)
   {
      return renderToPicture(widthInPixels, heightInPixels, null);
   }



   /**
    * Renders this SVG document to a {@link Picture}.
    *
    * @param renderOptions options that describe how to render this SVG on the Canvas.
    * @return a Picture object suitable for later rendering using {@link Canvas#drawPicture(Picture)}
    * @since 1.3
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Picture  renderToPicture(RenderOptions renderOptions)
   {
      return base.renderToPicture(renderOptions);
   }


   /**
    * Renders this SVG document to a {@link Picture}.
    *
    * @param widthInPixels the width of the {@code Picture}
    * @param heightInPixels the height of the {@code Picture}
    * @param renderOptions options that describe how to render this SVG on the Canvas.
    * @return a Picture object suitable for later rendering using {@link Canvas#drawPicture(Picture)}
    * @since 1.3
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Picture  renderToPicture(int widthInPixels, int heightInPixels, RenderOptions renderOptions)
   {
      return base.renderToPicture(widthInPixels, heightInPixels, renderOptions);
   }



   /**
    * Renders this SVG document to a {@link Picture} using the specified view defined in the document.
    * <p>
    * A View is an special element in a SVG document that describes a rectangular area in the document.
    * Calling this method with a {@code viewId} will result in the specified view being positioned and scaled
    * to the viewport.  In other words, use {@link #renderToPicture()} to render the whole document, or use this
    * method instead to render just a part of it.
    * 
    * @param viewId the id of a view element in the document that defines which section of the document is to be visible.
    * @param widthInPixels the width of the initial viewport
    * @param heightInPixels the height of the initial viewport
    * @return a Picture object suitable for later rendering using {@code Canvas.drawPicture()}, or null if the viewId was not found.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Picture  renderViewToPicture(String viewId, int widthInPixels, int heightInPixels)
   {
      return base.renderViewToPicture(viewId, widthInPixels, heightInPixels);
   }


   //===============================================================================
   // SVG document rendering to a canvas object (direct rendering)


   /**
    * Renders this SVG document to a Canvas object.  The full width and height of the canvas
    * will be used as the viewport into which the document will be rendered.
    * 
    * @param canvas the canvas to which the document should be rendered.
    * @since 1.3
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderToCanvas(Canvas canvas)
   {
      renderToCanvas(canvas, (RenderOptions) null);
   }


   /**
    * Renders this SVG document to a Canvas object.
    * 
    * @param canvas the canvas to which the document should be rendered.
    * @param viewPort the bounds of the area on the canvas you want the SVG rendered, or null for the whole canvas.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderToCanvas(Canvas canvas, RectF viewPort)
   {
      base.renderToCanvas(canvas, viewPort);
   }


   /**
    * Renders this SVG document to a Canvas object.
    *
    * @param canvas the canvas to which the document should be rendered.
    * @param renderOptions options that describe how to render this SVG on the Canvas.
    * @since 1.3
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderToCanvas(Canvas canvas, RenderOptions renderOptions)
   {
      base.renderToCanvas(canvas, renderOptions);
   }


   /**
    * Renders this SVG document to a Canvas using the specified view defined in the document.
    * <p>
    * A View is an special element in a SVG documents that describes a rectangular area in the document.
    * Calling this method with a {@code viewId} will result in the specified view being positioned and scaled
    * to the viewport.  In other words, use {@link #renderToPicture()} to render the whole document, or use this
    * method instead to render just a part of it.
    * <p>
    * If the {@code <view>} could not be found, nothing will be drawn.
    *
    * @param viewId the id of a view element in the document that defines which section of the document is to be visible.
    * @param canvas the canvas to which the document should be rendered.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderViewToCanvas(String viewId, Canvas canvas)
   {
      renderToCanvas(canvas, RenderOptions.create().view(viewId));
   }


   /**
    * Renders this SVG document to a Canvas using the specified view defined in the document.
    * <p>
    * A View is an special element in a SVG documents that describes a rectangular area in the document.
    * Calling this method with a {@code viewId} will result in the specified view being positioned and scaled
    * to the viewport.  In other words, use {@link #renderToPicture()} to render the whole document, or use this
    * method instead to render just a part of it.
    * <p>
    * If the {@code <view>} could not be found, nothing will be drawn.
    * 
    * @param viewId the id of a view element in the document that defines which section of the document is to be visible.
    * @param canvas the canvas to which the document should be rendered.
    * @param viewPort the bounds of the area on the canvas you want the SVG rendered, or null for the whole canvas.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderViewToCanvas(String viewId, Canvas canvas, RectF viewPort)
   {
      base.renderViewToCanvas(viewId, canvas, viewPort);
   }


   //===============================================================================
   // Other document utility API functions


   /**
    * Returns the version number of this library.
    * 
    * @return the version number in string format
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public static String  getVersion()
   {
      return VERSION;
   }


   /**
    * Returns the contents of the {@code <title>} element in the SVG document.
    * 
    * @return title contents if available, otherwise an empty string.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public String getDocumentTitle()
   {
      return base.getDocumentTitle();
   }


   /**
    * Returns the contents of the {@code <desc>} element in the SVG document.
    * 
    * @return desc contents if available, otherwise an empty string.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public String getDocumentDescription()
   {
      return base.getDocumentDescription();
   }


   /**
    * Returns the SVG version number as provided in the root {@code <svg>} tag of the document.
    * 
    * @return the version string if declared, otherwise an empty string.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public String getDocumentSVGVersion()
   {
      return base.getDocumentSVGVersion();
   }


   /**
    * Returns a list of ids for all {@code <view>} elements in this SVG document.
    * <p>
    * The returned view ids could be used when calling and of the {@code renderViewToX()} methods.
    * 
    * @return the list of id strings.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Set<String> getViewList()
   {
      return base.getViewList();
   }


   /**
    * Returns the width of the document as specified in the SVG file.
    * <p>
    * If the width in the document is specified in pixels, that value will be returned.
    * If the value is listed with a physical unit such as "cm", then the current
    * {@code RenderDPI} value will be used to convert that value to pixels. If the width
    * is missing, or in a form which can't be converted to pixels, such as "100%" for
    * example, -1 will be returned.
    *  
    * @return the width in pixels, or -1 if there is no width available.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getDocumentWidth()
   {
      return base.getDocumentWidth();
   }


   /**
    * Change the width of the document by altering the "width" attribute
    * of the root {@code <svg>} element.
    * 
    * @param pixels The new value of width in pixels.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentWidth(float pixels)
   {
      base.setDocumentWidth(pixels);
   }


   /**
    * Change the width of the document by altering the "width" attribute
    * of the root {@code <svg>} element.
    * 
    * @param value A valid SVG 'length' attribute, such as "100px" or "10cm".
    * @throws SVGParseException if {@code value} cannot be parsed successfully.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentWidth(String value) throws SVGParseException
   {
      base.setDocumentWidth(value);
   }


   /**
    * Returns the height of the document as specified in the SVG file.
    * <p>
    * If the height in the document is specified in pixels, that value will be returned.
    * If the value is listed with a physical unit such as "cm", then the current
    * {@code RenderDPI} value will be used to convert that value to pixels. If the height
    * is missing, or in a form which can't be converted to pixels, such as "100%" for
    * example, -1 will be returned.
    *  
    * @return the height in pixels, or -1 if there is no height available.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getDocumentHeight()
   {
      return base.getDocumentHeight();
   }


   /**
    * Change the height of the document by altering the "height" attribute
    * of the root {@code <svg>} element.
    * 
    * @param pixels The new value of height in pixels.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentHeight(float pixels)
   {
      base.setDocumentHeight(pixels);
   }


   /**
    * Change the height of the document by altering the "height" attribute
    * of the root {@code <svg>} element.
    * 
    * @param value A valid SVG 'length' attribute, such as "100px" or "10cm".
    * @throws SVGParseException if {@code value} cannot be parsed successfully.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentHeight(String value) throws SVGParseException
   {
      base.setDocumentHeight(value);
   }


   /**
    * Change the document view box by altering the "viewBox" attribute
    * of the root {@code <svg>} element.
    * <p>
    * The viewBox generally describes the bounding box dimensions of the
    * document contents.  A valid viewBox is necessary if you want the
    * document scaled to fit the canvas or viewport the document is to be
    * rendered into.
    * <p>
    * By setting a viewBox that describes only a portion of the document,
    * you can reproduce the effect of image sprites.
    * 
    * @param minX the left coordinate of the viewBox in pixels
    * @param minY the top coordinate of the viewBox in pixels.
    * @param width the width of the viewBox in pixels
    * @param height the height of the viewBox in pixels
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentViewBox(float minX, float minY, float width, float height)
   {
      base.setDocumentViewBox(minX, minY, width, height);
   }


   /**
    * Returns the viewBox attribute of the current SVG document.
    * 
    * @return the document's viewBox attribute as a {@code android.graphics.RectF} object, or null if not set.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public RectF  getDocumentViewBox()
   {
      return base.getDocumentViewBox();
   }


   /**
    * Change the document positioning by altering the "preserveAspectRatio"
    * attribute of the root {@code <svg>} element.  See the
    * documentation for {@link PreserveAspectRatio} for more information
    * on how positioning works.
    * 
    * @param preserveAspectRatio the new {@code preserveAspectRatio} setting for the root {@code <svg>} element.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentPreserveAspectRatio(PreserveAspectRatio preserveAspectRatio)
   {
      base.setDocumentPreserveAspectRatio(preserveAspectRatio);
   }


   /**
    * Return the "preserveAspectRatio" attribute of the root {@code <svg>}
    * element in the form of an {@link PreserveAspectRatio} object.
    * 
    * @return the preserveAspectRatio setting of the document's root {@code <svg>} element.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public PreserveAspectRatio  getDocumentPreserveAspectRatio()
   {
      return base.getDocumentPreserveAspectRatio();
   }


   /**
    * Returns the aspect ratio of the document as a width/height fraction.
    * <p>
    * If the width or height of the document are listed with a physical unit such as "cm",
    * then the current {@code renderDPI} setting will be used to convert that value to pixels.
    * <p>
    * If the width or height cannot be determined, -1 will be returned.
    * 
    * @return the aspect ratio as a width/height fraction, or -1 if the ratio cannot be determined.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getDocumentAspectRatio()
   {
      return base.getDocumentAspectRatio();
   }


   //===============================================================================================


   SVGBase.Svg  getRootElement()
   {
      return base.getRootElement();
   }
}
