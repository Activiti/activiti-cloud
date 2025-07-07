package org.activiti.cloud.services.test.asserts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.activiti.cloud.services.common.file.FileContent;
import org.junit.jupiter.api.Test;

class AssertZipContentTest {

    private FileContent zipWithJsonEntry(String entryName, String json) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
            zipOut.putNextEntry(new ZipEntry(entryName));
            zipOut.write(json.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
        return new FileContent("test.zip", "application/zip", byteOut.toByteArray());
    }

    @Test
    void shouldValidateJsonEntry_basicCheck() throws IOException {
        FileContent fileContent = zipWithJsonEntry("data.json", "{\"foo\":\"bar\"}");
        AssertZipContent zipAssert = new AssertZipContent(fileContent);

        // Just trigger the method for coverage
        zipAssert.hasJsonContent("data.json");
    }

    @Test
    void shouldValidateJsonEntryWithAssertion() throws IOException {
        FileContent fileContent = zipWithJsonEntry("data.json", "{\"foo\":\"bar\"}");
        AssertZipContent zipAssert = new AssertZipContent(fileContent);

        zipAssert.hasJsonContentSatisfying(
            "data.json",
            json -> {
                json.node("foo").isEqualTo("bar");
            }
        );
    }
}
