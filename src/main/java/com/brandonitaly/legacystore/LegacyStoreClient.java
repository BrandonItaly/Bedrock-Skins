package com.brandonitaly.legacystore;

import com.brandonitaly.legacystore.api.ContentCategory;
import com.brandonitaly.legacystore.gui.Legacy4JStoreScreen;

import java.util.ArrayList;
import java.util.List;

public class LegacyStoreClient {
    private static final List<ContentCategory> CATEGORIES = new ArrayList<>();
    private static boolean isInitialized = false;

    /**
     * Mods should call this to register their store contents.
     */
    public static void registerCategory(ContentCategory category) {
        CATEGORIES.add(category);
    }

    /**
     * Initializes the Legacy4J UI elements.
     */
    public static void init() {
        if (isInitialized) return;
        isInitialized = true;

        //? if legacy4j {
        /*
        wily.factoryapi.base.client.UIDefinitionManager.registerDefaultScreen(
            "legacy_store_screen", 
            (parentScreen) -> new Legacy4JStoreScreen(parentScreen, CATEGORIES)
        );
        
        wily.factoryapi.base.client.UIDefinitionManager.ElementType.registerConditional("add_menu_button", (definition, accessorFunction, name, e) -> {
            java.util.List<wily.factoryapi.base.client.UIDefinitionManager.WidgetAction.PressSupplier<net.minecraft.client.gui.components.AbstractWidget>> actions = wily.factoryapi.base.client.UIDefinitionManager.ElementType.parseActionsElement(definition, name, e);

            // 1. Parse Placement using Dynamic syntax
            boolean placeAbove = e.get("placement").asString().result().orElse("").equals("above");

            // 2. Parse New Button Text from the nested "message" object
            net.minecraft.network.chat.Component buttonText;
            java.util.Optional<String> msgTranslate = e.get("message").get("translate").asString().result();
            java.util.Optional<String> msgText = e.get("message").get("text").asString().result();
            
            if (msgTranslate.isPresent()) {
                buttonText = net.minecraft.network.chat.Component.translatable(msgTranslate.get());
            } else if (msgText.isPresent()) {
                buttonText = net.minecraft.network.chat.Component.literal(msgText.get());
            } else {
                buttonText = net.minecraft.network.chat.Component.translatable("menu.singleplayer"); // Fallback
            }

            // 3. Parse Target Key from the nested "target" object
            String targetKey = e.get("target").get("translate").asString().result().orElse("menu.singleplayer");

            int spacing = 24;

            definition.addStatic(wily.factoryapi.base.client.UIDefinition.createAfterInit(a -> {
                wily.factoryapi.base.client.UIAccessor accessor = accessorFunction.apply(a);
                net.minecraft.client.gui.components.AbstractWidget targetWidget = null;

                // 4. Find the target button
                for (Object child : accessor.getChildren()) {
                    if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                        if (widget.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                            if (translatable.getKey().equals(targetKey)) {
                                targetWidget = widget;
                                break;
                            }
                        }
                    }
                }

                if (targetWidget != null) {
                    
                    // 5. Calculate positions
                    int newButtonY;
                    int shiftThresholdY;

                    if (placeAbove) {
                        newButtonY = targetWidget.getY();
                        shiftThresholdY = targetWidget.getY();
                    } else {
                        newButtonY = targetWidget.getY() + spacing;
                        shiftThresholdY = targetWidget.getY() + 1;
                    }

                    // 6. Shift widgets down
                    for (Object child : accessor.getChildren()) {
                        if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                            if (widget.getY() >= shiftThresholdY) {
                                widget.setY(widget.getY() + spacing);
                            }
                        }
                    }

                    // 7. Create the new button
                    net.minecraft.client.gui.components.Button storeButton = net.minecraft.client.gui.components.Button.builder(buttonText, b -> {
                        actions.forEach(c -> c.press(a, b, wily.factoryapi.base.client.UIDefinitionManager.WidgetAction.Type.ENABLE));
                    }).bounds(targetWidget.getX(), newButtonY, targetWidget.getWidth(), targetWidget.getHeight()).build();

                    // 8. Add it
                    accessor.putWidget(name, accessor.addChild(name, storeButton));
                }
            }));
        });
        */
        //?}
    }
}