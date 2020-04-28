package org.activiti.cloud.services.common.util;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.getExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test ;

public class ContentTypeUtilsTest {

    @Test
    public void testGetExtension() {
        assertThat(getExtension(null)).isEqualTo(null);
        assertThat(getExtension("file.ext")).isEqualTo("ext");
        assertThat(getExtension("README")).isEqualTo("");
        assertThat(getExtension("domain.dot.com")).isEqualTo("com");
        assertThat(getExtension("image.jpeg")).isEqualTo("jpeg");
        assertThat(getExtension("a.b/c")).isEqualTo("");
        assertThat(getExtension("a.b/c.txt")).isEqualTo("txt");
        assertThat(getExtension("a/b/c")).isEqualTo("");
        assertThat(getExtension("a.b\\c")).isEqualTo("");
        assertThat(getExtension("a.b\\c.txt")).isEqualTo("txt");
        assertThat(getExtension("a\\b\\c")).isEqualTo("");
        assertThat(getExtension("C:\\temp\\foo.bar\\README")).isEqualTo("");
        assertThat(getExtension("../filename.ext")).isEqualTo("ext");
    }

    @Test
    public void testRemoveExtension() {
        assertThat(removeExtension(null)).isEqualTo(null);
        assertThat(removeExtension("file.ext")).isEqualTo("file");
        assertThat(removeExtension("README")).isEqualTo("README");
        assertThat(removeExtension("domain.dot.com")).isEqualTo("domain.dot");
        assertThat(removeExtension("image.jpeg")).isEqualTo("image");
        assertThat(removeExtension("a.b/c")).isEqualTo("a.b/c");
        assertThat(removeExtension("a.b/c.txt")).isEqualTo("a.b/c");
        assertThat(removeExtension("a/b/c")).isEqualTo("a/b/c");
        assertThat(removeExtension("a.b\\c")).isEqualTo("a.b\\c");
        assertThat(removeExtension("a.b\\c.txt")).isEqualTo("a.b\\c");
        assertThat(removeExtension("a\\b\\c")).isEqualTo("a\\b\\c");
        assertThat(removeExtension("C:\\temp\\foo.bar\\README")).isEqualTo("C:\\temp\\foo.bar\\README");
        assertThat(removeExtension("../filename.ext")).isEqualTo("../filename");
    }

}
