#!/usr/bin/env python3
"""Apply hostAliases, Keycloak env, and optional policy mounts in a single deployment update."""

from __future__ import annotations

import copy
import json
import subprocess
import sys
from typing import Any


def load_deployment(namespace: str, name: str) -> dict[str, Any]:
    result = subprocess.run(
        ["kubectl", "get", "deployment", name, "-n", namespace, "-o", "json"],
        check=True,
        capture_output=True,
        text=True,
    )
    return json.loads(result.stdout)


def upsert_env(container: dict[str, Any], name: str, value: str) -> None:
    env = container.setdefault("env", [])
    for item in env:
        if item.get("name") == name:
            item["value"] = value
            return
    env.append({"name": name, "value": value})


def upsert_volume(spec: dict[str, Any], volume: dict[str, Any]) -> None:
    volumes = spec.setdefault("volumes", [])
    for idx, existing in enumerate(volumes):
        if existing.get("name") == volume["name"]:
            volumes[idx] = volume
            return
    volumes.append(volume)


def upsert_mount(container: dict[str, Any], mount: dict[str, Any]) -> None:
    mounts = container.setdefault("volumeMounts", [])
    for idx, existing in enumerate(mounts):
        if existing.get("name") == mount["name"]:
            mounts[idx] = mount
            return
    mounts.append(mount)


def remove_mounts_and_volumes(spec: dict[str, Any], container: dict[str, Any], name: str) -> None:
    mounts = container.get("volumeMounts", [])
    container["volumeMounts"] = [m for m in mounts if m.get("name") != name]
    volumes = spec.get("volumes", [])
    spec["volumes"] = [v for v in volumes if v.get("name") != name]


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: patch-acceptance-deployment.py <patch.json>", file=sys.stderr)
        return 2

    patch = json.loads(sys.argv[1])
    namespace = patch["namespace"]
    deployment = patch["deployment"]

    doc = load_deployment(namespace, deployment)
    before = copy.deepcopy(doc["spec"]["template"])
    spec = doc["spec"]["template"]["spec"]
    container = spec["containers"][0]

    if patch.get("hostAliases"):
        spec["hostAliases"] = patch["hostAliases"]

    if patch.get("keycloakUrl"):
        upsert_env(container, "ACT_KEYCLOAK_URL", patch["keycloakUrl"])
    if patch.get("keycloakRealm"):
        upsert_env(container, "ACT_KEYCLOAK_REALM", patch["keycloakRealm"])
    if patch.get("jwtIssuerUri"):
        upsert_env(
            container,
            "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI",
            patch["jwtIssuerUri"],
        )
    if patch.get("jwtJwkSetUri"):
        upsert_env(
            container,
            "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI",
            patch["jwtJwkSetUri"],
        )

    if patch.get("policyConfig"):
        upsert_env(container, "SPRING_CONFIG_IMPORT", patch["policyConfig"])
        upsert_volume(
            spec,
            {
                "name": "acceptance-security-policies",
                "configMap": {"name": "acceptance-security-policies"},
            },
        )
        upsert_mount(
            container,
            {
                "name": "acceptance-security-policies",
                "mountPath": "/config/acceptance",
                "readOnly": True,
            },
        )

    if patch.get("supplementalProcesses"):
        upsert_volume(
            spec,
            {
                "name": "acceptance-supplemental-processes",
                "configMap": {"name": "acceptance-supplemental-processes"},
            },
        )
        upsert_mount(
            container,
            {
                "name": "acceptance-supplemental-processes",
                "mountPath": "/config/acceptance-supplemental-processes",
                "readOnly": True,
            },
        )
        upsert_env(container, "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_0", patch["processClasspath"])
        upsert_env(container, "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_1", patch["processSupplemental"])
        upsert_env(container, "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_0", patch["processClasspath"])
        upsert_env(container, "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_1", patch["processSupplemental"])
    elif patch.get("policyConfig") and patch.get("runtimeBundle"):
        remove_mounts_and_volumes(spec, container, "acceptance-supplemental-processes")
        upsert_env(container, "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_0", patch["processClasspath"])
        upsert_env(container, "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_0", patch["processClasspath"])
        for name in (
            "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_1",
            "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_1",
        ):
            env = container.setdefault("env", [])
            container["env"] = [e for e in env if e.get("name") != name]

    if before == doc["spec"]["template"]:
        print(f"UNCHANGED:{deployment}")
        return 0

    apply = subprocess.run(
        ["kubectl", "apply", "-f", "-"],
        input=json.dumps(doc),
        text=True,
        capture_output=True,
    )
    if apply.returncode != 0:
        print(apply.stderr, file=sys.stderr)
        return apply.returncode
    print(f"CHANGED:{deployment}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
