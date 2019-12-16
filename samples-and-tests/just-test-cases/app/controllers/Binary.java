package controllers;

import models.UserWithAvatar;
import play.data.Upload;
import play.mvc.Controller;
import play.mvc.Http;
import play.test.Fixtures;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Binary extends Controller {
    
    public static void deleteAll(){ // see Bug #491403
        Fixtures.deleteAll();
    }

    public static void index() {
        render();
    }

    public static void save(UserWithAvatar user) {
        user.create();
        show(user.id);
    }

    public static void show(Long id) {
        UserWithAvatar user = UserWithAvatar.findById(id);
        render(user);
    }

    public static void showAvatar(Long id) {
        UserWithAvatar user = UserWithAvatar.findById(id);
        if (user != null && user.avatar.exists()) {
            renderBinary(user.avatar.get());
        }
        notFound();
    }

    public static void uploadFile(File file) {
    	if(file != null){
    		Http.Response.current().headers.put("Content-Length", new Http.Header("Content-Length", String.valueOf(file.length())));
    		renderBinary(file);
    	}else{
    		renderText("File is null");
    	}
    }

    public static void upload(Upload upload) {
    	if(upload != null){
	        Http.Response.current().headers.put("Content-Length", new Http.Header("Content-Length", String.valueOf(upload.asBytes().length)));
	        renderBinary(upload.asFile());
        }else{
    		renderText("Upload is null");
        }
    }

    public static void uploadMultipleFiles(List<File> files) {
        Http.Response.current().headers.put("Content-Length", new Http.Header("Content-Length", String.valueOf(files.get(1).length())));
        renderBinary(files.get(1));
    }
    
    public static void getBinaryWithCustomContentType() throws Exception{
        InputStream inStream = new ByteArrayInputStream( "hello".getBytes("utf-8"));
        renderBinary(inStream, "filename.customContentType");
    }

    // Tests to check whether input streams to renderBinary get closed.

    // Test 1: Simulated empty input stream

    public static boolean emptyInputStreamClosed = true;

    private static class EmptyInputStream extends InputStream {
        public EmptyInputStream() {
        }

        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return -1;
        }

        @Override
        public long skip(long n) throws IOException {
            return 0L;
        }

        @Override
        public void close() throws IOException {
            emptyInputStreamClosed = true;
        }
    }

    public static void getEmptyBinary() {
        emptyInputStreamClosed = false;
        renderBinary(new EmptyInputStream(), "empty");
    }

    // Test 2: Simulated error input stream

    public static boolean errorInputStreamClosed = true;

    private static class ErrorInputStream extends InputStream {
        public ErrorInputStream() {
        }

        @Override
        public int read() throws IOException {
            throw new IOException("io ups");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("io failed");
        }

        @Override
        public long skip(long n) throws IOException {
            throw new IOException();
        }

        @Override
        public void close() {
            errorInputStreamClosed = true;
        }
    }

    public static void getErrorBinary() {
        errorInputStreamClosed = false;
        renderBinary(new ErrorInputStream(), "error");
    }
}
