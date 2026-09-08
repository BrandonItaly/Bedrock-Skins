package io.github.brandonitaly.bedrockskins.client.integration;

import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

//? if <=26.2 {
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//?} else {
/*import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;*/
//?}

/** Cross-version native file picker for the GLFW and SDL client backends. */
public final class NativeFileDialog {
    private NativeFileDialog() {}

    public static void open(String title, String pattern, String description,
                            Consumer<String> selection) {
        //? if <=26.2 {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8(pattern)).flip();
            String path = TinyFileDialogs.tinyfd_openFileDialog(
                title, "", filters, description, false);
            restoreWindow();
            if (path != null) selection.accept(path);
        }
        //?} else {
        /*openSdl(title, pattern, description, selection);*/
        //?}
    }

    //? if <=26.2 {
    private static void restoreWindow() {
        Minecraft.getInstance().execute(() -> {
            long handle = Minecraft.getInstance().getWindow().handle();
            if (handle != 0L) {
                org.lwjgl.glfw.GLFW.glfwRestoreWindow(handle);
                org.lwjgl.glfw.GLFW.glfwFocusWindow(handle);
            }
        });
    }
    //?} else {
    /*private static final Set<SdlRequest> ACTIVE_REQUESTS = ConcurrentHashMap.newKeySet();

    private static void openSdl(String title, String pattern, String description,
                                Consumer<String> selection) {
        Minecraft client = Minecraft.getInstance();
        String sdlPattern = pattern.replace("*.", "").replace("*", "");
        SdlRequest request = new SdlRequest(
            SDL_DialogFileFilter.calloc(1),
            MemoryUtil.memUTF8(description),
            MemoryUtil.memUTF8(sdlPattern));
        request.filters.get(0).set(request.description, request.pattern);
        request.callback = SDL_DialogFileCallback.create((userData, fileList, filter) -> {
            String path = null;
            if (fileList != 0L) {
                long firstPath = MemoryUtil.memGetAddress(fileList);
                if (firstPath != 0L) path = MemoryUtil.memUTF8(firstPath);
            }
            String selectedPath = path;
            client.execute(() -> {
                request.close();
                ACTIVE_REQUESTS.remove(request);
                long handle = client.getWindow().handle();
                if (handle != 0L) {
                    SDLVideo.SDL_RestoreWindow(handle);
                    SDLVideo.SDL_RaiseWindow(handle);
                }
                if (selectedPath != null) selection.accept(selectedPath);
            });
        });
        ACTIVE_REQUESTS.add(request);
        SDLDialog.SDL_ShowOpenFileDialog(request.callback, 0L, client.getWindow().handle(),
            request.filters, (ByteBuffer) null, false);
    }

    private static final class SdlRequest implements AutoCloseable {
        private final SDL_DialogFileFilter.Buffer filters;
        private final ByteBuffer description;
        private final ByteBuffer pattern;
        private SDL_DialogFileCallback callback;

        private SdlRequest(SDL_DialogFileFilter.Buffer filters, ByteBuffer description,
                           ByteBuffer pattern) {
            this.filters = filters;
            this.description = description;
            this.pattern = pattern;
        }

        @Override
        public void close() {
            if (callback != null) callback.free();
            filters.free();
            MemoryUtil.memFree(description);
            MemoryUtil.memFree(pattern);
        }
    }*/
    //?}
}
