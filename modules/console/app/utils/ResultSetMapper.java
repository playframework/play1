package utils;

import java.util.*;
import java.sql.*;

public class ResultSetMapper {
/**
* Helper method that converts a ResultSet into a list of maps, one per row
* @param query ResultSet
* @return list of maps, one per row, with column name as the key
* @throws SQLException if the connection fails
*/
public static final List<Map<String,Object>>  toList(ResultSet rs) throws SQLException {
	List wantedColumnNames = ResultSetMapper.getColumnNames(rs);
	return ResultSetMapper.toList(rs, wantedColumnNames);
}
/**
* Helper method that maps a ResultSet into a list of maps, one per row
* @param query ResultSet
* @param list of columns names to include in the result map
* @return list of maps, one per column row, with column names as keys
* @throws SQLException if the connection fails
*/
public static final List<Map<String,Object>> toList(ResultSet rs, List wantedColumnNames) throws SQLException
{
	List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();

	int numWantedColumns = wantedColumnNames.size();
	while (rs.next()) {
		Map<String,Object> row = new TreeMap<String,Object>();

		for (int i = 0; i < numWantedColumns; ++i) {
			String columnName = (String)wantedColumnNames.get(i);
			Object value = rs.getObject(columnName);
			row.put(columnName, value);
		}
		rows.add(row);
	}

	return rows;
}




/**
* Return all column names as a list of strings
* @param database query result set
* @return list of column name strings
* @throws SQLException if the query fails
*/
public static final List<String> getColumnNames(ResultSet rs) throws SQLException
{
	List<String> columnNames = new ArrayList<String>();

	ResultSetMetaData meta = rs.getMetaData();

	int numColumns = meta.getColumnCount();
	for (int i = 1; i <= numColumns; ++i) {
		columnNames.add(meta.getColumnName(i));
	}

	return columnNames;
}

}
