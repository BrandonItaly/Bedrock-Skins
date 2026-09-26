package io.github.brandonitaly.bedrockskins.pack.persona;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Bounded image conversion shared by the Persona bridge and its tests. */
public final class PersonaImages {
    public static final int MAX_PIXELS = 1_048_576;
    private PersonaImages() {}

    public static BufferedImage read(byte[] png) throws IOException {
        if (png == null || png.length == 0 || png.length > 1_048_576) throw new IOException("Invalid Persona PNG size");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Invalid Persona image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                checkSize(reader.getWidth(0), reader.getHeight(0));
                if (!reader.getFormatName().equalsIgnoreCase("png")) throw new IOException("Persona images must be PNG");
                return reader.read(0);
            } finally { reader.dispose(); }
        }
    }

    public static void checkSize(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) throw new IOException("Persona image exceeds pixel limit");
    }

    public static BufferedImage fromRgba(int width, int height, byte[] data) throws IOException {
        checkSize(width, height);
        if ((long) width * height * 4 != data.length) throw new IOException("Persona image dimensions do not match bytes");
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0, i = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int r = data[i++] & 255, g = data[i++] & 255, b = data[i++] & 255, a = data[i++] & 255;
            image.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
        }
        return image;
    }

    public static byte[] rgba(BufferedImage image) {
        byte[] data = new byte[image.getWidth() * image.getHeight() * 4];
        for (int y = 0, i = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int pixel = image.getRGB(x, y);
            data[i++] = (byte) (pixel >>> 16); data[i++] = (byte) (pixel >>> 8);
            data[i++] = (byte) pixel; data[i++] = (byte) (pixel >>> 24);
        }
        return data;
    }

    public static byte[] png(BufferedImage image) throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return output.toByteArray();
    }
}
