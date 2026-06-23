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
import { paths } from './config/paths';
import { getTestConfiguration } from './config/runtime/test-configuration';
import { timeouts } from './config/runtime/timeouts';
applyResolvedHostsToEnv();

const testConfig = getTestConfiguration();
const workers = Number(process.env.PLAYWRIGHT_WORKERS ?? (process.env.CI ? '4' : '2'));
const isCi = Boolean(process.env.CI);

/** Serial projects (subscriptions, admin bulk-delete). */
const serial = { workers: 1, fullyParallel: false } as const;

/**
 * CI suite (`npm run test`) selects acceptance → notifications → destructive-last.
 * `dependencies` enforce that order; slice scripts pass a single `--project=…`.
 */
export default defineConfig({
  testDir: './tests',
  timeout: timeouts.test,
  expect: {
    timeout: timeouts.expect,
  },

  fullyParallel: true,
  retries: isCi ? 2 : 0,
  forbidOnly: isCi,
  workers,
  projects: [
    {
      name: 'acceptance',
      grepInvert: /@destructive/,
      testIgnore: '**/notifications.spec.ts',
    },
    {
      name: 'notifications',
      testMatch: '**/notifications.spec.ts',
      dependencies: ['acceptance'],
      ...serial,
    },
    {
      name: 'destructive-last',
      grep: /@destructive/,
      dependencies: ['acceptance', 'notifications'],
      ...serial,
    },
    { name: 'smoke', grep: /@smoke/ },
    { name: 'identity', testDir: './tests/identity' },
    { name: 'security', testDir: './tests/security' },
    { name: 'runtime-process', testDir: './tests/runtime-process' },
    { name: 'runtime-task', testDir: './tests/runtime-task' },
    { name: 'runtime', testDir: './tests/runtime' },
  ],
  maxFailures: isCi ? 10 : undefined,

  reporter: [
    ['html'],
    ['list'],
    ['junit', { outputFile: `${paths.reporter}/junit.xml` }],
    ['json', { outputFile: `${paths.reporter}/results.json` }],
    ...(isCi ? ([['github']] as const) : []),
  ],

  outputDir: paths.testResults,

  use: {
    baseURL: testConfig.baseURL,
    trace: isCi ? 'on-first-retry' : 'retain-on-failure',
    screenshot: isCi ? 'off' : 'only-on-failure',
    video: isCi ? 'off' : 'retain-on-failure',
    actionTimeout: timeouts.action,
    navigationTimeout: timeouts.navigation,
  },

  globalSetup: './config/lifecycle/global-setup.ts',
  globalTeardown: testConfig.usePortForwarding ? './config/lifecycle/global-teardown.ts' : undefined,
});
