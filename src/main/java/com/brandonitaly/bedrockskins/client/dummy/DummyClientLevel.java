package com.brandonitaly.bedrockskins.client.dummy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

public class DummyClientLevel extends ClientLevel {
    private static DummyClientLevel instance;

    public static ClientLevel getPreviewLevel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level;
        }
        if (instance == null) {
            instance = new DummyClientLevel();
        }
        return instance;
    }

    private DummyClientLevel() {
        super(
            DummyClientPacketListener.getInstance(),
            new ClientLevelData(Difficulty.NORMAL, false, true),
            Level.OVERWORLD,
            DummyClientPacketListener.getDummyDimensionTypeHolder(),
            0,
            0,
            Minecraft.getInstance().levelRenderer,
            false,
            0L,
            63
        );
    }
}
