package openmods.debug.event;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = EventDebug.MOD_ID, name = EventDebug.MOD_NAME, version = "$VERSION$")
public class EventDebug {

    public static final String MOD_NAME = "Event Debug";
    public static final String MOD_ID = "eventdebug";

    public static class CommonProxy {
        public void preInit() {}
    }

    public static class ClientProxy extends CommonProxy {
        @Override
        public void preInit() {
            ClientCommandHandler.instance.registerCommand(new CommandEventList("list_events_c", false, instance.collector));
        }
    }

    @SidedProxy(serverSide = "openmods.debug.event.EventDebug$CommonProxy", clientSide = "openmods.debug.event.EventDebug$ClientProxy")
    public static CommonProxy proxy;

    @Instance
    public static EventDebug instance;

    private final EventCollector collector = new EventCollector();

    @EventHandler
    public void construct(FMLConstructionEvent evt) {
        MinecraftForge.EVENT_BUS.register(collector.createProbe("FORGE-EVENT"));
        MinecraftForge.ORE_GEN_BUS.register(collector.createProbe("FORGE-ORE"));
        MinecraftForge.TERRAIN_GEN_BUS.register(collector.createProbe("FORGE-TERRAIN"));
        FMLCommonHandler.instance().bus().register(collector.createProbe("FML"));
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        proxy.preInit();
    }

    @EventHandler
    public void severStart(FMLServerStartingEvent evt) {
        evt.registerServerCommand(new CommandEventList("list_events_s", true, collector));
    }
}
