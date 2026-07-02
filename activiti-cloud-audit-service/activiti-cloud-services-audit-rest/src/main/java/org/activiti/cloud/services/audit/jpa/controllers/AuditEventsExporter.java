/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.audit.jpa.controllers;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvChainedException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.jpa.controllers.csv.CsvLogEntry;
import org.activiti.cloud.services.audit.jpa.controllers.csv.ObjectToJsonStrategy;
import tools.jackson.databind.ObjectMapper;

public class AuditEventsExporter {

    private ObjectToJsonStrategy objectToJsonStrategy;

    public AuditEventsExporter(ObjectMapper objectMapper) {
        objectToJsonStrategy = new ObjectToJsonStrategy(objectMapper);
    }

    public void writeHeader(HttpServletResponse response) throws IOException, CsvFieldAssignmentException {
        PrintWriter writer = response.getWriter();
        String[] header = objectToJsonStrategy.generateHeader(CsvLogEntry.class);
        CSVWriter csvWriter = new CSVWriter(
            writer,
            ICSVWriter.DEFAULT_SEPARATOR,
            ICSVWriter.DEFAULT_QUOTE_CHARACTER,
            ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
            ICSVWriter.DEFAULT_LINE_END
        );
        csvWriter.writeNext(header, true);
        csvWriter.flushQuietly();
        writer.flush();
    }

    public void writeRows(List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events, HttpServletResponse response)
        throws IOException, CsvFieldAssignmentException, CsvChainedException {
        if (events.isEmpty()) {
            return;
        }

        PrintWriter writer = response.getWriter();
        CSVWriter csvWriter = new CSVWriter(
            writer,
            ICSVWriter.DEFAULT_SEPARATOR,
            ICSVWriter.DEFAULT_QUOTE_CHARACTER,
            ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
            ICSVWriter.DEFAULT_LINE_END
        );

        for (CloudRuntimeEvent<?, CloudRuntimeEventType> event : events) {
            CsvLogEntry entry = new CsvLogEntry(event);
            String[] line = objectToJsonStrategy.transmuteBean(entry);
            csvWriter.writeNext(line, true);
        }

        csvWriter.flushQuietly();
        writer.flush();
    }
}
