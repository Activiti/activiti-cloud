#!/usr/bin/env npx ts-node
/**
 * Preflight check for local Playwright acceptance tests.
 * Usage: npm run check:env [-- identity|security|process|all]
 */

import '../config/load-env';
import { applyResolvedHostsToEnv } from '../config/connection/env-hosts';
import { runPreflightChecks } from '../config/validation/environment-validator';

applyResolvedHostsToEnv();

const project = process.argv[2] || 'all';

async function main(): Promise<void> {
  console.log(`\n🔍 Playwright environment check (project: ${project})\n`);

  const result = await runPreflightChecks(project);

  for (const w of result.warnings) {
    console.warn(`⚠️  ${w}`);
  }

  if (!result.ok) {
    console.error('\n❌ Environment check failed:\n');
    for (const e of result.errors) {
      console.error(`   • ${e}`);
    }
    console.error('\nSee README.md and .env.example\n');
    process.exit(1);
  }

  console.log('\n✅ Environment ready — run tests with npm run test:all\n');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
