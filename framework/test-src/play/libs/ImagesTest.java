package play.libs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import play.Play;
import play.PlayBuilder;

/**
 * @author Alexandre Chatiron
 */
public class ImagesTest {

    @Test
    public void testImagesResizeGif() {
        new PlayBuilder().build();
        File folder = new File(Play.frameworkPath, "/framework/test-src/play/libs");

        File source = new File(folder, "angel.gif");
        File result = new File(folder, "angel_testResult.gif");

        Images.resize(source, result, 48, 48);
        try {
            BufferedImage buffSrc = ImageIO.read(source);
            BufferedImage buffDest = ImageIO.read(result);

            assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
            assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
            assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
            assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
        } catch (IOException e) {
            fail("cannot compare the 2 images");
        } finally {
            result.delete();
        }
    }

    @Test
    public void testImagesResizePng() {
        new PlayBuilder().build();
        File folder = new File(Play.frameworkPath, "/framework/test-src/play/libs");

        File source = new File(folder, "fond1.png");
        File result = new File(source.getParent(), "fond1_testResult.png");

        Images.resize(source, result, 278, 519);
        try {
            BufferedImage buffSrc = ImageIO.read(source);
            BufferedImage buffDest = ImageIO.read(result);

            assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
            assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
            assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
            assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
        } catch (IOException e) {
            fail("cannot compare the 2 images");
        } finally {
            result.delete();
        }
    }

    @Test
    public void testImagesResizePngTransparent() {
        new PlayBuilder().build();
        File folder = new File(Play.frameworkPath, "/framework/test-src/play/libs");

        File source = new File(folder, "fond2.png");
        File result = new File(source.getParent(), "fond2_testResult.png");

        Images.resize(source, result, 278, 519);
        try {
            BufferedImage buffSrc = ImageIO.read(source);
            BufferedImage buffDest = ImageIO.read(result);

            assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
            assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
            assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
            assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
        } catch (IOException e) {
            fail("cannot compare the 2 images");
        } finally {
            result.delete();
        }
    }

    @Test
    public void testImagesResizeJpg() {
        new PlayBuilder().build();
        File folder = new File(Play.frameworkPath, "/framework/test-src/play/libs");

        File source = new File(folder, "winie.jpg");
        File result = new File(source.getParent(), "winie_testResult.jpg");

        Images.resize(source, result, 1536, 2048);
        try {
            BufferedImage buffSrc = ImageIO.read(source);
            BufferedImage buffDest = ImageIO.read(result);

            assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
            assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
            assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
            assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
        } catch (IOException e) {
            fail("cannot compare the 2 images");
        } finally {
            result.delete();
        }
    }

}
