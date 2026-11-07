package com.gojo.mod.network;

import com.gojo.mod.GojoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GojoNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GojoMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private GojoNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, UseAbilityPacket.class,
                UseAbilityPacket::encode,
                UseAbilityPacket::new,
                UseAbilityPacket::handle);
    }
}
