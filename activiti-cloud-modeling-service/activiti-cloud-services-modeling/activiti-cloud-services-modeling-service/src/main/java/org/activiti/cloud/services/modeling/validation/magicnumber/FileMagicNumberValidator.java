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
        return fileMagicNumber != null && fileMagicNumber
            .stream()
            .filter(filter -> filter.accept(fileContent))
            .peek(filter -> logger.info("The file is executable because it is a $1", filter.getName()))
            .findAny()
            .isPresent();
    }

}
