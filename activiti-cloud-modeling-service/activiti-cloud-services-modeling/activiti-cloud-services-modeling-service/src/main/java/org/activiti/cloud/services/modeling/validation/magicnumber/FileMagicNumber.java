package org.activiti.cloud.services.modeling.validation.magicnumber;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

public class FileMagicNumber {

    private String name;
    private String string;
    private byte[] bytes;
    private int offset;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
        this.setBytes(string.getBytes());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean accept(byte[] fileContent) {
        boolean result = false;
        if (fileContent.length >= bytes.length + offset) {
            result = Arrays.compare(fileContent, offset, bytes.length, bytes, 0, bytes.length) == 0;
        }
        return result;
    }

    @Configuration
    @ConfigurationProperties(prefix = "executable-filter")
    public static class FileMagicNumberList {
        private List<FileMagicNumber> magicNumber;

        public List<FileMagicNumber> getMagicNumber() {
            return magicNumber;
        }

        public void setMagicNumber(List<FileMagicNumber> magicNumber) {
            this.magicNumber = magicNumber;
        }
    }

}
