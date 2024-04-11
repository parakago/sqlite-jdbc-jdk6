package org.sqlite.util;

public final class Convert {
	// temporary
	public static int[] toIntArray(long[] source) {
		int[] target = new int[source.length];
    	for (int x = 0; x < source.length; ++x) {
    		target[x] = (int) source[x];
    	}
    	return target;
	}
	
	public static int parseUnsignedInt(String s, int radix) throws NumberFormatException {
		if (s == null)  {
			throw new NumberFormatException("null");
		}
		
		int len = s.length();
		if (len > 0) {
			char firstChar = s.charAt(0);
			if (firstChar == '-') {
				throw new
				NumberFormatException(String.format("Illegal leading minus sign " +
						"on unsigned string %s.", s));
			} else {
				if (len <= 5 || // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
						(radix == 10 && len <= 9) ) { // Integer.MAX_VALUE in base 10 is 10 digits
					return Integer.parseInt(s, radix);
				} else {
					long ell = Long.parseLong(s, radix);
					if ((ell & 0xffffffff00000000L) == 0) {
						return (int) ell;
					} else {
						throw new
						NumberFormatException(String.format("String value %s exceeds " +
								"range of unsigned int.", s));
					}
				}
			}
		} else {
			throw new NumberFormatException("For input string: \"" + s + "\"");
		}
	}

	/**
	 * Parses the string argument as an unsigned decimal integer. The
	 * characters in the string must all be decimal digits, except
	 * that the first character may be an an ASCII plus sign {@code
	 * '+'} ({@code '\u005Cu002B'}). The resulting integer value
	 * is returned, exactly as if the argument and the radix 10 were
	 * given as arguments to the {@link
	 * #parseUnsignedInt(java.lang.String, int)} method.
	 *
	 * @param s   a {@code String} containing the unsigned {@code int}
	 *            representation to be parsed
	 * @return    the unsigned integer value represented by the argument in decimal.
	 * @throws    NumberFormatException  if the string does not contain a
	 *            parsable unsigned integer.
	 * @since 1.8
	 */
	public static int parseUnsignedInt(String s) throws NumberFormatException {
		return parseUnsignedInt(s, 10);
	}
}
