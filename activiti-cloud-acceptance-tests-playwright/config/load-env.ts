/**
 * Single quiet dotenv load — import this before any module reads process.env from .env.
 */
import dotenv from 'dotenv';
import { paths } from '../paths';

process.env.DOTENV_CONFIG_QUIET = 'true';
dotenv.config({ path: paths.dotEnvPath, override: true, quiet: true });
