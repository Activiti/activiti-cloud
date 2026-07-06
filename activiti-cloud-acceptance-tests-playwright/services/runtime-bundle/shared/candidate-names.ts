export function candidateNamesFromList(
    items: Record<string, unknown>[],
    field: 'user' | 'group'
): string[] {
    return items
        .map((item) => item[field])
        .filter((value): value is string => typeof value === 'string');
}
