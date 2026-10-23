package io.github.brandonitaly.bedrockskins.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;

//? if >=26.4-snapshot-5 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureResources;
import java.util.HashMap;
import java.util.Map;*/
//?} else {
import net.minecraft.client.renderer.texture.DynamicTexture;
//?}

public final class SkinTexture implements AutoCloseable {
    //? if >=26.4-snapshot-5 {
    /*private static final Map<Identifier, SkinTexture> REGISTERED = new HashMap<>();
    private final Supplier<String> name;
    private NativeImage pixels;
    private TextureResources resources;
    private TextureManager manager;
    private Identifier registeredId;*/
    //?} else {
    private final DynamicTexture texture;
    //?}

    public SkinTexture(Supplier<String> name, NativeImage pixels) {
        //? if >=26.4-snapshot-5 {
        /*this.name = name;
        this.pixels = pixels;*/
        //?} else {
        texture = new DynamicTexture(name, pixels);
        //?}
    }

    public NativeImage getPixels() {
        //? if >=26.4-snapshot-5 {
        /*if (resources != null && resources.texture().isClosed()) close();
        return pixels;*/
        //?} else {
        return texture.getPixels();
        //?}
    }

    public void upload() {
        //? if >=26.4-snapshot-5 {
        /*if (getPixels() == null) return;
        if (resources == null) resources = TextureResources.from2dImage(name, pixels);
        else pixels.writeToGpuTexture(RenderSystem.getDevice().createCommandEncoder(), resources.texture());*/
        //?} else {
        texture.upload();
        //?}
    }

    public void register(TextureManager manager, Identifier id) {
        //? if >=26.4-snapshot-5 {
        /*upload();
        if (pixels == null) throw new IllegalStateException("Cannot register a closed skin texture");
        manager.register(id, resources);
        this.manager = manager;
        registeredId = id;
        SkinTexture previous = REGISTERED.put(id, this);
        if (previous != null && previous != this) previous.close();*/
        //?} else {
        manager.register(id, texture);
        //?}
    }

    public static NativeImage pixels(TextureManager manager, Identifier id) {
        //? if >=26.4-snapshot-5 {
        /*SkinTexture texture = REGISTERED.get(id);
        return texture == null ? null : texture.getPixels();*/
        //?} else {
        return manager.getTexture(id) instanceof DynamicTexture texture ? texture.getPixels() : null;
        //?}
    }

    public static void release(TextureManager manager, Identifier id) {
        manager.release(id);
        //? if >=26.4-snapshot-5 {
        /*SkinTexture texture = REGISTERED.remove(id);
        if (texture != null) texture.close();*/
        //?}
    }

    public static void releaseAll(TextureManager manager) {
        //? if >=26.4-snapshot-5 {
        /*for (Identifier id : java.util.List.copyOf(REGISTERED.keySet())) release(manager, id);*/
        //?}
    }

    @Override
    public void close() {
        //? if >=26.4-snapshot-5 {
        /*if (registeredId != null && REGISTERED.remove(registeredId, this)) manager.release(registeredId);
        if (resources != null) {
            if (!resources.texture().isClosed()) resources.texture().close();
            if (!resources.textureView().isClosed()) resources.textureView().close();
        }
        if (pixels != null) {
            pixels.close();
            pixels = null;
        }*/
        //?} else {
        texture.close();
        //?}
    }
}
