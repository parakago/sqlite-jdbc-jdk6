package org.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests to ensure that incorrect usage of prepared statements does not result in a java
 * segmentation fault
 */
public class PreparedStatementThreadTest {
    private static ExecutorService executorService;
    private Connection conn;
    private PreparedStatement stat;

    @BeforeClass
    public static void beforeAll() {
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterClass
    public static void afterAll() {
        executorService.shutdownNow();
    }

    /**
     * Tests to make sure that if one thread is uses a PreparedStatement which another thread is
     * closing, that the application throws an exception rather than crashing due to undefined C
     * behavior
     */
    @Test
    public void testPreparedStmtConcurrentCloseSegFault() throws SQLException {
        connect();
        try {
            for (int i = 0; i < 100; i++) {
            	testRace(executorService, new CloseCallback() {
					@Override
					public void close() throws SQLException {
						stat.close();
					}
				});
            }
        } finally {
            close();
        }
    }

    /**
     * Tests to make sure that if one thread is uses a PreparedStatement which another thread closes
     * the underlying connection to, the application throws an exception rather than crashing due to
     * undefined C behavior
     */
    @Test
    public void testPreparedStmtConcurrentCloseConnSegFault() throws SQLException {
        for (int i = 0; i < 100; i++) {
            connect();
            testRace(executorService, new CloseCallback() {
            	@Override
            	public void close() throws SQLException {
            		conn.close();
            	}
            });
            close();
        }
    }

    public void connect() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:");
    }

    public void close() throws SQLException {
        conn.close();
    }

    private void testRace(ExecutorService executorService, final CloseCallback closeCallback)
            throws SQLException {
        final AtomicInteger countdown = new AtomicInteger(2);
        stat = conn.prepareStatement("select 1,2,3,4,5");

        Future<Boolean> queryThread =
                executorService.submit(new Callable<Boolean>() {
                	@Override
                	public Boolean call() throws Exception {
                		waitFor(countdown);
                        for (int i = 0; i < 100; i++) {
                            ResultSet set = stat.executeQuery();
                            assertTrue(set.next());
                            for (int j = 1; j <= 5; j++) assertEquals(set.getInt(j), j);
                        }
                        return true;
                	}
                });
        
        Future<Boolean> closeThread =
                executorService.submit(new Callable<Boolean>() {
                	@Override
                	public Boolean call() throws Exception {
                		waitFor(countdown);
                        closeCallback.close();
                        return true;
                	}
				});
        try {
            assertTrue(queryThread.get());
            assertTrue(closeThread.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e.getMessage());
        } catch (ExecutionException e) {
            // this is OK, so long as we don't seg fault
        }
    }

    private void waitFor(AtomicInteger countdown) {
        countdown.decrementAndGet();
        while (0 < countdown.get())
            ;
    }

    interface CloseCallback {
        void close() throws SQLException;
    }
}
