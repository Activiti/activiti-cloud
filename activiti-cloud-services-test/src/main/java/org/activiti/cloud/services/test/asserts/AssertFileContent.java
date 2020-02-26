/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.test.asserts;

import java.io.IOException;

import org.activiti.cloud.services.common.file.FileContent;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_ZIP;
import static org.assertj.core.api.Assertions.*;

/**
 * Asserts for file content
 */
public class AssertFileContent {

    private final FileContent fileContent;

    public AssertFileContent(FileContent fileContent) {
        this.fileContent = fileContent;
    }

    public AssertZipContent isZip() {
        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getContentType()).isEqualTo(CONTENT_TYPE_ZIP);
        try {
            return new AssertZipContent(fileContent);
        } catch (IOException e) {
            fail("Provided file content is not zip");
            return null;
        }
    }

    public AssertFileContent hasName(String expectedName) {
        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getFilename()).isEqualTo(expectedName);
        return this;
    }

    public AssertFileContent hasContentType(String expectedContentType) {
        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getContentType()).isEqualTo(expectedContentType);
        return this;
    }

    public AssertFileContent hasContent(byte[] expectedContent) {
        hasContent(new String(expectedContent));
        return this;
    }

    public AssertFileContent hasContent(String expectedContent) {
        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getFileContent()).isNotNull();
        assertThat(new String(fileContent.getFileContent())).isEqualTo(expectedContent);
        return this;
    }

    public static AssertFileContent assertThatFileContent(FileContent fileContent) {
        return new AssertFileContent(fileContent);
    }
}
