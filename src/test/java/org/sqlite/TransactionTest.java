package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sqlite.SQLiteConfig.TransactionMode;

/**
 * These tests assume that Statements and PreparedStatements are working as per normal and test the
 * interactions of commit(), rollback() and setAutoCommit(boolean) with multiple connections to the
 * same db.
 */
public class TransactionTest {
	@Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
	
    private Connection conn1, conn2, conn3;
    private Statement stat1, stat2, stat3;

    boolean done = false;

    @Before
    public void connect() throws Exception {
        File tmpFile = File.createTempFile("test-trans", ".db", tempDir.getRoot());

        Properties prop = new Properties();
        prop.setProperty("shared_cache", "false");

        conn1 = DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath(), prop);
        conn2 = DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath(), prop);
        conn3 = DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath(), prop);

        stat1 = conn1.createStatement();
        stat2 = conn2.createStatement();
        stat3 = conn3.createStatement();
    }

    @After
    public void close() throws Exception {
        stat1.close();
        stat2.close();
        stat3.close();
        conn1.close();
        conn2.close();
        conn3.close();
    }

    private void failedUpdatedPreventedFutureRollback(boolean prepared) throws SQLException {
        stat1.execute("create table test (c1);");
        stat1.execute("insert into test values (1);");

        // First transaction starts
        conn1.setAutoCommit(false);
        stat1.execute("insert into test values (2);");

        final PreparedStatement pstat2 =
                prepared ? conn2.prepareStatement("insert into test values (3);") : null;

        // Second transaction starts and tries to complete but fails because first is still running
        boolean gotException = false;
        try {
            ((SQLiteConnection) conn2).setBusyTimeout(10);
            conn2.setAutoCommit(false);
            if (pstat2 != null) {
                // The prepared case would fail regardless of whether this was "execute" or
                // "executeUpdate"
                pstat2.execute();
            } else {
                // If you changed this to "executeUpdate" instead of "execute", the test would pass
                stat2.execute("insert into test values (3);");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("is locked")) {
                gotException = true;
            } else {
                throw e;
            }
        }
        assertTrue(gotException);
        conn2.rollback();
        // The test would fail here: the trivial "transaction" created in between the rollback we
        // just
        // did and this point would fail to commit because "SQL statements in progress"
        conn2.setAutoCommit(true);

        // First transaction completes
        conn1.setAutoCommit(true);

        // Second transaction retries
        conn2.setAutoCommit(false);
        if (pstat2 != null) {
            pstat2.execute();
        } else {
            stat2.execute("insert into test values (3);");
        }
        conn2.setAutoCommit(true);

        final ResultSet rs = stat1.executeQuery("select c1 from test");
        final Set<Integer> seen = new HashSet<Integer>();
        while (rs.next()) {
            assertTrue(seen.add(rs.getInt(1)));
        }
        
        assertTrue(seen.contains(1));
        assertTrue(seen.contains(2));
        assertTrue(seen.contains(3));
    }

    @Test
    public void failedUpdatePreventedFutureRollbackUnprepared() throws SQLException {
        failedUpdatedPreventedFutureRollback(false);
    }

    @Test
    public void failedUpdatePreventedFutureRollbackPrepared() throws SQLException {
        failedUpdatedPreventedFutureRollback(true);
    }

    @Test
    public void multiConn() throws SQLException {
        stat1.executeUpdate("create table test (c1);");
        stat1.executeUpdate("insert into test values (1);");
        stat2.executeUpdate("insert into test values (2);");
        stat3.executeUpdate("insert into test values (3);");

        ResultSet rs = stat1.executeQuery("select sum(c1) from test;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 6);
        rs.close();

        rs = stat3.executeQuery("select sum(c1) from test;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 6);
        rs.close();
    }

    @Test
    public void locking() throws SQLException {
        stat1.executeUpdate("create table test (c1);");
        stat1.executeUpdate("begin immediate;");
        stat2.executeUpdate("select * from test;");
    }

    @Test
    public void insert() throws SQLException {
        ResultSet rs;
        String countSql = "select count(*) from trans;";

        stat1.executeUpdate("create table trans (c1);");
        conn1.setAutoCommit(false);
        
        assertEquals(stat1.executeUpdate("insert into trans values (4);"), 1);

        // transaction not yet committed, conn1 can see, conn2 can not
        rs = stat1.executeQuery(countSql);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1);
        rs.close();
        rs = stat2.executeQuery(countSql);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 0);
        rs.close();

        conn1.commit();

        // all connects can see data
        rs = stat2.executeQuery(countSql);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1);
        rs.close();
    }

    @Test
    public void rollback() throws SQLException {
        String select = "select * from trans;";
        ResultSet rs;

        stat1.executeUpdate("create table trans (c1);");
        conn1.setAutoCommit(false);
        stat1.executeUpdate("insert into trans values (3);");

        rs = stat1.executeQuery(select);
        assertTrue(rs.next());
        rs.close();

        conn1.rollback();

        rs = stat1.executeQuery(select);
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void multiRollback() throws SQLException {
        ResultSet rs;

        stat1.executeUpdate("create table t (c1);");
        conn1.setAutoCommit(false);
        stat1.executeUpdate("insert into t values (1);");
        conn1.commit();
        stat1.executeUpdate("insert into t values (1);");
        conn1.rollback();
        stat1.addBatch("insert into t values (2);");
        stat1.addBatch("insert into t values (3);");
        stat1.executeBatch();
        conn1.commit();
        stat1.addBatch("insert into t values (7);");
        stat1.executeBatch();
        conn1.rollback();
        stat1.executeUpdate("insert into t values (4);");
        conn1.setAutoCommit(true);
        stat1.executeUpdate("insert into t values (5);");
        conn1.setAutoCommit(false);
        PreparedStatement p = conn1.prepareStatement("insert into t values (?);");
        p.setInt(1, 6);
        p.executeUpdate();
        p.setInt(1, 7);
        p.executeUpdate();

        // conn1 can see (1+...+7), conn2 can see (1+...+5)
        rs = stat1.executeQuery("select sum(c1) from t;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1 + 2 + 3 + 4 + 5 + 6 + 7);
        rs.close();
        rs = stat2.executeQuery("select sum(c1) from t;");
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 1 + 2 + 3 + 4 + 5);
        rs.close();
    }

    @Test
    public void transactionsDontMindReads() throws SQLException {
        stat1.executeUpdate("create table t (c1);");
        stat1.executeUpdate("insert into t values (1);");
        stat1.executeUpdate("insert into t values (2);");
        ResultSet rs = stat1.executeQuery("select * from t;");
        assertTrue(rs.next()); // select is open

        conn2.setAutoCommit(false);
        stat1.executeUpdate("insert into t values (2);");

        rs.close();
        conn2.commit();
    }

    @Test
    public void secondConnWillWait() throws Exception {
        stat1.executeUpdate("create table t (c1);");
        stat1.executeUpdate("insert into t values (1);");
        stat1.executeUpdate("insert into t values (2);");
        ResultSet rs = stat1.executeQuery("select * from t;");
        assertTrue(rs.next());

        final TransactionTest lock = this;
        lock.done = false;
        new Thread(new Runnable() {
			@Override
			public void run() {
				try {
                    stat2.executeUpdate("insert into t values (3);");
                } catch (SQLException e) {
                    e.printStackTrace();
                    return;
                }

                synchronized (lock) {
                    lock.done = true;
                    lock.notify();
                }
			}
		}).start();

        Thread.sleep(100);
        rs.close();

        synchronized (lock) {
            if (!lock.done) {
                lock.wait(5000);
                if (!lock.done) {
                    throw new Exception("should be done");
                }
            }
        }
    }

    @Test
    public void secondConnMustTimeout() throws SQLException {
        stat1.setQueryTimeout(1);
        stat1.executeUpdate("create table t (c1);");
        stat1.executeUpdate("insert into t values (1);");
        stat1.executeUpdate("insert into t values (2);");
        ResultSet rs = stat1.executeQuery("select * from t;");
        assertTrue(rs.next());

        ((SQLiteConnection) conn2).setBusyTimeout(10);
        try {
        	stat2.executeUpdate("insert into t values (3);");
        } catch (Exception e) {
        	assertTrue(e instanceof SQLException);
        }
    }

    //    @Test(expected= SQLException.class)
    @Test
    public void cantUpdateWhileReading() throws SQLException {
        stat1.executeUpdate("create table t (c1);");
        stat1.executeUpdate("insert into t values (1);");
        stat1.executeUpdate("insert into t values (2);");
        ResultSet rs = conn1.createStatement().executeQuery("select * from t;");
        assertTrue(rs.next());

        // commit now succeeds since sqlite 3.6.5
        stat1.executeUpdate("insert into t values (3);"); // can't be done
    }

    @Test
    public void cantCommit() {
    	try {
    		conn1.commit();
    	} catch (Exception e) {
    		assertTrue(e instanceof SQLException);
    	}
    }

    @Test
    public void cantRollback() {
    	try {
    		conn1.rollback();
    	} catch (Exception e) {
    		assertTrue(e instanceof SQLException);
    	}
    }

    @Test
    public void transactionModes() throws Exception {
        File tmpFile = File.createTempFile("test-trans", ".db", tempDir.getRoot());

        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tmpFile.getAbsolutePath());

        // deferred
        SQLiteConnection con = (SQLiteConnection) ds.getConnection();
        try {
        	assertEquals(con.getConnectionConfig().getTransactionMode(), TransactionMode.DEFERRED);
        	assertEquals(con.getConnectionConfig().transactionPrefix(), "begin;");
            runUpdates(con, "tbl1");
        } finally {
        	con.close();
        }

        ds.setTransactionMode(TransactionMode.DEFERRED.name());
        
        SQLiteConnection con1 = (SQLiteConnection) ds.getConnection();
        try {
        	assertEquals(con1.getConnectionConfig().getTransactionMode(), TransactionMode.DEFERRED);
        	assertEquals(con1.getConnectionConfig().transactionPrefix(), "begin;");
        } finally {
        	con1.close();
        }

        // immediate
        ds.setTransactionMode(TransactionMode.IMMEDIATE.name());
        SQLiteConnection con2 = (SQLiteConnection) ds.getConnection();
        try {
        	assertEquals(con2.getConnectionConfig().getTransactionMode(), TransactionMode.IMMEDIATE);
        	assertEquals(con2.getConnectionConfig().transactionPrefix(), "begin immediate;");
            runUpdates(con2, "tbl2");
        } finally {
        	con2.close();
		}

        // exclusive
        ds.setTransactionMode(TransactionMode.EXCLUSIVE.name());
        SQLiteConnection con3 = (SQLiteConnection) ds.getConnection();
        try {
        	assertEquals(con3.getConnectionConfig().getTransactionMode(), TransactionMode.EXCLUSIVE);
        	assertEquals(con3.getConnectionConfig().transactionPrefix(), "begin exclusive;");
            runUpdates(con3, "tbl3");
        } finally {
        	con3.close();
		}
    }

    public void runUpdates(Connection con, String table) throws SQLException {
        Statement stat = con.createStatement();

        con.setAutoCommit(false);
        stat.execute("create table " + table + "(id)");
        stat.executeUpdate("insert into " + table + " values(1)");
        stat.executeUpdate("insert into " + table + " values(2)");
        con.commit();

        ResultSet rs = stat.executeQuery("select * from " + table);
        rs.next();
        assertEquals(rs.getInt(1), 1);
        rs.next();
        assertEquals(rs.getInt(1), 2);
        rs.close();
        con.close();
    }
}
