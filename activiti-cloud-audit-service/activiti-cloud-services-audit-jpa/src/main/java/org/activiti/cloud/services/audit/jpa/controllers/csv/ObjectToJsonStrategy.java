package org.activiti.cloud.services.audit.jpa.controllers.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvBadConverterException;

public class ObjectToJsonStrategy extends HeaderColumnNameMappingStrategy {

    private ObjectToJsonConvertor objectToJsonConvertor;

    public ObjectToJsonStrategy(ObjectMapper objectMapper) {
        objectToJsonConvertor = new ObjectToJsonConvertor(objectMapper);
        setType(CsvLogEntry.class);
    }

    @Override
    protected BeanField instantiateCustomConverter(Class converter) throws CsvBadConverterException {
        return objectToJsonConvertor;
    }
}
