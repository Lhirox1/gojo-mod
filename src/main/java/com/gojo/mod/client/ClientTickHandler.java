package com.gojo.mod.client;

import com.gojo.mod.GojoMod;
import com.gojo.mod.ability.AbilityManager;
import com.gojo.mod.network.GojoNetwork;
import com.gojo.mod.network.UseAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Polls the keybinds each client tick and tells the server which ability was pressed. */
@Mod.EventBusSubscriber(modid = GojoMod.MODID, value = Dist.CLIENT)
public final class ClientTickHandler {

    private ClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (ClientKeyBindings.BLUE.consumeClick()) send(AbilityManager.BLUE);
        while (ClientKeyBindings.RED.consumeClick()) send(AbilityManager.RED);
        while (ClientKeyBindings.PURPLE.consumeClick()) send(AbilityManager.PURPLE);
        while (ClientKeyBindings.DOMAIN.consumeClick()) send(AbilityManager.DOMAIN);
    }

    private static void send(int ability) {
        GojoNetwork.CHANNEL.sendToServer(new UseAbilityPacket(ability));
    }
}
