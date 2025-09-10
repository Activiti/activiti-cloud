/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { CustomAPIRequest } from "../context.models";

export interface UserContexts {
    hrUserContext: CustomAPIRequest;
    processAdminContext: CustomAPIRequest;
    modelerUserContext: CustomAPIRequest;
    modelerqaUserContext: CustomAPIRequest;
    devopsUserContext: CustomAPIRequest;
    superadminContext: CustomAPIRequest;
    salesUserContext: CustomAPIRequest;
    testAdminUserContext: CustomAPIRequest;
    testUserContext: CustomAPIRequest;
}
