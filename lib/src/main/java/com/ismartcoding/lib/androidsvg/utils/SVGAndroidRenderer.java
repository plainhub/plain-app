package com.ismartcoding.lib.androidsvg.utils;


import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.ismartcoding.lib.androidsvg.PreserveAspectRatio;
import com.ismartcoding.lib.androidsvg.SVGExternalFileResolver;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Box;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Circle;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.ClipPath;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Colour;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.CurrentColor;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Ellipse;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.GradientElement;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.GradientSpread;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.GraphicsElement;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Group;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Image;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Length;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Line;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Marker;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Mask;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.NotDirectlyRendered;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.PaintReference;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.PathDefinition;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.PathInterface;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Pattern;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.PolyLine;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Polygon;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Rect;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SolidColor;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Stop;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Svg;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgConditional;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgContainer;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgElement;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgElementBase;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgLinearGradient;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgObject;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgPaint;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.SvgRadialGradient;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Switch;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Symbol;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TRef;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TSpan;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Text;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TextContainer;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TextPath;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.TextSequence;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Unit;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.Use;
import com.ismartcoding.lib.androidsvg.utils.SVGBase.View;
import com.ismartcoding.lib.androidsvg.utils.Style.CSSBlendMode;
import com.ismartcoding.lib.androidsvg.utils.Style.FontStyle;
import com.ismartcoding.lib.androidsvg.utils.Style.Isolation;
import com.ismartcoding.lib.androidsvg.utils.Style.RenderQuality;
import com.ismartcoding.lib.androidsvg.utils.Style.TextAnchor;
import com.ismartcoding.lib.androidsvg.utils.Style.TextDecoration;
import com.ismartcoding.lib.androidsvg.utils.Style.VectorEffect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;


/*
 * The rendering part of AndroidSVG.
 */

@SuppressWarnings("deprecation")
public class SVGAndroidRenderer {
    private static final String TAG = "SVGAndroidRenderer";

    private static final boolean SUPPORTS_BLEND_MODE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;          // Android 10
    private static final boolean SUPPORTS_PAINT_WORD_SPACING = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    private static final boolean SUPPORTS_RADIAL_GRADIENT_WITH_FOCUS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

    private static final java.util.regex.Pattern PATTERN_TABS_OR_LINE_BREAKS = java.util.regex.Pattern.compile("[\\n\\t]");
    private static final java.util.regex.Pattern PATTERN_TABS = java.util.regex.Pattern.compile("\\t");
    private static final java.util.regex.Pattern PATTERN_LINE_BREAKS = java.util.regex.Pattern.compile("\\n");
    private static final java.util.regex.Pattern PATTERN_START_SPACES = java.util.regex.Pattern.compile("^\\s+");
    private static final java.util.regex.Pattern PATTERN_END_SPACES = java.util.regex.Pattern.compile("\\s+$");
    private static final java.util.regex.Pattern PATTERN_DOUBLE_SPACES = java.util.regex.Pattern.compile("\\s{2,}");

    private final Canvas canvas;
    private final float dpi;    // dots per inch. Needed for accurate conversion of length values that have real world units, such as "cm".

    // Renderer state
    private SVGBase document;
    private RendererState state;
    private Stack<RendererState> stateStack;  // Keeps track of render state as we render

    // Keep track of element stack while rendering.
    private Stack<SvgContainer> parentStack; // The 'render parent' for elements like Symbol cf. file parent
    private Stack<Matrix> matrixStack; // Keeps track of current transform as we descend into element tree

    private static final float BEZIER_ARC_FACTOR = 0.5522847498f;

    // The feColorMatrix luminance-to-alpha coefficient. Used for <mask>s.
    // Note we are using the CSS/SVG2 version of the coefficients here, rather than the older SVG1.1 coefficients.
    public static final float LUMINANCE_TO_ALPHA_RED = 0.2127f;
    public static final float LUMINANCE_TO_ALPHA_GREEN = 0.7151f;
    public static final float LUMINANCE_TO_ALPHA_BLUE = 0.0722f;

    private static final String DEFAULT_FONT_FAMILY = "serif";

    private static HashSet<String> supportedFeatures = null;

    private CSSParser.RuleMatchContext ruleMatchContext = null;

    private SVGExternalFileResolver externalFileResolver;


    public static class RendererState {
        Style style;
        boolean hasFill;
        boolean hasStroke;
        Box viewPort;
        Box viewBox;
        boolean spacePreserve;

        final Paint fillPaint;
        final Paint strokePaint;

        final CSSFontFeatureSettings fontFeatureSet;
        final CSSFontVariationSettings fontVariationSet;


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        RendererState() {
            fillPaint = new Paint();
            fillPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            fillPaint.setHinting(Paint.HINTING_OFF);
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setTypeface(Typeface.DEFAULT);

            strokePaint = new Paint();
            strokePaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            strokePaint.setHinting(Paint.HINTING_OFF);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setTypeface(Typeface.DEFAULT);

            fontFeatureSet = new CSSFontFeatureSettings();
            fontVariationSet = new CSSFontVariationSettings();

            style = Style.getDefaultStyle();
        }

        RendererState(RendererState copy) {
            hasFill = copy.hasFill;
            hasStroke = copy.hasStroke;
            fillPaint = new Paint(copy.fillPaint);
            strokePaint = new Paint(copy.strokePaint);
            if (copy.viewPort != null)
                viewPort = new Box(copy.viewPort);
            if (copy.viewBox != null)
                viewBox = new Box(copy.viewBox);
            spacePreserve = copy.spacePreserve;
            fontFeatureSet = new CSSFontFeatureSettings(copy.fontFeatureSet);
            fontVariationSet = new CSSFontVariationSettings(copy.fontVariationSet);
            try {
                style = (Style) copy.style.clone();
            } catch (CloneNotSupportedException e) {
                // Should never happen
                Log.e(TAG, "Unexpected clone error", e);
                style = Style.getDefaultStyle();
            }
        }
    }


    private void resetState() {
        state = new RendererState();
        stateStack = new Stack<>();

        // Initialise the style state properties like Paints etc using a fresh instance of Style
        updateStyle(state, Style.getDefaultStyle());

        state.viewPort = null;  // Get filled in later

        state.spacePreserve = false;

        // Push a copy of the state with 'default' style, so that inherit works for top level objects
        stateStack.push(new RendererState(state));   // Manual push here - don't use statePush();

        // Keep track of element stack while rendering.
        // The 'render parent' for some elements (eg <use> references) is different from its DOM parent.
        matrixStack = new Stack<>();
        parentStack = new Stack<>();
    }


    /*
     * Create a new renderer instance.
     *
     * @param canvas the canvas to draw to.
     * @param viewPort the default viewport to be rendered into. For example the dimensions of the bitmap.
     * @param defaultDPI the DPI setting to use when converting real-world units such as centimetres.
     */

    SVGAndroidRenderer(Canvas canvas, float defaultDPI, SVGExternalFileResolver externalFileResolver) {
        this.canvas = canvas;
        this.dpi = defaultDPI;
        this.externalFileResolver = externalFileResolver;
    }


    float getDPI() {
        return dpi;
    }


    float getCurrentFontSize() {
        return state.fillPaint.getTextSize();
    }


    float getCurrentFontXHeight() {
        // The CSS3 spec says to use 0.5em if there is no way to determine true x-height;
        return state.fillPaint.getTextSize() / 2f;
    }


    /*
     * Get the current view port in user units.
     * If a viewBox is in effect, then this will return the viewBox
     * since a viewBox transform will have already been applied.
     */
    Box getEffectiveViewPortInUserUnits() {
        if (state.viewBox != null)
            return state.viewBox;
        else
            return state.viewPort;
    }


    /*
     * Render the whole document.
     */
    void renderDocument(SVGBase document, RenderOptionsBase renderOptions) {
        if (renderOptions == null)
            throw new NullPointerException("renderOptions shouldn't be null");  // Sanity check. Should never happen

        this.document = document;

        Svg rootObj = document.getRootElement();

        if (rootObj == null) {
            warn("Nothing to render. Document is empty.");
            return;
        }

        Box viewBox;
        PreserveAspectRatio preserveAspectRatio;

        if (renderOptions.hasView()) {
            SvgObject obj = this.document.getElementById(renderOptions.viewId);
            if (!(obj instanceof View)) {
                Log.w(TAG, String.format("View element with id \"%s\" not found.", renderOptions.viewId));
                return;
            }
            View view = (View) obj;

            if (view.viewBox == null) {
                Log.w(TAG, String.format("View element with id \"%s\" is missing a viewBox attribute.", renderOptions.viewId));
                return;
            }
            viewBox = view.viewBox;
            preserveAspectRatio = view.preserveAspectRatio;
        } else {
            viewBox = renderOptions.hasViewBox() ? renderOptions.viewBox
                    : rootObj.viewBox;
            preserveAspectRatio = renderOptions.hasPreserveAspectRatio() ? renderOptions.preserveAspectRatio
                    : rootObj.preserveAspectRatio;
        }

        if (renderOptions.hasCss()) {
            if (renderOptions.css != null) {
                CSSParser parser = new CSSParser(CSSParser.Source.RenderOptions, externalFileResolver);
                document.addCSSRules(parser.parse(renderOptions.css));
            } else if (renderOptions.cssRuleset != null) {
                document.addCSSRules(renderOptions.cssRuleset);
            }
        }
        if (renderOptions.hasTarget()) {
            this.ruleMatchContext = new CSSParser.RuleMatchContext();
            this.ruleMatchContext.targetElement = document.getElementById(renderOptions.targetId);
        }

        // Initialise the state
        resetState();

        checkXMLSpaceAttribute(rootObj);

        // Save state
        statePush(true);

        Box viewPort = new Box(renderOptions.viewPort);
        // If root element specifies a width, then we need to adjust our default viewPort that was based on the canvas size
        if (rootObj.width != null)
            viewPort.width = rootObj.width.floatValue(this, viewPort.width);
        if (rootObj.height != null)
            viewPort.height = rootObj.height.floatValue(this, viewPort.height);

        // Render the document
        render(rootObj, viewPort, viewBox, preserveAspectRatio);

        // Restore state
        statePop();

        if (renderOptions.hasCss())
            document.clearRenderCSSRules();
    }


    //==============================================================================
    // Render dispatcher


    private void render(SvgObject obj) {
        if (obj instanceof NotDirectlyRendered)
            return;

        // Save state
        statePush();

        checkXMLSpaceAttribute(obj);

        if (obj instanceof Svg) {
            render((Svg) obj);
        } else if (obj instanceof Use) {
            render((Use) obj);
        } else if (obj instanceof Switch) {
            render((Switch) obj);
        } else if (obj instanceof Group) {   // Includes <a> elements
            render((Group) obj);
        } else if (obj instanceof Image) {
            render((Image) obj);
        } else if (obj instanceof SVGBase.Path) {
            render((SVGBase.Path) obj);
        } else if (obj instanceof Rect) {
            render((Rect) obj);
        } else if (obj instanceof Circle) {
            render((Circle) obj);
        } else if (obj instanceof Ellipse) {
            render((Ellipse) obj);
        } else if (obj instanceof Line) {
            render((Line) obj);
        } else if (obj instanceof Polygon) {
            render((Polygon) obj);
        } else if (obj instanceof PolyLine) {
            render((PolyLine) obj);
        } else if (obj instanceof Text) {
            render((Text) obj);
        }

        // Restore state
        statePop();
    }


    //==============================================================================


    private void renderChildren(SvgContainer obj, boolean isContainer) {
        if (isContainer) {
            parentPush(obj);
        }

        for (SvgObject child : obj.getChildren()) {
            render(child);
        }

        if (isContainer) {
            parentPop();
        }
    }


    //==============================================================================


    private void statePush() {
        statePush(false);
    }

    private void statePush(boolean isRootContext) {
        if (isRootContext) {
            // Root SVG context should be transparent. So we need to saveLayer
            // to avoid background messing with blend modes etc.
            canvasSaveLayer(canvas, null, null);
        } else {
            canvas.save();
        }
        // Save style state
        stateStack.push(state);
        state = new RendererState(state);
    }


    private void statePop() {
        // Restore matrix and clip
        canvas.restore();
        // Restore style state
        state = stateStack.pop();
    }


    /*
     * Canvas#saveLayer(bounds, paint, flags) is deprecated in SDK 28 and might be
     * removed at short notice. As save(flags) was in SDK 28.  So we have created
     * this method as future-proofing.
     */
    private void canvasSaveLayer(Canvas canvas, RectF bounds, Paint paint) {
        canvas.saveLayer(bounds, paint);
    }


    //==============================================================================


    private void parentPush(SvgContainer obj) {
        parentStack.push(obj);
        Matrix currentMatrix = new Matrix();
        canvas.getMatrix(currentMatrix);
        matrixStack.push(currentMatrix);
    }


    private void parentPop() {
        parentStack.pop();
        matrixStack.pop();
    }


    //==============================================================================


    private void updateStyleForElement(RendererState state, SvgElementBase obj) {
        boolean isRootSVG = (obj.parent == null);
        state.style.resetNonInheritingProperties(isRootSVG);

        // Apply the styles defined by style attributes on the element
        if (obj.baseStyle != null)
            updateStyle(state, obj.baseStyle);

        // Apply the styles from any CSS files or <style> elements
        if (document.hasCSSRules()) {
            for (CSSParser.Rule rule : document.getCSSRules()) {
                if (CSSParser.ruleMatch(this.ruleMatchContext, rule.selector, obj)) {
                    updateStyle(state, rule.style);
                }
            }
        }

        // Apply the styles defined by the 'style' attribute. They have the highest precedence.
        if (obj.style != null)
            updateStyle(state, obj.style);
    }


    /*
     * Check and update xml:space handling.
     */
    private void checkXMLSpaceAttribute(SvgObject obj) {
        if (!(obj instanceof SvgElementBase))
            return;

        SvgElementBase bobj = (SvgElementBase) obj;
        if (bobj.spacePreserve != null)
            state.spacePreserve = bobj.spacePreserve;
    }


    /*
     * Fill a path with either the given paint, or if a pattern is set, with the pattern.
     */
    private void doFilledPath(SvgElement obj, Path path) {
        // First check for pattern fill. It requires special handling.
        if (state.style.fill instanceof PaintReference) {
            SvgObject ref = document.resolveIRI(((PaintReference) state.style.fill).href);
            if (ref instanceof Pattern) {
                Pattern pattern = (Pattern) ref;
                fillWithPattern(obj, path, pattern);
                return;
            }
        }

        // Otherwise do a normal fill
        canvas.drawPath(path, state.fillPaint);
    }


    private void doStroke(Path path) {
        // TODO handle degenerate subpaths properly

        if (state.style.vectorEffect == VectorEffect.NonScalingStroke) {
            // For non-scaling-stroke, the stroke width is not transformed along with the path.
            // It will be rendered at the same width no matter how the document contents are transformed.

            // First step: get the current canvas matrix
            Matrix currentMatrix = new Matrix();
            canvas.getMatrix(currentMatrix);
            // Transform the path using this transform
            Path transformedPath = new Path();
            path.transform(currentMatrix, transformedPath);
            // Reset the current canvas transform completely
            canvas.setMatrix(new Matrix());

            // If there is a shader (such as a gradient), we need to update its transform also
            Shader shader = state.strokePaint.getShader();
            Matrix currentShaderMatrix = new Matrix();
            if (shader != null) {
                shader.getLocalMatrix(currentShaderMatrix);
                Matrix newShaderMatrix = new Matrix(currentShaderMatrix);
                newShaderMatrix.postConcat(currentMatrix);
                shader.setLocalMatrix(newShaderMatrix);
            }

            // Render the transformed path. The stroke width used will be in unscaled device units.
            canvas.drawPath(transformedPath, state.strokePaint);

            // Return the current canvas transform to what it was before all this happened
            canvas.setMatrix(currentMatrix);
            // And reset the shader matrix also
            if (shader != null)
                shader.setLocalMatrix(currentShaderMatrix);
        } else {
            canvas.drawPath(path, state.strokePaint);
        }
    }


    //==============================================================================


    private static void warn(String format, Object... args) {
        Log.w(TAG, String.format(format, args));
    }


    private static void error(String format, Object... args) {
        Log.e(TAG, String.format(format, args));
    }


    private static void debug(String format, Object... args) {
    }


    //==============================================================================
    // Renderers for each element type


    private void render(Svg obj) {
        // <svg> elements establish a new viewport.
        Box viewPort = makeViewPort(obj.x, obj.y, obj.width, obj.height);

        render(obj, viewPort, obj.viewBox, obj.preserveAspectRatio);
    }


    // When referenced by a <use> element, it's width and height take precedence over the ones in the <svg> object.
    private void render(Svg obj, Box viewPort) {
        render(obj, viewPort, obj.viewBox, obj.preserveAspectRatio);
    }


    // When called from renderDocument, we pass in our own viewBox.
    // If rendering the whole document, it will be rootObj.viewBox.  When rendering a view
    // it will be the viewBox from the <view> element.
    private void render(Svg obj, Box viewPort, Box viewBox, PreserveAspectRatio positioning) {
        debug("Svg render");

        if (viewPort.width == 0f || viewPort.height == 0f)
            return;

        // "If attribute 'preserveAspectRatio' is not specified, then the effect is as if a value of xMidYMid meet were specified."
        if (positioning == null)
            positioning = (obj.preserveAspectRatio != null) ? obj.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

        updateStyleForElement(state, obj);

        if (!display())
            return;

        state.viewPort = viewPort;

        if (!state.style.overflow) {
            setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width, state.viewPort.height);
        }

        checkForClipPath(obj, state.viewPort);

        if (viewBox != null) {
            canvas.concat(calculateViewBoxTransform(state.viewPort, viewBox, positioning));
            state.viewBox = obj.viewBox;  // Note: definitely obj.viewBox here. Not viewBox parameter.
        } else {
            canvas.translate(state.viewPort.minX, state.viewPort.minY);
            state.viewBox = null;
        }

        boolean compositing = pushLayer();

