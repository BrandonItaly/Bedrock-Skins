package com.brandonitaly.bedrockskins.client.pack;

import net.minecraft.resources./*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/;

public record ContentPack(
    String id,
    String name,
    String description,
    String downloadURI
) {}