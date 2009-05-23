package play.libs;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

/**
 * Images utils
 */
public class Images {

    /**
     * Resize an image
     * @param originalImage The image file
     * @param to The destination file
     * @param w The new width (or -1 to proportionally resize)
     * @param h The new height (or -1 to proportionally resize)
     */
    public static void resize(File originalImage, File to, Integer w, Integer h) {
        try {
            BufferedImage source = ImageIO.read(originalImage);
            int owidth = source.getWidth();
            int oheight = source.getHeight();
            double ratio = (double) owidth / oheight;

            if (w < 0 && h < 0) {
                w = owidth;
                h = oheight;
            }
            if (w < 0 && h > 0) {
                w = (int) (h * ratio);
            }
            if (w > 0 && h < 0) {
                h = (int) (w / ratio);
            }

            String mimeType = "image/jpeg";
            if (to.getName().endsWith(".png")) {
                mimeType = "image/png";
            }
            if (to.getName().endsWith(".gif")) {
                mimeType = "image/gif";
            }

            // out
            BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Image srcSized = source.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            Graphics graphics = dest.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, w, h);
            graphics.drawImage(srcSized, 0, 0, null);
            ImageWriter writer = ImageIO.getImageWritersByMIMEType(mimeType).next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            writer.setOutput(new FileImageOutputStream(to));
            IIOImage image = new IIOImage(dest, null, null);
            writer.write(null, image, params);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Crop an image
     * @param originalImage The image file
     * @param to The destination file
     * @param x1 The new x origin
     * @param y1 The new y origin
     * @param x2 The new x end
     * @param y2 The new y end
     */
    public static void crop(File originalImage, File to, Integer x1, Integer y1, Integer x2, Integer y2) {
        try {
            BufferedImage source = ImageIO.read(originalImage);

            String mimeType = "image/jpeg";
            if (to.getName().endsWith(".png")) {
                mimeType = "image/png";
            }
            if (to.getName().endsWith(".gif")) {
                mimeType = "image/gif";
            }
            int width = x2 - x1;
            int height = y2 - y1;

            // out
            BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Image croppedImage = source.getSubimage(x1, y1, width, height);
            Graphics graphics = dest.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(croppedImage, 0, 0, null);
            ImageWriter writer = ImageIO.getImageWritersByMIMEType(mimeType).next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            writer.setOutput(new FileImageOutputStream(to));
            IIOImage image = new IIOImage(dest, null, null);
            writer.write(null, image, params);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Encode an image to base64
     * @param image The image file
     * @return The base64 encoded value
     * @throws java.io.IOException
     */
    public static String toBase64(File image) throws IOException {
        return "data:" + MimeTypes.getMimeType(image.getName()) + ";base64," + Codec.encodeBASE64(IO.readContent(image));
    }
}
