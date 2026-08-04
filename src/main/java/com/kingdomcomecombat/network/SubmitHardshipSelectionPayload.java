package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SubmitHardshipSelectionPayload(List<String> ids) implements CustomPayload {
    private static final int MAX_SELECTIONS = 20;
    public static final Id<SubmitHardshipSelectionPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "submit_hardship_selection"));
    public static final PacketCodec<RegistryByteBuf, SubmitHardshipSelectionPayload> CODEC = PacketCodec.of(
            (value, buf) -> { buf.writeVarInt(value.ids().size()); value.ids().forEach(id -> buf.writeString(id, 128)); },
            buf -> {
                int size = buf.readVarInt();
                if (size < 0 || size > MAX_SELECTIONS) {
                    throw new IllegalArgumentException("Invalid hardship selection count: " + size);
                }
                List<String> ids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) ids.add(buf.readString(128));
                return new SubmitHardshipSelectionPayload(ids);
            }
    );
    public SubmitHardshipSelectionPayload { ids = ids == null ? List.of() : List.copyOf(ids); }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
