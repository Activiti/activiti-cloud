import { isDevelopProfile } from './cluster-profile';

export interface PortForwardTarget {
    namespace: string;
    service: string;
    label: string;
}

const CANDIDATES: PortForwardTarget[] = [
    { namespace: 'traefik', service: 'traefik', label: 'traefik/traefik' },
    { namespace: 'default', service: 'ingress-nginx-controller', label: 'default/ingress-nginx-controller' },
];

export function getPortForwardTarget(): PortForwardTarget {
    const ns = process.env.PORT_FORWARD_NAMESPACE?.trim();
    const svc = process.env.PORT_FORWARD_SERVICE?.trim();

    if (ns && svc) {
        return { namespace: ns, service: svc, label: `${ns}/${svc}` };
    }

    if (!ns && !svc) {
        return isDevelopProfile() ? CANDIDATES[0] : CANDIDATES[1];
    }

    if (ns === 'traefik' && (!svc || svc === 'traefik')) {
        return CANDIDATES[0];
    }

    return {
        namespace: ns || 'default',
        service: svc || 'ingress-nginx-controller',
        label: `${ns || 'default'}/${svc || 'ingress-nginx-controller'}`,
    };
}

export function getPortForwardKubectlArgs(localPort: string): string[] {
    const target = getPortForwardTarget();
    return ['port-forward', `svc/${target.service}`, `${localPort}:80`, '-n', target.namespace];
}

export function getPortForwardHelpCommand(localPort: string): string {
    return `kubectl ${getPortForwardKubectlArgs(localPort).join(' ')}`;
}
