package org.activiti.cloud.services.core;

import org.springframework.core.convert.converter.Converter;

public interface SpringProcessVariableValueConverter<T> extends Converter<String, T>  {

}
