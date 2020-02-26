/*
 * Copyright 2005-2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.activiti.cloud.services.modeling.security;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import org.activiti.cloud.services.common.security.keycloak.test.support.WithMockKeycloakUser;

/**
 * Annotation for testing with mock modeler user
 */
@Retention(RUNTIME)
@WithMockKeycloakUser(roles = {"ACTIVITI_MODELER"})
public @interface WithMockModelerUser {

}
