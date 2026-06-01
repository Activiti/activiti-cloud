/**
 * Single entry point for Playwright env bootstrap: load .env (local) and resolve gateway/identity hosts.
 */
import './load-env';
import { applyResolvedHostsToEnv } from './connection/env-hosts';

export function bootstrapAcceptanceEnv(): void {
    applyResolvedHostsToEnv();
}
