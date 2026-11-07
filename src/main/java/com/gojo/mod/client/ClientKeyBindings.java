package com.gojo.mod.client;

import com.gojo.mod.GojoMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyBindings {

    private static final String CATEGORY = "key.categories.gojomod";

    public static final KeyMapping BLUE = new KeyMapping(
            "key.gojomod.blue", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    public static final KeyMapping RED = new KeyMapping(
            "key.gojomod.red", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    public static final KeyMapping PURPLE = new KeyMapping(
            "key.gojomod.purple", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);

    public static final KeyMapping DOMAIN = new KeyMapping(
            "key.gojomod.domain", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    private ClientKeyBindings() {}

    @Mod.EventBusSubscriber(modid = GojoMod.MODID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(BLUE);
            event.register(RED);
            event.register(PURPLE);
            event.register(DOMAIN);
        }
    }
}