        // Action the viewport-fill property (if set)
        viewportFill();

        renderChildren(obj, true);

        if (compositing)
            popLayer(obj);

        updateParentBoundingBox(obj);
    }


    // Derive the viewport from the x, y, width and height attributes of an object
    private Box makeViewPort(Length x, Length y, Length width, Length height) {
        float _x = (x != null) ? x.floatValueX(this) : 0f;
        float _y = (y != null) ? y.floatValueY(this) : 0f;

        Box viewPortUser = getEffectiveViewPortInUserUnits();
        float _w = (width != null) ? width.floatValueX(this) : viewPortUser.width;  // default 100%
        float _h = (height != null) ? height.floatValueY(this) : viewPortUser.height;

        return new Box(_x, _y, _w, _h);
    }


    //==============================================================================


    // Render <g> and <a> elements
    private void render(Group obj) {
        debug(obj.getNodeName() + " render");

        updateStyleForElement(state, obj);

        if (!display())
            return;

        if (obj.transform != null) {
            canvas.concat(obj.transform);
        }

        checkForClipPath(obj);

        boolean compositing = pushLayer();

        renderChildren(obj, true);

        if (compositing)
            popLayer(obj);

        updateParentBoundingBox(obj);
    }


    //==============================================================================


    /*
     * Called by an object to update it's parent's bounding box.
     *
     * This operation is made more tricky because the child's bbox is in the child's coordinate space,
     * but the parent needs it in the parent's coordinate space.
     */
    private void updateParentBoundingBox(SvgElement obj) {
        if (obj.parent == null)       // skip this if obj is root element
            return;
        if (obj.boundingBox == null)  // empty bbox, possibly as a result of a badly defined element (eg bad use reference etc)
            return;

        // Convert the corners of the child bbox to world space
        Matrix m = new Matrix();
        // Get the inverse of the child transform
        if (matrixStack.peek().invert(m)) {
            float[] pts = {obj.boundingBox.minX, obj.boundingBox.minY,
                    obj.boundingBox.maxX(), obj.boundingBox.minY,
                    obj.boundingBox.maxX(), obj.boundingBox.maxY(),
                    obj.boundingBox.minX, obj.boundingBox.maxY()};
            // Now concatenate the parent's matrix to create a child-to-parent transform
            Matrix currentMatrix = new Matrix();
            canvas.getMatrix(currentMatrix);
            m.preConcat(currentMatrix);
            m.mapPoints(pts);
            // Finally, find the bounding box of the transformed points
            RectF rect = new RectF(pts[0], pts[1], pts[0], pts[1]);
            for (int i = 2; i <= 6; i += 2) {
                if (pts[i] < rect.left) rect.left = pts[i];
                if (pts[i] > rect.right) rect.right = pts[i];
                if (pts[i + 1] < rect.top) rect.top = pts[i + 1];
                if (pts[i + 1] > rect.bottom) rect.bottom = pts[i + 1];
            }
            // Update the parent bounding box with the transformed bbox
            SvgElement parent = (SvgElement) parentStack.peek();
            if (parent.boundingBox == null)
                parent.boundingBox = Box.fromLimits(rect.left, rect.top, rect.right, rect.bottom);
            else
                parent.boundingBox.union(Box.fromLimits(rect.left, rect.top, rect.right, rect.bottom));
        }
    }


    //==============================================================================


    private boolean pushLayer() {
        return pushLayer(1f);
    }


    private boolean pushLayer(float opacityAdjustment) {
        // opacityAdjustment is used by fillWithPattern() in order to apply the fillOpacity for the
        // pattern

        if (!requiresCompositing() && opacityAdjustment == 1f)
            return false;

        // Custom version of statePush() that also saves the layer
        Paint savePaint = new Paint();
        savePaint.setAlpha(clamp255(state.style.opacity * opacityAdjustment));
        if (SUPPORTS_BLEND_MODE && state.style.mixBlendMode != CSSBlendMode.normal) {
            setBlendMode(savePaint);
        }
        canvasSaveLayer(canvas, null, savePaint);

        // Save style state
        stateStack.push(state);
        state = new RendererState(state);

        if (state.style.mask != null) {
            SvgObject ref = document.resolveIRI(state.style.mask);
            // Check the we are referencing a mask element
            if (!(ref instanceof Mask)) {
                // This is an invalid mask reference - disable this object's mask
                error("Mask reference '%s' not found", state.style.mask);
                state.style.mask = null;
                return true;
            }

            // After this method completes, the caller will draw the masked object to it's own layer.
            // That will later be composited together with our mask layer (in popLayer())
        }

        return true;
    }


    private void popLayer(SvgElement obj) {
        popLayer(obj, obj.boundingBox);
    }


    /**
     * @param obj             The object we are compositing. Compositing happens if the obj is not fully opaque, or if it has a mask.
     * @param originalObjBBox Normally equal to obj.boundingBox. However, if obj is a mask, then this is the bounding box of the original object to which the mask was applied.
     */
    private void popLayer(SvgElement obj, Box originalObjBBox) {
        // If this is masked content, apply the mask now
        if (state.style.mask != null) {
            // The masked content has been drawn, now we have to composite it with our mask layer.
            // The mask has to be built from two parts:
            // Step 1: Apply a luminanceToAlpha conversion to the mask content.
            // Step 2: Multiply the mask's alpha to the alpha channel generated in step 1.

            // Final mask gets composited using Porter Duff mode DST_IN
            Paint maskPaintCombined = new Paint();
            maskPaintCombined.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvasSaveLayer(canvas, null, maskPaintCombined);

            // Step 1
            Paint maskPaint1 = new Paint();
            // ColorFilter that does the SVG luminanceToAlpha conversion
            ColorMatrix luminanceToAlpha = new ColorMatrix(new float[]{0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0,
                    SVGAndroidRenderer.LUMINANCE_TO_ALPHA_RED, SVGAndroidRenderer.LUMINANCE_TO_ALPHA_GREEN, SVGAndroidRenderer.LUMINANCE_TO_ALPHA_BLUE, 0, 0});
            maskPaint1.setColorFilter(new ColorMatrixColorFilter(luminanceToAlpha));
            canvasSaveLayer(canvas, null, maskPaint1);   // TODO use real mask bounds

            // Render the mask content into the step 1 layer
            SvgObject ref = document.resolveIRI(state.style.mask);
            renderMask((Mask) ref, obj, originalObjBBox);

            // The restore applies the luminanceToAlpha conversion
            canvas.restore();

            // Step 2
            Paint maskPaint2 = new Paint();
            maskPaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvasSaveLayer(canvas, null, maskPaint2);

            // Render the mask content (again) into the step 2 part
            renderMask((Mask) ref, obj, originalObjBBox);

            // The retore composites the luminanceToAlpha layer with the masks alpha
            canvas.restore();

            // Apply the final mask to the original object waiting in the open layer created in pushLayer()
            canvas.restore();
        }

        statePop();
    }


    private boolean requiresCompositing() {
        return (state.style.opacity < 1.0f) ||
                (state.style.mask != null) ||
                (state.style.isolation == Isolation.isolate) ||
                (SUPPORTS_BLEND_MODE && state.style.mixBlendMode != CSSBlendMode.normal);
    }


    @TargetApi(Build.VERSION_CODES.Q)
    private void setBlendMode(Paint paint) {
        debug("Setting blend mode to " + state.style.mixBlendMode);
        switch (state.style.mixBlendMode) {
            case multiply:
                paint.setBlendMode(BlendMode.MULTIPLY);
                break;
            case screen:
                paint.setBlendMode(BlendMode.SCREEN);
                break;
            case overlay:
                paint.setBlendMode(BlendMode.OVERLAY);
                break;
            case darken:
                paint.setBlendMode(BlendMode.DARKEN);
                break;
            case lighten:
                paint.setBlendMode(BlendMode.LIGHTEN);
                break;
            case color_dodge:
                paint.setBlendMode(BlendMode.COLOR_DODGE);
                break;
            case color_burn:
                paint.setBlendMode(BlendMode.COLOR_BURN);
                break;
            case hard_light:
                paint.setBlendMode(BlendMode.HARD_LIGHT);
                break;
            case soft_light:
                paint.setBlendMode(BlendMode.SOFT_LIGHT);
                break;
            case difference:
                paint.setBlendMode(BlendMode.DIFFERENCE);
                break;
            case exclusion:
                paint.setBlendMode(BlendMode.EXCLUSION);
                break;
            case hue:
                paint.setBlendMode(BlendMode.HUE);
                break;
            case saturation:
                paint.setBlendMode(BlendMode.SATURATION);
                break;
            case color:
                paint.setBlendMode(BlendMode.COLOR);
                break;
            case luminosity:
                paint.setBlendMode(BlendMode.LUMINOSITY);
                break;
            case normal:
            default:
                paint.setBlendMode(null);
                break;
        }
    }


    //==============================================================================


    /*
     * Find the first child of the switch that passes the feature tests and render only that child.
     */
    private void render(Switch obj) {
        debug("Switch render");

        updateStyleForElement(state, obj);

        if (!display())
            return;

        if (obj.transform != null) {
            canvas.concat(obj.transform);
        }

        checkForClipPath(obj);

        boolean compositing = pushLayer();

        renderSwitchChild(obj);

        if (compositing)
            popLayer(obj);

        updateParentBoundingBox(obj);
    }


    private void renderSwitchChild(Switch obj) {
        String deviceLanguage = Locale.getDefault().getLanguage();

        ChildLoop:
        for (SvgObject child : obj.getChildren()) {
            // Ignore any objects that don't belong in a <switch>
            if (!(child instanceof SvgConditional)) {
                continue;
            }
            SvgConditional condObj = (SvgConditional) child;

            // We don't support extensions
            if (condObj.getRequiredExtensions() != null) {
                continue;
            }
            // Check language
            Set<String> syslang = condObj.getSystemLanguage();
            if (syslang != null && (syslang.isEmpty() || !syslang.contains(deviceLanguage))) {
                continue;
            }
            // Check features
            Set<String> reqfeat = condObj.getRequiredFeatures();
            if (reqfeat != null) {
                if (supportedFeatures == null)
                    initialiseSupportedFeaturesMap();
                if (reqfeat.isEmpty() || !supportedFeatures.containsAll(reqfeat)) {
                    continue;
                }
            }
            // Check formats (MIME types)
            Set<String> reqfmts = condObj.getRequiredFormats();
            if (reqfmts != null) {
                if (reqfmts.isEmpty() || externalFileResolver == null)
                    continue;
                for (String mimeType : reqfmts) {
                    if (!externalFileResolver.isFormatSupported(mimeType))
                        continue ChildLoop;
                }
            }
            // Check fonts
            Set<String> reqfonts = condObj.getRequiredFonts();
            if (reqfonts != null) {
                if (reqfonts.isEmpty() || externalFileResolver == null)
                    continue;
                for (String fontName : reqfonts) {
                    if (externalFileResolver.resolveFont(fontName, state.style.fontWeight, String.valueOf(state.style.fontStyle), state.style.fontStretch) == null)
                        continue ChildLoop;
                }
            }

            // All checks passed!  Render this one element and exit
            render(child);
            break;
        }
    }


    private static synchronized void initialiseSupportedFeaturesMap() {
        supportedFeatures = new HashSet<>();

        // SVG features this SVG implementation supports
        // Actual feature strings have the prefix: FEATURE_STRING_PREFIX (see above)
        // NO indicates feature will probable not ever be implemented
        // NYI indicates support is in progress, or is planned

        // Feature sets that represent sets of other feature strings (ie a group of features strings)
        //supportedFeatures.add("SVG");                       // NO
        //supportedFeatures.add("SVGDOM");                    // NO
        //supportedFeatures.add("SVG-static");                // NO
        //supportedFeatures.add("SVGDOM-static");             // NO
        //supportedFeatures.add("SVG-animation");             // NO
        //supportedFeatures.add("SVGDOM-animation");          // NO
        //supportedFeatures.add("SVG-dynamic");               // NO
        //supportedFeatures.add("SVGDOM-dynamic");            // NO

        // Individual features
        //supportedFeatures.add("CoreAttribute");             // NO
        supportedFeatures.add("Structure");                   // YES (although desc title and metadata are ignored)
        supportedFeatures.add("BasicStructure");              // YES (although desc title and metadata are ignored)
        //supportedFeatures.add("ContainerAttribute");        // NO (filter related. NYI)
        supportedFeatures.add("ConditionalProcessing");       // YES
        supportedFeatures.add("Image");                       // YES (bitmaps only - not SVG files)
        supportedFeatures.add("Style");                       // YES
        supportedFeatures.add("ViewportAttribute");           // YES
        supportedFeatures.add("Shape");                       // YES
        //supportedFeatures.add("Text");                      // NO
        supportedFeatures.add("BasicText");                   // YES
        supportedFeatures.add("PaintAttribute");              // YES (except color-interpolation and color-rendering)
        supportedFeatures.add("BasicPaintAttribute");         // YES (except color-rendering)
        supportedFeatures.add("OpacityAttribute");            // YES
        //supportedFeatures.add("GraphicsAttribute");         // NO
        supportedFeatures.add("BasicGraphicsAttribute");      // YES
        supportedFeatures.add("Marker");                      // YES
        //supportedFeatures.add("ColorProfile");              // NO
        supportedFeatures.add("Gradient");                    // YES
        supportedFeatures.add("Pattern");                     // YES
        supportedFeatures.add("Clip");                        // YES
        supportedFeatures.add("BasicClip");                   // YES
        supportedFeatures.add("Mask");                        // YES
        //supportedFeatures.add("Filter");                    // NO
        //supportedFeatures.add("BasicFilter");               // NO
        //supportedFeatures.add("DocumentEventsAttribute");   // NO
        //supportedFeatures.add("GraphicalEventsAttribute");  // NO
        //supportedFeatures.add("AnimationEventsAttribute");  // NO
        //supportedFeatures.add("Cursor");                    // NO
        //supportedFeatures.add("Hyperlinking");              // NO
        //supportedFeatures.add("XlinkAttribute");            // NO
        //supportedFeatures.add("ExternalResourcesRequired"); // NO
        supportedFeatures.add("View");                        // YES
        //supportedFeatures.add("Script");                    // NO
        //supportedFeatures.add("Animation");                 // NO
        //supportedFeatures.add("Font");                      // NO
        //supportedFeatures.add("BasicFont");                 // NO
        //supportedFeatures.add("Extensibility");             // NO

        // SVG 1.0 features - all are too general and include things we are not likely to ever support.
        // If we ever do support these, we'll need to change how FEATURE_STRING_PREFIX is used.
        //supportedFeatures.add("org.w3c.svg");
        //supportedFeatures.add("org.w3c.dom.svg");
        //supportedFeatures.add("org.w3c.svg.static");
        //supportedFeatures.add("org.w3c.dom.svg.static");
        //supportedFeatures.add("org.w3c.svg.animation");
        //supportedFeatures.add("org.w3c.dom.svg.animation");
        //supportedFeatures.add("org.w3c.svg.dynamic");
        //supportedFeatures.add("org.w3c.dom.svg.dynamic");
        //supportedFeatures.add("org.w3c.svg.all");
        //supportedFeatures.add("org.w3c.dom.svg.all" );
    }


    //==============================================================================


    private void render(Use obj) {
        debug("Use render");

        if ((obj.width != null && obj.width.isZero()) ||
                (obj.height != null && obj.height.isZero()))
            return;

        updateStyleForElement(state, obj);

        if (!display())
            return;

        // Locate the referenced object
        SvgObject ref = obj.document.resolveIRI(obj.href);
        if (ref == null) {
            error("Use reference '%s' not found", obj.href);
            return;
        }

        if (obj.transform != null) {
            canvas.concat(obj.transform);
        }

        // Handle the x,y attributes
        float _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
        float _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
        canvas.translate(_x, _y);

        checkForClipPath(obj);

        boolean compositing = pushLayer();

        parentPush(obj);

        if (ref instanceof Svg) {
            Svg svgElem = (Svg) ref;
            Box viewPort = makeViewPort(null, null, obj.width, obj.height);

            statePush();
            render(svgElem, viewPort);
            statePop();
        } else if (ref instanceof Symbol) {
            Length _w = (obj.width != null) ? obj.width : new Length(100, Unit.percent);
            Length _h = (obj.height != null) ? obj.height : new Length(100, Unit.percent);
            Box viewPort = makeViewPort(null, null, _w, _h);

            statePush();
            render((Symbol) ref, viewPort);
            statePop();
        } else {
            render(ref);
        }

        parentPop();

        if (compositing)
            popLayer(obj);

        updateParentBoundingBox(obj);
    }


    //==============================================================================


    private void render(SVGBase.Path obj) {
        debug("Path render");

        if (obj.d == null)
            return;

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;
        if (!state.hasStroke && !state.hasFill)
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        Path path = (new PathConverter(obj.d)).getPath();

        if (obj.boundingBox == null) {
            obj.boundingBox = calculatePathBounds(path);
        }
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        if (state.hasFill) {
            path.setFillType(getFillTypeFromState());
            doFilledPath(obj, path);
        }
        if (state.hasStroke)
            doStroke(path);

        renderMarkers(obj);

        if (compositing)
            popLayer(obj);
    }


    private Box calculatePathBounds(Path path) {
        RectF pathBounds = new RectF();
        path.computeBounds(pathBounds, true);
        return new Box(pathBounds.left, pathBounds.top, pathBounds.width(), pathBounds.height());
    }


    //==============================================================================


    private void render(Rect obj) {
        debug("Rect render");

        if (obj.width == null || obj.height == null || obj.width.isZero() || obj.height.isZero())
            return;

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        Path path = makePathAndBoundingBox(obj);
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        if (state.hasFill)
            doFilledPath(obj, path);
        if (state.hasStroke)
            doStroke(path);


        if (compositing)
            popLayer(obj);
    }


    //==============================================================================


    private void render(Circle obj) {
        debug("Circle render");

        if (obj.r == null || obj.r.isZero())
            return;

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        Path path = makePathAndBoundingBox(obj);
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        if (state.hasFill)
            doFilledPath(obj, path);
        if (state.hasStroke)
            doStroke(path);

        if (compositing)
            popLayer(obj);
    }


    //==============================================================================


    private void render(Ellipse obj) {
        debug("Ellipse render");

        if (obj.rx == null || obj.ry == null || obj.rx.isZero() || obj.ry.isZero())
            return;

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        Path path = makePathAndBoundingBox(obj);
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        if (state.hasFill)
            doFilledPath(obj, path);
        if (state.hasStroke)
            doStroke(path);

        if (compositing)
            popLayer(obj);
    }


    //==============================================================================


    private void render(Line obj) {
        debug("Line render");

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;
        if (!state.hasStroke)
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        Path path = makePathAndBoundingBox(obj);
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        doStroke(path);

        renderMarkers(obj);

        if (compositing)
            popLayer(obj);
    }


    private List<MarkerVector> calculateMarkerPositions(Line obj) {
        float _x1, _y1, _x2, _y2;
        _x1 = (obj.x1 != null) ? obj.x1.floatValueX(this) : 0f;
        _y1 = (obj.y1 != null) ? obj.y1.floatValueY(this) : 0f;
        _x2 = (obj.x2 != null) ? obj.x2.floatValueX(this) : 0f;
        _y2 = (obj.y2 != null) ? obj.y2.floatValueY(this) : 0f;

        List<MarkerVector> markers = new ArrayList<>(2);
        markers.add(new MarkerVector(_x1, _y1, (_x2 - _x1), (_y2 - _y1)));
        markers.add(new MarkerVector(_x2, _y2, (_x2 - _x1), (_y2 - _y1)));
        return markers;
    }


    //==============================================================================


    private void render(PolyLine obj) {
        debug("PolyLine render");

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;
        if (!state.hasStroke && !state.hasFill)
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        int numPoints = (obj.points != null) ? obj.points.length : 0;
        if (numPoints < 2 ||     // pointless
                numPoints % 2 == 1)  // error
            return;

        Path path = makePathAndBoundingBox(obj);
        updateParentBoundingBox(obj);

        path.setFillType(getFillTypeFromState());

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        if (state.hasFill)
            doFilledPath(obj, path);
        if (state.hasStroke)
            doStroke(path);

        renderMarkers(obj);

        if (compositing)
            popLayer(obj);
    }


    private List<MarkerVector> calculateMarkerPositions(PolyLine obj) {
        int numPoints = (obj.points != null) ? obj.points.length : 0;
        if (numPoints < 2)
            return null;

        List<MarkerVector> markers = new ArrayList<>();
        MarkerVector lastPos = new MarkerVector(obj.points[0], obj.points[1], 0, 0);
        float x = 0, y = 0;

        for (int i = 2; i < numPoints; i += 2) {
            x = obj.points[i];
            y = obj.points[i + 1];
            lastPos.add(x, y);
            markers.add(lastPos);
            lastPos = new MarkerVector(x, y, x - lastPos.x, y - lastPos.y);
        }

        // Deal with last point
        if (obj instanceof Polygon) {
            if (x != obj.points[0] && y != obj.points[1]) {
                x = obj.points[0];
                y = obj.points[1];
                lastPos.add(x, y);
                markers.add(lastPos);
                // Last marker point needs special handling because its orientation depends
                // on the orientation of the very first segment of the path
                MarkerVector newPos = new MarkerVector(x, y, x - lastPos.x, y - lastPos.y);
                newPos.add(markers.get(0));
                markers.add(newPos);
                markers.set(0, newPos);  // Start marker is the same
            }
        } else {
            markers.add(lastPos);
        }
        return markers;
    }


    //==============================================================================


    private void render(Polygon obj) {
        debug("Polygon render");

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;
        if (!state.hasStroke && !state.hasFill)
            return;

        if (obj.transform != null)
            canvas.concat(obj.transform);

        int numPoints = (obj.points != null) ? obj.points.length : 0;
        if (numPoints < 2)
            return;

        Path path = makePathAndBoundingBox(obj);
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        if (state.hasFill)
            doFilledPath(obj, path);
        if (state.hasStroke)
            doStroke(path);

        renderMarkers(obj);

        if (compositing)
            popLayer(obj);
    }


    //==============================================================================


    private void render(Text obj) {
        debug("Text render");

        updateStyleForElement(state, obj);

        if (!display())
            return;

        selectTypefaceAndFontStyling();

        if (obj.transform != null)
            canvas.concat(obj.transform);

        // Get the first coordinate pair from the lists in the x and y properties.
        float x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
        float y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
        float dx = (obj.dx == null || obj.dx.size() == 0) ? 0f : obj.dx.get(0).floatValueX(this);
        float dy = (obj.dy == null || obj.dy.size() == 0) ? 0f : obj.dy.get(0).floatValueY(this);

        // Handle text alignment
        Style.TextAnchor anchor = getAnchorPosition();
        if (anchor != Style.TextAnchor.Start) {
            float textWidth = calculateTextWidth(obj);
            if (anchor == Style.TextAnchor.Middle) {
                x -= (textWidth / 2);
            } else {
                x -= textWidth;  // 'End' (right justify)
            }
        }

        if (obj.boundingBox == null) {
            TextBoundsCalculator proc = new TextBoundsCalculator(x, y);
            enumerateTextSpans(obj, proc);
            obj.boundingBox = new Box(proc.bbox.left, proc.bbox.top, proc.bbox.width(), proc.bbox.height());
        }
        updateParentBoundingBox(obj);

        checkForGradientsAndPatterns(obj);
        checkForClipPath(obj);

        boolean compositing = pushLayer();

        enumerateTextSpans(obj, new PlainTextDrawer(x + dx, y + dy));

        if (compositing)
            popLayer(obj);
    }


    private void selectTypefaceAndFontStyling() {
        Typeface font = null;

        if (state.style.fontFamily != null && document != null) {
            for (String fontName : state.style.fontFamily) {
                font = checkGenericFont(fontName, state.style.fontWeight, state.style.fontStyle);
                if (font == null && externalFileResolver != null) {
                    font = externalFileResolver.resolveFont(fontName, state.style.fontWeight, String.valueOf(state.style.fontStyle), state.style.fontStretch);
                }
                if (font != null)
                    break;
            }
        }
        if (font == null) {
            // Fall back to default font
            font = checkGenericFont(DEFAULT_FONT_FAMILY, state.style.fontWeight, state.style.fontStyle);
        }
        state.fillPaint.setTypeface(font);
        state.strokePaint.setTypeface(font);

        // Just in case this is a variable font, let's also set the fontVariationSettings
        // In order to get the desired font weight and style
        state.fontVariationSet.addSetting(CSSFontVariationSettings.VARIATION_WEIGHT, state.style.fontWeight);
        if (state.style.fontStyle == FontStyle.italic) {
            state.fontVariationSet.addSetting(CSSFontVariationSettings.VARIATION_ITALIC, CSSFontVariationSettings.VARIATION_ITALIC_VALUE_ON);
            // Add oblique as well - as a fallback in case it has not "ital" axis
            state.fontVariationSet.addSetting(CSSFontVariationSettings.VARIATION_OBLIQUE, CSSFontVariationSettings.VARIATION_OBLIQUE_VALUE_ON);
        } else if (state.style.fontStyle == FontStyle.oblique)
            state.fontVariationSet.addSetting(CSSFontVariationSettings.VARIATION_OBLIQUE, CSSFontVariationSettings.VARIATION_OBLIQUE_VALUE_ON);
        state.fontVariationSet.addSetting(CSSFontVariationSettings.VARIATION_WIDTH, state.style.fontStretch);

        String fontVariationSettings = state.fontVariationSet.toString();
        debug("fontVariationSettings = " + fontVariationSettings);
        state.fillPaint.setFontVariationSettings(fontVariationSettings);
        state.strokePaint.setFontVariationSettings(fontVariationSettings);

        String fontFeatureSettings = state.fontFeatureSet.toString();
        debug("fontFeatureSettings = " + fontFeatureSettings);
        state.fillPaint.setFontFeatureSettings(fontFeatureSettings);
        state.strokePaint.setFontFeatureSettings(fontFeatureSettings);
    }


    private Style.TextAnchor getAnchorPosition() {
        if (state.style.direction == Style.TextDirection.LTR || state.style.textAnchor == TextAnchor.Middle)
            return state.style.textAnchor;

        // Handle RTL case where Start and End are reversed
        return (state.style.textAnchor == TextAnchor.Start) ? TextAnchor.End : TextAnchor.Start;
    }


    private class PlainTextDrawer extends TextProcessor {
        float x;
        float y;

        PlainTextDrawer(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void processText(String text) {
            debug("TextSequence render");

            if (visible()) {
                // Android/Skia divides letterspacing and puts half before and after each letter.
                // We need to readjust initial text X position to counter that.
                float letterspacingAdj = state.style.letterSpacing.floatValue(SVGAndroidRenderer.this) / 2;
                if (state.hasFill)
                    canvas.drawText(text, x - letterspacingAdj, y, state.fillPaint);
                if (state.hasStroke)
                    canvas.drawText(text, x - letterspacingAdj, y, state.strokePaint);
            }

            // Update the current text position
            x += measureText(text, state.fillPaint);
        }
    }


    //==============================================================================
    // Text sequence enumeration


    private static abstract class TextProcessor {
        public boolean doTextContainer(TextContainer obj) {
            return true;
        }

        public abstract void processText(String text);
    }


    /*
     * Given a text container, recursively visit its children invoking the TextDrawer
     * handler for each segment of text found.
     */
    private void enumerateTextSpans(TextContainer obj, TextProcessor textprocessor) {
        if (!display())
            return;

        Iterator<SvgObject> iter = obj.children.iterator();
        boolean isFirstChild = true;

        while (iter.hasNext()) {
            SvgObject child = iter.next();

            if (child instanceof TextSequence) {
                textprocessor.processText(textXMLSpaceTransform(((TextSequence) child).text, isFirstChild, !iter.hasNext() /*isLastChild*/));
            } else {
                processTextChild(child, textprocessor);
            }
            isFirstChild = false;
        }
    }


    private void processTextChild(SvgObject obj, TextProcessor textprocessor) {
        // Ask the processor implementation if it wants to process this object
        if (!textprocessor.doTextContainer((TextContainer) obj))
            return;

        if (obj instanceof TextPath) {
            // Save state
            statePush();

            renderTextPath((TextPath) obj);

            // Restore state
            statePop();
        } else if (obj instanceof TSpan) {
            debug("TSpan render");

            // Save state
            statePush();

            TSpan tspan = (TSpan) obj;

            updateStyleForElement(state, tspan);

            if (display()) {
                selectTypefaceAndFontStyling();

                // Get the first coordinate pair from the lists in the x and y properties.
                float x = 0, y = 0, dx = 0, dy = 0;
                boolean specifiedX = (tspan.x != null && tspan.x.size() > 0);
                if (textprocessor instanceof PlainTextDrawer) {
                    x = !specifiedX ? ((PlainTextDrawer) textprocessor).x : tspan.x.get(0).floatValueX(this);
                    y = (tspan.y == null || tspan.y.size() == 0) ? ((PlainTextDrawer) textprocessor).y : tspan.y.get(0).floatValueY(this);
                    dx = (tspan.dx == null || tspan.dx.size() == 0) ? 0f : tspan.dx.get(0).floatValueX(this);
                    dy = (tspan.dy == null || tspan.dy.size() == 0) ? 0f : tspan.dy.get(0).floatValueY(this);
                }

                // If x was specified on tspan, then we need to recalculate the alignment
                if (specifiedX) {
                    Style.TextAnchor anchor = getAnchorPosition();
                    if (anchor != Style.TextAnchor.Start) {
                        float textWidth = calculateTextWidth(tspan);
                        if (anchor == Style.TextAnchor.Middle) {
                            x -= (textWidth / 2);
                        } else {
                            x -= textWidth;  // 'End' (right justify)
                        }
                    }
                }

                checkForGradientsAndPatterns((SvgElement) tspan.getTextRoot());

                if (textprocessor instanceof PlainTextDrawer) {
                    ((PlainTextDrawer) textprocessor).x = x + dx;
                    ((PlainTextDrawer) textprocessor).y = y + dy;
                }

                boolean compositing = pushLayer();

                enumerateTextSpans(tspan, textprocessor);

                if (compositing)
                    popLayer(tspan);
            }

            // Restore state
            statePop();
        } else if (obj instanceof TRef) {
            // Save state
            statePush();

            TRef tref = (TRef) obj;

            updateStyleForElement(state, tref);

            if (display()) {
                checkForGradientsAndPatterns((SvgElement) tref.getTextRoot());

                // Locate the referenced object
                SvgObject ref = obj.document.resolveIRI(tref.href);
                if (ref instanceof TextContainer) {
                    StringBuilder str = new StringBuilder();
                    extractRawText((TextContainer) ref, str);
                    if (str.length() > 0) {
                        textprocessor.processText(str.toString());
                    }
                } else {
                    error("Tref reference '%s' not found", tref.href);
                }
            }

            // Restore state
            statePop();
        }
    }


    //==============================================================================


    private void renderTextPath(TextPath obj) {
        debug("TextPath render");

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        selectTypefaceAndFontStyling();

        SvgObject ref = obj.document.resolveIRI(obj.href);
        if (ref == null) {
            error("TextPath reference '%s' not found", obj.href);
            return;
        }

        SVGBase.Path pathObj = (SVGBase.Path) ref;
        Path path = (new PathConverter(pathObj.d)).getPath();

        if (pathObj.transform != null)
            path.transform(pathObj.transform);

        PathMeasure measure = new PathMeasure(path, false);

        float startOffset = (obj.startOffset != null) ? obj.startOffset.floatValue(this, measure.getLength()) : 0f;

        // Handle text alignment
        Style.TextAnchor anchor = getAnchorPosition();
        if (anchor != Style.TextAnchor.Start) {
            float textWidth = calculateTextWidth(obj);
            if (anchor == Style.TextAnchor.Middle) {
                startOffset -= (textWidth / 2);
            } else {
                startOffset -= textWidth;  // 'End' (right justify)
            }
        }

        checkForGradientsAndPatterns((SvgElement) obj.getTextRoot());

        boolean compositing = pushLayer();

        enumerateTextSpans(obj, new PathTextDrawer(path, startOffset, 0f));

        if (compositing)
            popLayer(obj);
    }


    private class PathTextDrawer extends PlainTextDrawer {
        private final Path path;

        PathTextDrawer(Path path, float x, float y) {
            super(x, y);
            this.path = path;
        }

        @Override
        public void processText(String text) {
            if (visible()) {
                // Android/Skia divides letterspacing and puts half before and after each letter.
                // We need to readjust initial text X position to counter that.
                float letterspacingAdj = state.style.letterSpacing.floatValue(SVGAndroidRenderer.this) / 2;
                if (state.hasFill)
                    canvas.drawTextOnPath(text, path, x - letterspacingAdj, y, state.fillPaint);
                if (state.hasStroke)
                    canvas.drawTextOnPath(text, path, x - letterspacingAdj, y, state.strokePaint);
            }

            // Update the current text position
            x += measureText(text, state.fillPaint);
        }
    }


    //==============================================================================


    /*
     * Calculate the approximate width of this line of text.
     * To simplify, we will ignore font changes and just assume that all the text
     * uses the current font.
     */
    private float calculateTextWidth(TextContainer parentTextObj) {
        TextWidthCalculator proc = new TextWidthCalculator();
        enumerateTextSpans(parentTextObj, proc);
        return proc.x;
    }

    private class TextWidthCalculator extends TextProcessor {
        float x = 0;

        @Override
        public void processText(String text) {
            x += measureText(text, state.fillPaint);
        }
    }


    /*
     * Calculate an accurate text width.
     * In the case of very small font sizes, Paint.measureText() returns a result that is too large,
     * because it rounds up (Maih.ceil()) the total width before returning.
     */
    private float measureText(String text, Paint paint) {
        float[] widths = new float[text.length()];
        paint.getTextWidths(text, widths);
        float total = 0;
        for (int i = 0; i < widths.length; i++) {
            total += widths[i];
        }
        return total;
    }


    //==============================================================================


    /*
     * Use the TextDrawer process to determine the bounds of a <text> element
     */
    private class TextBoundsCalculator extends TextProcessor {
        float x;
        float y;
        final RectF bbox = new RectF();

        TextBoundsCalculator(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean doTextContainer(TextContainer obj) {
            if (obj instanceof TextPath) {
                // Since we cheat a bit with our textPath rendering, we need
                // to cheat a bit with our bbox calculation.
                TextPath tpath = (TextPath) obj;
                SvgObject ref = obj.document.resolveIRI(tpath.href);
                if (ref == null) {
                    error("TextPath path reference '%s' not found", tpath.href);
                    return false;
                }
                SVGBase.Path pathObj = (SVGBase.Path) ref;
                Path path = (new PathConverter(pathObj.d)).getPath();
                if (pathObj.transform != null)
                    path.transform(pathObj.transform);
                RectF pathBounds = new RectF();
                path.computeBounds(pathBounds, true);
                bbox.union(pathBounds);
                return false;
            }
            return true;
        }

        @Override
        public void processText(String text) {
            if (visible()) {
                android.graphics.Rect rect = new android.graphics.Rect();
                // Get text bounding box (for offset 0)
                state.fillPaint.getTextBounds(text, 0, text.length(), rect);
                RectF textbounds = new RectF(rect);
                // Adjust bounds to offset at text position
                textbounds.offset(x, y);
                // Merge with accumulated bounding box
                bbox.union(textbounds);
            }

            // Update the current text position
            x += measureText(text, state.fillPaint);
        }
    }


    /*
     * Extract the raw text from a TextContainer. Used by <tref> handler code.
     */
    private void extractRawText(TextContainer parent, StringBuilder str) {
        Iterator<SvgObject> iter = parent.children.iterator();
        boolean isFirstChild = true;

        while (iter.hasNext()) {
            SvgObject child = iter.next();

            if (child instanceof TextContainer) {
                extractRawText((TextContainer) child, str);
            } else if (child instanceof TextSequence) {
                str.append(textXMLSpaceTransform(((TextSequence) child).text, isFirstChild, !iter.hasNext() /*isLastChild*/));
            }
            isFirstChild = false;
        }
    }


    //==============================================================================

    // Process the text string according to the xml:space rules
    private String textXMLSpaceTransform(String text, boolean isFirstChild, boolean isLastChild) {
        if (state.spacePreserve)  // xml:space = "preserve"
            return PATTERN_TABS_OR_LINE_BREAKS.matcher(text).replaceAll(" ");

        // xml:space = "default"
        text = PATTERN_TABS.matcher(text).replaceAll("");
        text = PATTERN_LINE_BREAKS.matcher(text).replaceAll(" ");
        //text = text.trim();
        if (isFirstChild)
            text = PATTERN_START_SPACES.matcher(text).replaceAll("");
        if (isLastChild)
            text = PATTERN_END_SPACES.matcher(text).replaceAll("");
        return PATTERN_DOUBLE_SPACES.matcher(text).replaceAll(" ");
    }


    //==============================================================================


    private void render(Symbol obj, Box viewPort) {
        debug("Symbol render");

        if (viewPort.width == 0f || viewPort.height == 0f)
            return;

        // "If attribute 'preserveAspectRatio' is not specified, then the effect is as if a value of xMidYMid meet were specified."
        PreserveAspectRatio positioning = (obj.preserveAspectRatio != null) ? obj.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

        updateStyleForElement(state, obj);

        state.viewPort = viewPort;

        if (!state.style.overflow) {
            setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width, state.viewPort.height);
        }

        if (obj.viewBox != null) {
            canvas.concat(calculateViewBoxTransform(state.viewPort, obj.viewBox, positioning));
            state.viewBox = obj.viewBox;
        } else {
            canvas.translate(state.viewPort.minX, state.viewPort.minY);
            state.viewBox = null;
        }

        boolean compositing = pushLayer();

        renderChildren(obj, true);

        if (compositing)
            popLayer(obj);

        updateParentBoundingBox(obj);
    }


    //==============================================================================


    private void render(Image obj) {
        debug("Image render");

        if (obj.width == null || obj.width.isZero() ||
                obj.height == null || obj.height.isZero())
            return;

        if (obj.href == null)
            return;

        // "If attribute 'preserveAspectRatio' is not specified, then the effect is as if a value of xMidYMid meet were specified."
        PreserveAspectRatio positioning = (obj.preserveAspectRatio != null) ? obj.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

        // Locate the referenced image
        Bitmap image = checkForImageDataURL(obj.href);
        if (image == null) {
            if (externalFileResolver == null)
                return;

            image = externalFileResolver.resolveImage(obj.href);
        }
        if (image == null) {
            error("Could not locate image '%s'", obj.href);
            return;
        }
        Box imageNaturalSize = new Box(0, 0, image.getWidth(), image.getHeight());

        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null) {
            canvas.concat(obj.transform);
        }

        float _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
        float _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
        float _w = obj.width.floatValueX(this);
        float _h = obj.height.floatValueX(this);
        state.viewPort = new Box(_x, _y, _w, _h);

        if (!state.style.overflow) {
            setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width, state.viewPort.height);
        }

        obj.boundingBox = state.viewPort;
        updateParentBoundingBox(obj);

        checkForClipPath(obj);

        boolean compositing = pushLayer();

        viewportFill();

        canvas.save();

        // Local transform from image's natural dimensions to the specified SVG dimensions
        canvas.concat(calculateViewBoxTransform(state.viewPort, imageNaturalSize, positioning));

        Paint bmPaint = new Paint((state.style.imageRendering == RenderQuality.optimizeSpeed) ? 0 : Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(image, 0, 0, bmPaint);

        canvas.restore();

        if (compositing)
            popLayer(obj);
    }


    //==============================================================================


    /*
     * Check for and decode an image encoded in a data URL.
     * We don't handle all permutations of data URLs. Only base64 ones.
     */
    private Bitmap checkForImageDataURL(String url) {
        if (!url.startsWith("data:"))
            return null;
        if (url.length() < 14)
            return null;

        int comma = url.indexOf(',');
        if (comma < 12) // "< 12"  test also covers not found (-1) case
            return null;
        if (!";base64".equals(url.substring(comma - 7, comma)))
            return null;
        try {
            byte[] imageData = Base64.decode(url.substring(comma + 1), Base64.DEFAULT);  // throws IllegalArgumentException for bad data
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        } catch (Exception e) {
            Log.e(TAG, "Could not decode bad Data URL", e);
            return null;
        }
    }


    private boolean display() {
        if (state.style.display != null)
            return state.style.display;
        return true;
    }


    private boolean visible() {
        if (state.style.visibility != null)
            return state.style.visibility;
        return true;
    }


    /*
     * Calculate the transform required to fit the supplied viewBox into the current viewPort.
     * See spec section 7.8 for an explanation of how this works.
     *
     * aspectRatioRule determines where the graphic is placed in the viewPort when aspect ration
     *    is kept.  xMin means left justified, xMid is centred, xMax is right justified etc.
     * slice determines whether we see the whole image or not. True fill the whole viewport.
     *    If slice is false, the image will be "letter-boxed".
     *
     * Note values in the two Box parameters whould be in user units. If you pass values
     * that are in "objectBoundingBox" space, you will get incorrect results.
     */
    private Matrix calculateViewBoxTransform(Box viewPort, Box viewBox, PreserveAspectRatio positioning) {
        Matrix m = new Matrix();

        if (positioning == null || positioning.getAlignment() == null)
            return m;

        float xScale = viewPort.width / viewBox.width;
        float yScale = viewPort.height / viewBox.height;
        float xOffset = -viewBox.minX;
        float yOffset = -viewBox.minY;

        // 'none' means scale both dimensions to fit the viewport
        if (positioning.equals(PreserveAspectRatio.STRETCH)) {
            m.preTranslate(viewPort.minX, viewPort.minY);
            m.preScale(xScale, yScale);
            m.preTranslate(xOffset, yOffset);
            return m;
        }

        // Otherwise, the aspect ratio of the image is kept.
        // What scale are we going to use?
        float scale = (positioning.getScale() == PreserveAspectRatio.Scale.slice) ? Math.max(xScale, yScale) : Math.min(xScale, yScale);
        // What size will the image end up being?
        float imageW = viewPort.width / scale;
        float imageH = viewPort.height / scale;
        // Determine final X position
        switch (positioning.getAlignment()) {
            case xMidYMin:
            case xMidYMid:
            case xMidYMax:
                xOffset -= (viewBox.width - imageW) / 2;
                break;
            case xMaxYMin:
            case xMaxYMid:
            case xMaxYMax:
                xOffset -= (viewBox.width - imageW);
                break;
            default:
                // nothing to do
                break;
        }
        // Determine final Y position
        switch (positioning.getAlignment()) {
            case xMinYMid:
            case xMidYMid:
            case xMaxYMid:
                yOffset -= (viewBox.height - imageH) / 2;
                break;
            case xMinYMax:
            case xMidYMax:
            case xMaxYMax:
                yOffset -= (viewBox.height - imageH);
                break;
            default:
                // nothing to do
                break;
        }

        m.preTranslate(viewPort.minX, viewPort.minY);
        m.preScale(scale, scale);
        m.preTranslate(xOffset, yOffset);
        return m;
    }


    private boolean isSpecified(Style style, long flag) {
        return (style.specifiedFlags & flag) != 0;
    }


    /*
     * Updates the global style state with the style defined by the current object.
     * Will also update the current paints etc where appropriate.
     */
    private void updateStyle(RendererState state, Style style) {
        // Now update each style property we know about
        if (isSpecified(style, Style.SPECIFIED_COLOR)) {
            state.style.color = style.color;
        }

        if (isSpecified(style, Style.SPECIFIED_OPACITY)) {
            state.style.opacity = style.opacity;
        }

        if (isSpecified(style, Style.SPECIFIED_FILL)) {
            state.style.fill = style.fill;
            state.hasFill = (style.fill != null && style.fill != Colour.TRANSPARENT);
        }

        if (isSpecified(style, Style.SPECIFIED_FILL_OPACITY)) {
            state.style.fillOpacity = style.fillOpacity;
        }

        // If either fill or its opacity has changed, update the fillPaint
        if (isSpecified(style, Style.SPECIFIED_FILL | Style.SPECIFIED_FILL_OPACITY | Style.SPECIFIED_COLOR | Style.SPECIFIED_OPACITY)) {
            setPaintColour(state, true, state.style.fill);
        }

        if (isSpecified(style, Style.SPECIFIED_FILL_RULE)) {
            state.style.fillRule = style.fillRule;
        }


        if (isSpecified(style, Style.SPECIFIED_STROKE)) {
            state.style.stroke = style.stroke;
            state.hasStroke = (style.stroke != null && style.stroke != Colour.TRANSPARENT);
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_OPACITY)) {
            state.style.strokeOpacity = style.strokeOpacity;
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE | Style.SPECIFIED_STROKE_OPACITY | Style.SPECIFIED_COLOR | Style.SPECIFIED_OPACITY)) {
            setPaintColour(state, false, state.style.stroke);
        }

        if (isSpecified(style, Style.SPECIFIED_VECTOR_EFFECT)) {
            state.style.vectorEffect = style.vectorEffect;
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_WIDTH)) {
            state.style.strokeWidth = style.strokeWidth;
            state.strokePaint.setStrokeWidth(state.style.strokeWidth.floatValue(this));
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_LINECAP)) {
            state.style.strokeLineCap = style.strokeLineCap;
            switch (style.strokeLineCap) {
                case Butt:
                    state.strokePaint.setStrokeCap(Paint.Cap.BUTT);
                    break;
                case Round:
                    state.strokePaint.setStrokeCap(Paint.Cap.ROUND);
                    break;
                case Square:
                    state.strokePaint.setStrokeCap(Paint.Cap.SQUARE);
                    break;
                default:
                    break;
            }
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_LINEJOIN)) {
            state.style.strokeLineJoin = style.strokeLineJoin;
            switch (style.strokeLineJoin) {
                case Miter:
                    state.strokePaint.setStrokeJoin(Paint.Join.MITER);
                    break;
                case Round:
                    state.strokePaint.setStrokeJoin(Paint.Join.ROUND);
                    break;
                case Bevel:
                    state.strokePaint.setStrokeJoin(Paint.Join.BEVEL);
                    break;
                default:
                    break;
            }
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_MITERLIMIT)) {
            // FIXME: must be >= 0
            state.style.strokeMiterLimit = style.strokeMiterLimit;
            state.strokePaint.setStrokeMiter(style.strokeMiterLimit);
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_DASHARRAY)) {
            state.style.strokeDashArray = style.strokeDashArray;
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_DASHOFFSET)) {
            state.style.strokeDashOffset = style.strokeDashOffset;
        }

        if (isSpecified(style, Style.SPECIFIED_STROKE_DASHARRAY | Style.SPECIFIED_STROKE_DASHOFFSET)) {
            // Either the dash array or dash offset has changed.
            if (state.style.strokeDashArray == null) {
                state.strokePaint.setPathEffect(null);
            } else {
                float intervalSum = 0f;
                int n = state.style.strokeDashArray.length;
                // SVG dash arrays can be odd length, whereas Android dash arrays must have an even length.
                // So we solve the problem by doubling the array length.
                int arrayLen = (n % 2 == 0) ? n : n * 2;
                float[] intervals = new float[arrayLen];
                for (int i = 0; i < arrayLen; i++) {
                    intervals[i] = state.style.strokeDashArray[i % n].floatValue(this);
                    intervalSum += intervals[i];
                }
                if (intervalSum == 0f) {
                    state.strokePaint.setPathEffect(null);
                } else {
                    float offset = state.style.strokeDashOffset.floatValue(this);
                    if (offset < 0) {
                        // SVG offsets can be negative. Not sure if Android ones can be.
                        // Just in case we will convert it.
                        offset = intervalSum + (offset % intervalSum);
                    }
                    state.strokePaint.setPathEffect(new DashPathEffect(intervals, offset));
                }
            }
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_SIZE)) {
            float currentFontSize = getCurrentFontSize();
            state.style.fontSize = style.fontSize;
            state.fillPaint.setTextSize(style.fontSize.floatValue(this, currentFontSize));
            state.strokePaint.setTextSize(style.fontSize.floatValue(this, currentFontSize));
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_FAMILY)) {
            state.style.fontFamily = style.fontFamily;
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_WEIGHT)) {
            // Font weights are 0..1000
            // Relative weight rules from CSS-Fonts-4: https://www.w3.org/TR/css-fonts-4/#relative-weights
            if (style.fontWeight == Style.FONT_WEIGHT_LIGHTER) {
                float fw = state.style.fontWeight;
                if (fw >= 100f && fw < 550f)
                    state.style.fontWeight = 100f;
                else if (fw >= 550f && fw < 750f)
                    state.style.fontWeight = 400f;
                else if (fw >= 750f)
                    state.style.fontWeight = 700f;
            } else if (style.fontWeight == Style.FONT_WEIGHT_BOLDER) {
                float fw = state.style.fontWeight;
                if (fw < 350f)
                    state.style.fontWeight = 400f;
                else if (fw >= 350f && fw < 550f)
                    state.style.fontWeight = 700f;
                else if (fw >= 550f && fw < 900f)
                    state.style.fontWeight = 900f;
            } else
                state.style.fontWeight = style.fontWeight;
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_STYLE)) {
            state.style.fontStyle = style.fontStyle;
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_STRETCH)) {
            // Typical font stretch values are 50...200 (percent)
            state.style.fontStretch = style.fontStretch;
        }

        if (isSpecified(style, Style.SPECIFIED_TEXT_DECORATION)) {
            state.style.textDecoration = style.textDecoration;
            state.fillPaint.setStrikeThruText(style.textDecoration == TextDecoration.LineThrough);
            state.fillPaint.setUnderlineText(style.textDecoration == TextDecoration.Underline);
            state.strokePaint.setStrikeThruText(style.textDecoration == TextDecoration.LineThrough);
            state.strokePaint.setUnderlineText(style.textDecoration == TextDecoration.Underline);
        }

        if (isSpecified(style, Style.SPECIFIED_DIRECTION)) {
            state.style.direction = style.direction;
        }

        if (isSpecified(style, Style.SPECIFIED_TEXT_ANCHOR)) {
            state.style.textAnchor = style.textAnchor;
        }

        if (isSpecified(style, Style.SPECIFIED_OVERFLOW)) {
            state.style.overflow = style.overflow;
        }

        if (isSpecified(style, Style.SPECIFIED_MARKER_START)) {
            state.style.markerStart = style.markerStart;
        }

        if (isSpecified(style, Style.SPECIFIED_MARKER_MID)) {
            state.style.markerMid = style.markerMid;
        }

        if (isSpecified(style, Style.SPECIFIED_MARKER_END)) {
            state.style.markerEnd = style.markerEnd;
        }

        if (isSpecified(style, Style.SPECIFIED_DISPLAY)) {
            state.style.display = style.display;
        }

        if (isSpecified(style, Style.SPECIFIED_VISIBILITY)) {
            state.style.visibility = style.visibility;
        }

        if (isSpecified(style, Style.SPECIFIED_CLIP)) {
            state.style.clip = style.clip;
        }

        if (isSpecified(style, Style.SPECIFIED_CLIP_PATH)) {
            state.style.clipPath = style.clipPath;
        }

        if (isSpecified(style, Style.SPECIFIED_CLIP_RULE)) {
            state.style.clipRule = style.clipRule;
        }

        if (isSpecified(style, Style.SPECIFIED_MASK)) {
            state.style.mask = style.mask;
        }

        if (isSpecified(style, Style.SPECIFIED_STOP_COLOR)) {
            state.style.stopColor = style.stopColor;
        }

        if (isSpecified(style, Style.SPECIFIED_STOP_OPACITY)) {
            state.style.stopOpacity = style.stopOpacity;
        }

        if (isSpecified(style, Style.SPECIFIED_VIEWPORT_FILL)) {
            state.style.viewportFill = style.viewportFill;
        }

        if (isSpecified(style, Style.SPECIFIED_VIEWPORT_FILL_OPACITY)) {
            state.style.viewportFillOpacity = style.viewportFillOpacity;
        }

        if (isSpecified(style, Style.SPECIFIED_IMAGE_RENDERING)) {
            state.style.imageRendering = style.imageRendering;
        }

        if (isSpecified(style, Style.SPECIFIED_ISOLATION)) {
            state.style.isolation = style.isolation;
        }

        if (isSpecified(style, Style.SPECIFIED_MIX_BLEND_MODE)) {
            state.style.mixBlendMode = style.mixBlendMode;
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_KERNING)) {
            state.style.fontKerning = style.fontKerning;
            state.fontFeatureSet.applyKerning(style.fontKerning);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_FEATURE_SETTINGS)) {
            state.style.fontFeatureSettings = style.fontFeatureSettings;
            state.fontFeatureSet.applySettings(style.fontFeatureSettings);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_VARIANT_LIGATURES)) {
            state.style.fontVariantLigatures = style.fontVariantLigatures;
            state.fontFeatureSet.applySettings(style.fontVariantLigatures);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_VARIANT_POSITION)) {
            state.style.fontVariantPosition = style.fontVariantPosition;
            state.fontFeatureSet.applySettings(style.fontVariantPosition);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_VARIANT_CAPS)) {
            state.style.fontVariantCaps = style.fontVariantCaps;
            state.fontFeatureSet.applySettings(style.fontVariantCaps);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_VARIANT_NUMERIC)) {
            state.style.fontVariantNumeric = style.fontVariantNumeric;
            state.fontFeatureSet.applySettings(style.fontVariantNumeric);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_VARIANT_EAST_ASIAN)) {
            state.style.fontVariantEastAsian = style.fontVariantEastAsian;
            state.fontFeatureSet.applySettings(style.fontVariantEastAsian);
        }

        if (isSpecified(style, Style.SPECIFIED_FONT_VARIATION_SETTINGS)) {
            state.style.fontVariationSettings = style.fontVariationSettings;
            state.fontVariationSet.applySettings(style.fontVariationSettings);
        }

        if (isSpecified(style, Style.SPECIFIED_WRITING_MODE)) {
            state.style.writingMode = style.writingMode;
        }

        if (isSpecified(style, Style.SPECIFIED_GLYPH_ORIENTATION_VERTICAL)) {
            state.style.glyphOrientationVertical = style.glyphOrientationVertical;
        }

        if (isSpecified(style, Style.SPECIFIED_TEXT_ORIENTATION)) {
            state.style.textOrientation = style.textOrientation;
        }

        if (isSpecified(style, Style.SPECIFIED_LETTER_SPACING)) {
            state.style.letterSpacing = style.letterSpacing;
            // Note: Paint.setLetterSpacing() takes a value in ems.
            state.fillPaint.setLetterSpacing(style.letterSpacing.floatValue(this) / getCurrentFontSize());
            state.strokePaint.setLetterSpacing(style.letterSpacing.floatValue(this) / getCurrentFontSize());
        }

        if (isSpecified(style, Style.SPECIFIED_WORD_SPACING)) {
            state.style.wordSpacing = style.wordSpacing;
            if (SUPPORTS_PAINT_WORD_SPACING) {
                state.fillPaint.setWordSpacing(style.wordSpacing.floatValue(this));
                state.strokePaint.setWordSpacing(style.wordSpacing.floatValue(this));
            }
        }

    }


    private void setPaintColour(RendererState state, boolean isFill, SvgPaint paint) {
        float paintOpacity = (isFill) ? state.style.fillOpacity : state.style.strokeOpacity;
        int col;
        if (paint instanceof Colour) {
            col = ((Colour) paint).colour;
        } else if (paint instanceof CurrentColor) {
            col = state.style.color.colour;
        } else {
            return;
        }
        col = colourWithOpacity(col, paintOpacity);
        if (isFill)
            state.fillPaint.setColor(col);
        else
            state.strokePaint.setColor(col);
    }


    private Typeface checkGenericFont(String fontName, Float fontWeight, FontStyle fontStyle) {
        Typeface font = null;
        int typefaceStyle;

        boolean italic = (fontStyle == Style.FontStyle.italic);
        typefaceStyle = (fontWeight >= Style.FONT_WEIGHT_BOLD) ? (italic ? Typeface.BOLD_ITALIC : Typeface.BOLD)
                : (italic ? Typeface.ITALIC : Typeface.NORMAL);

        switch (fontName) {
            case "serif":
                font = Typeface.create(Typeface.SERIF, typefaceStyle);
                break;
            case "sans-serif":
            case "cursive":
            case "fantasy":
                font = Typeface.create(Typeface.SANS_SERIF, typefaceStyle);
                break;
            case "monospace":
                font = Typeface.create(Typeface.MONOSPACE, typefaceStyle);
                break;
        }
        return font;
    }


    // Convert a float in range 0..1 to an int in range 0..255.
    private static int clamp255(float val) {
        int i = (int) (val * 256f);
        return (i < 0) ? 0 : Math.min(i, 255);
    }


    private static int colourWithOpacity(int colour, float opacity) {
        int alpha = (colour >> 24) & 0xff;
        alpha = Math.round(alpha * opacity);
        alpha = (alpha < 0) ? 0 : Math.min(alpha, 255);
        return (alpha << 24) | (colour & 0xffffff);
    }


    private Path.FillType getFillTypeFromState() {
        if (state.style.fillRule != null && state.style.fillRule == Style.FillRule.EvenOdd)
            return Path.FillType.EVEN_ODD;
        else
            return Path.FillType.WINDING;
    }


    private void setClipRect(float minX, float minY, float width, float height) {
        float left = minX;
        float top = minY;
        float right = minX + width;
        float bottom = minY + height;

        if (state.style.clip != null) {
            left += state.style.clip.left.floatValueX(this);
            top += state.style.clip.top.floatValueY(this);
            right -= state.style.clip.right.floatValueX(this);
            bottom -= state.style.clip.bottom.floatValueY(this);
        }

        canvas.clipRect(left, top, right, bottom);
    }


    /*
     * Viewport fill colour. A new feature in SVG 1.2.
     */
    private void viewportFill() {
        int col;
        if (state.style.viewportFill instanceof Colour) {
            col = ((Colour) state.style.viewportFill).colour;
        } else if (state.style.viewportFill instanceof CurrentColor) {
            col = state.style.color.colour;
        } else {
            return;
        }
        if (state.style.viewportFillOpacity != null)
            col = colourWithOpacity(col, state.style.viewportFillOpacity);

        canvas.drawColor(col);
    }


    //==============================================================================

    /*
     *  Convert an internal PathDefinition to an android.graphics.Path object
     */
    protected static class PathConverter implements PathInterface {
        final Path path = new Path();
        float lastX, lastY;

        PathConverter(PathDefinition pathDef) {
            if (pathDef == null)
                return;
            pathDef.enumeratePath(this);
        }

        Path getPath() {
            return path;
        }

        @Override
        public void moveTo(float x, float y) {
            path.moveTo(x, y);
            lastX = x;
            lastY = y;
        }

        @Override
        public void lineTo(float x, float y) {
            path.lineTo(x, y);
            lastX = x;
            lastY = y;
        }

        @Override
        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            path.cubicTo(x1, y1, x2, y2, x3, y3);
            lastX = x3;
            lastY = y3;
        }

        @Override
        public void quadTo(float x1, float y1, float x2, float y2) {
            path.quadTo(x1, y1, x2, y2);
            lastX = x2;
            lastY = y2;
        }

        @Override
        public void arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y) {
            SVGAndroidRenderer.arcTo(lastX, lastY, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y, this);
            lastX = x;
            lastY = y;
        }

        @Override
        public void close() {
            path.close();
        }

    }


    //=========================================================================
    // Handling of Arcs

    /*
     * SVG arc representation uses "endpoint parameterisation" where we specify the start and endpoint of the arc.
     * This is to be consistent with the other path commands.  However we need to convert this to "centre point
     * parameterisation" in order to calculate the arc. Handily, the SVG spec provides all the required maths
     * in section "F.6 Elliptical arc implementation notes".
     *
     * Some of this code has been borrowed from the Batik library (Apache-2 license).
     *
     * Previously, to work around issue #62, we converted this function to use floats. However in issue #155,
     * we discovered that there are some arcs that fail due of a lack of precision. So we have switched back to doubles.
     */

    private static void arcTo(float lastX, float lastY, float rx, float ry, float angle, boolean largeArcFlag, boolean sweepFlag, float x, float y, PathInterface pather) {
        if (lastX == x && lastY == y) {
            // If the endpoints (x, y) and (x0, y0) are identical, then this
            // is equivalent to omitting the elliptical arc segment entirely.
            // (behaviour specified by the spec)
            return;
        }

        // Handle degenerate case (behaviour specified by the spec)
        if (rx == 0 || ry == 0) {
            pather.lineTo(x, y);
            return;
        }

        // Sign of the radii is ignored (behaviour specified by the spec)
        rx = Math.abs(rx);
        ry = Math.abs(ry);

        // Convert angle from degrees to radians
        double angleRad = Math.toRadians(angle % 360.0);
        double cosAngle = Math.cos(angleRad);
        double sinAngle = Math.sin(angleRad);

        // We simplify the calculations by transforming the arc so that the origin is at the
        // midpoint calculated above followed by a rotation to line up the coordinate axes
        // with the axes of the ellipse.

        // Compute the midpoint of the line between the current and the end point
        double dx2 = (lastX - x) / 2.0;
        double dy2 = (lastY - y) / 2.0;

        // Step 1 : Compute (x1', y1')
        // x1,y1 is the midpoint vector rotated to take the arc's angle out of consideration
        double x1 = (cosAngle * dx2 + sinAngle * dy2);
        double y1 = (-sinAngle * dx2 + cosAngle * dy2);

        double rx_sq = rx * rx;
        double ry_sq = ry * ry;
        double x1_sq = x1 * x1;
        double y1_sq = y1 * y1;

        // Check that radii are large enough.
        // If they are not, the spec says to scale them up so they are.
        // This is to compensate for potential rounding errors/differences between SVG implementations.
        double radiiCheck = x1_sq / rx_sq + y1_sq / ry_sq;
        if (radiiCheck > 0.99999) {
            double radiiScale = Math.sqrt(radiiCheck) * 1.00001;
            rx = (float) (radiiScale * rx);
            ry = (float) (radiiScale * ry);
            rx_sq = rx * rx;
            ry_sq = ry * ry;
        }

        // Step 2 : Compute (cx1, cy1) - the transformed centre point
        double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
        double sq = ((rx_sq * ry_sq) - (rx_sq * y1_sq) - (ry_sq * x1_sq)) / ((rx_sq * y1_sq) + (ry_sq * x1_sq));
        sq = (sq < 0) ? 0 : sq;
        double coef = (sign * Math.sqrt(sq));
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);

        // Step 3 : Compute (cx, cy) from (cx1, cy1)
        double sx2 = (lastX + x) / 2.0;
        double sy2 = (lastY + y) / 2.0;
        double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
        double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double p, n;

        // Angle betwen two vectors is +/- acos( u.v / len(u) * len(v))
        // Where '.' is the dot product. And +/- is calculated from the sign of the cross product (u x v)

        final double TWO_PI = Math.PI * 2.0;

        // Compute the start angle
        // The angle between (ux,uy) and the 0deg angle (1,0)
        n = Math.sqrt((ux * ux) + (uy * uy));  // len(u) * len(1,0) == len(u)
        p = ux;                                // u.v == (ux,uy).(1,0) == (1 * ux) + (0 * uy) == ux
        sign = (uy < 0) ? -1.0 : 1.0;          // u x v == (1 * uy - ux * 0) == uy
        double angleStart = sign * Math.acos(p / n);  // No need for checkedArcCos() here. (p >= n) should always be true.

        // Compute the angle extent
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        sign = (ux * vy - uy * vx < 0) ? -1.0f : 1.0f;
        double angleExtent = sign * checkedArcCos(p / n);

        // Catch angleExtents of 0, which will cause problems later in arcToBeziers
        if (angleExtent == 0f) {
            pather.lineTo(x, y);
            return;
        }

        if (!sweepFlag && angleExtent > 0) {
            angleExtent -= TWO_PI;
        } else if (sweepFlag && angleExtent < 0) {
            angleExtent += TWO_PI;
        }
        angleExtent %= TWO_PI;
        angleStart %= TWO_PI;

        // Many elliptical arc implementations including the Java2D and Android ones, only
        // support arcs that are axis aligned.  Therefore we need to substitute the arc
        // with bezier curves.  The following method call will generate the beziers for
        // a unit circle that covers the arc angles we want.
        float[] bezierPoints = arcToBeziers(angleStart, angleExtent);

        // Calculate a transformation matrix that will move and scale these bezier points to the correct location.
        Matrix m = new Matrix();
        m.postScale(rx, ry);
        m.postRotate(angle);
        m.postTranslate((float) cx, (float) cy);
        m.mapPoints(bezierPoints);

        // The last point in the bezier set should match exactly the last coord pair in the arc (ie: x,y). But
        // considering all the mathematical manipulation we have been doing, it is bound to be off by a tiny
        // fraction. Experiments show that it can be up to around 0.00002.  So why don't we just set it to
        // exactly what it ought to be.
        bezierPoints[bezierPoints.length - 2] = x;
        bezierPoints[bezierPoints.length - 1] = y;

        // Final step is to add the bezier curves to the path
        for (int i = 0; i < bezierPoints.length; i += 6) {
            pather.cubicTo(bezierPoints[i], bezierPoints[i + 1], bezierPoints[i + 2], bezierPoints[i + 3], bezierPoints[i + 4], bezierPoints[i + 5]);
        }
    }


    // Check input to Math.acos() in case rounding or other errors result in a val < -1 or > +1.
    // For example, see the possible KitKat JIT error described in issue #62.
    private static double checkedArcCos(double val) {
        return (val < -1.0) ? Math.PI : (val > 1.0) ? 0 : Math.acos(val);
    }


    /*
     * Generate the control points and endpoints for a set of bezier curves that match
     * a circular arc starting from angle 'angleStart' and sweep the angle 'angleExtent'.
     * The circle the arc follows will be centred on (0,0) and have a radius of 1.0.
     *
     * Each bezier can cover no more than 90 degrees, so the arc will be divided evenly
     * into a maximum of four curves.
     *
     * The resulting control points will later be scaled and rotated to match the final
     * arc required.
     *
     * The returned array has the format [x0,y0, x1,y1,...] and excludes the start point
     * of the arc.
     */
    private static float[] arcToBeziers(double angleStart, double angleExtent) {
        int numSegments = (int) Math.ceil(Math.abs(angleExtent) * 2.0 / Math.PI);  // (angleExtent / 90deg)

        double angleIncrement = angleExtent / numSegments;

        // The length of each control point vector is given by the following formula.
        double controlLength = 4.0 / 3.0 * Math.sin(angleIncrement / 2.0) / (1.0 + Math.cos(angleIncrement / 2.0));

        float[] coords = new float[numSegments * 6];
        int pos = 0;

        for (int i = 0; i < numSegments; i++) {
            double angle = angleStart + i * angleIncrement;
            // Calculate the control vector at this angle
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            // First control point
            coords[pos++] = (float) (dx - controlLength * dy);
            coords[pos++] = (float) (dy + controlLength * dx);
            // Second control point
            angle += angleIncrement;
            dx = Math.cos(angle);
            dy = Math.sin(angle);
            coords[pos++] = (float) (dx + controlLength * dy);
            coords[pos++] = (float) (dy - controlLength * dx);
            // Endpoint of bezier
            coords[pos++] = (float) dx;
            coords[pos++] = (float) dy;
        }
        return coords;
    }


    //==============================================================================
    // Marker handling
    //==============================================================================


    private static class MarkerVector {
        final float x, y;
        float dx = 0f, dy = 0f;
        boolean isAmbiguous = false;

        MarkerVector(float x, float y, float dx, float dy) {
            this.x = x;
            this.y = y;
            // normalise direction vector
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len != 0) {
                this.dx = (float) (dx / len);
                this.dy = (float) (dy / len);
            }
        }

        void add(float x, float y) {
            // In order to get accurate angles, we have to normalise
            // all vectors before we add them.  As long as they are
            // all the same length, the angles will work out correctly.
            float dx = (x - this.x);
            float dy = (y - this.y);
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len != 0) {
                dx = (float) (dx / len);
                dy = (float) (dy / len);
            }
            // Check for degenerate result where the two unit vectors cancelled each other out
            if (dx == -this.dx && dy == -this.dy) {
                this.isAmbiguous = true;
                // Choose one of the perpendiculars now. We will get a chance to switch it later.
                this.dx = -dy;
                this.dy = dx;
            } else {
                this.dx += dx;
                this.dy += dy;
            }
        }

        void add(MarkerVector v2) {
            // Check for degenerate result where the two unit vectors cancelled each other out
            if (v2.dx == -this.dx && v2.dy == -this.dy) {
                this.isAmbiguous = true;
                // Choose one of the perpendiculars now. We will get a chance to switch it later.
                this.dx = -v2.dy;
                this.dy = v2.dx;
            } else {
                this.dx += v2.dx;
                this.dy += v2.dy;
            }
        }


        @Override
        public String toString() {
            return "(" + x + "," + y + " " + dx + "," + dy + ")";
        }
    }


    /*
     *  Calculates the positions and orientations of any markers that should be placed on the given path.
     */
    private class MarkerPositionCalculator implements PathInterface {
        private final List<MarkerVector> markers = new ArrayList<>();

        private float startX, startY;
        private MarkerVector lastPos = null;
        private boolean startArc = false, normalCubic = true;
        private int subpathStartIndex = -1;
        private boolean closepathReAdjustPending;


        MarkerPositionCalculator(PathDefinition pathDef) {
            if (pathDef == null)
                return;

            // Generate and add markers for the first N-1 points
            pathDef.enumeratePath(this);

            if (closepathReAdjustPending) {
                // Now correct the start and end marker points of the subpath.
                // They should both be oriented as if this was a midpoint (ie sum the vectors).
                lastPos.add(markers.get(subpathStartIndex));
                // Overwrite start marker. Other (end) marker will be written on exit or at start of next subpath.
                markers.set(subpathStartIndex, lastPos);
                closepathReAdjustPending = false;
            }
            // Add the marker for the pending last point
            if (lastPos != null) {
                markers.add(lastPos);
            }
        }

        List<MarkerVector> getMarkers() {
            return markers;
        }

        @Override
        public void moveTo(float x, float y) {
            if (closepathReAdjustPending) {
                // Now correct the start and end marker points of the subpath.
                // They should both be oriented as if this was a midpoint (ie sum the vectors).
                lastPos.add(markers.get(subpathStartIndex));
                // Overwrite start marker. Other (end) marker will be written on exit or at start of next subpath.
                markers.set(subpathStartIndex, lastPos);
                closepathReAdjustPending = false;
            }
            if (lastPos != null) {
                markers.add(lastPos);
            }
            startX = x;
            startY = y;
            lastPos = new MarkerVector(x, y, 0, 0);
            subpathStartIndex = markers.size();
        }

        @Override
        public void lineTo(float x, float y) {
            lastPos.add(x, y);
            markers.add(lastPos);
            lastPos = new MarkerVector(x, y, x - lastPos.x, y - lastPos.y);
            closepathReAdjustPending = false;
        }

        @Override
        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            if (normalCubic || startArc) {
                lastPos.add(x1, y1);
                markers.add(lastPos);
                startArc = false;
            }
            lastPos = new MarkerVector(x3, y3, x3 - x2, y3 - y2);
            closepathReAdjustPending = false;
        }

        @Override
        public void quadTo(float x1, float y1, float x2, float y2) {
            lastPos.add(x1, y1);
            markers.add(lastPos);
            lastPos = new MarkerVector(x2, y2, x2 - x1, y2 - y1);
            closepathReAdjustPending = false;
        }

        @Override
        public void arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y) {
            // We'll piggy-back on the arc->bezier conversion to get our start and end vectors
            startArc = true;
            normalCubic = false;
            SVGAndroidRenderer.arcTo(lastPos.x, lastPos.y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y, this);
            normalCubic = true;
            closepathReAdjustPending = false;
        }

        @Override
        public void close() {
            markers.add(lastPos);
            lineTo(startX, startY);
            // We may need to readjust the first and last markers on this subpath so that
            // the orientation is a sum of the inward and outward vectors.
            // But this only happens if the path ends or the next subpath starts with a Move.
            // See description of "orient" attribute in section 11.6.2.
            closepathReAdjustPending = true;
        }

    }


    private void renderMarkers(GraphicsElement obj) {
        if (state.style.markerStart == null && state.style.markerMid == null && state.style.markerEnd == null)
            return;

        Marker _markerStart = null;
        Marker _markerMid = null;
        Marker _markerEnd = null;

        if (state.style.markerStart != null) {
            SvgObject ref = obj.document.resolveIRI(state.style.markerStart);
            if (ref != null)
                _markerStart = (Marker) ref;
            else
                error("Marker reference '%s' not found", state.style.markerStart);
        }

        if (state.style.markerMid != null) {
            SvgObject ref = obj.document.resolveIRI(state.style.markerMid);
            if (ref != null)
                _markerMid = (Marker) ref;
            else
                error("Marker reference '%s' not found", state.style.markerMid);
        }

        if (state.style.markerEnd != null) {
            SvgObject ref = obj.document.resolveIRI(state.style.markerEnd);
            if (ref != null)
                _markerEnd = (Marker) ref;
            else
                error("Marker reference '%s' not found", state.style.markerEnd);
        }

        List<MarkerVector> markers;
        if (obj instanceof SVGBase.Path)
            markers = (new MarkerPositionCalculator(((SVGBase.Path) obj).d)).getMarkers();
        else if (obj instanceof Line)
            markers = calculateMarkerPositions((Line) obj);
        else // PolyLine and Polygon
            markers = calculateMarkerPositions((PolyLine) obj);

        if (markers == null)
            return;

        int markerCount = markers.size();
        if (markerCount == 0)
            return;

        // We don't want the markers to inherit themselves as markers, otherwise we get infinite recursion.
        state.style.markerStart = state.style.markerMid = state.style.markerEnd = null;

        if (_markerStart != null)
            renderMarker(_markerStart, markers.get(0));

        if (_markerMid != null && markers.size() > 2) {
            MarkerVector lastPos = markers.get(0);
            MarkerVector thisPos = markers.get(1);

            for (int i = 1; i < (markerCount - 1); i++) {
                MarkerVector nextPos = markers.get(i + 1);
                if (thisPos.isAmbiguous)
                    thisPos = realignMarkerMid(lastPos, thisPos, nextPos);
                renderMarker(_markerMid, thisPos);
                lastPos = thisPos;
                thisPos = nextPos;
            }
        }

        if (_markerEnd != null)
            renderMarker(_markerEnd, markers.get(markerCount - 1));
    }


    /*
     * This was one of the ambiguous markers. Try to see if we can find a better direction for
     * it, now that we have more info available on the neighbouring marker positions.
     */
    private MarkerVector realignMarkerMid(MarkerVector lastPos, MarkerVector thisPos, MarkerVector nextPos) {
        // Check the temporary marker vector against the incoming vector
        float dot = dotProduct(thisPos.dx, thisPos.dy, (thisPos.x - lastPos.x), (thisPos.y - lastPos.y));
        if (dot == 0f) {
            // Those two were perpendicular, so instead try the outgoing vector
            dot = dotProduct(thisPos.dx, thisPos.dy, (nextPos.x - thisPos.x), (nextPos.y - thisPos.y));
        }
        if (dot > 0)
            return thisPos;
        if (dot == 0f) {
            // If that was perpendicular also, then give up.
            // Else use the one that points in the same direction as 0deg (1,0) or has non-negative y.
            if (thisPos.dx > 0f || thisPos.dy >= 0)
                return thisPos;
        }
        // Reverse this vector and point the marker in the opposite direction.
        thisPos.dx = -thisPos.dx;
        thisPos.dy = -thisPos.dy;
        return thisPos;
    }


    /*
     * Calculate the dot product of two vectors.
     */
    private float dotProduct(float x1, float y1, float x2, float y2) {
        return x1 * x2 + y1 * y2;
    }


    /*
     * Render the given marker type at the given position
     */
    private void renderMarker(Marker marker, MarkerVector pos) {
        float angle = 0f;
        float unitsScale;

        statePush();

        // Calculate vector angle
        if (marker.orient != null) {
            if (Float.isNaN(marker.orient))  // Indicates "auto"
            {
                if (pos.dx != 0 || pos.dy != 0) {
                    angle = (float) Math.toDegrees(Math.atan2(pos.dy, pos.dx));
                }
            } else {
                angle = marker.orient;
            }
        }
        // Calculate units scale
        unitsScale = marker.markerUnitsAreUser ? 1f : state.style.strokeWidth.floatValue(dpi);

        // "Properties inherit into the <marker> element from its ancestors; properties do not
        // inherit from the element referencing the <marker> element." (sect 11.6.2)
        state = findInheritFromAncestorState(marker);

        Matrix m = new Matrix();
        m.preTranslate(pos.x, pos.y);
        m.preRotate(angle);
        m.preScale(unitsScale, unitsScale);
        // Scale and/or translate the marker to fit in the marker viewPort
        float _refX = (marker.refX != null) ? marker.refX.floatValueX(this) : 0f;
        float _refY = (marker.refY != null) ? marker.refY.floatValueY(this) : 0f;
        float _markerWidth = (marker.markerWidth != null) ? marker.markerWidth.floatValueX(this) : 3f;
        float _markerHeight = (marker.markerHeight != null) ? marker.markerHeight.floatValueY(this) : 3f;

        if (marker.viewBox != null) {
            // We now do a simplified version of calculateViewBoxTransform().  For now we will
            // ignore the alignment setting because refX and refY have to be aligned with the
            // marker position, and alignment would complicate the calculations.
            float xScale, yScale;

            xScale = _markerWidth / marker.viewBox.width;
            yScale = _markerHeight / marker.viewBox.height;

            // If we are keeping aspect ratio, then set both scales to the appropriate value depending on 'slice'
            PreserveAspectRatio positioning = (marker.preserveAspectRatio != null) ? marker.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;
            if (!positioning.equals(PreserveAspectRatio.STRETCH)) {
                float aspectScale = (positioning.getScale() == PreserveAspectRatio.Scale.slice) ? Math.max(xScale, yScale) : Math.min(xScale, yScale);
                xScale = yScale = aspectScale;
            }

            //m.preTranslate(viewPort.minX, viewPort.minY);
            m.preTranslate(-_refX * xScale, -_refY * yScale);
            canvas.concat(m);

            // Now we need to take account of alignment setting, because it affects the
            // size and position of the clip rectangle.
            float imageW = marker.viewBox.width * xScale;
            float imageH = marker.viewBox.height * yScale;
            float xOffset = 0f;
            float yOffset = 0f;
            switch (positioning.getAlignment()) {
                case xMidYMin:
                case xMidYMid:
                case xMidYMax:
                    xOffset -= (_markerWidth - imageW) / 2;
                    break;
                case xMaxYMin:
                case xMaxYMid:
                case xMaxYMax:
                    xOffset -= (_markerWidth - imageW);
                    break;
                default:
                    // nothing to do
                    break;
            }
            // Determine final Y position
            switch (positioning.getAlignment()) {
                case xMinYMid:
                case xMidYMid:
                case xMaxYMid:
                    yOffset -= (_markerHeight - imageH) / 2;
                    break;
                case xMinYMax:
                case xMidYMax:
                case xMaxYMax:
                    yOffset -= (_markerHeight - imageH);
                    break;
                default:
                    // nothing to do
                    break;
            }

            if (!state.style.overflow) {
                setClipRect(xOffset, yOffset, _markerWidth, _markerHeight);
            }

            m.reset();
            m.preScale(xScale, yScale);
            canvas.concat(m);
        } else {
            // No viewBox provided

            m.preTranslate(-_refX, -_refY);
            canvas.concat(m);

            if (!state.style.overflow) {
                setClipRect(0, 0, _markerWidth, _markerHeight);
            }
        }

        boolean compositing = pushLayer();

        renderChildren(marker, false);

        if (compositing)
            popLayer(marker);

        statePop();
    }


    /*
     * Determine an elements style based on it's ancestors in the tree rather than
     * it's render time ancestors.
     */
    private RendererState findInheritFromAncestorState(SvgObject obj) {
        RendererState newState = new RendererState();
        updateStyle(newState, Style.getDefaultStyle());
        return findInheritFromAncestorState(obj, newState);
    }


    private RendererState findInheritFromAncestorState(SvgObject obj, RendererState newState) {
        List<SvgElementBase> ancestors = new ArrayList<>();

        // Traverse up the document tree adding element styles to a list.
        while (true) {
            if (obj instanceof SvgElementBase) {
                ancestors.add(0, (SvgElementBase) obj);
            }
            if (obj.parent == null)
                break;
            obj = (SvgObject) obj.parent;
        }

        // Now apply the ancestor styles in reverse order to a fresh RendererState object
        for (SvgElementBase ancestor : ancestors)
            updateStyleForElement(newState, ancestor);

        // Caller may also need a valid viewBox in order to calculate percentages
        newState.viewBox = state.viewBox;
        newState.viewPort = state.viewPort;

        return newState;
    }


    //==============================================================================
    // Gradients
    //==============================================================================


    /*
     * Check for gradient fills or strokes on this object.  These are always relative
     * to the object, so can't be preconfigured. They have to be initialised at the
     * time each object is rendered.
     */
    private void checkForGradientsAndPatterns(SvgElement obj) {
        if (state.style.fill instanceof PaintReference) {
            decodePaintReference(true, obj.boundingBox, (PaintReference) state.style.fill);
        }
        if (state.style.stroke instanceof PaintReference) {
            decodePaintReference(false, obj.boundingBox, (PaintReference) state.style.stroke);
        }
    }


    /*
     * Takes a PaintReference object and generates an appropriate Android Shader object from it.
     */
    private void decodePaintReference(boolean isFill, Box boundingBox, PaintReference paintref) {
        SvgObject ref = document.resolveIRI(paintref.href);
        if (ref == null) {
            error("%s reference '%s' not found", (isFill ? "Fill" : "Stroke"), paintref.href);
            if (paintref.fallback != null) {
                setPaintColour(state, isFill, paintref.fallback);
            } else {
                if (isFill)
                    state.hasFill = false;
                else
                    state.hasStroke = false;
            }
            return;
        }
        if (ref instanceof SvgLinearGradient)
            makeLinearGradient(isFill, boundingBox, (SvgLinearGradient) ref);
        else if (ref instanceof SvgRadialGradient)
            makeRadialGradient(isFill, boundingBox, (SvgRadialGradient) ref);
        else if (ref instanceof SolidColor)
            setSolidColor(isFill, (SolidColor) ref);
        //if (ref instanceof Pattern) {}  // May be needed later if/when we do direct rendering
    }


    private void makeLinearGradient(boolean isFill, Box boundingBox, SvgLinearGradient gradient) {
        if (gradient.href != null)
            fillInChainedGradientFields(gradient, gradient.href);

        boolean userUnits = (gradient.gradientUnitsAreUser != null && gradient.gradientUnitsAreUser);
        Paint paint = isFill ? state.fillPaint : state.strokePaint;

        float _x1, _y1, _x2, _y2;
        if (userUnits) {
            _x1 = (gradient.x1 != null) ? gradient.x1.floatValueX(this) : 0f;
            _y1 = (gradient.y1 != null) ? gradient.y1.floatValueY(this) : 0f;
            _x2 = (gradient.x2 != null) ? gradient.x2.floatValueX(this) : Length.PERCENT_100.floatValueX(this);  // default is 1.0/100%
            _y2 = (gradient.y2 != null) ? gradient.y2.floatValueY(this) : 0f;
        } else {
            _x1 = (gradient.x1 != null) ? gradient.x1.floatValue(this, 1f) : 0f;
            _y1 = (gradient.y1 != null) ? gradient.y1.floatValue(this, 1f) : 0f;
            _x2 = (gradient.x2 != null) ? gradient.x2.floatValue(this, 1f) : 1f;  // default is 1.0/100%
            _y2 = (gradient.y2 != null) ? gradient.y2.floatValue(this, 1f) : 0f;
        }

        // Push the state
        statePush();

        // Set the style for the gradient (inherits from its own ancestors, not from callee's state)
        state = findInheritFromAncestorState(gradient);

        // Calculate the gradient transform matrix
        Matrix m = new Matrix();
        if (!userUnits) {
            m.preTranslate(boundingBox.minX, boundingBox.minY);
            m.preScale(boundingBox.width, boundingBox.height);
        }
        if (gradient.gradientTransform != null) {
            m.preConcat(gradient.gradientTransform);
        }

        // Create the colour and position arrays for the shader
        int numStops = gradient.children.size();
        if (numStops == 0) {
            // If there are no stops defined, we are to treat it as paint = 'none' (see spec 13.2.4)
            statePop();
            if (isFill)
                state.hasFill = false;
            else
                state.hasStroke = false;
            return;
        }

        int[] colours = new int[numStops];
        float[] positions = new float[numStops];
        int i = 0;
        float lastOffset = -1;
        for (SvgObject child : gradient.children) {
            Stop stop = (Stop) child;
            float offset = (stop.offset != null) ? stop.offset : 0f;
            if (i == 0 || offset >= lastOffset) {
                positions[i] = offset;
                lastOffset = offset;
            } else {
                // Each offset must be equal or greater than the last one.
                // If it doesn't we need to replace it with the previous value.
                positions[i] = lastOffset;
            }

            statePush();

            updateStyleForElement(state, stop);
            Colour col = (Colour) state.style.stopColor;
            if (col == null)
                col = Colour.BLACK;
            colours[i] = colourWithOpacity(col.colour, state.style.stopOpacity);
            i++;

            statePop();
        }

        // If gradient vector is zero length, we instead fill with last stop colour
        if ((_x1 == _x2 && _y1 == _y2) || numStops == 1) {
            statePop();
            paint.setColor(colours[numStops - 1]);
            return;
        }

        // Convert spreadMethod->TileMode
        TileMode tileMode = TileMode.CLAMP;
        if (gradient.spreadMethod != null) {
            if (gradient.spreadMethod == GradientSpread.reflect)
                tileMode = TileMode.MIRROR;
            else if (gradient.spreadMethod == GradientSpread.repeat)
                tileMode = TileMode.REPEAT;
        }

        statePop();

        // Create shader instance
        LinearGradient gr = new LinearGradient(_x1, _y1, _x2, _y2, colours, positions, tileMode);
        gr.setLocalMatrix(m);
        paint.setShader(gr);
        paint.setAlpha(clamp255(state.style.fillOpacity));
    }


    private void makeRadialGradient(boolean isFill, Box boundingBox, SvgRadialGradient gradient) {
        if (gradient.href != null)
            fillInChainedGradientFields(gradient, gradient.href);

        boolean userUnits = (gradient.gradientUnitsAreUser != null && gradient.gradientUnitsAreUser);
        Paint paint = isFill ? state.fillPaint : state.strokePaint;

        float _cx, _cy, _r,
                _fx = 0, _fy = 0, _fr = 0;
        if (userUnits) {
            Length fiftyPercent = new Length(50f, Unit.percent);
            _cx = (gradient.cx != null) ? gradient.cx.floatValueX(this) : fiftyPercent.floatValueX(this);
            _cy = (gradient.cy != null) ? gradient.cy.floatValueY(this) : fiftyPercent.floatValueY(this);
            _r = (gradient.r != null) ? gradient.r.floatValue(this) : fiftyPercent.floatValue(this);

            if (SUPPORTS_RADIAL_GRADIENT_WITH_FOCUS) {
                _fx = (gradient.fx != null) ? gradient.fx.floatValueX(this) : _cx;
                _fy = (gradient.fy != null) ? gradient.fy.floatValueY(this) : _cy;
                _fr = (gradient.fr != null) ? gradient.fr.floatValue(this) : 0;
            }
        } else {
            _cx = (gradient.cx != null) ? gradient.cx.floatValue(this, 1f) : 0.5f;
            _cy = (gradient.cy != null) ? gradient.cy.floatValue(this, 1f) : 0.5f;
            _r = (gradient.r != null) ? gradient.r.floatValue(this, 1f) : 0.5f;

            if (SUPPORTS_RADIAL_GRADIENT_WITH_FOCUS) {
                _fx = (gradient.fx != null) ? gradient.fx.floatValue(this, 1f) : 0.5f;
                _fy = (gradient.fy != null) ? gradient.fy.floatValue(this, 1f) : 0.5f;
                _fr = (gradient.fr != null) ? gradient.fr.floatValue(this, 1f) : 0;
            }
        }
        // fx and fy are ignored because Android RadialGradient doesn't support a
        // 'focus' point that is different from cx,cy.

        // Push the state
        statePush();

        // Set the style for the gradient (inherits from its own ancestors, not from callee's state)
        state = findInheritFromAncestorState(gradient);

        // Calculate the gradient transform matrix
        Matrix m = new Matrix();
        if (!userUnits) {
            m.preTranslate(boundingBox.minX, boundingBox.minY);
            m.preScale(boundingBox.width, boundingBox.height);
        }
        if (gradient.gradientTransform != null) {
            m.preConcat(gradient.gradientTransform);
        }

        // Create the colour and position arrays for the shader
        int numStops = gradient.children.size();
        if (numStops == 0) {
            // If there are no stops defined, we are to treat it as paint = 'none' (see spec 13.2.4)
            statePop();
            if (isFill)
                state.hasFill = false;
            else
                state.hasStroke = false;
            return;
        }

        int[] colours = null;
        //@ColorLong
        long[] colourLongs = null;

        if (SUPPORTS_RADIAL_GRADIENT_WITH_FOCUS) {
            colourLongs = new long[numStops];
        } else {
            colours = new int[numStops];
        }

        float[] positions = new float[numStops];
        int i = 0;
        float lastOffset = -1;
        for (SvgObject child : gradient.children) {
            Stop stop = (Stop) child;
            float offset = (stop.offset != null) ? stop.offset : 0f;
            if (i == 0 || offset >= lastOffset) {
                positions[i] = offset;
                lastOffset = offset;
            } else {
                // Each offset must be equal or greater than the last one.
                // If it doesn't we need to replace it with the previous value.
                positions[i] = lastOffset;
            }

            statePush();

            updateStyleForElement(state, stop);
            Colour col = (Colour) state.style.stopColor;
            if (col == null)
                col = Colour.BLACK;
            if (SUPPORTS_RADIAL_GRADIENT_WITH_FOCUS) {
                colourLongs[i] = Color.pack(colourWithOpacity(col.colour, state.style.stopOpacity));
            } else {
                colours[i] = colourWithOpacity(col.colour, state.style.stopOpacity);
            }
            i++;

            statePop();
        }

        // If gradient radius is zero, we instead fill with last stop colour
        if (_r == 0 || numStops == 1) {
            statePop();
            paint.setColor(colours[numStops - 1]);
            return;
        }

        // Convert spreadMethod->TileMode
        TileMode tileMode = TileMode.CLAMP;
        if (gradient.spreadMethod != null) {
            if (gradient.spreadMethod == GradientSpread.reflect)
                tileMode = TileMode.MIRROR;
            else if (gradient.spreadMethod == GradientSpread.repeat)
                tileMode = TileMode.REPEAT;
        }

        statePop();

        // Create shader instance
        RadialGradient gr = SUPPORTS_RADIAL_GRADIENT_WITH_FOCUS ? new RadialGradient(_fx, _fy, _fr, _cx, _cy, _r, colourLongs, positions, tileMode)
                : new RadialGradient(_cx, _cy, _r, colours, positions, tileMode);
        gr.setLocalMatrix(m);
        paint.setShader(gr);
        paint.setAlpha(clamp255(state.style.fillOpacity));
    }


    /*
     * Any unspecified fields in this gradient can be 'borrowed' from another
     * gradient specified by the href attribute.
     */
    private void fillInChainedGradientFields(GradientElement gradient, String href) {
        // Locate the referenced object
        SvgObject ref = gradient.document.resolveIRI(href);
        if (ref == null) {
            // Non-existent
            warn("Gradient reference '%s' not found", href);
            return;
        }
        if (!(ref instanceof GradientElement)) {
            error("Gradient href attributes must point to other gradient elements");
            return;
        }
        if (ref == gradient) {
            error("Circular reference in gradient href attribute '%s'", href);
            return;
        }

        GradientElement grRef = (GradientElement) ref;

        if (gradient.gradientUnitsAreUser == null)
            gradient.gradientUnitsAreUser = grRef.gradientUnitsAreUser;
        if (gradient.gradientTransform == null)
            gradient.gradientTransform = grRef.gradientTransform;
        if (gradient.spreadMethod == null)
            gradient.spreadMethod = grRef.spreadMethod;
        if (gradient.children.isEmpty())
            gradient.children = grRef.children;

        try {
            if (gradient instanceof SvgLinearGradient) {
                fillInChainedGradientFields((SvgLinearGradient) gradient, (SvgLinearGradient) ref);
            } else {
                fillInChainedGradientFields((SvgRadialGradient) gradient, (SvgRadialGradient) ref);
            }
        } catch (ClassCastException e) { /* expected - do nothing */ }

        if (grRef.href != null)
            fillInChainedGradientFields(gradient, grRef.href);
    }


    private void fillInChainedGradientFields(SvgLinearGradient gradient, SvgLinearGradient grRef) {
        if (gradient.x1 == null)
            gradient.x1 = grRef.x1;
        if (gradient.y1 == null)
            gradient.y1 = grRef.y1;
        if (gradient.x2 == null)
            gradient.x2 = grRef.x2;
        if (gradient.y2 == null)
            gradient.y2 = grRef.y2;
    }


    private void fillInChainedGradientFields(SvgRadialGradient gradient, SvgRadialGradient grRef) {
        if (gradient.cx == null)
            gradient.cx = grRef.cx;
        if (gradient.cy == null)
            gradient.cy = grRef.cy;
        if (gradient.r == null)
            gradient.r = grRef.r;
        if (gradient.fx == null)
            gradient.fx = grRef.fx;
        if (gradient.fy == null)
            gradient.fy = grRef.fy;
        if (gradient.fr == null)
            gradient.fr = grRef.fr;
    }


    private void setSolidColor(boolean isFill, SolidColor ref) {
        // Make a Style object that has fill or stroke color values set depending on the value of isFill.
        if (isFill) {
            if (isSpecified(ref.baseStyle, Style.SPECIFIED_SOLID_COLOR)) {
                state.style.fill = ref.baseStyle.solidColor;
                state.hasFill = (ref.baseStyle.solidColor != null);
            }

            if (isSpecified(ref.baseStyle, Style.SPECIFIED_SOLID_OPACITY)) {
                state.style.fillOpacity = ref.baseStyle.solidOpacity;
            }

            // If either fill or its opacity has changed, update the fillPaint
            if (isSpecified(ref.baseStyle, Style.SPECIFIED_SOLID_COLOR | Style.SPECIFIED_SOLID_OPACITY)) {
                //noinspection ConstantConditions
                setPaintColour(state, isFill, state.style.fill);
            }
        } else {
            if (isSpecified(ref.baseStyle, Style.SPECIFIED_SOLID_COLOR)) {
                state.style.stroke = ref.baseStyle.solidColor;
                state.hasStroke = (ref.baseStyle.solidColor != null);
            }

            if (isSpecified(ref.baseStyle, Style.SPECIFIED_SOLID_OPACITY)) {
                state.style.strokeOpacity = ref.baseStyle.solidOpacity;
            }

            // If either fill or its opacity has changed, update the fillPaint
            if (isSpecified(ref.baseStyle, Style.SPECIFIED_SOLID_COLOR | Style.SPECIFIED_SOLID_OPACITY)) {
                //noinspection ConstantConditions
                setPaintColour(state, isFill, state.style.stroke);
            }
        }

    }


    //==============================================================================
    // Clip paths
    //==============================================================================


    private void checkForClipPath(SvgElement obj) {
        checkForClipPath(obj, obj.boundingBox);
    }


    private void checkForClipPath(SvgElement obj, Box boundingBox) {
        if (state.style.clipPath == null)
            return;

        // KitKat introduced Path.Op which allows us to do boolean operations on Paths
        Path combinedPath = calculateClipPath(obj, boundingBox);
        if (combinedPath != null)
            canvas.clipPath(combinedPath);
    }


    //-----------------------------------------------------------------------------------------------
    // New-style clippath handling (KitKat onwards).
    // Used Path.op(Path, Path.Op) methods.
    //

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Path calculateClipPath(SvgElement obj, Box boundingBox) {
        // Locate the referenced object
        SvgObject ref = obj.document.resolveIRI(state.style.clipPath);
        if (ref == null) {
            error("ClipPath reference '%s' not found", state.style.clipPath);
            return null;
        }
        // https://drafts.fxtf.org/css-masking-1/#the-clip-path
        // If the URI reference is not valid (e.g it points to an object that doesn’t
        // exist or the object is not a clipPath element), no clipping is applied.
        if (ref.getNodeName() != ClipPath.NODE_NAME)
            return null;

        ClipPath clipPath = (ClipPath) ref;

        // Save style state
        stateStack.push(state);

        // "Properties inherit into the <clipPath> element from its ancestors; properties do not
        // inherit from the element referencing the <clipPath> element." (sect 14.3.5)
        state = findInheritFromAncestorState(clipPath);

        boolean userUnits = (clipPath.clipPathUnitsAreUser == null || clipPath.clipPathUnitsAreUser);
        Matrix m = new Matrix();
        if (!userUnits) {
            m.preTranslate(boundingBox.minX, boundingBox.minY);
            m.preScale(boundingBox.width, boundingBox.height);
        }
        if (clipPath.transform != null) {
            m.preConcat(clipPath.transform);
        }

        Path combinedPath = new Path();
        for (SvgObject child : clipPath.children) {
            if (!(child instanceof SvgElement))
                continue;
            Path part = objectToPath((SvgElement) child, true);
            if (part != null)
                combinedPath.op(part, Path.Op.UNION);
        }

        // Does the clippath also have a clippath?
        if (state.style.clipPath != null) {
            if (clipPath.boundingBox == null)
                clipPath.boundingBox = calculatePathBounds(combinedPath);
            Path clipClipPath = calculateClipPath(clipPath, clipPath.boundingBox);
            if (clipClipPath != null)
                combinedPath.op(clipClipPath, Path.Op.INTERSECT);
        }

        combinedPath.transform(m);

        // Restore style state
        state = stateStack.pop();

        return combinedPath;
    }


    /*
     * Convert the clipPath child element to a path. Transformed if need be, and clipped also if it has its own clippath.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Path objectToPath(SvgElement obj, boolean allowUse) {
        // Save style state
        stateStack.push(state);
        state = new RendererState(state);

        updateStyleForElement(state, obj);

        if (!display() || !visible()) {
            state = stateStack.pop();
            return null;
        }

        Path path = null;

        if (obj instanceof Use) {
            if (!allowUse) {
                error("<use> elements inside a <clipPath> cannot reference another <use>");
            }

            // Locate the referenced object
            Use useElement = (Use) obj;
            SvgObject ref = obj.document.resolveIRI(useElement.href);
            if (ref == null) {
                error("Use reference '%s' not found", useElement.href);
                state = stateStack.pop();
                return null;
            }
            if (!(ref instanceof SvgElement)) {
                state = stateStack.pop();
                return null;
            }

            path = objectToPath((SvgElement) ref, false);
            if (path == null)
                return null;

            if (useElement.boundingBox == null) {
                useElement.boundingBox = calculatePathBounds(path);
            }

            if (useElement.transform != null)
                path.transform(useElement.transform);
        } else if (obj instanceof GraphicsElement) {
            GraphicsElement elem = (GraphicsElement) obj;

            if (obj instanceof SVGBase.Path) {
                SVGBase.Path pathElem = (SVGBase.Path) obj;
                path = (new PathConverter(pathElem.d)).getPath();
                if (obj.boundingBox == null)
                    obj.boundingBox = calculatePathBounds(path);
            } else if (obj instanceof Rect)
                path = makePathAndBoundingBox((Rect) obj);
            else if (obj instanceof Circle)
                path = makePathAndBoundingBox((Circle) obj);
            else if (obj instanceof Ellipse)
                path = makePathAndBoundingBox((Ellipse) obj);
            else if (obj instanceof PolyLine)
                path = makePathAndBoundingBox((PolyLine) obj);

            if (path == null)
                return null;

            if (elem.boundingBox == null) {
                elem.boundingBox = calculatePathBounds(path);
            }

            if (elem.transform != null)
                path.transform(elem.transform);

            path.setFillType(getClipRuleFromState());
        } else if (obj instanceof Text) {
            Text textElem = (Text) obj;
            path = makePathAndBoundingBox(textElem);

            if (textElem.transform != null)
                path.transform(textElem.transform);

            path.setFillType(getClipRuleFromState());
        } else {
            error("Invalid %s element found in clipPath definition", obj.getNodeName());
            return null;
        }

        // Does the clippath child element also have a clippath?
        if (state.style.clipPath != null) {
            Path childsClipPath = calculateClipPath(obj, obj.boundingBox);
            if (childsClipPath != null)
                path.op(childsClipPath, Path.Op.INTERSECT);
        }

        // Restore style state
        state = stateStack.pop();

        return path;
    }


    //-----------------------------------------------------------------------------------------------
    // Old-style clippath handling.
    // Pre-KitKat. Kept for backwards compatibility.


    private void checkForClipPath_OldStyle(SvgElement obj, Box boundingBox) {
        // Use the old/original method for clip paths

        // Locate the referenced object
        SvgObject ref = obj.document.resolveIRI(state.style.clipPath);
        if (ref == null) {
            error("ClipPath reference '%s' not found", state.style.clipPath);
            return;
        }
        // https://drafts.fxtf.org/css-masking-1/#the-clip-path
        // If the URI reference is not valid (e.g it points to an object that doesn’t
        // exist or the object is not a clipPath element), no clipping is applied.
        if (ref.getNodeName() != ClipPath.NODE_NAME)
            return;

        ClipPath clipPath = (ClipPath) ref;

        // An empty clipping path will completely clip away the element (sect 14.3.5).
        if (clipPath.children.isEmpty()) {
            canvas.clipRect(0, 0, 0, 0);
            return;
        }

        boolean userUnits = (clipPath.clipPathUnitsAreUser == null || clipPath.clipPathUnitsAreUser);

        if ((obj instanceof Group) && !userUnits) {
            warn("<clipPath clipPathUnits=\"objectBoundingBox\"> is not supported when referenced from container elements (like %s)", obj.getNodeName());
            return;
        }

        clipStatePush();

        if (!userUnits) {
            Matrix m = new Matrix();
            m.preTranslate(boundingBox.minX, boundingBox.minY);
            m.preScale(boundingBox.width, boundingBox.height);
            canvas.concat(m);
        }
        if (clipPath.transform != null) {
            canvas.concat(clipPath.transform);
        }

        // "Properties inherit into the <clipPath> element from its ancestors; properties do not
        // inherit from the element referencing the <clipPath> element." (sect 14.3.5)
        state = findInheritFromAncestorState(clipPath);

        checkForClipPath(clipPath);

        Path combinedPath = new Path();
        for (SvgObject child : clipPath.children) {
            addObjectToClip(child, true, combinedPath, new Matrix());
        }
        canvas.clipPath(combinedPath);

        clipStatePop();
    }


    private void addObjectToClip(SvgObject obj, boolean allowUse, Path combinedPath, Matrix combinedPathMatrix) {
        if (!display())
            return;

        // Save state
        clipStatePush();

        if (obj instanceof Use) {
            if (allowUse) {
                addObjectToClip((Use) obj, combinedPath, combinedPathMatrix);
            } else {
                error("<use> elements inside a <clipPath> cannot reference another <use>");
            }
        } else if (obj instanceof SVGBase.Path) {
            addObjectToClip((SVGBase.Path) obj, combinedPath, combinedPathMatrix);
        } else if (obj instanceof Text) {
            addObjectToClip((Text) obj, combinedPath, combinedPathMatrix);
        } else if (obj instanceof GraphicsElement) {
            addObjectToClip((GraphicsElement) obj, combinedPath, combinedPathMatrix);
        } else {
            error("Invalid %s element found in clipPath definition", obj.toString());
        }

        // Restore state
        clipStatePop();
    }


    // The clip state push and pop methods only save the matrix.
    // The normal push/pop save the clip region also which would
    // destroy the clip region we are trying to build.
    private void clipStatePush() {
        // Save matrix but not clip
        CanvasLegacy.save(canvas, CanvasLegacy.MATRIX_SAVE_FLAG);
        // Save style state
        stateStack.push(state);
        state = new RendererState(state);
    }


    private void clipStatePop() {
        // Restore matrix and clip
        canvas.restore();
        // Restore style state
        state = stateStack.pop();
    }


    private Path.FillType getClipRuleFromState() {
        if (state.style.clipRule != null && state.style.clipRule == Style.FillRule.EvenOdd)
            return Path.FillType.EVEN_ODD;
        else
            return Path.FillType.WINDING;
    }


    private void addObjectToClip(SVGBase.Path obj, Path combinedPath, Matrix combinedPathMatrix) {
        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null)
            combinedPathMatrix.preConcat(obj.transform);

        Path path = (new PathConverter(obj.d)).getPath();

        if (obj.boundingBox == null) {
            obj.boundingBox = calculatePathBounds(path);
        }
        checkForClipPath(obj);

        //path.setFillType(getClipRuleFromState());
        combinedPath.setFillType(getClipRuleFromState());
        combinedPath.addPath(path, combinedPathMatrix);
    }


    private void addObjectToClip(GraphicsElement obj, Path combinedPath, Matrix combinedPathMatrix) {
        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null)
            combinedPathMatrix.preConcat(obj.transform);

        Path path;
        if (obj instanceof Rect)
            path = makePathAndBoundingBox((Rect) obj);
        else if (obj instanceof Circle)
            path = makePathAndBoundingBox((Circle) obj);
        else if (obj instanceof Ellipse)
            path = makePathAndBoundingBox((Ellipse) obj);
        else if (obj instanceof PolyLine)
            path = makePathAndBoundingBox((PolyLine) obj);
        else
            return;

        if (path == null)  // For safety, or object has an error
            return;

        checkForClipPath(obj);

        combinedPath.setFillType(getClipRuleFromState());
        combinedPath.addPath(path, combinedPathMatrix);
    }


    private void addObjectToClip(Use obj, Path combinedPath, Matrix combinedPathMatrix) {
        updateStyleForElement(state, obj);

        if (!display())
            return;
        if (!visible())
            return;

        if (obj.transform != null)
            combinedPathMatrix.preConcat(obj.transform);

        // Locate the referenced object
        SvgObject ref = obj.document.resolveIRI(obj.href);
        if (ref == null) {
            error("Use reference '%s' not found", obj.href);
            return;
        }

        checkForClipPath(obj);

        addObjectToClip(ref, false, combinedPath, combinedPathMatrix);
    }


    private void addObjectToClip(Text obj, Path combinedPath, Matrix combinedPathMatrix) {
        updateStyleForElement(state, obj);

        if (!display())
            return;

        if (obj.transform != null)
            combinedPathMatrix.preConcat(obj.transform);

        // Get the first coordinate pair from the lists in the x and y properties.
        float x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
        float y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
        float dx = (obj.dx == null || obj.dx.size() == 0) ? 0f : obj.dx.get(0).floatValueX(this);
        float dy = (obj.dy == null || obj.dy.size() == 0) ? 0f : obj.dy.get(0).floatValueY(this);

        // Handle text alignment
        if (state.style.textAnchor != Style.TextAnchor.Start) {
            float textWidth = calculateTextWidth(obj);
            if (state.style.textAnchor == Style.TextAnchor.Middle) {
                x -= (textWidth / 2);
            } else {
                x -= textWidth;  // 'End' (right justify)
            }
        }

        if (obj.boundingBox == null) {
            TextBoundsCalculator proc = new TextBoundsCalculator(x, y);
            enumerateTextSpans(obj, proc);
            obj.boundingBox = new Box(proc.bbox.left, proc.bbox.top, proc.bbox.width(), proc.bbox.height());
        }
        checkForClipPath(obj);

        Path textAsPath = new Path();
        enumerateTextSpans(obj, new PlainTextToPath(x + dx, y + dy, textAsPath));

        combinedPath.setFillType(getClipRuleFromState());
        combinedPath.addPath(textAsPath, combinedPathMatrix);
    }


    //-----------------------------------------------------------------------------------------------


    private class PlainTextToPath extends TextProcessor {
        float x;
        float y;
        final Path textAsPath;

        PlainTextToPath(float x, float y, Path textAsPath) {
            this.x = x;
            this.y = y;
            this.textAsPath = textAsPath;
        }

        @Override
        public boolean doTextContainer(TextContainer obj) {
            if (obj instanceof TextPath) {
                warn("Using <textPath> elements in a clip path is not supported.");
                return false;
            }
            return true;
        }

        @Override
        public void processText(String text) {
            if (visible()) {
                //state.fillPaint.getTextPath(text, 0, text.length(), x, y, textAsPath);
                Path spanPath = new Path();
                state.fillPaint.getTextPath(text, 0, text.length(), x, y, spanPath);
                textAsPath.addPath(spanPath);
            }

            // Update the current text position
            x += measureText(text, state.fillPaint);
        }
    }


    //==============================================================================
    // Convert the different shapes to paths
    //==============================================================================


    private Path makePathAndBoundingBox(Line obj) {
        float x1 = (obj.x1 == null) ? 0 : obj.x1.floatValueX(this);
        float y1 = (obj.y1 == null) ? 0 : obj.y1.floatValueY(this);
        float x2 = (obj.x2 == null) ? 0 : obj.x2.floatValueX(this);
        float y2 = (obj.y2 == null) ? 0 : obj.y2.floatValueY(this);

        if (obj.boundingBox == null) {
            obj.boundingBox = new Box(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
        }

        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return p;
    }


    private Path makePathAndBoundingBox(Rect obj) {
        float x, y, w, h, rx, ry;

        if (obj.rx == null && obj.ry == null) {
            rx = 0;
            ry = 0;
        } else if (obj.rx == null) {
            rx = ry = obj.ry.floatValueY(this);
        } else if (obj.ry == null) {
            rx = ry = obj.rx.floatValueX(this);
        } else {
            rx = obj.rx.floatValueX(this);
            ry = obj.ry.floatValueY(this);
        }
        rx = Math.min(rx, obj.width.floatValueX(this) / 2f);
        ry = Math.min(ry, obj.height.floatValueY(this) / 2f);
        x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
        y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
        w = obj.width.floatValueX(this);
        h = obj.height.floatValueY(this);

        if (obj.boundingBox == null) {
            obj.boundingBox = new Box(x, y, w, h);
        }

        float right = x + w;
        float bottom = y + h;

        Path p = new Path();
        if (rx == 0 || ry == 0) {
            // Simple rect
            p.moveTo(x, y);
            p.lineTo(right, y);
            p.lineTo(right, bottom);
            p.lineTo(x, bottom);
            p.lineTo(x, y);
        } else {
            // Rounded rect

            // Bezier control point lengths for a 90 degree arc
            float cpx = rx * BEZIER_ARC_FACTOR;
            float cpy = ry * BEZIER_ARC_FACTOR;

            p.moveTo(x, y + ry);
            p.cubicTo(x, y + ry - cpy, x + rx - cpx, y, x + rx, y);
            p.lineTo(right - rx, y);
            p.cubicTo(right - rx + cpx, y, right, y + ry - cpy, right, y + ry);
            p.lineTo(right, bottom - ry);
            p.cubicTo(right, bottom - ry + cpy, right - rx + cpx, bottom, right - rx, bottom);
            p.lineTo(x + rx, bottom);
            p.cubicTo(x + rx - cpx, bottom, x, bottom - ry + cpy, x, bottom - ry);
            p.lineTo(x, y + ry);
        }
        p.close();
        return p;
    }


    private Path makePathAndBoundingBox(Circle obj) {
        float cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
        float cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
        float r = obj.r.floatValue(this);

        float left = cx - r;
        float top = cy - r;
        float right = cx + r;
        float bottom = cy + r;

        if (obj.boundingBox == null) {
            obj.boundingBox = new Box(left, top, r * 2, r * 2);
        }

        float cp = r * BEZIER_ARC_FACTOR;

        Path p = new Path();
        p.moveTo(cx, top);
        p.cubicTo(cx + cp, top, right, cy - cp, right, cy);
        p.cubicTo(right, cy + cp, cx + cp, bottom, cx, bottom);
        p.cubicTo(cx - cp, bottom, left, cy + cp, left, cy);
        p.cubicTo(left, cy - cp, cx - cp, top, cx, top);
        p.close();
        return p;
    }


    private Path makePathAndBoundingBox(Ellipse obj) {
        float cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
        float cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
        float rx = obj.rx.floatValueX(this);
        float ry = obj.ry.floatValueY(this);

        float left = cx - rx;
        float top = cy - ry;
        float right = cx + rx;
        float bottom = cy + ry;

        if (obj.boundingBox == null) {
            obj.boundingBox = new Box(left, top, rx * 2, ry * 2);
        }

        float cpx = rx * BEZIER_ARC_FACTOR;
        float cpy = ry * BEZIER_ARC_FACTOR;

        Path p = new Path();
        p.moveTo(cx, top);
        p.cubicTo(cx + cpx, top, right, cy - cpy, right, cy);
        p.cubicTo(right, cy + cpy, cx + cpx, bottom, cx, bottom);
        p.cubicTo(cx - cpx, bottom, left, cy + cpy, left, cy);
        p.cubicTo(left, cy - cpy, cx - cpx, top, cx, top);
        p.close();
        return p;
    }


    private Path makePathAndBoundingBox(PolyLine obj) {
        Path path = new Path();

        int numPoints = (obj.points != null) ? obj.points.length : 0;
        // Odd number of points is an error
        if (numPoints % 2 != 0)
            return null;

        if (numPoints > 0) {
            int i = 0;
            while (numPoints >= 2) {
                if (i == 0)
                    path.moveTo(obj.points[i], obj.points[i + 1]);
                else
                    path.lineTo(obj.points[i], obj.points[i + 1]);
                i += 2;
                numPoints -= 2;
            }
            if (obj instanceof Polygon)
                path.close();
        }

        if (obj.boundingBox == null) {
            obj.boundingBox = calculatePathBounds(path);
        }
        return path;
    }


    private Path makePathAndBoundingBox(Text obj) {
        // Get the first coordinate pair from the lists in the x and y properties.
        float x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
        float y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
        float dx = (obj.dx == null || obj.dx.size() == 0) ? 0f : obj.dx.get(0).floatValueX(this);
        float dy = (obj.dy == null || obj.dy.size() == 0) ? 0f : obj.dy.get(0).floatValueY(this);

        // Handle text alignment
        if (state.style.textAnchor != Style.TextAnchor.Start) {
            float textWidth = calculateTextWidth(obj);
            if (state.style.textAnchor == Style.TextAnchor.Middle) {
                x -= (textWidth / 2);
            } else {
                x -= textWidth;  // 'End' (right justify)
            }
        }

        if (obj.boundingBox == null) {
            TextBoundsCalculator proc = new TextBoundsCalculator(x, y);
            enumerateTextSpans(obj, proc);
            obj.boundingBox = new Box(proc.bbox.left, proc.bbox.top, proc.bbox.width(), proc.bbox.height());
        }

        Path textAsPath = new Path();
        enumerateTextSpans(obj, new PlainTextToPath(x + dx, y + dy, textAsPath));

        return textAsPath;
    }


    //==============================================================================
    // Pattern fills
    //==============================================================================


    /*
     * Fill a path with a pattern by setting the path as a clip path and
     * drawing the pattern element as a repeating tile inside it.
     */
    private void fillWithPattern(SvgElement obj, Path path, Pattern pattern) {
        boolean patternUnitsAreUser = (pattern.patternUnitsAreUser != null && pattern.patternUnitsAreUser);
        float x, y, w, h;
        float originX, originY;
        float objFillOpacity = state.style.fillOpacity;

        if (pattern.href != null)
            fillInChainedPatternFields(pattern, pattern.href);

        if (patternUnitsAreUser) {
            x = (pattern.x != null) ? pattern.x.floatValueX(this) : 0f;
            y = (pattern.y != null) ? pattern.y.floatValueY(this) : 0f;
            w = (pattern.width != null) ? pattern.width.floatValueX(this) : 0f;
            h = (pattern.height != null) ? pattern.height.floatValueY(this) : 0f;
        } else {
            // Convert objectBoundingBox space to user space
            x = (pattern.x != null) ? pattern.x.floatValue(this, 1f) : 0f;
            y = (pattern.y != null) ? pattern.y.floatValue(this, 1f) : 0f;
            w = (pattern.width != null) ? pattern.width.floatValue(this, 1f) : 0f;
            h = (pattern.height != null) ? pattern.height.floatValue(this, 1f) : 0f;
            x = obj.boundingBox.minX + x * obj.boundingBox.width;
            y = obj.boundingBox.minY + y * obj.boundingBox.height;
            w *= obj.boundingBox.width;
            h *= obj.boundingBox.height;
        }
        if (w == 0 || h == 0)
            return;

        // "If attribute 'preserveAspectRatio' is not specified, then the effect is as if a value of xMidYMid meet were specified."
        PreserveAspectRatio positioning = (pattern.preserveAspectRatio != null) ? pattern.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

        // Push the state
        statePush();
        // Set path as the clip region
        canvas.clipPath(path);

        // Set the style for the pattern (inherits from its own ancestors, not from callee's state)
        RendererState baseState = new RendererState();
        updateStyle(baseState, Style.getDefaultStyle());
        baseState.style.overflow = false;    // By default patterns do not overflow

        // SVG2 TODO: Patterns now inherit from the element referencing the pattern
        state = findInheritFromAncestorState(pattern, baseState);

        // The bounds of the area we need to cover with pattern to ensure that our shape is filled
        Box patternArea = obj.boundingBox;
        // Apply the patternTransform
        if (pattern.patternTransform != null) {
            canvas.concat(pattern.patternTransform);

            // A pattern transform will affect the area we need to cover with the pattern.
            // So we need to alter the area bounding rectangle.
            Matrix inverse = new Matrix();
            if (pattern.patternTransform.invert(inverse)) {
                float[] pts = {obj.boundingBox.minX, obj.boundingBox.minY,
                        obj.boundingBox.maxX(), obj.boundingBox.minY,
                        obj.boundingBox.maxX(), obj.boundingBox.maxY(),
                        obj.boundingBox.minX, obj.boundingBox.maxY()};
                inverse.mapPoints(pts);
                // Find the bounding box of the shape created by the inverse transform
                RectF rect = new RectF(pts[0], pts[1], pts[0], pts[1]);
                for (int i = 2; i <= 6; i += 2) {
                    if (pts[i] < rect.left) rect.left = pts[i];
                    if (pts[i] > rect.right) rect.right = pts[i];
                    if (pts[i + 1] < rect.top) rect.top = pts[i + 1];
                    if (pts[i + 1] > rect.bottom) rect.bottom = pts[i + 1];
                }
                patternArea = new Box(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
            }
        }

        // Calculate the pattern origin
        originX = x + (float) Math.floor((patternArea.minX - x) / w) * w;
        originY = y + (float) Math.floor((patternArea.minY - y) / h) * h;

        // For each Y step, then each X step
        float right = patternArea.maxX();
        float bottom = patternArea.maxY();
        Box stepViewBox = new Box(0, 0, w, h);

        boolean compositing = pushLayer(objFillOpacity);

        for (float stepY = originY; stepY < bottom; stepY += h) {
            for (float stepX = originX; stepX < right; stepX += w) {
                stepViewBox.minX = stepX;
                stepViewBox.minY = stepY;

                // Push the state
                statePush();

                // Set pattern clip rectangle if appropriate
                if (!state.style.overflow) {
                    setClipRect(stepViewBox.minX, stepViewBox.minY, stepViewBox.width, stepViewBox.height);
                }
                // Calculate and set the viewport for each instance of the pattern
                if (pattern.viewBox != null) {
                    canvas.concat(calculateViewBoxTransform(stepViewBox, pattern.viewBox, positioning));
                } else {
                    boolean patternContentUnitsAreUser = (pattern.patternContentUnitsAreUser == null || pattern.patternContentUnitsAreUser);
                    // Simple translate of pattern to step position
                    canvas.translate(stepX, stepY);
                    if (!patternContentUnitsAreUser) {
                        canvas.scale(obj.boundingBox.width, obj.boundingBox.height);
                    }
                }


                // Render the pattern
                for (SvgObject child : pattern.children) {
                    render(child);
                }

                // Pop the state
                statePop();
            }
        }

        if (compositing)
            popLayer(pattern);

        // Pop the state
        statePop();
    }


    /*
     * Any unspecified fields in this pattern can be 'borrowed' from another
     * pattern specified by the href attribute.
     */
    private void fillInChainedPatternFields(Pattern pattern, String href) {
        // Locate the referenced object
        SvgObject ref = pattern.document.resolveIRI(href);
        if (ref == null) {
            // Non-existent
            warn("Pattern reference '%s' not found", href);
            return;
        }
        if (!(ref instanceof Pattern)) {
            error("Pattern href attributes must point to other pattern elements");
            return;
        }
        if (ref == pattern) {
            error("Circular reference in pattern href attribute '%s'", href);
            return;
        }

        Pattern pRef = (Pattern) ref;

        if (pattern.patternUnitsAreUser == null)
            pattern.patternUnitsAreUser = pRef.patternUnitsAreUser;
        if (pattern.patternContentUnitsAreUser == null)
            pattern.patternContentUnitsAreUser = pRef.patternContentUnitsAreUser;
        if (pattern.patternTransform == null)
            pattern.patternTransform = pRef.patternTransform;
        if (pattern.x == null)
            pattern.x = pRef.x;
        if (pattern.y == null)
            pattern.y = pRef.y;
        if (pattern.width == null)
            pattern.width = pRef.width;
        if (pattern.height == null)
            pattern.height = pRef.height;
        // attributes from superclasses
        if (pattern.children.isEmpty())
            pattern.children = pRef.children;
        if (pattern.viewBox == null)
            pattern.viewBox = pRef.viewBox;
        if (pattern.preserveAspectRatio == null) {
            pattern.preserveAspectRatio = pRef.preserveAspectRatio;
        }

        if (pRef.href != null)
            fillInChainedPatternFields(pattern, pRef.href);
    }


    //==============================================================================
    // Masks
    //==============================================================================


    /*
     * Render the contents of a mask element.
     */
    private void renderMask(Mask mask, SvgElement obj, Box originalObjBBox) {
        debug("Mask render");

        boolean maskUnitsAreUser = (mask.maskUnitsAreUser != null && mask.maskUnitsAreUser);
        float w, h;

        if (maskUnitsAreUser) {
            w = (mask.width != null) ? mask.width.floatValueX(this) : originalObjBBox.width;
            h = (mask.height != null) ? mask.height.floatValueY(this) : originalObjBBox.height;
            //x = (mask.x != null) ? mask.x.floatValueX(this): (float)(obj.boundingBox.minX - 0.1 * obj.boundingBox.width);
            //y = (mask.y != null) ? mask.y.floatValueY(this): (float)(obj.boundingBox.minY - 0.1 * obj.boundingBox.height);
        } else {
            // Convert objectBoundingBox space to user space
            //x = (mask.x != null) ? mask.x.floatValue(this, 1f): -0.1f;
            //y = (mask.y != null) ? mask.y.floatValue(this, 1f): -0.1f;
            w = (mask.width != null) ? mask.width.floatValue(this, 1f) : 1.2f;
            h = (mask.height != null) ? mask.height.floatValue(this, 1f) : 1.2f;
            //x = originalObjBBox.minX + x * originalObjBBox.width;
            //y = originalObjBBox.minY + y * originalObjBBox.height;
            w *= originalObjBBox.width;
            h *= originalObjBBox.height;
        }
        if (w == 0 || h == 0)
            return;

        // Push the state
        statePush();

        state = findInheritFromAncestorState(mask);
        // Set the style for the mask (inherits from its own ancestors, not from callee's state)
        // The 'opacity', 'filter' and 'display' properties do not apply to the 'mask' element" (sect 14.4)
        state.style.opacity = 1f;
        //state.style.filter = null;

        boolean compositing = pushLayer();

        // Save the current transform matrix, as we need to undo the following transform straight away
        canvas.save();

        boolean maskContentUnitsAreUser = (mask.maskContentUnitsAreUser == null || mask.maskContentUnitsAreUser);
        if (!maskContentUnitsAreUser) {
            canvas.translate(originalObjBBox.minX, originalObjBBox.minY);
            canvas.scale(originalObjBBox.width, originalObjBBox.height);
        }

        // Render the mask
        renderChildren(mask, false);

        // Restore the matrix so that, if this mask has a mask, it is not affected by the objectBoundingBox transform
        canvas.restore();

        if (compositing)
            popLayer(obj, originalObjBBox);

        // Pop the state
        statePop();
    }


}
