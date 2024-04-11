package mock.java.nio.file;

import java.io.IOException;

public class FileSystemException extends IOException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     *  String identifying the file or {@code null} if not known.
     */
	private final String file;
	
	/**
     *  String identifying the other file or {@code null} if there isn't
     *  another file or if not known.
     */
    private final String other;
    
	public FileSystemException(String file) {
        super((String)null);
        this.file = file;
        this.other = null;
    }
	
	public FileSystemException(String file, String other, String reason) {
        super(reason);
        this.file = file;
        this.other = other;
    }
	
	/**
     * Returns the file used to create this exception.
     *
     * @return  the file (can be {@code null})
     */
    public String getFile() {
        return file;
    }

    /**
     * Returns the other file used to create this exception.
     *
     * @return  the other file (can be {@code null})
     */
    public String getOtherFile() {
        return other;
    }

    /**
     * Returns the string explaining why the file system operation failed.
     *
     * @return  the string explaining why the file system operation failed
     */
    public String getReason() {
        return super.getMessage();
    }
    
    /**
     * Returns the detail message string.
     */
    @Override
    public String getMessage() {
        if (file == null && other == null)
            return getReason();
        StringBuilder sb = new StringBuilder();
        if (file != null)
            sb.append(file);
        if (other != null) {
            sb.append(" -> ");
            sb.append(other);
        }
        if (getReason() != null) {
            sb.append(": ");
            sb.append(getReason());
        }
        return sb.toString();
    }
}
