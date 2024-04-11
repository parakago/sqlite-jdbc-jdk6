package org.sqlite;

public class NativeLibraryNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

	public NativeLibraryNotFoundException(String message) {
        super(message);
    }
}
