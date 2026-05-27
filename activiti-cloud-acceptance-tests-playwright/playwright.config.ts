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

import './config/load-env';
import { defineConfig } from '@playwright/test';
import { applyResolvedHostsToEnv } from './config/connection/env-hosts';
import { paths } from './paths';
import { getTestConfiguration } from './config/runtime/test-configuration';
import { timeouts } from './config/runtime/timeouts';
applyResolvedHostsToEnv();

const testConfig = getTestConfiguration();
const workers = Number(process.env.PLAYWRIGHT_WORKERS ?? (process.env.CI ? '4' : '4'));

export default defineConfig({
  testDir: './tests',
  timeout: timeouts.test,
  expect: {
    timeout: timeouts.expect,
  },

  // Test configuration
  fullyParallel: true,
  //forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  forbidOnly: !!process.env.CI,
  workers,
  maxFailures: process.env.CI ? 10 : undefined,

  reporter: [
    ['html'],
    ['list'],
    ['junit', { outputFile: `${paths.reporter}/junit.xml` }],
    ['json', { outputFile: `${paths.reporter}/results.json` }],
    ...(process.env.CI ? ([['github']] as const) : []),
  ],

  outputDir: paths.testResults,

  use: {
    baseURL: testConfig.baseURL,
    trace: process.env.CI ? 'on-first-retry' : 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: timeouts.action,
    navigationTimeout: timeouts.navigation,
  },

  projects: [
    {
      name: 'identity-adapter',
      testMatch: 'tests/identity-adapter.spec.ts',
    },
    {
      name: 'security-policies',
      testMatch: 'tests/*security-policies.spec.ts',
    },
    {
      name: 'process-instance-actions',
      testMatch: 'tests/process-instance-actions.spec.ts',
    },
    {
      name: 'runtime-process-instance',
      testMatch: 'tests/runtime/process-instance*.spec.ts',
    },
    {
      name: 'runtime-tasks',
      testMatch: 'tests/runtime/task*.spec.ts',
    },
    {
      name: 'runtime',
      testMatch: 'tests/runtime/**/*.spec.ts',
    },
    {
      name: 'all-tests',
      testMatch: 'tests/**/*.spec.ts',
    }
  ],

  globalSetup: './config/lifecycle/global-setup.ts',
  globalTeardown: testConfig.usePortForwarding ? './config/lifecycle/global-teardown.ts' : undefined,
});
