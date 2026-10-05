package com.gecalc;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class GECalcEvaluatorTest
{
    private static final BigDecimal LEGACY_MAX = GECalcEvaluator.LEGACY_MAX;
    private static final BigDecimal GE_MAX = GECalcEvaluator.GE_MAX;

    @Test
    public void testPlainNumbers()
    {
        assertEquals(BigDecimal.valueOf(100), GECalcEvaluator.evaluate("100", GE_MAX));
        assertEquals(BigDecimal.valueOf(1), GECalcEvaluator.evaluate("1", GE_MAX));
        assertEquals(BigDecimal.valueOf(5000), GECalcEvaluator.evaluate("5000", GE_MAX));
    }

    @Test
    public void testUnits()
    {
        assertEquals(BigDecimal.valueOf(1_000), GECalcEvaluator.evaluate("1k", GE_MAX));
        assertEquals(BigDecimal.valueOf(1_500), GECalcEvaluator.evaluate("1.5k", GE_MAX));
        assertEquals(BigDecimal.valueOf(10_000_000), GECalcEvaluator.evaluate("10m", GE_MAX));
        assertEquals(BigDecimal.valueOf(2_500_000_000L), GECalcEvaluator.evaluate("2.5b", GE_MAX));
        assertEquals(BigDecimal.valueOf(1_000_000_000_000L), GECalcEvaluator.evaluate("1t", GE_MAX));
        assertEquals(BigDecimal.valueOf(500_000), GECalcEvaluator.evaluate(".5m", GE_MAX));
    }

    @Test
    public void testBasicArithmetic()
    {
        assertEquals(BigDecimal.valueOf(200), GECalcEvaluator.evaluate("100 + 100", GE_MAX));
        assertEquals(BigDecimal.valueOf(50), GECalcEvaluator.evaluate("100 - 50", GE_MAX));
        assertEquals(BigDecimal.valueOf(180), GECalcEvaluator.evaluate("45 * 4", GE_MAX));
        assertEquals(BigDecimal.valueOf(45), GECalcEvaluator.evaluate("180 / 4", GE_MAX));
    }

    @Test
    public void testOperatorPrecedence()
    {
        // 2 + (3 * 4) = 14, not (2 + 3) * 4 = 20
        assertEquals(BigDecimal.valueOf(14), GECalcEvaluator.evaluate("2 + 3 * 4", GE_MAX));
        // (10 * 50000) + 10000 = 510000
        assertEquals(BigDecimal.valueOf(510_000), GECalcEvaluator.evaluate("10 * 50k + 10k", GE_MAX));
    }

    @Test
    public void testChainedExpressions()
    {
        assertEquals(BigDecimal.valueOf(60_000), GECalcEvaluator.evaluate("10k + 20k + 30k", GE_MAX));
        assertEquals(BigDecimal.valueOf(30), GECalcEvaluator.evaluate("100 - 50 - 20", GE_MAX));
        assertEquals(BigDecimal.valueOf(5), GECalcEvaluator.evaluate("100 / 4 / 5", GE_MAX));
    }

    @Test
    public void testParentheses()
    {
        assertEquals(BigDecimal.valueOf(20), GECalcEvaluator.evaluate("(2 + 3) * 4", GE_MAX));
        assertEquals(BigDecimal.valueOf(120_000), GECalcEvaluator.evaluate("(100 + 20) * 1k", GE_MAX));
        assertEquals(BigDecimal.valueOf(50_000), GECalcEvaluator.evaluate("100k / (1 + 1)", GE_MAX));
    }

    @Test
    public void testPercentages()
    {
        // 10m - 1% tax = 9,900,000
        assertEquals(BigDecimal.valueOf(9_900_000), GECalcEvaluator.evaluate("10m - 1%", GE_MAX));
        // 100k + 10% = 110,000
        assertEquals(BigDecimal.valueOf(110_000), GECalcEvaluator.evaluate("100k + 10%", GE_MAX));
        // 500k * 2% = 10,000
        assertEquals(BigDecimal.valueOf(10_000), GECalcEvaluator.evaluate("500k * 2%", GE_MAX));
    }

    @Test
    public void testDivisionRoundingCeiling()
    {
        // 10 / 3 = 3.333... -> ceil to 4
        assertEquals(BigDecimal.valueOf(4), GECalcEvaluator.evaluate("10 / 3", GE_MAX));
        // 100 / 6 = 16.666... -> ceil to 17
        assertEquals(BigDecimal.valueOf(17), GECalcEvaluator.evaluate("100 / 6", GE_MAX));
    }

    @Test
    public void testCommasAndFormatting()
    {
        assertEquals(BigDecimal.valueOf(1_500_000), GECalcEvaluator.evaluate("1,000,000 + 500,000", GE_MAX));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("1,5k", GE_MAX));
        assertEquals(BigDecimal.valueOf(50_000), GECalcEvaluator.evaluate(" 10k   *  5 ", GE_MAX));
    }

    @Test
    public void testExpressionValidation()
    {
        org.junit.Assert.assertTrue(GECalcEvaluator.isValid("100 + 10%"));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid("100 + "));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid("100 / 0"));
    }

    @Test
    public void testClamping()
    {
        // Clamp to legacy max (2,147,483,647)
        assertEquals(LEGACY_MAX, GECalcEvaluator.evaluate("3b", LEGACY_MAX));
        // In 64-bit mode, 3b fits fine
        assertEquals(BigDecimal.valueOf(3_000_000_000L), GECalcEvaluator.evaluate("3b", GE_MAX));
        // Clamp to GE max (2,149,631,130,647)
        assertEquals(GE_MAX, GECalcEvaluator.evaluate("3t", GE_MAX));
        // Negative result clamps to 1
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("50 - 100", GE_MAX));
        // Zero clamps to 1
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("0", GE_MAX));
    }

    @Test
    public void testErrorHandling()
    {
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("100 / 0", GE_MAX));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("abc", GE_MAX));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("10k + ", GE_MAX));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("", GE_MAX));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate(null, GE_MAX));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate("10kk", GE_MAX));
    }

    /**
     * evaluate() and isValid() must share one sanitisation path, otherwise an
     * expression could validate and then evaluate to something else entirely.
     *
     * <p>Each case pins an exact expected value. Asserting merely that a valid input
     * is not {@code ONE} would be wrong, since many legitimate expressions really do
     * evaluate to 1.
     */
    @Test
    public void testValidateAgreesWithEvaluate()
    {
        Object[][] cases = {
                // input, expected value, isValid
                {"100", BigDecimal.valueOf(100), true},
                {"1.5k", BigDecimal.valueOf(1_500), true},
                {" 10k * 5 ", BigDecimal.valueOf(50_000), true},
                {"1,000,000 + 500,000", BigDecimal.valueOf(1_500_000), true},
                {"100 + 10%", BigDecimal.valueOf(110), true},
                {"10m - 1%", BigDecimal.valueOf(9_900_000), true},
                {"((((1))))", BigDecimal.ONE, true},
                {"5.63k", BigDecimal.valueOf(5_630), true},
                {"13.8k * 11.3k", BigDecimal.valueOf(155_940_000L), true},
                {"1,5k", BigDecimal.ONE, false},
                {"100 +", BigDecimal.ONE, false},
                {"100 / 0", BigDecimal.ONE, false},
                {"abc", BigDecimal.ONE, false},
                {"", BigDecimal.ONE, false},
                {"   ", BigDecimal.ONE, false},
                {"10kk", BigDecimal.ONE, false},
                {"(1 + 2", BigDecimal.ONE, false},
        };

        for (Object[] c : cases)
        {
            String input = (String) c[0];
            BigDecimal expected = (BigDecimal) c[1];
            boolean valid = (Boolean) c[2];

            assertEquals("isValid mismatch for: " + input, valid, GECalcEvaluator.isValid(input));
            assertEquals("evaluate mismatch for: " + input, expected, GECalcEvaluator.evaluate(input, GE_MAX));
        }
    }

    @Test
    public void testValidationEdgeCases()
    {
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid(null));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid(""));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid("   "));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid("(1 + 2"));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid("1)"));
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid("*"));
        org.junit.Assert.assertTrue(GECalcEvaluator.isValid("((((1))))"));
        org.junit.Assert.assertTrue(GECalcEvaluator.isValid("10m - 1%"));
    }

    /**
     * Deeply nested input must be rejected cleanly rather than overflowing the
     * stack (StackOverflowError is an Error and would escape the Exception handlers).
     */
    @Test
    public void testDeeplyNestedParenthesesDoNotOverflow()
    {
        int depth = 5000;
        String input = "(".repeat(depth) + "1" + ")".repeat(depth);
        org.junit.Assert.assertFalse(GECalcEvaluator.isValid(input));
        assertEquals(BigDecimal.ONE, GECalcEvaluator.evaluate(input, GE_MAX));

        // A reasonably nested expression still works.
        assertEquals(BigDecimal.valueOf(6), GECalcEvaluator.evaluate("(((1 + 1) * 3))", GE_MAX));
    }
}
