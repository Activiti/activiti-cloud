package org.activiti.cloud.services.query.app.repository.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class CustomFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions
            .getFunctionRegistry()
            .patternDescriptorBuilder(CustomSQLFunction.COUNT_OVER_FULL_WINDOW.name(), "COUNT(*) OVER ()")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.LONG)
            )
            .setExactArgumentCount(0)
            .register();
    }
}
