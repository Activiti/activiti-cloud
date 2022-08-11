package org.activiti.cloud.services.modeling.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.activiti.cloud.modeling.api.config.ModelingApiAutoConfiguration;
import org.activiti.cloud.services.modeling.validation.magicnumber.FileMagicNumberValidator;
import org.activiti.cloud.services.modeling.validation.magicnumber.FileContentValidatorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {FileContentValidatorConfiguration.class, ModelingApiAutoConfiguration.class})
class FileContentValidatorTest {

    @Autowired
    private FileMagicNumberValidator fileContentValidator;

    @Test
    void checkFileIsExecutableExe() throws IOException {
        //use a fake extension to verify if the test works regardless of extension
        File file = new File("src/test/resources/executables/windows-file.windows");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isTrue();
    }

    @Test
    void checkFileIsExecutableMsi() throws IOException {
        File file = new File("src/test/resources/executables/Chrome.msi");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isTrue();
    }

    @Test
    void checkFileIsExecutableBash() throws IOException {
        File file = new File("src/test/resources/executables/test.sh");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isTrue();
    }

    @Test
    void checkFileIsExecutableLinux() throws IOException {
        File file = new File("src/test/resources/executables/linuxExFile");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isTrue();
    }

    @Test
    void checkFileIsExecutableMacOs() throws IOException {
        File file = new File("src/test/resources/executables/test");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isTrue();
    }

    @Test
    void checkFileIsNotExecutablePlainText() throws IOException {
        //use a fake extension to verify if the test works regardless of extension
        File file = new File("src/test/resources/executables/text.txt");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isFalse();
    }

    @Test
    void checkFileIsNotExecutablePngImage() throws IOException {
        //use a fake extension to verify if the test works regardless of extension
        File file = new File("src/test/resources/executables/image.png");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isFalse();
    }

    @Test
    void checkFileIsNotExecutableZipFile() throws IOException {
        //use a fake extension to verify if the test works regardless of extension
        File file = new File("src/test/resources/executables/testzip.isAZip");
        byte[] firstBytes = readBytes(file, 100);
        assertThat(fileContentValidator.checkFileIsExecutable(firstBytes)).isFalse();
    }

    private byte[] readBytes(File file, int length) throws IOException {
        byte[] firstBytes = new byte[length];
        FileInputStream input = new FileInputStream(file);
        input.read(firstBytes);
        return firstBytes;
    }
}
