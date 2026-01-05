package immersive_aircraft.neoforge;

import immersive_aircraft.Main;
import immersive_aircraft.Renderer;
import immersive_aircraft.WeaponRendererRegistry;
import immersive_aircraft.client.KeyBindings;
import immersive_aircraft.neoforge.cobalt.network.NetworkHandlerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = Main.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = Main.MOD_ID)
public final class ClientNeoForge {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        Renderer.bootstrap();
        WeaponRendererRegistry.bootstrap();

    }

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBindings.list.forEach(event::register);
    }

    @SubscribeEvent // 仅在物理客户端的模组事件总线上
    public static void addClientResourceListeners(AddClientReloadListenersEvent event) {
        // 使用事件系统注册监听器
        NeoForgeBusEvents.RESOURCE_REGISTRY.getLoaders().forEach(loader -> {
            Identifier id = Identifier.fromNamespaceAndPath(Main.MOD_ID, loader.getName().toLowerCase());
            event.addListener(id, loader);
        });


    }
}