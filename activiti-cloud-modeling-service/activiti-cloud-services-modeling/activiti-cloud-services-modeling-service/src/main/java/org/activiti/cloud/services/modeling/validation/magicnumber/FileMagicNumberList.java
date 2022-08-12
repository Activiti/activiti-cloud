package org.activiti.cloud.services.modeling.validation.magicnumber;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "executable-filter")
public class FileMagicNumberList {
    private List<FileMagicNumber> magicNumber;

    public List<FileMagicNumber> getMagicNumber() {
        int i = (byte)0xcafebabe;
        return magicNumber;
    }

    public void setMagicNumber(List<FileMagicNumber> magicNumber) {
        this.magicNumber = magicNumber;
    }
}
