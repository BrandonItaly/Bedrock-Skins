package io.github.brandonitaly.bedrockskins.pack.persona;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

/** Decoder for the uncompressed and RLE true-color TGA images used by Persona assets. */
final class TgaReader {
    private TgaReader() {}

    static BufferedImage read(File file) throws Exception {
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
            int idLength = input.readUnsignedByte();
            int colorMapType = input.readUnsignedByte();
            int imageType = input.readUnsignedByte();
            input.skipNBytes(9);
            int width = readUnsignedShortLE(input);
            int height = readUnsignedShortLE(input);
            int bits = input.readUnsignedByte();
            int descriptor = input.readUnsignedByte();
            if (colorMapType != 0 || (imageType != 2 && imageType != 10) || (bits != 24 && bits != 32)
                    || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Unsupported Persona TGA format in " + file.getName());
            }
            input.skipNBytes(idLength);
            int[] pixels = decodePixels(input, imageType, bits, width * height);
            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            boolean topOrigin = (descriptor & 0x20) != 0;
            boolean rightOrigin = (descriptor & 0x10) != 0;
            for (int sourceY = 0; sourceY < height; sourceY++) {
                int y = topOrigin ? sourceY : height - 1 - sourceY;
                for (int sourceX = 0; sourceX < width; sourceX++) {
                    int x = rightOrigin ? width - 1 - sourceX : sourceX;
                    result.setRGB(x, y, pixels[sourceY * width + sourceX]);
                }
            }
            return result;
        }
    }

    private static int[] decodePixels(DataInputStream input, int imageType, int bits, int size) throws Exception {
        int[] pixels = new int[size];
        int written = 0;
        while (written < size) {
            int count = 1;
            boolean repeated = false;
            if (imageType == 10) {
                int packet = input.readUnsignedByte();
                count = (packet & 0x7F) + 1;
                repeated = (packet & 0x80) != 0;
            }
            if (repeated) {
                int pixel = readPixel(input, bits);
                for (int i = 0; i < count && written < size; i++) pixels[written++] = pixel;
            } else {
                for (int i = 0; i < count && written < size; i++) pixels[written++] = readPixel(input, bits);
            }
        }
        return pixels;
    }

    private static int readUnsignedShortLE(DataInputStream input) throws Exception {
        int low = input.readUnsignedByte();
        return low | (input.readUnsignedByte() << 8);
    }

    private static int readPixel(DataInputStream input, int bits) throws Exception {
        int blue = input.readUnsignedByte();
        int green = input.readUnsignedByte();
        int red = input.readUnsignedByte();
        int alpha = bits == 32 ? input.readUnsignedByte() : 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
