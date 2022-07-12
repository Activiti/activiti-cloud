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

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class AuditEventsExporter {

    private static final String HEADER_ATTACHMENT_FILENAME = "attachment;filename=";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CSV_CONTENT_TYPE = "text/csv";


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
        StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder<List<CloudRuntimeEvent>>(writer).build();
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

    public class CsvLogEntry implements CloudRuntimeEvent{

        private String time;
        private Integer sequenceNumber;
        private String messageId;
        private String entityId;
        private String id;
        private Object entity;
        private Enum<?> eventType;
        private String appVersion;
        private String serviceVersion;
        private String serviceType;
        private String serviceFullName;
        private String processInstanceId;
        private String appName;
        private String serviceName;
        private String businessKey;
        private String parentProcessInstanceId;
        private String processDefinitionId;
        private String processDefinitionKey;
        private Integer processDefinitionVersion;

        public CsvLogEntry(CloudRuntimeEvent event) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            this.time = dateFormat.format(new Date(event.getTimestamp()));
            this.sequenceNumber = event.getSequenceNumber();
            this.messageId = event.getMessageId();
            this.entityId = event.getEntityId();
            this.id = event.getId();
            this.entity = event.getEntity();
            this.eventType = event.getEventType();
            this.appVersion = event.getAppVersion();
            this.serviceVersion = event.getServiceVersion();
            this.serviceType = event.getServiceType();
            this.serviceFullName = event.getServiceFullName();
            this.processInstanceId = event.getProcessInstanceId();
            this.appName = event.getAppName();
            this.serviceName = event.getServiceName();
            this.businessKey = event.getBusinessKey();
            this.parentProcessInstanceId = event.getParentProcessInstanceId();
            this.processDefinitionId = event.getProcessDefinitionId();
            this.processDefinitionKey = event.getProcessDefinitionKey();
            this.processDefinitionVersion = event.getProcessDefinitionVersion();
        }

        public String getTime() {
            return this.time;
        }

        @Override
        public Integer getSequenceNumber() {
            return this.sequenceNumber;
        }

        @Override
        public String getMessageId() {
            return this.messageId;
        }

        @Override
        public String getEntityId() {
            return this.entityId;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public Object getEntity() {
            return this.entity;
        }

        @Override
        public Long getTimestamp() {
            // ignoring, replaced by time property that returns human-readable date
            return null;
        }

        @Override
        public Enum<?> getEventType() {
            return this.eventType;
        }

        @Override
        public String getProcessInstanceId() {
            return this.processInstanceId;
        }

        @Override
        public String getParentProcessInstanceId() {
            return this.parentProcessInstanceId;
        }

        @Override
        public String getProcessDefinitionId() {
            return this.processDefinitionId;
        }

        @Override
        public String getProcessDefinitionKey() {
            return this.processDefinitionKey;
        }

        @Override
        public Integer getProcessDefinitionVersion() {
            return this.processDefinitionVersion;
        }

        @Override
        public String getBusinessKey() {
            return this.businessKey;
        }

        @Override
        public String getAppName() {
            return this.appName;
        }

        @Override
        public String getServiceName() {
            return this.serviceName;
        }

        @Override
        public String getServiceFullName() {
            return this.serviceFullName;
        }

        @Override
        public String getServiceType() {
            return this.serviceType;
        }

        @Override
        public String getServiceVersion() {
            return this.serviceVersion;
        }

        @Override
        public String getAppVersion() {
            return this.appVersion;
        }
    }
}
