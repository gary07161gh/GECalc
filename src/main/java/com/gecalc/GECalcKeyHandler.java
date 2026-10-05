package com.gecalc;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyListener;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;

@Slf4j
@SuppressWarnings("deprecation")
class GECalcKeyHandler implements KeyListener
{
    /*
     * RuneLite / OSRS GE input modes:
     *
     * 7  = legacy input mode (quantity input for GE, trade, bank)
     * 30 = new 64-bit Grand Exchange input mode introduced with
     *      the Platinum Token / expanded GE price update.
     */
    private static final int LEGACY_INPUT_MODE = 7;
    private static final int GE_64_BIT_INPUT_MODE = 30;

    /**
     * Maximum number of characters the GE input box accepts on its own. Past this the
     * plugin appends digits directly, bypassing the client's limit.
     */
    private static final int CLIENT_INPUT_LIMIT = 10;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    public boolean isQuantityInput()
    {
        int inputType = client.getVarcIntValue(VarClientInt.INPUT_TYPE);
        return inputType == LEGACY_INPUT_MODE || inputType == GE_64_BIT_INPUT_MODE;
    }

    private BigDecimal getMaximumValue()
    {
        int inputType = client.getVarcIntValue(VarClientInt.INPUT_TYPE);
        if (inputType == GE_64_BIT_INPUT_MODE)
        {
            return GECalcEvaluator.GE_MAX;
        }
        return GECalcEvaluator.LEGACY_MAX;
    }

    private void parseQuantity()
    {
        final String rawInput = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
        if (rawInput == null || rawInput.isEmpty())
        {
            return;
        }

        if (!GECalcEvaluator.isValid(rawInput))
        {
            log.debug("GE Calc - Keeping invalid expression in input field: {}", rawInput);
            return;
        }

        BigDecimal calculatedValue = GECalcEvaluator.evaluate(rawInput, getMaximumValue());
        final String finalCalculatedValue = calculatedValue.toBigInteger().toString();

        clientThread.invoke(() ->
                client.setVarcStrValue(
                        VarClientStr.INPUT_TEXT,
                        finalCalculatedValue
                )
        );
    }

    public void appendStringToValue(String toAppend)
    {
        final String currentValue = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
        if (currentValue == null)
        {
            return;
        }

        String newValue = currentValue + toAppend;
        clientThread.invoke(() ->
                client.setVarcStrValue(
                        VarClientStr.INPUT_TEXT,
                        newValue
                )
        );
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (!isQuantityInput())
        {
            return;
        }

        char keyChar = e.getKeyChar();

        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            parseQuantity();
        }
        else if (isCalculationCharacter(keyChar))
        {
            // The GE input box rejects these characters itself, so the plugin is the
            // only thing that can insert them.
            appendStringToValue(String.valueOf(keyChar));
        }
        else if (Character.isDigit(keyChar) && hasReachedClientInputLimit())
        {
            // Digits are different: below CLIENT_INPUT_LIMIT the client inserts the
            // digit itself, and appending here as well would duplicate it. Only
            // override once the client has stopped accepting more input.
            appendStringToValue(String.valueOf(keyChar));
        }
    }

    private boolean hasReachedClientInputLimit()
    {
        final String currentValue = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
        return currentValue != null && currentValue.length() >= CLIENT_INPUT_LIMIT;
    }

    private static boolean isCalculationCharacter(char c)
    {
        switch (c)
        {
            case '+':
            case '-':
            case '*':
            case '/':
            case '(':
            case ')':
            case '%':
            case 'k':
            case 'm':
            case 'b':
            case 't':
            case 'K':
            case 'M':
            case 'B':
            case 'T':
            case '.':
            case ',':
            case ' ':
                return true;
            default:
                return false;
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }
}
