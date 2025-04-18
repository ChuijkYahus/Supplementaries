package net.mehvahdjukaar.supplementaries.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.common.events.fabric.ClientEventsFabric;
import net.mehvahdjukaar.supplementaries.common.events.fabric.ServerEventsFabric;
import net.mehvahdjukaar.supplementaries.common.utils.VibeChecker;
import net.mehvahdjukaar.supplementaries.reg.ModSetup;

public class SupplementariesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Supplementaries.commonInit();

        ServerEventsFabric.init();

        if (PlatHelper.getPhysicalSide().isClient()) {
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> VibeChecker.checkVibe());
            ClientEventsFabric.init();

            SupplementariesFabricClient.init();
        }
        PlatHelper.addCommonSetup(ModSetup::setup);
        PlatHelper.addCommonSetup(ModSetup::asyncSetup);

    }
}
