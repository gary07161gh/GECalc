package com.gecalc;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class GECalcEvaluator
{
    public static final BigDecimal LEGACY_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
    public static final BigDecimal GE_MAX = BigDecimal.valueOf(2_149_631_130_647L);

    private static final BigDecimal K_MULTIPLIER = BigDecimal.valueOf(1_000L);
    private static final BigDecimal M_MULTIPLIER = BigDecimal.valueOf(1_000_000L);
    private static final BigDecimal B_MULTIPLIER = BigDecimal.valueOf(1_000_000_000L);
    private static final BigDecimal T_MULTIPLIER = BigDecimal.valueOf(1_000_000_000_000L);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);

    private GECalcEvaluator()
    {
    }

    /**
     * Normalises raw GE input into a form the tokenizer understands.
     *
     * <p>Kept in one place because {@link #evaluate(String, BigDecimal)} and
     * {@link #isValid(String)} must agree exactly; if they disagreed, an
     * expression could pass validation and then be replaced with a bogus value.
     */
    private static String sanitize(String rawInput)
    {
        if (rawInput == null)
        {
            return "";
        }

        return rawInput
                .toLowerCase()
                // Treat commas as thousands separators only when followed by a
                // three-digit group, so decimal-comma input remains numeric.
                .replaceAll("(?<=\\d),(?=\\d{3}(?:\\D|$))", "")
                .replaceAll("\\s+", "")
                .replaceAll("\\.{2,}", ".");
    }

    /**
     * Evaluates a mathematical expression and clamps the result between 1 and {@code max}.
     *
     * @param rawInput The raw input string from the GE input prompt.
     * @param max      The maximum value allowed for the current input mode.
     * @return The calculated and clamped BigDecimal value, or {@link BigDecimal#ONE} on error.
     */
    public static BigDecimal evaluate(String rawInput, BigDecimal max)
    {
        String cleanInput = sanitize(rawInput);
        if (cleanInput.isEmpty())
        {
            return BigDecimal.ONE;
        }

        try
        {
            List<Token> tokens = tokenize(cleanInput);
            if (tokens.isEmpty())
            {
                return BigDecimal.ONE;
            }

            Parser parser = new Parser(tokens);
            BigDecimal result = parser.parse();

            result = result.setScale(0, RoundingMode.CEILING);

            if (result.compareTo(BigDecimal.ONE) < 0)
            {
                return BigDecimal.ONE;
            }

            if (max != null && result.compareTo(max) > 0)
            {
                return max;
            }

            return result;
        }
        catch (Exception e)
        {
            log.debug("GE Calc - Failed to evaluate expression: {}", rawInput, e);
            return BigDecimal.ONE;
        }
    }

    /** Returns whether the expression can be evaluated without substituting a value. */
    public static boolean isValid(String rawInput)
    {
        if (rawInput == null || rawInput.trim().isEmpty())
        {
            return false;
        }

        try
        {
            new Parser(tokenize(sanitize(rawInput))).parse();
            return true;
        }
        catch (RuntimeException e)
        {
            return false;
        }
    }

    private enum TokenType
    {
        NUMBER,
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,
        LPAREN,
        RPAREN
    }

    private static final class Token
    {
        final TokenType type;
        final BigDecimal numberValue;
        final boolean isPercent;

        Token(TokenType type)
        {
            this(type, BigDecimal.ZERO, false);
        }

        Token(TokenType type, BigDecimal numberValue, boolean isPercent)
        {
            this.type = type;
            this.numberValue = numberValue;
            this.isPercent = isPercent;
        }
    }

    private static List<Token> tokenize(String input)
    {
        List<Token> tokens = new ArrayList<>();
        char[] chars = input.toCharArray();
        int pos = 0;

        while (pos < chars.length)
        {
            char c = chars[pos];

            switch (c)
            {
                case '+':
                    tokens.add(new Token(TokenType.PLUS));
                    pos++;
                    break;

                case '-':
                    tokens.add(new Token(TokenType.MINUS));
                    pos++;
                    break;

                case '*':
                    tokens.add(new Token(TokenType.MULTIPLY));
                    pos++;
                    break;

                case '/':
                    tokens.add(new Token(TokenType.DIVIDE));
                    pos++;
                    break;

                case '(':
                    tokens.add(new Token(TokenType.LPAREN));
                    pos++;
                    break;

                case ')':
                    tokens.add(new Token(TokenType.RPAREN));
                    pos++;
                    break;

                default:
                    if (Character.isDigit(c) || c == '.')
                    {
                        int start = pos;
                        boolean hasDot = false;

                        if (chars[pos] == '.')
                        {
                            hasDot = true;
                            pos++;
                            if (pos >= chars.length || !Character.isDigit(chars[pos]))
                            {
                                throw new IllegalArgumentException("Invalid decimal point in input");
                            }
                        }

                        while (pos < chars.length && Character.isDigit(chars[pos]))
                        {
                            pos++;
                            if (pos < chars.length && chars[pos] == '.' && !hasDot)
                            {
                                hasDot = true;
                                pos++;
                            }
                        }

                        String numStr = input.substring(start, pos);
                        BigDecimal value = new BigDecimal(numStr);
                        boolean isPercent = false;

                        if (pos < chars.length)
                        {
                            char suffix = chars[pos];
                            switch (suffix)
                            {
                                case 'k':
                                    value = value.multiply(K_MULTIPLIER);
                                    pos++;
                                    break;
                                case 'm':
                                    value = value.multiply(M_MULTIPLIER);
                                    pos++;
                                    break;
                                case 'b':
                                    value = value.multiply(B_MULTIPLIER);
                                    pos++;
                                    break;
                                case 't':
                                    value = value.multiply(T_MULTIPLIER);
                                    pos++;
                                    break;
                                case '%':
                                    isPercent = true;
                                    pos++;
                                    break;
                                default:
                                    break;
                            }
                        }

                        if (pos < chars.length && (Character.isLetter(chars[pos]) || chars[pos] == '%'))
                        {
                            throw new IllegalArgumentException("Unexpected character after number: " + chars[pos]);
                        }

                        tokens.add(new Token(TokenType.NUMBER, value, isPercent));
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unexpected character: " + c);
                    }
                    break;
            }
        }

        return tokens;
    }

    private static final class Parser
    {
        /**
         * Caps parenthesis nesting. Without this, input such as {@code ((((((...} recurses
         * once per opening bracket and blows the stack with a {@link StackOverflowError},
         * which is an {@link Error} and would slip past the {@code Exception} handlers.
         */
        private static final int MAX_NESTING_DEPTH = 64;

        private final List<Token> tokens;
        private int current = 0;
        private int depth = 0;

        Parser(List<Token> tokens)
        {
            this.tokens = tokens;
        }

        BigDecimal parse()
        {
            BigDecimal result = parseExpression();
            if (current < tokens.size())
            {
                throw new IllegalArgumentException("Unexpected tokens after expression");
            }
            return result;
        }

        private BigDecimal parseExpression()
        {
            BigDecimal result = parseTerm();

            while (match(TokenType.PLUS, TokenType.MINUS))
            {
                Token op = previous();
                Token nextTok = peek();

                if (nextTok != null && nextTok.type == TokenType.NUMBER && nextTok.isPercent)
                {
                    Token pTok = advance();
                    BigDecimal percentDelta = result.multiply(pTok.numberValue)
                            .divide(HUNDRED, 20, RoundingMode.HALF_UP);

                    if (op.type == TokenType.PLUS)
                    {
                        result = result.add(percentDelta);
                    }
                    else
                    {
                        result = result.subtract(percentDelta);
                    }
                }
                else
                {
                    BigDecimal term = parseTerm();
                    if (op.type == TokenType.PLUS)
                    {
                        result = result.add(term);
                    }
                    else
                    {
                        result = result.subtract(term);
                    }
                }
            }

            return result;
        }

        private BigDecimal parseTerm()
        {
            BigDecimal result = parseFactor();

            while (match(TokenType.MULTIPLY, TokenType.DIVIDE))
            {
                Token op = previous();
                BigDecimal factor = parseFactor();

                if (op.type == TokenType.MULTIPLY)
                {
                    result = result.multiply(factor);
                }
                else
                {
                    if (factor.compareTo(BigDecimal.ZERO) == 0)
                    {
                        throw new ArithmeticException("Division by zero");
                    }
                    result = result.divide(factor, 20, RoundingMode.HALF_UP);
                }
            }

            return result;
        }

        private BigDecimal parseFactor()
        {
            if (match(TokenType.PLUS))
            {
                return parseFactor();
            }
            if (match(TokenType.MINUS))
            {
                return parseFactor().negate();
            }
            return parsePrimary();
        }

        private BigDecimal parsePrimary()
        {
            if (match(TokenType.NUMBER))
            {
                Token token = previous();
                if (token.isPercent)
                {
                    return token.numberValue.divide(HUNDRED, 20, RoundingMode.HALF_UP);
                }
                return token.numberValue;
            }

            if (match(TokenType.LPAREN))
            {
                depth++;
                if (depth > MAX_NESTING_DEPTH)
                {
                    throw new IllegalArgumentException("Expression nested too deeply");
                }

                try
                {
                    BigDecimal value = parseExpression();
                    if (!match(TokenType.RPAREN))
                    {
                        throw new IllegalArgumentException("Mismatched parentheses: expected ')'");
                    }
                    return value;
                }
                finally
                {
                    depth--;
                }
            }

            throw new IllegalArgumentException("Expected expression");
        }

        private boolean match(TokenType... types)
        {
            for (TokenType type : types)
            {
                if (check(type))
                {
                    advance();
                    return true;
                }
            }
            return false;
        }

        private boolean check(TokenType type)
        {
            if (isAtEnd())
            {
                return false;
            }
            return peek().type == type;
        }

        private Token advance()
        {
            if (!isAtEnd())
            {
                current++;
            }
            return previous();
        }

        private boolean isAtEnd()
        {
            return current >= tokens.size();
        }

        private Token peek()
        {
            if (isAtEnd())
            {
                return null;
            }
            return tokens.get(current);
        }

        private Token previous()
        {
            return tokens.get(current - 1);
        }
    }
}
