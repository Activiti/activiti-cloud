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
package org.activiti.cloud.services.query.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "ProcessVariableHistory")
@Table(
    name = "PROCESS_VARIABLE_HISTORY",
    indexes = {
        @Index(name = "idx_pvh_process_var", columnList = "processInstanceId, variableName, eventTime", unique = false),
        @Index(name = "idx_pvh_record_create_time", columnList = "recordCreateTime", unique = false),
    }
)
public class ProcessVariableHistoryEntity {

    @Id
    @GeneratedValue(generator = "process_variable_history_sequence", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
        name = "process_variable_history_sequence",
        sequenceName = "process_variable_history_sequence",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false)
    private String processInstanceId;

    @Column(nullable = false)
    private String variableName;

    private String type;

    @Convert(converter = VariableValueJsonConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "`value`", columnDefinition = "jsonb")
    private VariableValue<?> value;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private Date eventTime;

    @Column(nullable = false)
    private Date recordCreateTime;

    private String messageId;

    private String commandId;

    private Integer sequenceNumber;

    public Long getId() {
        return id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return value != null ? (T) value.getValue() : null;
    }

    public <T> void setValue(T val) {
        this.value = new VariableValue<>(val);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public Date getRecordCreateTime() {
        return recordCreateTime;
    }

    public void setRecordCreateTime(Date recordCreateTime) {
        this.recordCreateTime = recordCreateTime;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProcessVariableHistoryEntity other = (ProcessVariableHistoryEntity) obj;
        return id != null && Objects.equals(id, other.id);
    }
}
