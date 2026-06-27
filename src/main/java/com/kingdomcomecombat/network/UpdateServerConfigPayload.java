package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record UpdateServerConfigPayload(
        boolean modEquipmentGenerationEnabled,
        boolean zombieLeaderHealthFixEnabled,
        boolean mobToughnessEnabled,
        int hitStopTicks,
        double vanillaHurtSoundVolumeMultiplier,
        int masterCounterWindowTicks,
        int blockWindowTicks,
        double combatMinDistance,
        List<String> vanillaAttackWeaponIds
) implements CustomPayload {
    public static final Id<UpdateServerConfigPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "update_server_config"));
    public static final PacketCodec<RegistryByteBuf, UpdateServerConfigPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBoolean(value.modEquipmentGenerationEnabled());
                buf.writeBoolean(value.zombieLeaderHealthFixEnabled());
                buf.writeBoolean(value.mobToughnessEnabled());
                buf.writeVarInt(value.hitStopTicks());
                buf.writeDouble(value.vanillaHurtSoundVolumeMultiplier());
                buf.writeVarInt(value.masterCounterWindowTicks());
                buf.writeVarInt(value.blockWindowTicks());
                buf.writeDouble(value.combatMinDistance());
                writeStringList(buf, value.vanillaAttackWeaponIds());
            },
            buf -> new UpdateServerConfigPayload(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    readStringList(buf)
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void writeStringList(RegistryByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeString(value);
        }
    }

    private static List<String> readStringList(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readString());
        }
        return values;
    }
}
