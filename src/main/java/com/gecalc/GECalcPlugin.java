package com.gecalc;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@PluginDescriptor
(
        name = "GE Calc",
        description = "Use maths to set price and quantity in GE!",
        tags = {"ge", "grand", "exchange", "price", "prices", "math", "maths", "calc", "calculator"}
)
public class GECalcPlugin extends Plugin
{
    @Inject
    private KeyManager keyManager;

    @Inject
    private GECalcKeyHandler keyInputHandler;

    @Inject
    private GECalcOfferPriceUpdater offerPriceUpdater;

    @Override
    protected void startUp() throws Exception
    {
        keyManager.registerKeyListener(keyInputHandler);
        log.info("GE Calc - Started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        keyManager.unregisterKeyListener(keyInputHandler);
        offerPriceUpdater.restore();
        log.info("GE Calc - Stopped!");
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event)
    {
        offerPriceUpdater.update();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.GeOffers.UNIVERSE >>> 16)
        {
            offerPriceUpdater.onGrandExchangeInterfaceLoaded();
        }
    }
}
