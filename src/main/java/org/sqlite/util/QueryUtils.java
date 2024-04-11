package org.sqlite.util;

import java.util.ArrayList;
import java.util.List;

public class QueryUtils {
    /**
     * Build a SQLite query using the VALUES clause to return arbitrary values.
     *
     * @param columns list of column names
     * @param valuesList values to return as rows
     * @return SQL query as string
     */
    public static String valuesQuery(List<String> columns, List<List<Object>> valuesList) {
    	for (List<Object> list : valuesList) {
    		if (list.size() != columns.size()) {
    			throw new IllegalArgumentException("values and columns must have the same size");
    		}
    	}
    	
    	List<String> rows = new ArrayList<String>();
    	for (List<Object> values : valuesList) {
    		List<String> items = new ArrayList<String>();
    		for (Object o : values) {
    			if (o instanceof String)
    				items.add("'" + o + "'");
    			else if (o == null)
    				items.add("null");
    			else
    				items.add(o.toString());
    		}
    		rows.add("(" + StringUtils.join(",", items) + ")");
    	}
    	
        return "with cte("
                + StringUtils.join(",", columns)
                + ") as (values "
                + StringUtils.join(",", rows)
                + ") select * from cte";
    }
}
