/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.common.util;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.changeToJsonFilename;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.getExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
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


    @Test
    public void testToJsonFilename() {
        assertThat(toJsonFilename(null)).isEqualTo(null);
        assertThat(toJsonFilename("file")).isEqualTo("file.json");
        assertThat(toJsonFilename("file.json")).isEqualTo("file.json");
        assertThat(toJsonFilename("file.v1")).isEqualTo("file.v1.json");
    }

    @Test
    public void testChangeToJsonFilename() {
        assertThat(changeToJsonFilename(null)).isEqualTo(null);
        assertThat(changeToJsonFilename("file")).isEqualTo("file.json");
        assertThat(changeToJsonFilename("file.json")).isEqualTo("file.json");
        assertThat(changeToJsonFilename("file.v1")).isEqualTo("file.json");
    }

}
