/**
 * Load activiti-cloud-acceptance-tests-playwright/.env for local runs only.
 * CI must use workflow env (PREVIEW_NAME, GATEWAY_HOST, …) — never .env.example.
 */
import dotenv from 'dotenv';
import { existsSync } from 'fs';
import { paths } from './paths';

const isCi = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';

if (existsSync(paths.dotEnvFile)) {
    process.env.DOTENV_CONFIG_QUIET = 'true';
    dotenv.config({
        path: paths.dotEnvFile,
        // In CI, GITHUB_ENV / workflow env wins over a checked-in or copied .env
        override: !isCi,
        quiet: true,
    });
}
