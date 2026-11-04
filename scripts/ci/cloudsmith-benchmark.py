#!/usr/bin/env python3
"""Compare Cloudsmith-only publishing with and without Maven transfer logs."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import resource
import shutil
import subprocess
import time


def measure(command, project, log_file):
    before = resource.getrusage(resource.RUSAGE_CHILDREN)
    started = time.perf_counter()
    lines = 0
    with log_file.open("w") as log:
        with subprocess.Popen(
            command,
            cwd=project,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            env={**os.environ, "MAVEN_ARGS": ""},
        ) as process:
            for line in process.stdout:
                log.write(line)
                print(line, end="", flush=True)
                lines += 1
            exit_code = process.wait()
    after = resource.getrusage(resource.RUSAGE_CHILDREN)
    return {
        "seconds": round(time.perf_counter() - started, 3),
        "cpu_seconds": round(after.ru_utime + after.ru_stime - before.ru_utime - before.ru_stime, 3),
        "exit_code": exit_code,
        "log_lines": lines,
        "log_bytes": log_file.stat().st_size,
        "command": command,
    }


def artifacts(project):
    result = []
    for path in sorted(project.rglob("target/*")):
        if path.is_file() and path.suffix in {".jar", ".json", ".yaml"}:
            result.append({
                "path": str(path.relative_to(project)),
                "bytes": path.stat().st_size,
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            })
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    source, output = args.source.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    global_settings = output / "global-settings.xml"
    global_settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"/>\n')
    warm_cache = output / "warm-m2"

    # Hold deferral constant and disable the second execution in every measured run.
    pom = source / "pom.xml"
    pom.write_text(pom.read_text().replace("<deployAtEnd>true</deployAtEnd>", "<deployAtEnd>false</deployAtEnd>"))
    common = [
        "mvn", "--batch-mode", "--show-version", "--settings", "settings.xml",
        "--global-settings", str(global_settings),
        "-P!cloudsmith,!attach-sources",
        "-DskipTests", "-DskipITs", "-Dskip.common.tests=true", "-Ddocker.skip",
        "-Dlogging.root.level=off", "-Dspring.main.banner-mode=off",
        "-Dhttp.keepAlive=false", "-Dmaven.wagon.http.pool=false",
        "-Dmaven.wagon.httpconnectionManager.ttlSeconds=120",
    ]
    results = {
        "source_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip(),
        "version": args.version,
        "setup": [],
        "measurements": [],
    }

    def save():
        (output / "results.json").write_text(json.dumps(results, indent=2) + "\n")

    for name, goals in [
        ("set-version", ["versions:set", f"-DnewVersion={args.version}", "-DprocessAllModules=true", "-DgenerateBackupPoms=false"]),
        ("warm-build", ["install"]),
    ]:
        result = measure(common + [f"-Dmaven.repo.local={warm_cache}", "--no-transfer-progress"] + goals, source, output / f"{name}.log")
        results["setup"].append({"name": name, **result})
        save()
        if result["exit_code"]:
            return result["exit_code"]

    namespace = os.environ["CLOUDSMITH_NAMESPACE"]
    destination = f"cloudsmith::https://maven.cloudsmith.io/{namespace}/activiti-snapshots/"
    for name, transfer_options in [("verbose", []), ("quiet", ["--no-transfer-progress"])]:
        case_project, case_cache = output / "case-project", output / "case-m2"
        shutil.copytree(source, case_project)
        shutil.copytree(warm_cache, case_cache)
        command = common + [
            f"-Dmaven.repo.local={case_cache}", "deploy",
            f"-DaltDeploymentRepository={destination}",
            f"-DaltReleaseDeploymentRepository={destination}",
            f"-DaltSnapshotDeploymentRepository={destination}",
            "-Daether.connector.basic.upstreamThreads.cloudsmith=10",
        ] + transfer_options
        result = measure(command, case_project, output / f"{name}.log")
        result.update({"name": name, "artifacts": artifacts(case_project)})
        results["measurements"].append(result)
        save()
        shutil.rmtree(case_project)
        shutil.rmtree(case_cache)
        if result["exit_code"]:
            return result["exit_code"]

    summary = [
        "## Cloudsmith-only upload benchmark",
        "",
        f"Source: `{results['source_commit']}`. Maven 3.10.0, Java 25; identical copies of a warm Maven cache and built project.",
        "Both runs upload only to Cloudsmith with immediate deployment. Only transfer logging differs.",
        "",
        "| Transfer logs | Maven deploy seconds | CPU seconds | Log lines |",
        "| --- | ---: | ---: | ---: |",
    ]
    for result in results["measurements"]:
        summary.append(f"| {result['name']} | {result['seconds']} | {result['cpu_seconds']} | {result['log_lines']} |")
    summary += ["", "This is one pair of measurements; repeat in reverse order before attributing a small difference to logging.", ""]
    print("\n".join(summary))
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as stream:
            stream.write("\n".join(summary))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
