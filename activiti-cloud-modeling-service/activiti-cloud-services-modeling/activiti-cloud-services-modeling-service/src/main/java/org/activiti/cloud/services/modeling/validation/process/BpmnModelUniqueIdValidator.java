package org.activiti.cloud.services.modeling.validation.process;

import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.core.error.SyntacticModelValidationException;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class BpmnModelUniqueIdValidator implements BpmnCommonModelValidator {

    public static final String DUPLICATED_PROCESS_ID_ERROR = "Process ids must be unique";
    public static final String DUPLICATED_PROCESS_ID_ERROR_DESCRIPTION = "Process [name: '%s', id: '%s'] must be unique in the process workspace";
    public static final String DUPLICATED_ID_PROCESS_NAME = "BPMN process id duplicated";

    private final ProcessModelContentConverter processModelContentConverter;
    private ProcessModelType processModelType;

    public BpmnModelUniqueIdValidator(ProcessModelType processModelType, ProcessModelContentConverter processModelContentConverter){
        this.processModelContentConverter = processModelContentConverter;
        this.processModelType = processModelType;
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext) {
        List<ModelValidationError> aggregatedErrors = new ArrayList();
        List<Process> projectProcesses = getAllBpmnProcesses(validationContext);
        List<Process> duplicatedProcesses = findDuplicatedProcesses(bpmnModel, projectProcesses);

        duplicatedProcesses.stream().forEach(process ->
            aggregatedErrors.add(createModelValidationError(DUPLICATED_PROCESS_ID_ERROR,
            format(DUPLICATED_PROCESS_ID_ERROR_DESCRIPTION, process.getName(), process.getId()),
                DUPLICATED_ID_PROCESS_NAME,
            null,
                process.getId())));

        return aggregatedErrors.stream();
    }

    private List<Process> findDuplicatedProcesses(BpmnModel bpmnModel, List<Process> projectProcesses) {
        return bpmnModel.getProcesses()
            .stream().filter(
                process ->
                    projectProcesses.stream()
                        .filter(checkProcess -> checkProcess.getId().equals(process.getId()))
                        .collect(Collectors.toList()).size() >= 2
                ).collect(Collectors.toList());
    }

    private List<Process> getAllBpmnProcesses(ValidationContext validationContext) {
        return validationContext
            .getAvailableModels(processModelType).stream()
            .map(model -> processContentToBpmnModel(model.getContent()))
            .flatMap(currentModel -> currentModel.getProcesses().stream()).collect(Collectors.toList());
    }

    private BpmnModel processContentToBpmnModel(byte[] processContent) {
        try {
            return processModelContentConverter.convertToBpmnModel(processContent);
        } catch (IOException | XMLStreamException | XMLException ex) {
            Throwable errorCause = Optional.ofNullable(ex.getCause())
                .filter(XMLStreamException.class::isInstance)
                .orElse(ex);
            String messageError = "Syntactic process model XML validation errors encountered: " + errorCause;
            throw new SyntacticModelValidationException(messageError,
                errorCause);
        }
    }
}
