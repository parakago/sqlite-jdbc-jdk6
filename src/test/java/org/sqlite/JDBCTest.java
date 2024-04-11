// --------------------------------------
// sqlite-jdbc Project
//
// JDBCTest.java
// Since: Apr 8, 2009
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JDBCTest {
	@Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
	
    @Test
    public void enableLoadExtensionTest() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("enable_load_extension", "true");
        
        Connection conn = null;
        try {
        	conn = DriverManager.getConnection("jdbc:sqlite:", prop);
            Statement stat = conn.createStatement();

            // How to build shared lib in Windows
            // # mingw32-gcc -fPIC -c extension-function.c
            // # mingw32-gcc -shared -Wl -o extension-function.dll extension-function.o

            //            stat.executeQuery("select load_extension('extension-function.dll')");
            //
            //            ResultSet rs = stat.executeQuery("select sqrt(4)");
            //            System.out.println(rs.getDouble(1));

        } finally {
        	if (conn != null) conn.close();
        }
    }
    
    @Test
    public void shouldReturnNullIfProtocolUnhandled() throws Exception {
    	assertNull(JDBC.createConnection("jdbc:anotherpopulardatabaseprotocol:", null));
        // assertThat(JDBC.createConnection("jdbc:anotherpopulardatabaseprotocol:", null)).isNull();
    }

    @Test
    public void allDriverPropertyInfoShouldHaveADescription() throws Exception {
        Driver driver = DriverManager.getDriver("jdbc:sqlite:");
        DriverPropertyInfo[] infos = driver.getPropertyInfo(null, null);
        for (DriverPropertyInfo info : infos) {
        	assertNotNull(info.description);
        }
    }

    @Test
    public void pragmaReadOnly() throws SQLException {
        SQLiteConnection connection =
                (SQLiteConnection)
                        DriverManager.getConnection(
                                "jdbc:sqlite::memory:?jdbc.explicit_readonly=true");
        assertTrue(connection.getDatabase().getConfig().isExplicitReadOnly());
    }

    @Test
    public void canSetJdbcConnectionToReadOnly() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            assertFalse(connection.isReadOnly());
            connection.setReadOnly(true);
            assertTrue(connection.isReadOnly());
            connection.setReadOnly(false);
            assertFalse(connection.isReadOnly());
            connection.setReadOnly(true);
            assertTrue(connection.isReadOnly());
        } finally {
        	connection.close();
        }
    }

    @Test
    public void cannotSetJdbcConnectionToReadOnlyAfterFirstStatement() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            // execute a statement
            Statement statement = connection.createStatement();
            try {
                boolean success = statement.execute("SELECT * FROM sqlite_schema");
                assertTrue(success);
            } finally {
            	statement.close();
            }
            
            try {
            	// Managed to set readOnly = true on a dirty connection!
            	connection.setReadOnly(true);
            } catch (Exception e) {
            	assertTrue(e.getClass() == SQLException.class);
            }
        } finally {
        	connection.close();
        }
    }

    @Test
    public void canSetJdbcConnectionToReadOnlyAfterCommit() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            // execute a statement
            Statement statement = connection.createStatement();
            try {
                boolean success = statement.execute("SELECT * FROM sqlite_schema");
                assertTrue(success);
            } finally {
            	statement.close();
            }
            connection.commit();

            // try to assign a new read-only value
            connection.setReadOnly(false);
        } finally {
        	connection.close();
        }
    }

    @Test
    public void canSetJdbcConnectionToReadOnlyAfterRollback() throws Exception {
        System.out.println("Creating JDBC Datasource");
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        System.out.println("Creating JDBC Connection");
        
        Connection connection = dataSource.getConnection();
        try {
            System.out.println("JDBC Connection created");
            System.out.println("Disabling auto-commit");
            connection.setAutoCommit(false);
            System.out.println("Creating statement");
            // execute a statement
            Statement statement = connection.createStatement();
            try {
                System.out.println("Executing query");
                boolean success = statement.execute("SELECT * FROM sqlite_schema");
                assertTrue(success);
            } finally {
            	statement.close();
                System.out.println("Closing statement");
            }
            System.out.println("Performing rollback");
            connection.rollback();

            System.out.println("Setting connection to read-only");
            // try to assign read-only
            connection.setReadOnly(true);
            // execute a statement
            Statement statement2 = connection.createStatement();
            try {
                System.out.println("Executing query 2");
                boolean success = statement2.execute("SELECT * FROM sqlite_schema");
                assertTrue(success);
            } finally {
            	statement2.close();
                System.out.println("Closing statement 2");
            }
            System.out.println("Performing rollback 2");
            connection.rollback();
        } finally {
        	connection.close();
        }
    }

    @Test
    public void cannotExecuteUpdatesWhenConnectionIsSetToReadOnly() throws Exception {
        SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
        
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);

            // execute a statement
            Statement statement = connection.createStatement();
            try {
            	try {
            		statement.execute("CREATE TABLE TestTable(ID VARCHAR(255), PRIMARY KEY(ID))");
            	} catch (Exception e) {
            		assertTrue(e instanceof SQLException);
            	}
            } finally {
            	statement.close();
            }
            connection.rollback();

            // try to assign read-only
            connection.setReadOnly(true);
        } finally {
        	connection.close();
        }
    }

    @Test
    public void jdbcHammer() throws Exception {
    	final SQLiteDataSource dataSource = createDatasourceWithExplicitReadonly();
    	File tempFile = File.createTempFile("myTestDB", ".db", tempDir.getRoot());
    	dataSource.setUrl("jdbc:sqlite:" + tempFile.getAbsolutePath());

    	Connection connection = dataSource.getConnection();
    	try {
    		connection.setAutoCommit(false);

    		Statement stmt = connection.createStatement();
    		try {
    			stmt.executeUpdate("CREATE TABLE TestTable(ID INT, testval INT, PRIMARY KEY(ID));");
    			stmt.executeUpdate("INSERT INTO TestTable (ID, testval) VALUES(1, 0);");
    		} finally {
    			stmt.close();
    		}
    		connection.commit();
    	} finally {
    		connection.close();
    	}

    	final AtomicInteger count = new AtomicInteger();
    	List<Thread> threads = new ArrayList<Thread>();
    	for (int i = 0; i < 10; i++) {
    		Thread thread = new Thread(new Runnable() {
    			public void run() {
    				for (int i1 = 0; i1 < 100; i1++) {
    					try {
    						Connection connection = dataSource.getConnection();
    						try {
    							connection.setAutoCommit(false);
    							boolean read = Math.random() < 0.5;
    							if (read) {
    								connection.setReadOnly(true);
    								Statement statement = connection.createStatement();
    								try {
    									ResultSet rs =
    											statement.executeQuery(
    													"SELECT * FROM TestTable");
    									rs.close();
    								} finally {
    									statement.close();
    								}
    							} else {
    								Statement statement = connection.createStatement();
    								try {
    									ResultSet rs = statement.executeQuery("SELECT * FROM TestTable");
    									try {
    										while (rs.next()) {
    											int id = rs.getInt("ID");
    											int value = rs.getInt("testval");
    											count.incrementAndGet();
    											statement.executeUpdate(
    													"UPDATE TestTable SET testval = "
    															+ (value + 1)
    															+ " WHERE ID = "
    															+ id);
    										}
    									} finally {
    										rs.close();
    									}
    								} finally {
    									statement.close();
    								}
    								connection.commit();
    							}
    						}
    						finally {
    							connection.close();
    						}
    					} catch (SQLException e) {
    						throw new RuntimeException("Worker failed", e);
    					}
    				}
    			}
    		});


    		thread.setName("Worker #" + (i + 1));
    		threads.add(thread);
    	}
    	for (Thread thread : threads) {
    		thread.start();
    	}
    	for (Thread thread : threads) {
    		thread.join();
    	}

    	Connection connection2 = dataSource.getConnection();
    	try {
    		connection2.setAutoCommit(false);
    		connection2.setReadOnly(true);
    		Statement stmt = connection2.createStatement();
    		try {
    			ResultSet rs = stmt.executeQuery("SELECT * FROM TestTable");
    			try {
    				assertTrue(rs.next());

    				int id = rs.getInt("ID");
    				int val = rs.getInt("testval");
    				assertEquals(id, 1);
    				assertEquals(val, count.get());
    				assertFalse(rs.next());
    			} finally {
    				rs.close();
    			}
    		} finally {
    			stmt.close();
    		}
    		connection2.commit();
    	} finally {
    		connection2.close();
    	}
    }

    // helper methods -----------------------------------------------------------------

    private SQLiteDataSource createDatasourceWithExplicitReadonly() {
        //        DriverManager.setLogWriter(new PrintWriter(System.out));
        SQLiteConfig config = new SQLiteConfig();
        config.setExplicitReadOnly(true);
        config.setBusyTimeout(10000);

        return new SQLiteDataSource(config);
    }
}
