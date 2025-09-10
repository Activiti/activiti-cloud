/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import dotenv from 'dotenv';
import { paths } from './paths';
const { env } = process;

dotenv.config({ path: paths.dotEnvPath });

export const users = {
    hruser: {
        username: env.HRUSER_USERNAME || 'no-HRUSER_USERNAME-data',
        password: env.HRUSER_PASSWORD || 'no-HRUSER_PASSWORD-data',
    },
    processadmin: {
        username: env.PROCESSADMINUSER_USERNAME || 'no-PROCESSADMINUSER_USERNAME-data',
        password: env.PROCESSADMINUSER_PASSWORD || 'no-PROCESSADMINUSER_PASSWORD-data',
    },
    modeler: {
        username: env.MODELER_USERNAME || 'no-MODELER_USERNAME-data',
        password: env.MODELER_PASSWORD || 'no-MODELER_PASSWORD-data',
    },
    modelerqa: {
        username: env.MODELERQA_USERNAME || 'no-MODELERQA_USERNAME-data',
        password: env.MODELERQA_PASSWORD || 'no-MODELERQA_PASSWORD-data',
    },
    devopsuser: {
        username: env.DEVOPSUSER_USERNAME || 'no-DEVOPSUSER_USERNAME-data',
        password: env.DEVOPSUSER_PASSWORD || 'no-DEVOPSUSER_PASSWORD-data',
    },
    superadminuser: {
        username: env.SUPERADMINUSER_USERNAME || 'no-SUPERADMINUSER_USERNAME-data',
        password: env.SUPERADMINUSER_PASSWORD || 'no-SUPERADMINUSER_PASSWORD-data',
    },
    alfrescoAdministrator: {
        username: env.ALFRESCO_ADMINISTRATOR_USERNAME || 'no-ALFRESCO_ADMINISTRATOR_USERNAME-data',
        password: env.ALFRESCO_ADMINISTRATOR_PASSWORD || 'no-ALFRESCO_ADMINISTRATOR_PASSWORD-data',
    },
    salesUser: {
        username: env.SALESUSER_USERNAME || 'no-SALESUSER_USERNAME-data',
        password: env.SALESUSER_PASSWORD || 'no-SALESUSER_PASSWORD-data',
    },
    testAdminUser: {
        username: env.TESTADMIN_USERNAME || 'no-TESTADMIN_USERNAME-data',
        password: env.TESTADMIN_PASSWORD || 'no-TESTADMIN_PASSWORD-data',
    },
    testUser: {
        username: env.TESTUSER_USERNAME || 'no-TESTUSER_USERNAME-data',
        password: env.TESTUSER_PASSWORD || 'no-TESTUSER_PASSWORD-data',
    }
};
