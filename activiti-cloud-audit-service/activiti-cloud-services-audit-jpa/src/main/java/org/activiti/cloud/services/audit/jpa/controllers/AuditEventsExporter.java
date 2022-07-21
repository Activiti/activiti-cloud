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
package org.activiti.cloud.services.audit.jpa.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.jpa.controllers.csv.CsvLogEntry;
import org.activiti.cloud.services.audit.jpa.controllers.csv.ObjectToJsonStrategy;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AuditEventsExporter {


    private static final String HEADER_ATTACHMENT_FILENAME = "attachment;filename=";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CSV_CONTENT_TYPE = "text/csv";

    private ObjectToJsonStrategy objectToJsonStrategy;

    public AuditEventsExporter(ObjectMapper objectMapper) {
        objectToJsonStrategy = new ObjectToJsonStrategy(objectMapper);
    }

    public void exportCsv(List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events,
                          String fileName,
                          HttpServletResponse response) throws Exception {
        setHttpHeaders(fileName, response);
        writeEventsAsCsv(events, response);
    }

    private void setHttpHeaders(String fileName, HttpServletResponse response) {
        response.setContentType(CSV_CONTENT_TYPE);
        response.setHeader(HEADER_CONTENT_DISPOSITION, HEADER_ATTACHMENT_FILENAME + fileName);
    }

    private void writeEventsAsCsv(List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events,
                              HttpServletResponse response) throws Exception {

        List<CsvLogEntry> entries = toCsvLogEntryList(events);

        PrintWriter writer = response.getWriter();
        StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder<List<CloudRuntimeEvent>>(writer)
                                          .withMappingStrategy(objectToJsonStrategy)
                                          .build();
        beanToCsv.write(entries);
        writer.close();
    }

    private List<CsvLogEntry> toCsvLogEntryList(List<CloudRuntimeEvent<?, CloudRuntimeEventType>> events) {
        List<CsvLogEntry> entries = new ArrayList<>();
        for(CloudRuntimeEvent event: events) {
            entries.add(new CsvLogEntry(event));
        }
        return entries;
    }
}
