package org.sqlite.util;

import java.util.List;

import mock.java.util.Objects;
import mock.java.util.StringJoiner;

public class StringUtils {
    public static String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list) {
            if (first) first = false;
            else sb.append(separator);

            sb.append(item);
        }
        return sb.toString();
    }
    
	public static String join(CharSequence delimiter, CharSequence... elements) {
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		// Number of elements not likely worth Arrays.stream overhead.
		StringJoiner joiner = new StringJoiner(delimiter);
		for (CharSequence cs: elements) {
			joiner.add(cs);
		}
		return joiner.toString();
	}
	
	public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringJoiner joiner = new StringJoiner(delimiter);
		for (CharSequence cs: elements) {
			joiner.add(cs);
		}
		return joiner.toString();
	}
}
