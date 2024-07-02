package com.ismartcoding.lib.androidsvg.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.lang.reflect.Method;

public class CanvasLegacy {
    public static final int MATRIX_SAVE_FLAG;
    public static final int ALL_SAVE_FLAG;

    private static final Method SAVE;
    private static final Method SAVE_LAYER;

    static {
        try {
            MATRIX_SAVE_FLAG = (int) Canvas.class.getField("MATRIX_SAVE_FLAG").get(null);
            ALL_SAVE_FLAG = (int) Canvas.class.getField("ALL_SAVE_FLAG").get(null);

            SAVE = Canvas.class.getMethod("save", int.class);

            SAVE_LAYER = Canvas.class.getMethod("saveLayer", RectF.class, Paint.class, int.class);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    public static void save(Canvas canvas, int saveFlags) {
        try {
            SAVE.invoke(canvas, saveFlags);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    private static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) throw new NullPointerException("t");
        return CanvasLegacy.sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }

    public static void saveLayer(Canvas canvas, RectF bounds, Paint paint, int saveFlags) {
        try {
            SAVE_LAYER.invoke(canvas, bounds, paint, saveFlags);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

}
