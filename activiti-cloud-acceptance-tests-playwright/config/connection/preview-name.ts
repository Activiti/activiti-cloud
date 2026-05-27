/**
 * Build preview namespace name (same rules as scripts/lib/cluster-discovery.sh).
 */

export function previewNameFromEnvName(
    envName: string,
    broker: string = process.env.MESSAGING_BROKER ?? 'rabbitmq',
    partitioned: string = process.env.MESSAGING_PARTITIONED ?? 'non-partitioned',
    destinations: string = process.env.MESSAGING_DESTINATIONS ?? 'default'
): string {
    const partitionedSuffix =
        partitioned === 'true' || partitioned === 'partitioned' || partitioned === 'prefix' ? 'p' : 'n';

    let destinationsSuffix = 'd';
    if (destinations === 'override' || destinations === 'override-destinations') {
        destinationsSuffix = 'o';
    } else if (destinations === 'pdb') {
        destinationsSuffix = 'p';
    }

    const brokerShort = broker.slice(0, 6);
    return `pr-${envName}-${brokerShort}-${partitionedSuffix}-${destinationsSuffix}`;
}
