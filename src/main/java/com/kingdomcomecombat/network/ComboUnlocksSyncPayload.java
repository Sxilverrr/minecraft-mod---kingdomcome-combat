package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ComboUnlocksSyncPayload(List<String> comboIds) implements CustomPayload {
    public static final Id<ComboUnlocksSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "combo_unlocks_sync"));

    public static final PacketCodec<RegistryByteBuf, ComboUnlocksSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.comboIds().size());
                        for (String comboId : value.comboIds()) {
                            buf.writeString(comboId);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<String> comboIds = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            comboIds.add(buf.readString());
                        }
                        return new ComboUnlocksSyncPayload(comboIds);
                    }
            );

    public ComboUnlocksSyncPayload {
        comboIds = List.copyOf(comboIds);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
