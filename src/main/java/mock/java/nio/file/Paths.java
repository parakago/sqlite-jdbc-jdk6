package mock.java.nio.file;

import java.io.File;

public final class Paths {
	public static Path get(String first, String... more) {
		File target = new File(first);
		
		for (int i = 0; i < more.length; ++i) {
			target = new File(target, more[i]);
		}
		
		return new PathImpl(target);
	}
	
	public static Path get(File file) {
		return new PathImpl(file);
	}
	
	static class PathImpl implements Path {
		private final File file;
		public PathImpl(File file) {
			this.file = file;
		}
		
		@Override
		public File toFile() {
			return file;
		}
		
		@Override
		public String toString() {
			return file.toString();
		}
	}
}
