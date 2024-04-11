package mock.java.nio.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class Files {
	private static final int BUFFER_SIZE = 8192;
	
	public static boolean exists(Path path) {
		return path.toFile().exists();
	}
	
	public static boolean notExists(Path path) {
		return !path.toFile().exists();
	}

	public static Path createFile(Path path) throws IOException {
		File file = path.toFile();
		if (file.exists()) {
			throw new FileAlreadyExistsException(path.toString());
		}

		file.mkdirs();

		return path;
	}

	public static boolean deleteIfExists(Path path) throws IOException {
		File file = path.toFile();
		if (file.isDirectory()) {
			if (file.list().length > 0) {
				throw new DirectoryNotEmptyException(path.toString());
			}
		}

		if (file.exists()) {
			return file.delete();
		}

		return false;
	}
	
	private static void prepareCopy(Path target, StandardCopyOption... options) throws IOException {
		boolean replaceExisting = false;
		for (StandardCopyOption option : options) {
			if (option == StandardCopyOption.REPLACE_EXISTING) {
				replaceExisting = true;
			}
		}

		if (replaceExisting)
			Files.deleteIfExists(target);
		else if (exists(target))
			throw new FileAlreadyExistsException(target.toString());
	}
	
	private static long copy(InputStream source, OutputStream sink) throws IOException {
		long nread = 0L;
		byte[] buf = new byte[BUFFER_SIZE];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
			nread += n;
		}
		return nread;
	}
	
	public static long copy(InputStream source, Path target, StandardCopyOption... options) throws IOException {
		prepareCopy(target, options);
		
		OutputStream os = null;
	    try {
	    	os = new FileOutputStream(target.toFile());
	    	return copy(source, os);
	    } finally {
	        closeQuietly(os);
	    }
	}
	
	public static Path copy(Path source, Path target, StandardCopyOption... options) throws IOException {
		prepareCopy(target, options);

		if (source.toFile().isDirectory()) {
			target.toFile().mkdirs();
		} else {
			FileInputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(source.toFile());
				os = new FileOutputStream(target.toFile());
				
				FileChannel isc = null;
				FileChannel osc = null;
				try {
					isc = is.getChannel();
					osc = os.getChannel();
					
					osc.transferFrom(isc, 0, isc.size());
				} finally {
					closeQuietly(osc);
					closeQuietly(isc);
				}
			} finally {
				closeQuietly(os);
				closeQuietly(is);
			}
		}

		return target;
	}
	
	private static void closeQuietly(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();
			} catch (IOException e) {
				// do nothing
			}
		}
	}
}
