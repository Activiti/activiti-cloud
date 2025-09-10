import dotenv from 'dotenv';
import { defineConfig, devices } from '@playwright/test';
import { paths } from './activiti-cloud-acceptance-tests-playwright/paths';

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
dotenv.config({ path: paths.dotEnvPath });

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './activiti-cloud-acceptance-tests-playwright/tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */

  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['html'],
    ['list'],
    ['junit', { outputFile: `${paths.reporter}/junit.xml` }],
    ['json', { outputFile: `${paths.reporter}/results.json` }]
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: process.env.GATEWAY_HOST || 'http://localhost:8080',

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
    screenshot: 'off',
    video: 'off',
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* Run your local dev server before starting the tests */
  // webServer: {
  //   command: 'npm run start',
  //   url: 'http://127.0.0.1:3000',
  //   reuseExistingServer: !process.env.CI,
  // },

  /* Global setup and teardown */
  globalSetup: require.resolve('./activiti-cloud-acceptance-tests-playwright/setup/global-setup.ts'),
  // globalTeardown: require.resolve('./activiti-cloud-acceptance-tests-playwright/setup/global-teardown.ts'),

  /* Test timeout */
  timeout: 30000,
  expect: {
    timeout: 5000
  },

  /* Output directory for test artifacts */
  outputDir: paths.rootFolder,
});
