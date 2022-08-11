package org.activiti.cloud.services.modeling.validation.magicnumber;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileMagicNumberValidator {

    private static final Logger logger = LoggerFactory.getLogger(FileMagicNumberValidator.class);

    private final List<FileMagicNumber> fileMagicNumber;

    public FileMagicNumberValidator(List<FileMagicNumber> fileMagicNumber) {
        this.fileMagicNumber = fileMagicNumber;
    }

    public boolean checkFileIsExecutable(byte[] fileContent) {
        return fileMagicNumber
            .stream()
            .filter(filter -> filter.accept(fileContent))
            .peek(filter -> logger.info("The file is executable because it is a $1", filter.getName()))
            .findAny()
            .isPresent();
    }

}
