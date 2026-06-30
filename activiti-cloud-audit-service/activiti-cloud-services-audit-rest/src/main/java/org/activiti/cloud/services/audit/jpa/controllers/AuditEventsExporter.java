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
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.jpa.controllers.csv.CsvLogEntry;
import org.activiti.cloud.services.audit.jpa.controllers.csv.ObjectToJsonStrategy;
import tools.jackson.databind.ObjectMapper;

public class AuditEventsExporter {

    private static final String HEADER_ATTACHMENT_FILENAME = "attachment;filename=";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CSV_CONTENT_TYPE = "text/csv";

    private ObjectToJsonStrategy objectToJsonStrategy;

    public AuditEventsExporter(ObjectMapper objectMapper) {
        objectToJsonStrategy = new ObjectToJsonStrategy(objectMapper);
    }

    public void exportCsv(
        List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events,
        String fileName,
        HttpServletResponse response
    ) throws Exception {
        response.setContentType(CSV_CONTENT_TYPE);
        response.setHeader(HEADER_CONTENT_DISPOSITION, HEADER_ATTACHMENT_FILENAME + fileName);

        List<CsvLogEntry> entries = toCsvLogEntryList(events);
        PrintWriter writer = response.getWriter();
        StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder<CsvLogEntry>(writer)
            .withMappingStrategy(objectToJsonStrategy)
            .build();
        beanToCsv.write(entries);
        writer.close();
    }

    public void writeHeader(HttpServletResponse response) throws Exception {
        PrintWriter writer = response.getWriter();
        String[] header = objectToJsonStrategy.generateHeader(CsvLogEntry.class);
        try (
            CSVWriter csvWriter = new CSVWriter(
                writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ""
            )
        ) {
            csvWriter.writeNext(header, true);
        }
        writer.flush();
    }

    public void writeRows(List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events, HttpServletResponse response)
        throws Exception {
        if (events.isEmpty()) {
            return;
        }

        List<CsvLogEntry> entries = toCsvLogEntryList(events);
        PrintWriter writer = response.getWriter();

        try (
            CSVWriter csvWriter = new CSVWriter(
                writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ""
            )
        ) {
            for (CsvLogEntry entry : entries) {
                String[] line = objectToJsonStrategy.transmuteBean(entry);
                csvWriter.writeNext(line, true);
            }
        }
        writer.flush();
    }

    private List<CsvLogEntry> toCsvLogEntryList(List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events) {
        List<CsvLogEntry> entries = new ArrayList<>();
        for (CloudRuntimeEvent event : events) {
            entries.add(new CsvLogEntry(event));
        }
        return entries;
    }
}
