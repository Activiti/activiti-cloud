package org.activiti.cloud.starter.audit.configuration;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventModel;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.springframework.beans.factory.annotation.Autowired;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger Api Models
 */
public class PayloadsDocketCustomizer implements DocketCustomizer {
    
    @Autowired
    private TypeResolver typeResolver;
    

    public Docket customize(final Docket docket) {
        ResolvedType resourceTypeWithWildCard = typeResolver.resolve(CloudRuntimeEvent.class,
                                                                     WildcardType.class,
                                                                     CloudRuntimeEventType.class);
        
        return docket.forCodeGeneration(true)
                     .alternateTypeRules(AlternateTypeRules.newRule(resourceTypeWithWildCard,
                                                                    CloudRuntimeEventModel.class)
                                         );            
   }
}