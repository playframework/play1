package play.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedInput;

import play.Logger;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

public class FileService  {

    public static void serve(File localFile, HttpRequest nettyRequest, HttpResponse nettyResponse, ChannelHandlerContext ctx, Request request, Response response, Channel channel) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        try {
            long fileLength = raf.length();
            
            boolean isKeepAlive = HttpHeaders.isKeepAlive(nettyRequest) && nettyRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_1);
            
            if(Logger.isTraceEnabled()) {
                Logger.trace("keep alive %s", String.valueOf(isKeepAlive));
                Logger.trace("content type %s", (response.contentType != null ? response.contentType : MimeTypes.getContentType(localFile.getName(), "text/plain")));
            }
            
            if (!nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                // Add 'Content-Length' header only for a keep-alive connection.
                if(Logger.isTraceEnabled()){
                    Logger.trace("file length " + fileLength);
                }
                nettyResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(fileLength));
            }

            if (response.contentType != null) {
                nettyResponse.headers().set(CONTENT_TYPE, response.contentType);
            } else {
                nettyResponse.headers().set(CONTENT_TYPE, (MimeTypes.getContentType(localFile.getName(), "text/plain")));
            }

            nettyResponse.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);

            // Write the initial line and the header.
            ChannelFuture writeFuture = null;

            // Write the content.
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD)) {
                ChunkedInput chunkedInput = getChunckedInput(raf, MimeTypes.getContentType(localFile.getName(), "text/plain"), channel, nettyRequest, nettyResponse);
                if (channel.isOpen()) {
                    channel.write(nettyResponse);
                    writeFuture = channel.write(chunkedInput);
                }else{
                    Logger.debug("Try to write on a closed channel[keepAlive:%s]: Remote host may have closed the connection", String.valueOf(isKeepAlive)); 
                }
            } else {
                if (channel.isOpen()) {
                    writeFuture = channel.write(nettyResponse);
                }else{
                    Logger.debug("Try to write on a closed channel[keepAlive:%s]: Remote host may have closed the connection", String.valueOf(isKeepAlive)); 
                }
                raf.close();
            }

            if (writeFuture != null && !isKeepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Throwable exx) {
            exx.printStackTrace();
            closeQuietly(raf);
            try {
                if (ctx.getChannel().isOpen()) {
                    ctx.getChannel().close();
                }
            } catch (Throwable ex) { /* Left empty */ }
        }
    }
    
    public static ChunkedInput getChunckedInput(RandomAccessFile raf, String contentType, Channel channel, HttpRequest nettyRequest, HttpResponse nettyResponse) throws IOException {
        if(ByteRangeInput.accepts(nettyRequest)) {
            ByteRangeInput server = new ByteRangeInput(raf, contentType, nettyRequest);
            server.prepareNettyResponse(nettyResponse);
            return server;
        } else {
            return new ChunkedFile(raf);
        }
    }
    
    public static class ByteRangeInput implements ChunkedInput{
        RandomAccessFile raf;
        HttpRequest request;
        int chunkSize = 8096;
        ByteRange[] byteRanges;
        int currentByteRange = 0;
        String contentType;
        
        boolean unsatisfiable = false;
        
        long fileLength;
        
        public ByteRangeInput(File file, String contentType, HttpRequest request) throws FileNotFoundException, IOException {
            this(new RandomAccessFile(file, "r"), contentType, request);
        }
        
        public ByteRangeInput(RandomAccessFile raf, String contentType, HttpRequest request) throws FileNotFoundException, IOException {
            this.raf = raf;
            this.request = request;
            fileLength = raf.length();
            this.contentType = contentType;
            initRanges();
            if (Logger.isDebugEnabled()) {
                Logger.debug("Invoked ByteRangeServer, found byteRanges: %s (with header Range: %s)",
                        Arrays.toString(byteRanges), request.headers().get("range"));
            }
        }
        
        public void prepareNettyResponse(HttpResponse nettyResponse) {
            nettyResponse.headers().add("Accept-Ranges", "bytes");
            if(unsatisfiable) {
                nettyResponse.setStatus(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                nettyResponse.headers().set("Content-Range", "bytes " + 0 + "-" + (fileLength-1) + "/" + fileLength);
                nettyResponse.headers().set("Content-length", 0);
            } else {
                nettyResponse.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
                if(byteRanges.length == 1) {
                    ByteRange range = byteRanges[0];
                    nettyResponse.headers().set("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileLength);
                } else {
                    nettyResponse.headers().set("Content-type", "multipart/byteranges; boundary="+DEFAULT_SEPARATOR);
                }
                long length = 0;
                for(ByteRange range: byteRanges) {
                    length += range.computeTotalLength();
                }
                nettyResponse.headers().set("Content-length", length);
            }
        }
        
        @Override
        public Object nextChunk() throws Exception {
            if(Logger.isTraceEnabled())
                Logger.trace("FileService nextChunk");
            try {
                int count = 0;
                byte[] buffer = new byte[chunkSize];
                while(count < chunkSize && currentByteRange < byteRanges.length && byteRanges[currentByteRange] != null) {
                    if(byteRanges[currentByteRange].remaining() > 0) {
                        count += byteRanges[currentByteRange].fill(buffer, count);
                    } else {
                        currentByteRange++;
                    }
                }
                if(count == 0){
                    return null;
                }
                
                return wrappedBuffer(buffer);
            } catch (Exception e) {
                Logger.error(e, "error sending file");
                throw e;
            }
        }
        
        @Override
        public boolean hasNextChunk() throws Exception {
            if(Logger.isTraceEnabled())
                Logger.trace("FileService hasNextChunk() : " + (currentByteRange < byteRanges.length && byteRanges[currentByteRange].remaining() > 0));
            return currentByteRange < byteRanges.length && byteRanges[currentByteRange].remaining() > 0;
        }
        
        @Override
        public boolean isEndOfInput() throws Exception {
            return !hasNextChunk();
        }
        
        @Override
        public void close() throws Exception {
            raf.close();
        }
        
        public static boolean accepts(HttpRequest request) {
            return request.headers().contains("range");
        }
        
        private void initRanges() {
            try {
                String headerValue = request.headers().get("range").trim().substring("bytes=".length());
                String[] rangesValues = headerValue.split(",");
                ArrayList<long[]> ranges = new ArrayList<>(rangesValues.length);
                for(int i = 0; i < rangesValues.length; i++) {
                    String rangeValue = rangesValues[i];
                    long start, end;
                    if(rangeValue.startsWith("-")) {
                        end = fileLength - 1;
                        start = fileLength - 1 - Long.parseLong(rangeValue.substring("-".length()));
                    } else {
                        String[] range = rangeValue.split("-");
                        start = Long.parseLong(range[0]);
                        end = range.length > 1 ? Long.parseLong(range[1]) : fileLength - 1;
                    }
                    if (end > fileLength - 1) {
                        end = fileLength - 1;
                    }
                    if(start <= end){
                        ranges.add(new long[] { start, end });
                    }
                }
                long[][] reducedRanges = reduceRanges(ranges.toArray(new long[0][]));
                ByteRange[] byteRanges = new ByteRange[reducedRanges.length];
                for(int i = 0; i < reducedRanges.length; i++) {
                    long[] range = reducedRanges[i];
                    byteRanges[i] = new ByteRange(range[0], range[1], fileLength, contentType, reducedRanges.length > 1);
                }
                this.byteRanges = byteRanges;
                if(this.byteRanges.length == 0){
                    unsatisfiable = true;
                }
            } catch (Exception e) {
                if(Logger.isDebugEnabled())
                    Logger.debug(e, "byterange error");
                unsatisfiable = true;
            }
        }
        
        private static boolean rangesIntersect(long[] r1, long[] r2) {
            return r1[0] >= r2[0] && r1[0] <= r2[1] || r1[1] >= r2[0]
                    && r1[0] <= r2[1];
        }

        private static long[] mergeRanges(long[] r1, long[] r2) {
            return new long[] { r1[0] < r2[0] ? r1[0] : r2[0],
                    r1[1] > r2[1] ? r1[1] : r2[1] };
        }

        private static long[][] reduceRanges(long[]... chunks) {
            if (chunks.length == 0)
                return new long[0][];
            long[][] sortedChunks = Arrays.copyOf(chunks, chunks.length);
            Arrays.sort(sortedChunks, new Comparator<long[]>() {
                @Override
                public int compare(long[] t1, long[] t2) {
                    return new Long(t1[0]).compareTo(t2[0]);
                }
            });
            ArrayList<long[]> result = new ArrayList<>();
            result.add(sortedChunks[0]);
            for (int i = 1; i < sortedChunks.length; i++) {
                long[] c1 = sortedChunks[i];
                long[] r1 = result.get(result.size() - 1);
                if (rangesIntersect(c1, r1)) {
                    result.set(result.size() - 1, mergeRanges(c1, r1));
                } else {
                    result.add(c1);
                }
            }
            return result.toArray(new long[0][]);
        }
        
        private static String makeRangeBodyHeader(String separator, String contentType, long start, long end, long fileLength) {
            return  "--" + separator + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "ContentRange: bytes " + start + "-" + end + "/" + fileLength + "\r\n" +
                    "\r\n";
        }
        
        private class ByteRange {
            public long start;
            public long end;
            public byte[] header;
            
            public long length() {
                return end - start + 1;
            }
            public long remaining() {
                return end - start + 1 - servedRange;
            }
            
            public long computeTotalLength() {
                return length() + header.length;
            }
            
            public int servedHeader = 0;
            public int servedRange = 0;
            
            public ByteRange(long start, long end, long fileLength, String contentType, boolean includeHeader) {
                this.start = start;
                this.end = end;
                if(includeHeader) {
                    header = makeRangeBodyHeader(DEFAULT_SEPARATOR, contentType, start, end, fileLength).getBytes();
                } else {
                    header = new byte[0];
                }
            }
            
            public int fill(byte[] into, int offset) throws IOException {
                if(Logger.isTraceEnabled())
                    Logger.trace("FileService fill at " + offset);
                int count = 0;
                for(; offset < into.length && servedHeader < header.length; offset++, servedHeader++, count++) {
                    into[offset] = header[servedHeader];
                }
                if(offset < into.length) {
                    try {
                        raf.seek(start + servedRange);
                        long maxToRead = remaining() > (into.length - offset) ? (into.length - offset) : remaining();
                        if(maxToRead > Integer.MAX_VALUE) {
                            if(Logger.isDebugEnabled())
                                Logger.debug("FileService: maxToRead >= 2^32 !");
                            maxToRead = Integer.MAX_VALUE;
                        }
                        int read = raf.read(into, offset, (int) maxToRead);
                        if(read < 0) {
                            throw new UnexpectedException("error while reading file : no more to read ! length=" + raf.length() + ", seek=" + (start + servedRange));
                        }
                        count += read;
                        servedRange += read;
                    } catch(IOException e) {
                        throw new UnexpectedException(e);
                    }
                }
                return count;
            }
            
            @Override
            public String toString() {
                return "ByteRange("+start+","+end+")";
            }
        }
        
        private static final String DEFAULT_SEPARATOR = "$$$THIS_STRING_SEPARATES$$$";
    }
}
