package wizzy.ietfc_crossover_calendar_buds;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityAttachHandler {
    @SubscribeEvent
    public static void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().getCapability(QuartzCapabilityProvider.CHANCE_CAP).isPresent()) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(Ietfc_crossover_calendar_buds.MODID, "quartz_data"), new QuartzCapabilityProvider());
        }
    }
}
