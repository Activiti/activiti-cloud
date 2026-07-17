/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { timeouts } from './config/runtime/timeouts';

const { env } = process;
const environmentVariables = [
    'GITHUB_SERVER_URL',
    'GITHUB_REPOSITORY',
    'GITHUB_RUN_ID',
    'GITHUB_RUN_ATTEMPT',
    'REPORT_PORTAL_TRIGGER',
    'REPORT_PORTAL_ENVIRONMENT',
    'GITHUB_EVENT_NAME',
    'BRANCH_NAME',
];

export const getReportPortalConfig = () => {
    environmentVariables.forEach((envVar) => {
        if (!env[envVar]) {
            console.warn(`Missing environment variable: ${envVar}`);
            env[envVar] = 'No value';
        }
    });

    let title = `Run on GitHub Actions ${env.GITHUB_RUN_ID}`;

    const runAttempt = `${env.GITHUB_RUN_ATTEMPT}`;
    if (runAttempt !== '1') {
        title += ` (attempt #${env.GITHUB_RUN_ATTEMPT})`;
    }

    const url = `${env.GITHUB_SERVER_URL}/${env.GITHUB_REPOSITORY}/actions/runs/${env.GITHUB_RUN_ID}/attempts/${runAttempt}`;
    const attributes = [
        {
            key: 'ghrun',
            value: env.GITHUB_RUN_ID,
        },
        {
            key: 'event',
            value: env.GITHUB_EVENT_NAME,
        },
        {
            key: 'repository',
            value: env.GITHUB_REPOSITORY,
        },
        {
            key: 'branch',
            value: env.BRANCH_NAME,
        },
        {
            key: 'flavour',
            value: 'activiti-cloud',
        },
        {
            key: 'trigger',
            value: env.REPORT_PORTAL_TRIGGER,
        },
        {
            key: 'environment',
            value: env.REPORT_PORTAL_ENVIRONMENT,
        },
        {
            key: 'environmentType',
            value: 'acceptance-tests',
        },
    ];

    return {
        apiKey: env.REPORT_PORTAL_TOKEN,
        endpoint: `${env.REPORT_PORTAL_URL}`,
        project: `${env.REPORT_PORTAL_PROJECT}`,
        launch: `${env.REPORT_PORTAL_LAUNCH_KEY}`,
        includeTestSteps: false,
        restClientConfig: {
            timeout: timeouts.test,
        },
        attributes,
        description: `[${title}](${url})`,
    };
};
