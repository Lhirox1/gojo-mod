package com.gojo.mod.network;

import com.gojo.mod.ability.AbilityManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: "player pressed ability #id". */
public class UseAbilityPacket {
    private final int ability;

    public UseAbilityPacket(int ability) {
        this.ability = ability;
    }

    public UseAbilityPacket(FriendlyByteBuf buf) {
        this.ability = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ability);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                AbilityManager.use(sender, ability);
            }
        });
        ctx.setPacketHandled(true);
    }
}
