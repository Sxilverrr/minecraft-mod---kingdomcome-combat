package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record HardshipSelectionSyncPayload(List<String> ids) implements CustomPayload {
    public static final Id<HardshipSelectionSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "hardship_selection_sync"));
    public static final PacketCodec<RegistryByteBuf, HardshipSelectionSyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> { buf.writeVarInt(value.ids().size()); value.ids().forEach(id -> buf.writeString(id, 128)); },
            buf -> { int size = buf.readVarInt(); List<String> ids = new ArrayList<>(); for (int i = 0; i < size; i++) { String id = buf.readString(128); if (i < 20) ids.add(id); } return new HardshipSelectionSyncPayload(ids); }
    );
    public HardshipSelectionSyncPayload { ids = ids == null ? List.of() : List.copyOf(ids); }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
