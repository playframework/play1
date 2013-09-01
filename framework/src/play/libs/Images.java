package play.libs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import jj.play.ns.nl.captcha.backgrounds.BackgroundProducer;
import jj.play.ns.nl.captcha.backgrounds.FlatColorBackgroundProducer;
import jj.play.ns.nl.captcha.backgrounds.GradiatedBackgroundProducer;
import jj.play.ns.nl.captcha.backgrounds.SquigglesBackgroundProducer;
import jj.play.ns.nl.captcha.backgrounds.TransparentBackgroundProducer;
import jj.play.ns.nl.captcha.gimpy.GimpyRenderer;
import jj.play.ns.nl.captcha.gimpy.RippleGimpyRenderer;
import jj.play.ns.nl.captcha.noise.CurvedLineNoiseProducer;
import jj.play.ns.nl.captcha.text.renderer.DefaultWordRenderer;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Response;

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
    public static void resize(File originalImage, File to, int w, int h) {
      resize(originalImage, to, w, h, false);
    }
    
    /**
     * Resize an image
     * @param originalImage The image file
     * @param to The destination file
     * @param w The new width (or -1 to proportionally resize) or the maxWidth if keepRatio is true
     * @param h The new height (or -1 to proportionally resize) or the maxHeight if keepRatio is true
     * @param keepRatio : if true, resize will keep the original image ratio and use w and h as max dimensions
     */
    public static void resize(File originalImage, File to, int w, int h, boolean keepRatio) {
        try {
            BufferedImage source = ImageIO.read(originalImage);
            int owidth = source.getWidth();
            int oheight = source.getHeight();
            double ratio = (double) owidth / oheight;
            
            int maxWidth = w;
            int maxHeight = h;
            
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
            
            if(keepRatio) {
                h = (int) (w / ratio);
                if(h > maxHeight) {
                    h = maxHeight;
                    w = (int) (h * ratio);
                }
                if(w > maxWidth) {
                    w = maxWidth;
                    h = (int) (w / ratio);
                }
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
            FileImageOutputStream toFs = new FileImageOutputStream(to);
            writer.setOutput(toFs);
            IIOImage image = new IIOImage(dest, null, null);
            writer.write(null, image, params);
            toFs.flush();
            toFs.close();
            writer.dispose();
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
    public static void crop(File originalImage, File to, int x1, int y1, int x2, int y2) {
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
            writer.dispose();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Encode an image to base64 using a data: URI
     * @param image The image file
     * @return The base64 encoded value
     * @throws java.io.IOException
     */
    public static String toBase64(File image) throws IOException {
        return "data:" + MimeTypes.getMimeType(image.getName()) + ";base64," + Codec.encodeBASE64(IO.readContent(image));
    }

    /**
     * Create a captche image
     */
    public static Captcha captcha(int width, int height) {
        return new Captcha(width, height);
    }

    /**
     * Create a 150x150 captcha image
     */
    public static Captcha captcha() {
        return captcha(150, 50);
    }

    /**
     * A captcha image.
     */
    public static class Captcha extends InputStream {

        public String text = null;
        public BackgroundProducer background = new TransparentBackgroundProducer();
        public GimpyRenderer gimpy = new RippleGimpyRenderer();
        public Color textColor = Color.BLACK;
        public List<Font> fonts = new ArrayList<Font>(2);
        public int w, h;
        public Color noise = null;

        public Captcha(int w, int h) {
            this.w = w;
            this.h = h;
            this.fonts.add(new Font("Arial", Font.BOLD, 40));
            this.fonts.add(new Font("Courier", Font.BOLD, 40));
        }

        /**
         * Tell the captche to draw a text and retrieve it
         */
        public String getText() {
            return getText(5);
        }

        /**
         * Tell the captche to draw a text using the specified color (ex. #000000) and retrieve it
         */
        public String getText(String color) {
            this.textColor = Color.decode(color);
            return getText();
        }

        /**
         * Tell the captche to draw a text of the specified size and retrieve it
         */
        public String getText(int length) {
            return getText(length, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789");
        }

        /**
         * Tell the captche to draw a text of the specified size using the specified color (ex. #000000) and retrieve it
         */
        public String getText(String color, int length) {
            this.textColor = Color.decode(color);
            return getText(length);
        }

        public String getText(int length, String chars) {
            char[] charsArray = chars.toCharArray();
            Random random = new Random(System.currentTimeMillis());
            StringBuffer sb = new StringBuffer(length);
            for (int i = 0; i < length; i++) {
                sb.append(charsArray[random.nextInt(charsArray.length)]);
            }
            text = sb.toString();
            return text;
        }

        public String getText(String color, int length, String chars) {
            this.textColor = Color.decode(color);
            return getText(length, chars);
        }

        /**
         * Add noise to the captcha.
         */
        public Captcha addNoise() {
            noise = Color.BLACK;
            return this;
        }

        /**
         * Add noise to the captcha.
         */
        public Captcha addNoise(String color) {
            noise = Color.decode(color);
            return this;
        }

        /**
         * Set a gradient background.
         */
        public Captcha setBackground(String from, String to) {
            GradiatedBackgroundProducer bg = new GradiatedBackgroundProducer();
            bg.setFromColor(Color.decode(from));
            bg.setToColor(Color.decode(to));
            background = bg;
            return this;
        }

        /**
         * Set a solid background.
         */
        public Captcha setBackground(String color) {
            background = new FlatColorBackgroundProducer(Color.decode(color));
            return this;
        }

        /**
         * Set a squiggles background
         */
        public Captcha setSquigglesBackground() {
            background = new SquigglesBackgroundProducer();
            return this;
        }        // ~~ rendering
        ByteArrayInputStream bais = null;

        @Override
        public int read() throws IOException {
            check();
            return bais.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            check();
            return bais.read(b);
        }

        void check() {
            try {
                if (bais == null) {
                    if (text == null) {
                        text = getText();
                    }
                    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    DefaultWordRenderer renderer = new DefaultWordRenderer(textColor, fonts);
                    bi = background.addBackground(bi);
                    renderer.render(text, bi);
                    if (noise != null) {
                        new CurvedLineNoiseProducer(noise, 3.0f).makeNoise(bi);
                    }
                    gimpy.gimp(bi);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bi, "png", baos);
                    bais = new ByteArrayInputStream(baos.toByteArray());
                    //
                    Response.current().contentType = "image/png";
                }
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        }
    }
}
