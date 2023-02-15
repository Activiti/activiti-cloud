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

package org.activiti.cloud.services.modeling.service.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import java.io.IOException;

public class ModelExtensionsPrettyPrinter extends DefaultPrettyPrinter {

    private boolean useEmptyObjectSeparator = true;

    @Override
    public ModelExtensionsPrettyPrinter createInstance() {
        return new ModelExtensionsPrettyPrinter()
            .withEmptyObjectSeparator(useEmptyObjectSeparator);
    }

    public ModelExtensionsPrettyPrinter withEmptyObjectSeparator(boolean useEmptyObjectSeparator) {
        this.useEmptyObjectSeparator = useEmptyObjectSeparator;
        return this;
    }

    @Override
    public DefaultPrettyPrinter withSeparators(Separators separators) {
        _separators = separators;
        _objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
        return this;
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException
    {
        if (!_objectIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfEntries > 0) {
            _objectIndenter.writeIndentation(g, _nesting);
        } else {
            if(useEmptyObjectSeparator) {
                g.writeRaw(' ');
            }
        }
        g.writeRaw('}');
    }
}
