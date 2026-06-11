/*
 * Worker- and test-scoped identifiers for parallel-safe API tests.
 * Every mutable resource should use scopedName / scopedBusinessKey unless the test
 * intentionally asserts on a fixed catalog value (process definition key, etc.).
 */

import type { TestInfo } from '@playwright/test';

export interface TestScope {
    workerIndex: number;
    parallelIndex: number;
    testId: string;
    /** Short id embedded in resource names (worker + parallel slot + time). */
    shortId: string;
    /** Prefix for names / business keys: pw-w0p1-abc123- */
    prefix: string;
}

export function getTestScope(testInfo: TestInfo): TestScope {
    const workerIndex = testInfo.workerIndex;
    const parallelIndex = testInfo.parallelIndex;
    const timePart = Date.now().toString(36);
    const shortId = `w${workerIndex}p${parallelIndex}-${timePart}`;
    const prefix = `pw-${shortId}-`;

    return {
        workerIndex,
        parallelIndex,
        testId: testInfo.testId,
        shortId,
        prefix,
    };
}

export function scopedName(scope: TestScope, label: string): string {
    const sanitized = label.replace(/[^a-zA-Z0-9_-]/g, '_');
    return `${scope.prefix}${sanitized}`.slice(0, 255);
}

export function scopedBusinessKey(scope: TestScope, label = 'bk'): string {
    return scopedName(scope, label);
}
