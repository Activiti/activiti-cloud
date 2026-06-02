/*
 * Colored service personas + vibes for setup, prereqs hand-off, cleanup, and teardown.
 * Parity with scripts/lib/prereqs-progress.sh
 */

import chalk from 'chalk';

export type AcceptanceActor =
    | 'discovery'
    | 'coordinator'
    | 'registry'
    | 'runtime-bundle'
    | 'traefik'
    | 'identity'
    | 'query'
    | 'connector'
    | 'audit'
    | 'keycloak'
    | 'policies';

/** @deprecated use AcceptanceActor */
export type CleanupActor = Exclude<AcceptanceActor, 'discovery'>;

const PROGRESS_START_TS = Date.now();
let vibeTick = 0;

function useColor(): boolean {
    return Boolean(process.stdout.isTTY && !process.env.NO_COLOR);
}

function elapsed(): string {
    return `${Math.floor((Date.now() - PROGRESS_START_TS) / 1000)}s`;
}

function timeStamp(): string {
    return new Date().toTimeString().slice(0, 8);
}

function colorFor(actor: AcceptanceActor): (text: string) => string {
    const map: Record<AcceptanceActor, (text: string) => string> = {
        discovery: chalk.cyan,
        coordinator: chalk.cyan,
        registry: chalk.yellow,
        'runtime-bundle': chalk.green,
        traefik: chalk.blue,
        identity: chalk.magenta,
        query: chalk.cyanBright,
        connector: chalk.greenBright,
        audit: chalk.white,
        keycloak: chalk.redBright,
        policies: chalk.magentaBright,
    };
    return map[actor] ?? chalk.gray;
}

export function acceptanceActorLabel(actor: AcceptanceActor): string {
    const labels: Record<AcceptanceActor, string> = {
        discovery: '🧭  Discovery Squad',
        coordinator: '🧹  Cleanup Squad',
        registry: '📦  Registry Gremlin',
        'runtime-bundle': '⚙️  Runtime-Bundle',
        traefik: '🌐  Traefik DJ',
        identity: '🔐  Identity-Adapter',
        query: '🔍  Query-Service',
        connector: '🔌  Cloud-Connector',
        audit: '📋  Audit-Service',
        keycloak: '👑  Keycloak Realm',
        policies: '📜  Policy Goblins',
    };
    return labels[actor] ?? '☕  Acceptance Coordinator';
}

function vibeLines(actor: AcceptanceActor): string[] {
    const lines: Record<AcceptanceActor, string[]> = {
        discovery: [
            'Playwright global setup — scanning kubeconfig, namespace, and vibes.',
            'If the cluster is reachable, we proceed. If not, we learn patience.',
            'kubectl and I are having a moment. A productive moment.',
        ],
        coordinator: [
            'Sweeping the namespace after the party — no confetti left behind.',
            'Teardown patrol reporting for duty. BPMN ghosts begone.',
            'If it was created in this test, it shall be un-created. Dramatically.',
        ],
        registry: [
            'Registry gremlin verifies BPMN catalog entries exist. Probably.',
            'Process definitions on the guest list — none shall be missing.',
            'HeadersConnectorProcess sends regards. Eventually.',
        ],
        'runtime-bundle': [
            'Runtime-Bundle warming up for acceptance — pods need their beauty sleep.',
            'DELETE sent. The pod heard you. The pod is thinking about it.',
            'Tasks and processes filed under "gone but not forgotten".',
        ],
        traefik: [
            'Traefik is mixing hostnames into the perfect localhost cocktail.',
            'Port-forward incoming — no second terminal required.',
            'Routing vibes only — Host header on :8080, chef\'s kiss.',
        ],
        identity: [
            'Identity-Adapter is syncing with Keycloak. Very professional. Very slow.',
            'Tokens incoming — SSO patience is a virtue. So is coffee.',
        ],
        query: [
            'Query-Service is indexing tasks you have not created yet.',
            'Read models take time. Blame eventual consistency.',
        ],
        connector: [
            'Cloud-Connector is stretching before the acceptance marathon.',
        ],
        audit: [
            'Audit-Service is filing paperwork in triplicate.',
        ],
        keycloak: [
            'Keycloak realm activiti is having a committee meeting.',
        ],
        policies: [
            'Policy Goblins are stapling security rules to ConfigMaps.',
        ],
    };
    return lines[actor] ?? ['Still working. The cluster is thinking very hard.'];
}

function pickVibe(actor: AcceptanceActor): string {
    const lines = vibeLines(actor);
    const idx = (vibeTick + Math.floor(Math.random() * lines.length)) % lines.length;
    vibeTick += 1;
    return lines[idx] ?? lines[0];
}

export function acceptanceLog(actor: AcceptanceActor, message: string): void {
    const time = timeStamp();
    const elapsedLabel = elapsed();
    const tag = acceptanceActorLabel(actor);

    if (useColor()) {
        const color = colorFor(actor);
        const line =
            chalk.dim(`[${time} +${elapsedLabel}]`) +
            ' ' +
            chalk.bold(color(`[${tag}]`)) +
            ' ' +
            message;
        console.log(line);
    } else {
        console.log(`[${time} +${elapsedLabel}] [${tag}] ${message}`);
    }
}

export function acceptancePhase(actor: AcceptanceActor, title: string): void {
    console.log('');
    acceptanceLog(actor, `━━ ${title} ━━`);
    acceptanceLog(actor, pickVibe(actor));
}

export function acceptanceStep(actor: AcceptanceActor, message: string): void {
    acceptanceLog(actor, `→ ${message}`);
}

export function acceptanceVibe(actor: AcceptanceActor): void {
    acceptanceLog(actor, pickVibe(actor));
}

export function inferActorFromPath(path: string): AcceptanceActor {
    const p = path.toLowerCase();
    if (p.includes('/rb/') || p.includes('/runtime')) return 'runtime-bundle';
    if (p.includes('/query')) return 'query';
    if (p.includes('/identity')) return 'identity';
    if (p.includes('/audit')) return 'audit';
    if (p.includes('/connector')) return 'connector';
    if (p.includes('security-policies') || p.includes('/policies')) return 'policies';
    if (p.includes('keycloak')) return 'keycloak';
    if (p.includes('/registry')) return 'registry';
    return 'coordinator';
}

export function describeResourcePath(path: string): string {
    if (path.includes('/process-instances/')) return 'process instance';
    if (path.includes('/tasks/')) return 'task';
    return 'resource';
}

// Aliases used by cleanup / teardown modules
export const cleanupLog = acceptanceLog;
export const cleanupPhase = acceptancePhase;
export const cleanupStep = acceptanceStep;
export const cleanupVibe = acceptanceVibe;
export const cleanupActorLabel = acceptanceActorLabel;
