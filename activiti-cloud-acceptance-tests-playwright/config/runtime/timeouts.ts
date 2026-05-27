/**
 * Central timeout and polling configuration for Playwright acceptance tests.
 * Override via env (milliseconds) when needed, e.g. PLAYWRIGHT_POLL_QUERY_SYNC_MS=90000
 */

export type PollProfile = 'default' | 'querySync' | 'auditEvents' | 'processStatus' | 'signalProcess';

function envMs(name: string, fallback: number): number {
    const raw = process.env[name]?.trim();
    if (!raw) {
        return fallback;
    }
    const parsed = Number.parseInt(raw, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export const timeouts = {
    test: envMs('PLAYWRIGHT_TEST_TIMEOUT_MS', 60_000),
    expect: envMs('PLAYWRIGHT_EXPECT_TIMEOUT_MS', 10_000),
    action: envMs('PLAYWRIGHT_ACTION_TIMEOUT_MS', 15_000),
    navigation: envMs('PLAYWRIGHT_NAVIGATION_TIMEOUT_MS', 30_000),

    http: {
        default: envMs('PLAYWRIGHT_HTTP_TIMEOUT_MS', 15_000),
        healthCheck: envMs('PLAYWRIGHT_HTTP_HEALTH_TIMEOUT_MS', 10_000),
        portForwardReady: envMs('PLAYWRIGHT_PORT_FORWARD_READY_MS', 10_000),
        localPortProbe: envMs('PLAYWRIGHT_LOCAL_PORT_PROBE_MS', 5_000),
    },

    poll: {
        default: envMs('PLAYWRIGHT_POLL_DEFAULT_MS', 30_000),
        querySync: envMs('PLAYWRIGHT_POLL_QUERY_SYNC_MS', 60_000),
        auditEvents: envMs('PLAYWRIGHT_POLL_AUDIT_EVENTS_MS', 20_000),
        processStatus: envMs('PLAYWRIGHT_POLL_PROCESS_STATUS_MS', 30_000),
        signalProcess: envMs('PLAYWRIGHT_POLL_SIGNAL_PROCESS_MS', 60_000),
    },

    intervals: {
        fast: [500, 1000] as const,
        standard: [500, 1000, 2000] as const,
    },
} as const;

const pollTimeoutByProfile: Record<PollProfile, number> = {
    default: timeouts.poll.default,
    querySync: timeouts.poll.querySync,
    auditEvents: timeouts.poll.auditEvents,
    processStatus: timeouts.poll.processStatus,
    signalProcess: timeouts.poll.signalProcess,
};

export function pollOptions(
    profile: PollProfile = 'default',
    intervals: readonly number[] = timeouts.intervals.standard
): { timeout: number; intervals: number[] } {
    return {
        timeout: pollTimeoutByProfile[profile],
        intervals: [...intervals],
    };
}
