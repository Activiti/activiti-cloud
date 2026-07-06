import { CloudProcessDefinition } from '../../../models/process-definition.models';

export function pickHighestVersionByKey(
    definitions: CloudProcessDefinition[],
    key: string
): CloudProcessDefinition {
    const matches = definitions.filter((def) => def.key === key);
    if (matches.length === 0) {
        throw new Error(`No process definition found matching key ${key}`);
    }
    return matches.reduce((best, current) => {
        const bestVersion = parseInt(String(best.appVersion ?? '0'), 10);
        const currentVersion = parseInt(String(current.appVersion ?? '0'), 10);
        return currentVersion > bestVersion ? current : best;
    });
}
