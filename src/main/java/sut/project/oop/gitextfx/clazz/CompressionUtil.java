package sut.project.oop.gitextfx.clazz;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CompressionUtil {
    public static byte[] compress(String content) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        var gzip = new GZIPOutputStream(outputStream);

        var data = content.getBytes(StandardCharsets.UTF_8);

        gzip.write(data);
        gzip.close();

        return outputStream.toByteArray();
    }

    public static String decompressed(byte[] compressed) throws IOException {
        var inputStream = new ByteArrayInputStream(compressed);
        var outputStream = new ByteArrayOutputStream();

        GZIPInputStream gzip = new GZIPInputStream(inputStream);
        byte[] buffer = new byte[4096]; // 4 KB
        int len;

        while ((len = gzip.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }

        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
