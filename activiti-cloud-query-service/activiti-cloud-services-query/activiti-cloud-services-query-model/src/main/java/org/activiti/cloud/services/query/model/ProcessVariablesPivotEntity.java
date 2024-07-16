package org.activiti.cloud.services.query.model;

import jakarta.persistence.*;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "ProcessVariablesPivot")
@Table(
    name = "process_variable_pivot",
    indexes = { @Index(name = "proc_var_processInstanceId_idx", columnList = "processInstanceId", unique = true) }
)
public class ProcessVariablesPivotEntity {

    @Id
    private String processInstanceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "`process_variables`", columnDefinition = "jsonb")
    private Map<String, Object> values;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}
