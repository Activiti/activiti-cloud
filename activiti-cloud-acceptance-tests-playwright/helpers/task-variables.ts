/*
 * Task variable assertions — query sync via expectPoll.
 */

import { QueryService } from '../services/query.service';
import { expectPoll } from './expect-poll';

function normalizeValues(value: unknown): unknown[] {
    if (Array.isArray(value)) {
        return value;
    }
    return value !== undefined ? [value] : [];
}

export async function expectTaskVariable(
    queryService: QueryService,
    taskId: string,
    variableName: string
): Promise<void> {
    await expectPoll(async () => {
        const variables = await queryService.getTaskVariables(taskId);
        return variables.some((variable) => variable.name === variableName);
    }, 'querySync').toBe(true);
}

export async function expectTaskVariableValue(
    queryService: QueryService,
    taskId: string,
    variableName: string,
    expectedValue: unknown
): Promise<void> {
    await expectPoll(async () => {
        const variables = await queryService.getTaskVariables(taskId);
        const match = variables.find((variable) => variable.name === variableName);
        if (!match) {
            return false;
        }
        const actual = normalizeValues(match.value);
        const expected = normalizeValues(expectedValue);
        return (
            actual.length === expected.length &&
            actual.every((item, index) => String(item) === String(expected[index]))
        );
    }, 'querySync').toBe(true);
}
