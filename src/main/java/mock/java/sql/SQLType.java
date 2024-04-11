package mock.java.sql;

public interface SQLType {

    /**
     * Returns the {@code SQLType} name that represents a SQL data type.
     *
     * @return The name of this {@code SQLType}.
     */
    String getName();

    /**
     * Returns the name of the vendor that supports this data type. The value
     * returned typically is the package name for this vendor.
     *
     * @return The name of the vendor for this data type
     */
    String getVendor();

    /**
     * Returns the vendor specific type number for the data type.
     *
     * @return An Integer representing the vendor specific data type
     */
    Integer getVendorTypeNumber();
}