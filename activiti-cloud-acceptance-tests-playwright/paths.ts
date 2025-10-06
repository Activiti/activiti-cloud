import * as path from 'path';
/*
 * Copyright © 2005 - 2021 Alfresco Software, Ltd. All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

const rootFolder = 'test-results';

export const paths = {
    rootFolder,
    reporter: path.resolve(`${process.cwd()}/activiti-cloud-acceptance-tests-playwright/reporter`),
    resources: path.resolve(`${process.cwd()}/activiti-cloud-acceptance-tests-playwright/resources`),
    testResults: path.resolve(`${process.cwd()}/${rootFolder}`),
    dotEnvPath: path.resolve(`${process.cwd()}/activiti-cloud-acceptance-tests-playwright/.env`),
};
