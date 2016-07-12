package play.libs;

import org.junit.After;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandre Chatiron
 */
public class ImagesTest {

    File result;

    @Test
    public void testImagesResizeGif() throws IOException, URISyntaxException {
        File source = find("angel.gif");
        result = File.createTempFile("play", "angel.gif");

        Images.resize(source, result, 48, 48);
        BufferedImage buffSrc = ImageIO.read(source);
        BufferedImage buffDest = ImageIO.read(result);

        assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
        assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
        assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
        assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
    }

    @Test
    public void testImagesResizePng() throws IOException, URISyntaxException {
        File source = find("fond1.png");
        result = File.createTempFile("play", "fond1.png");

        Images.resize(source, result, 278, 519);
        BufferedImage buffSrc = ImageIO.read(source);
        BufferedImage buffDest = ImageIO.read(result);

        assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
        assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
        assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
        assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
    }

    @Test
    public void testImagesResizePngTransparent() throws IOException, URISyntaxException {
        File source = find("fond2.png");
        result = File.createTempFile("play", "fond2.png");

        Images.resize(source, result, 278, 519);
        BufferedImage buffSrc = ImageIO.read(source);
        BufferedImage buffDest = ImageIO.read(result);

        assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
        assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
        assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
        assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
    }

    @Test
    public void testImagesResizeJpg() throws IOException, URISyntaxException {
        File source = find("winie.jpg");
        result = File.createTempFile("play", "winie.jpg");

        Images.resize(source, result, 1536, 2048);
        BufferedImage buffSrc = ImageIO.read(source);
        BufferedImage buffDest = ImageIO.read(result);

        assertEquals(buffSrc.getColorModel().hasAlpha(), buffDest.getColorModel().hasAlpha());
        assertEquals(buffSrc.getColorModel().getPixelSize(), buffDest.getColorModel().getPixelSize());
        assertEquals(buffSrc.getColorModel().getTransferType(), buffDest.getColorModel().getTransferType());
        assertEquals(buffSrc.getColorModel().getTransparency(), buffDest.getColorModel().getTransparency());
    }

    private File find(String name) throws URISyntaxException {
        return new File(getClass().getResource(name).toURI());
    }

    @After
    public void tearDown() {
        result.delete();
    }
}
