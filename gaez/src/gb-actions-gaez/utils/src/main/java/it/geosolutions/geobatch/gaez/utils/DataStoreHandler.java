/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.gaez.utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.jndi.JndiTemplate;

/**
 * Abstract class to provide read/write/update Database functions to the GAEZ
 * flow
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class DataStoreHandler {

	/**
	 * Default logger
	 */
	private final static Logger LOGGER = LoggerFactory
			.getLogger(DataStoreHandler.class);

	/**
	 * column id
	 */
	protected final static String ID_KEY = "id";

	/**
	 * TO BE FOUND INTO THE jdbc.properties file
	 */
	private final static String SCHEMA_KEY = "schema";
	private final static String TABLE_KEY = "table";
	private final static String JNDI_KEY = "jndiReferenceName";
	// optional limit of locked rows to check for
	private final static String MAXLOCKED_KEY = "maxLocked";

	public static boolean jdbcUpdateStatus(final Properties jndiProps,
			final List<Map<String, Object>> mapList) throws Throwable {
		if (jndiProps==null)
			return false; // TODO log

		final String schema = jndiProps.getProperty(SCHEMA_KEY);
		final String table = jndiProps.getProperty(TABLE_KEY);
		if (schema == null || table == null) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("Unable to get \'schema\' or \'table\' from the properties file");
			return false;
		}
			
		final DataSource source=getConnection(jndiProps);
		if (source==null)
			return false; // TODO log
		
		Connection conn = null;
		final JdbcTemplate template = new JdbcTemplate(source);

		try {
			final Iterator<Map<String, Object>> itList = mapList.iterator();
			StringBuilder sqlBuilder = null;
			while (itList.hasNext()) {

				final Map<String, Object> rowMap = itList.next();
				final Set<Map.Entry<String, Object>> rowSet = rowMap.entrySet();

				/*
				 * update gaez.rst_to_tif SET id=?, ..., no_data_value=? where
				 * id = ?;
				 */
				final Object[] values = new Object[rowSet.size() + 1];

				
				
				if (sqlBuilder == null) {
					final Iterator<Map.Entry<String, Object>> it = rowSet
							.iterator();
					sqlBuilder = new StringBuilder("UPDATE ");
					sqlBuilder.append(schema).append(".").append(table)
							.append(" SET ");
					int valuesIt = 0;
					while (it.hasNext()) {
						Map.Entry<String, Object> rowEntry = it.next();
						sqlBuilder.append(rowEntry.getKey()).append("=?");
						if (it.hasNext()) {
							sqlBuilder.append(",");
						} else {
							sqlBuilder.append(" WHERE " + ID_KEY + "=?");
						}
						if (LOGGER.isDebugEnabled()){
							LOGGER.debug("Values: ["+valuesIt+"]: "+rowEntry.getValue());
						}
						values[valuesIt++] = rowEntry.getValue();
						
					}
					// id for where clause
					values[valuesIt] = rowMap.get(ID_KEY);

				} else {
					final Iterator<Map.Entry<String, Object>> it = rowSet
							.iterator();
					int valuesIt = 0;
					while (it.hasNext()) {
						Map.Entry<String, Object> rowEntry = it.next();
						if (LOGGER.isDebugEnabled()){
							LOGGER.debug("Values: ["+valuesIt+"]: "+rowEntry.getValue());
						}
						values[valuesIt++] = rowEntry.getValue();
						
					}
					// id for where clause
					values[valuesIt] = rowMap.get(ID_KEY);
				}

				// update
				final int updated = template.update(sqlBuilder.toString(),
						values);

				if (LOGGER.isInfoEnabled())
					LOGGER.info("Updated " + updated + "number of rows");
			}

			return true;
			// return template.query(psc, rm);

		} catch (DataSourceLookupFailureException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(),e);
			throw e;
		} catch (Throwable e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(),e);
			throw e;
		} finally {
			if (conn != null && source != null) {
				// properly release our connection
				DataSourceUtils.releaseConnection(conn, source);
			}
		}
	}
	
	/**
	 * get the DataSource
	 * @param jndiProps
	 * @return
	 */
	private static DataSource getConnection(final Properties jndiProps){
		DataSource source;
		// JndiObjectFactoryBean jndiFactory = new JndiObjectFactoryBean();
		// jndiFactory.setJndiEnvironment(jndiProps);
		// jndiFactory.getJndiTemplate();
		JndiTemplate jndi = new JndiTemplate();
		final String jndiResourceName = jndiProps.getProperty(JNDI_KEY);
		try {
			if (jndiResourceName == null)
				throw new Exception(
						"Unable to find the key \'"
								+ JNDI_KEY
								+ "\' into the jdbc.property file, lets try for a Simple Driver Data Source");

			source = jndi.lookup(jndiResourceName, DataSource.class);
			if (LOGGER.isInfoEnabled())
				LOGGER.info("Succesfully load the JNDI resource named: "
						+ jndiResourceName);
			return source;
		} catch (Exception e) {
			if (LOGGER.isWarnEnabled())
				LOGGER.warn("Unable to get the JNDI resource named: "
						+ jndiResourceName + ". Error: "
						+ e.getLocalizedMessage());
			try {
				final SimpleDriverDataSource simpleSource = new SimpleDriverDataSource();
				simpleSource.setDriverClass((Class<java.sql.Driver>) Class
						.forName(jndiProps.getProperty("driverClass")));
				simpleSource.setUrl(jndiProps.getProperty("url"));
				simpleSource.setConnectionProperties(jndiProps);
				return simpleSource;
			} catch (ClassNotFoundException e1) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e1.getLocalizedMessage(), e1);
				return null;
			}
			
		}
	}

	
	private static int getLimit(final Properties jndiProps,
			final JdbcTemplate template, final String schema,
			final String table, final String where) {
		
		String maxLocked = jndiProps.getProperty(MAXLOCKED_KEY);
		int maxLockedRows = -1;
		
		if (maxLocked != null) {
			try {
				maxLockedRows = Integer.parseInt(maxLocked);
				// get the actual number of locked rows
				final int locked = template.queryForInt("select count(\'"
						+ ID_KEY + "\') from " + schema + "." + table
						+ " WHERE " + where); // // STATUS_KEY + "='RDY' AND
												// IGNORE='1'

				if (maxLockedRows > 0) {
					if (locked < maxLockedRows) {
						maxLockedRows -= locked;
					} else {
						maxLockedRows = 0;
					}
				} else if (LOGGER.isWarnEnabled())
					LOGGER.warn("The string \'" + maxLocked
							+ "\' assigned to the key: \'" + MAXLOCKED_KEY
							+ "\' is negative. No limit will be applied");

			} catch (NumberFormatException e) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn("The string \'"
							+ maxLocked
							+ "\' assigned to the key: \'"
							+ MAXLOCKED_KEY
							+ "\' does not contain a parsable integer. No limit will be applied");
			}
		} else {
			if (LOGGER.isWarnEnabled())
				LOGGER.warn("The key: \'"
						+ MAXLOCKED_KEY
						+ "\' is not set into the passed properties file.");			
		}

		return maxLockedRows;
	}

	
	/**
	 * 
	 * Emanuele Tajariol (proposal): while( collected.size < #_OF_WANTED_ROWS) {
	 * selectedList = fetch(SELECT FROM .. .WHERE STATUS ='RDY') for(selected:
	 * selectedList) { updated = update(UPDATE .. SET STATUS='lck' WHERE STATUS
	 * ='RDY' AND ID=...) if(updated == 1) collected.add(selected) } }
	 * 
	 * NEW!!![POSTGRESQL] Carlo Cancellieri (proposal): update gaez.rst_to_tif
	 * SET status_code='LCK' where id = ? and where status_code='RDY' RETURNING
	 * *; UPDATE gaez.data SET status_code='LCK' where IGNORE=1 and id IN
	 * (SELECT id FROM gaez.data where status_code='RDY' limit 10) RETURNING *;
	 * 
	 * @param jndiSourceName
	 * @param query
	 * @param jndiProps
	 * @param sizeLimit
	 * @param where
	 *            the where clause to select tple
	 * @param set
	 *            the set clause to lock lines
	 * @return
	 * @throws Throwable 
	 */
	public static List<Map<String, Object>> select4UpdatePrepStat(
			final Properties jndiProps, int sizeLimit, final String where,
			final String set) throws Throwable {
		if (jndiProps==null){
			
			final String message="Unable to start update using a null properties object";
			if (LOGGER.isErrorEnabled())
				LOGGER.error(message);
			throw new Exception(message);
		}

		final String schema = jndiProps.getProperty(SCHEMA_KEY);
		final String table = jndiProps.getProperty(TABLE_KEY);
		if (schema == null || table == null) {
			final String message="Unable to get \'schema\' or \'table\' from the properties file";
			if (LOGGER.isErrorEnabled())
				LOGGER.error(message);
			throw new Exception(message);
		}
			
		final DataSource source=getConnection(jndiProps);
		if (source==null){
			final String message="Failed to get the DataSource";
			if (LOGGER.isErrorEnabled())
				LOGGER.error(message);
			throw new Exception(message);	
		}

		Connection conn = null;
		
		final JdbcTemplate template = new JdbcTemplate(source);

		try {
			final List<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();

			int checkedLimit = getLimit(jndiProps, template, schema, table, set);
			// if the recalculated size limit is 0 return an empty list
			if (checkedLimit!=-1){
				// checkedLimit is not set using default as sizeLimit
				if (checkedLimit == 0 || sizeLimit<1)
					return retList;
				else if (checkedLimit < sizeLimit) {
					sizeLimit = checkedLimit;
				}
			}
			
			// else limit is the passed one

//			final List<Long> idList = template.queryForList("SELECT " + ID_KEY
//					+ " FROM " + schema + "." + table + " WHERE " + where,
//					Long.class); // STATUS_KEY + "='RDY' AND IGNORE='1'
//			final List<Long> idToGetList = new ArrayList<Long>();
//			final Iterator<Long> it = idList.iterator();
//			int updateSize = 0;
//			final String updateQuery = "UPDATE " + schema + "." + table
//					+ " SET " + set + " WHERE " + ID_KEY + " = ? AND " + where;// set=
//																				// STATUS_KEY
//																				// +
//																				// "='LCK' - where=STATUS_KEY + "='RDY'"
//			while (it.hasNext() && updateSize < sizeLimit) {
//				final Long id = it.next();
//				if (template.update(updateQuery, new Object[] { id }) == 1) {
//					idToGetList.add(id);
//					++updateSize;
//				}
//			}
//
//			final String selectToGetQuery = "select * from " + schema + "."
//					+ table + " WHERE " + ID_KEY + " = ?";
//			for (Long id : idToGetList) {
//				retList.add(template.queryForMap(selectToGetQuery,
//						new Object[] { id }));
//			}
//			return retList;

			// DO NOT WORK IN MULTITHREAD
//			final String selectToGetQuery = "SELECT * FROM " + schema + "." + table + " WHERE "
//			 + where + " limit " + sizeLimit + " FOR UPDATE";
//			 return template.queryForList(selectToGetQuery);
			 
			// DO NOT WORK IN MULTITHREAD
			 final String selectToGetQuery = "UPDATE " + schema + "." + table
			 + " SET " + set + " WHERE " + where +" AND "+ ID_KEY + " IN (SELECT "
			 + ID_KEY + " FROM " + schema + "." + table + " WHERE "
			 + where + " LIMIT " + sizeLimit + ") RETURNING *";
			 return template.queryForList(selectToGetQuery);
			  
		} catch (DataSourceLookupFailureException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(),e);
			throw e;
		} catch (Throwable e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(),e);
			throw e;
		} finally {
			if (conn != null && source != null) {
				// properly release our connection
				DataSourceUtils.releaseConnection(conn, source);
			}
		}
	}

}
