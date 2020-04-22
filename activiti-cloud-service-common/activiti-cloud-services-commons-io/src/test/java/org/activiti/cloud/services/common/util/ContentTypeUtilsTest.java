package org.activiti.cloud.services.common.util;

import static junit.framework.TestCase.assertEquals;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.getExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;

import org.junit.Test;

public class ContentTypeUtilsTest {

    @Test
    public void testGetExtension() {

        assertEquals(null, getExtension(null));
        assertEquals("ext", getExtension("file.ext"));
        assertEquals("", getExtension("README"));
        assertEquals("com", getExtension("domain.dot.com"));
        assertEquals("jpeg", getExtension("image.jpeg"));
        assertEquals("", getExtension("a.b/c"));
        assertEquals("txt", getExtension("a.b/c.txt"));
        assertEquals("", getExtension("a/b/c"));
        assertEquals("", getExtension("a.b\\c"));
        assertEquals("txt", getExtension("a.b\\c.txt"));
        assertEquals("", getExtension("a\\b\\c"));
        assertEquals("", getExtension("C:\\temp\\foo.bar\\README"));
        assertEquals("ext", getExtension("../filename.ext"));


    }

    @Test
    public void testRemoveExtension() {

        assertEquals(null, removeExtension(null));
        assertEquals("file", removeExtension("file.ext"));
        assertEquals("README", removeExtension("README"));
        assertEquals("domain.dot", removeExtension("domain.dot.com"));
        assertEquals("image", removeExtension("image.jpeg"));
        assertEquals("a.b/c", removeExtension("a.b/c"));
        assertEquals("a.b/c", removeExtension("a.b/c.txt"));
        assertEquals("a/b/c", removeExtension("a/b/c"));
        assertEquals("a.b\\c", removeExtension("a.b\\c"));
        assertEquals("a.b\\c", removeExtension("a.b\\c.txt"));
        assertEquals("a\\b\\c", removeExtension("a\\b\\c"));
        assertEquals("C:\\temp\\foo.bar\\README", removeExtension("C:\\temp\\foo.bar\\README"));
        assertEquals("../filename", removeExtension("../filename.ext"));
    }
}
