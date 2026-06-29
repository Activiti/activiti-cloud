import * as path from 'path';
/*
 * Copyright © 2005 - 2021 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

/** Package root (activiti-cloud-acceptance-tests-playwright/), independent of process.cwd(). */
const packageRoot = path.resolve(__dirname, '..');

const rootFolder = 'test-results';

export const paths = {
    packageRoot,
    rootFolder,
    reporter: path.join(packageRoot, 'reporter'),
    playswagCoverage: path.join(packageRoot, 'playswag-coverage'),
    openapiSpecs: path.join(packageRoot, 'playswag-coverage', 'specs'),
    resources: path.join(packageRoot, 'resources'),
    modelingProjects: {
        /** BPMN + extensions mounted when chart RB image lacks a process (see docs/MODELING_PROJECTS.md). */
        acceptance: path.join(packageRoot, 'resources', 'modeling-projects', 'acceptance'),
    },
    testResults: path.resolve(process.cwd(), rootFolder),
    portForwardPidFile: path.join(packageRoot, 'port-forward.pid'),
    cluster: {
        dir: path.join(packageRoot, 'config', 'cluster'),
        securityPoliciesFile: path.join(packageRoot, 'config', 'cluster', 'acceptance-security-policies.properties'),
    },
    /** Local secrets and preview settings (gitignored). Never load .env.example at runtime. */
    dotEnvFile: path.join(packageRoot, '.env'),
    dotEnvExampleFile: path.join(packageRoot, '.env.example'),
};
