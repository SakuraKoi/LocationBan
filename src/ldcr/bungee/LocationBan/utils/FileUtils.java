package ldcr.bungee.LocationBan.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static String readFile(final File file) throws IOException {
	final FileInputStream fin = new FileInputStream(file);
	return readFile(fin);
    }

    public static String readFile(final InputStream stream) throws IOException {
	final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	final byte[] buffer = new byte[4096];
	int bytesRead;
	while ((bytesRead = stream.read(buffer)) >= 0) {
	    outStream.write(buffer, 0, bytesRead);
	}
	stream.close();
	return outStream.toString();
    }

    public static void copyFile(final File from, final File to)
	    throws IOException {
	final FileInputStream inStream = new FileInputStream(from);
	final FileOutputStream outStream = new FileOutputStream(to);
	final byte[] buffer = new byte[4096];
	int bytesRead;
	while ((bytesRead = inStream.read(buffer)) >= 0) {
	    outStream.write(buffer, 0, bytesRead);
	}
	outStream.flush();
	inStream.close();
	outStream.close();
    }

    public static void writeFile(final File file, final String data)
	    throws IOException {
	if (!file.exists()) {
	    file.createNewFile();
	}
	final FileWriter writer = new FileWriter(file);
	writer.write(data);
	writer.flush();
	writer.close();
    }

    public static void writeFile(final File configFile, final InputStream stream)
	    throws IOException {
	writeFile(configFile, readFile(stream));
    }
    public static File getFileIgnoreCase(final File path,final String name) {
	if (path==null) return null;
	if (!path.exists()) return null;
	final String lowName = name.replace('-', ' ');
	for (final File file : path.listFiles()) {
	    if (file.getName().replace('-', ' ').equalsIgnoreCase(lowName)) return file;
	}
	return new File(path,name);
    }

    public static byte[] readFileByte(final File file) throws IOException {
	final FileInputStream stream = new FileInputStream(file);
	final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	final byte[] buffer = new byte[4096];
	int bytesRead;
	while ((bytesRead = stream.read(buffer)) >= 0) {
	    outStream.write(buffer, 0, bytesRead);
	}
	stream.close();
	return outStream.toByteArray();
    }
    public static void writeFileByte(final File file, final byte[] data)
	    throws IOException {
	if (!file.exists()) {
	    file.createNewFile();
	}
	final FileOutputStream out = new FileOutputStream(file);
	out.write(data);;
	out.flush();
	out.close();
    }
}
