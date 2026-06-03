import { spawn } from 'child_process';
import * as path from 'path';

/** Monorepo root (activiti-cloud/), two levels above package config/. */
export const REPO_ROOT = path.join(__dirname, '..', '..', '..', '..');

export function runCommandInherit(command: string, args: string[]): Promise<void> {
    return new Promise((resolve, reject) => {
        const child = spawn(command, args, {
            cwd: REPO_ROOT,
            stdio: 'inherit',
            env: process.env,
        });
        child.on('error', reject);
        child.on('close', (code) => {
            if (code === 0) {
                resolve();
            } else {
                reject(new Error(`${command} ${args.join(' ')} exited with code ${code}`));
            }
        });
    });
}
