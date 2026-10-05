package com.gecalc;

import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/** Writes exact offer prices into the Grand Exchange overview widgets. */
class GECalcOfferPriceUpdater
{
    private static final int[] OFFER_SLOT_WIDGETS =
    {
        InterfaceID.GeOffers.INDEX_0,
        InterfaceID.GeOffers.INDEX_1,
        InterfaceID.GeOffers.INDEX_2,
        InterfaceID.GeOffers.INDEX_3,
        InterfaceID.GeOffers.INDEX_4,
        InterfaceID.GeOffers.INDEX_5,
        InterfaceID.GeOffers.INDEX_6,
        InterfaceID.GeOffers.INDEX_7
    };

    private final Client client;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);
    private final Widget[] priceWidgets = new Widget[OFFER_SLOT_WIDGETS.length];
    private final String[] originalTexts = new String[OFFER_SLOT_WIDGETS.length];

    @Inject
    GECalcOfferPriceUpdater(Client client)
    {
        this.client = client;
    }

    void update()
    {
        Widget window = client.getWidget(InterfaceID.GeOffers.UNIVERSE);
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        if (window == null || window.isHidden() || offers == null)
        {
            return;
        }

        for (int slotIndex = 0; slotIndex < Math.min(OFFER_SLOT_WIDGETS.length, offers.length); slotIndex++)
        {
            Widget slot = client.getWidget(OFFER_SLOT_WIDGETS[slotIndex]);
            GrandExchangeOffer offer = offers[slotIndex];
            if (slot == null || slot.isHidden() || offer == null || offer.getState() == GrandExchangeOfferState.EMPTY)
            {
                continue;
            }

            Widget discoveredWidget = findCoinsWidget(slot);
            Widget priceWidget = discoveredWidget != null ? discoveredWidget : priceWidgets[slotIndex];
            if (priceWidget == null || priceWidget.isHidden() || !belongsToSlot(priceWidget, slot))
            {
                continue;
            }

            if (priceWidgets[slotIndex] != priceWidget)
            {
                priceWidgets[slotIndex] = priceWidget;
                originalTexts[slotIndex] = priceWidget.getText();
            }

            // getPrice() is the per-item offer price, matching the GE's detail view.
            priceWidget.setText(numberFormat.format(offer.getPrice()) + " gp");
        }
    }

    void onGrandExchangeInterfaceLoaded()
    {
        // RuneLite can rebuild the slot widgets when the GE is closed and reopened.
        // Drop the old references so the next client tick discovers the new labels.
        for (int slotIndex = 0; slotIndex < priceWidgets.length; slotIndex++)
        {
            priceWidgets[slotIndex] = null;
            originalTexts[slotIndex] = null;
        }
    }

    void restore()
    {
        for (int slotIndex = 0; slotIndex < priceWidgets.length; slotIndex++)
        {
            Widget widget = priceWidgets[slotIndex];
            String originalText = originalTexts[slotIndex];
            if (widget != null && originalText != null && !widget.isHidden())
            {
                widget.setText(originalText);
            }

            priceWidgets[slotIndex] = null;
            originalTexts[slotIndex] = null;
        }
    }

    private static boolean belongsToSlot(Widget widget, Widget slot)
    {
        for (Widget current = widget; current != null; current = current.getParent())
        {
            if (current == slot)
            {
                return true;
            }
        }
        return false;
    }

    private static Widget findCoinsWidget(Widget widget)
    {
        String text = widget.getText();
        if (text != null && text.endsWith(" coins"))
        {
            return widget;
        }

        Widget[] children = widget.getChildren();
        if (children == null)
        {
            return null;
        }

        for (Widget child : children)
        {
            if (child == null || child.isHidden())
            {
                continue;
            }

            Widget candidate = findCoinsWidget(child);
            if (candidate != null)
            {
                return candidate;
            }
        }

        return null;
    }
}
