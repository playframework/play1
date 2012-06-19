package play.server;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Useless channel buffer only used to wrap the input stream....
 */
public class FileChannelBuffer extends InputStreamChannelBuffer {
	
	/**
	 * Classe permettant d'effacer le fichier temporaire en fin de lecture
	 * @author Nicolas DOUILLET
	 */
	static class TmpFileInputStream extends FileInputStream {
		File file;
		
		public TmpFileInputStream(File file) throws FileNotFoundException {
			super(file);
			this.file = file;
		}

		@Override
		public void close() throws IOException {
			super.close();
			try {
				file.delete();
			} catch(Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
// --------------------------->

    private final FileInputStream is;
    
// --------------------------->

    public FileChannelBuffer(File file) {
        this(file, false);
    }
    
    public FileChannelBuffer(File file, boolean deleteOnClose) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        try {
        	// maintain the previous FileInputStream in case of sombedody was using it !!
            this.is = deleteOnClose ? new TmpFileInputStream(file) : new FileInputStream(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

// --------------------------->

    public InputStream getInputStream() {
        return is;
    }
}
