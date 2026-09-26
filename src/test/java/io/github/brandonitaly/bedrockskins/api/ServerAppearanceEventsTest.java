package io.github.brandonitaly.bedrockskins.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerAppearanceEventsTest {
    @Test void coreRunsWithoutGeyserAndListenerSurvivesServerRestart() throws Exception {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.geysermc.geyser.api.GeyserApi"));
        List<String> calls = new ArrayList<>();
        UUID player = UUID.randomUUID();
        var registration = ServerAppearanceEvents.register(new ServerAppearanceEvents.Listener() {
            public void skinChanged(UUID id) { assertEquals(player, id); calls.add("skin"); }
            public void disconnect(UUID id) { assertEquals(player, id); calls.add("disconnect"); }
            public void stop() { calls.add("stop"); }
        });
        try {
            ServerAppearanceEvents.tick(null);
            ServerAppearanceEvents.skinChanged(player);
            ServerAppearanceEvents.disconnect(player);
            ServerAppearanceEvents.stop();
            ServerAppearanceEvents.skinChanged(player);
            assertEquals(List.of("skin", "disconnect", "stop", "skin"), calls);
        } finally { registration.close(); }
        ServerAppearanceEvents.skinChanged(player);
        ServerAppearanceEvents.stop();
        assertEquals(4, calls.size());
    }
}
