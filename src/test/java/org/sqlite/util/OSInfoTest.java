// --------------------------------------
// sqlite-jdbc Project
//
// OSInfoTest.java
// Since: May 20, 2008
//
// $URL$
// $Author$
// --------------------------------------
package org.sqlite.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

public class OSInfoTest {
    private static final Logger logger = Logger.getLogger(OSInfoTest.class.getName());
    
    @Test
    public void osName() {
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Windows XP"), "Windows");
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Windows 2000"), "Windows");
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Windows Vista"), "Windows");
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Windows 98"), "Windows");
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Windows 95"), "Windows");

        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Mac OS"), "Mac");
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Mac OS X"), "Mac");

        Assert.assertEquals(OSInfo.translateOSNameToFolderName("AIX"), "AIX");

        Assert.assertEquals(OSInfo.translateOSNameToFolderName("Linux"), "Linux");
        Assert.assertEquals(OSInfo.translateOSNameToFolderName("OS2"), "OS2");

        Assert.assertEquals(OSInfo.translateOSNameToFolderName("HP UX"), "HPUX");
    }

    @Test
    public void archName() {
        Assert.assertEquals(OSInfo.translateArchNameToFolderName("i386"), "i386");
        Assert.assertEquals(OSInfo.translateArchNameToFolderName("x86"), "x86");
        Assert.assertEquals(OSInfo.translateArchNameToFolderName("ppc"), "ppc");
        Assert.assertEquals(OSInfo.translateArchNameToFolderName("amd64"), "amd64");
    }

    @Test
    public void folderPath() {
        String[] component = OSInfo.getNativeLibFolderPathForCurrentOS().split("/");
        Assert.assertEquals(component.length, 2);
        Assert.assertEquals(component[0], OSInfo.getOSName());
        Assert.assertEquals(component[1], OSInfo.getArchName());
    }

    @Test
    public void testMainForOSName() {

        // preserve the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] {"--os"});
            Assert.assertEquals(buf.toString(), OSInfo.getOSName());
        } finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

    @Test
    public void testMainForArchName() {

        // preserver the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] {"--arch"});
            Assert.assertEquals(buf.toString(), OSInfo.getArchName());
        } finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

    @Test
    public void displayOSInfo() {
        logger.info("Hardware name: " + OSInfo.getHardwareName());
        logger.info("OS name: " + OSInfo.getOSName());
        logger.info("Architecture name: " + OSInfo.getArchName());
    }

    // it's unlikely we run tests on an Android device
    // @Test
    void testIsNotAndroid() {
        Assert.assertFalse(OSInfo.isAndroidRuntime());
        Assert.assertFalse(OSInfo.isAndroidTermux());
        Assert.assertFalse(OSInfo.isAndroid());
    }

    @Test
    public void testIsAndroidTermux() throws Exception {
        try {
            ProcessRunner mockRunner = mock(ProcessRunner.class);
            OSInfo.processRunner = mockRunner;
            when(mockRunner.runAndWaitFor("uname -o")).thenReturn("Android");

            Assert.assertTrue(OSInfo.isAndroidTermux());
            Assert.assertFalse(OSInfo.isAndroidRuntime());
            Assert.assertTrue(OSInfo.isAndroid());
        } finally {
            OSInfo.processRunner = new ProcessRunner();
        }
    }
    
    // @Test
    void testOverride() {
    	System.setProperty("org.sqlite.osinfo.architecture", "overridden");
    	System.setProperty("os.name", "Windows");
    	
        Assert.assertEquals(OSInfo.getArchName(), "overridden");
        Assert.assertEquals(OSInfo.getNativeLibFolderPathForCurrentOS(), "Windows/overridden");
        
        System.setProperty("org.sqlite.osinfo.architecture", null);
    	System.setProperty("os.name", null);
    }
}
