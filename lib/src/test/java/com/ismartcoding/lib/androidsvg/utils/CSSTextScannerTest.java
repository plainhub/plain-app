package com.ismartcoding.lib.androidsvg.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CSSTextScannerTest
{
   @Test
   public void stripsClosedBlockComments()
   {
      CSSTextScanner scanner = new CSSTextScanner("a/* comment */b/*two*/c");

      assertEquals("abc", scanner.input);
   }


   @Test
   public void stripsMultilineBlockComments()
   {
      CSSTextScanner scanner = new CSSTextScanner("fill:red;/* line1\nline2 */stroke:blue;");

      assertEquals("fill:red;stroke:blue;", scanner.input);
   }


   @Test
   public void leavesUnclosedBlockCommentsUntouched()
   {
      String css = "fill:red;/* unterminated";

      CSSTextScanner scanner = new CSSTextScanner(css);

      assertEquals(css, scanner.input);
   }


   @Test(timeout = 2000L)
   public void handlesRepeatedSlashStarShapeWithoutBacktrackingBlowup()
   {
      String css = "/*" + "a/*".repeat(4000);

      CSSTextScanner scanner = new CSSTextScanner(css);

      assertEquals(css, scanner.input);
   }
}