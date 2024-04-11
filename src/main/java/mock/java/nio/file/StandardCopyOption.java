package mock.java.nio.file;

public enum StandardCopyOption {
    /**
     * Replace an existing file if it exists.
     */
    REPLACE_EXISTING,
    /**
     * Copy attributes to the new file.
     */
    COPY_ATTRIBUTES,
    /**
     * Move the file as an atomic file system operation.
     */
    ATOMIC_MOVE;
}
