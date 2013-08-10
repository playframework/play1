package classloading;

import java.io.File;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.Play;
import play.classloading.BytecodeCache;
import play.test.UnitTest;

public class ByteCodeCacheTest  extends UnitTest {
	
	/**
	 * prove fix of #1689 - java.lang.OutOfMemoryError: Java heap space on zero byte/corrupted cache file
	 */
	
    @Test 
    public void getByteCode_emptyFile_expectOK() {
    
    	String cacheFileName= "abcd";
        File file = new File(Play.tmpDir, "bytecode/" + Play.mode.name() + "/"+cacheFileName);
        
        try{
        	if(file.exists()){
        		file.delete();
        	}
        	file.createNewFile();
        	// not no
        	byte[] code =  BytecodeCache.getBytecode(cacheFileName, "");
        	assertTrue(code == null);
        }catch(Throwable e){
        	assertFalse("exception received :" + e , true);
        }finally{
        	file.delete();
        }
    }
    
 }



