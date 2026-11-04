#!/usr/bin/env python3
"""Build the same reactor using current and Cloudsmith-only repositories."""

import argparse
import collections
import json
import os
from pathlib import Path
import resource
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

POM_NS = "http://maven.apache.org/POM/4.0.0"
SETTINGS_NS = "http://maven.apache.org/SETTINGS/1.0.0"
NS = {"m": POM_NS}


def repository_id(key):
    return re.sub(r"-[0-9a-f]{40}$", "", key)


def verify_results(path):
    results = json.loads(path.read_text())
    failed = len(results["measurements"]) != 2
    for result in results["measurements"]:
        origins = result["origins"]
        origins["raw_counts"] = origins.get("raw_counts", dict(origins["counts"]))
        normalized = collections.Counter()
        for key, count in origins["raw_counts"].items():
            normalized[repository_id(key)] += count
        origins["counts"] = dict(normalized)
        for record in origins["records"]:
            record["repository_key"] = record.get("repository_key", record["repository"])
            record["repository"] = repository_id(record["repository_key"])
        failed |= bool(result["exit_code"]) or not bool(origins["records"])
        if result["name"] == "cloudsmith-only":
            result["unexpected_repositories"] = sorted(set(normalized) - {"cloudsmith-dependencies", "cloudsmith-releases"})
            failed |= bool(result["unexpected_repositories"])
    results["origin_validation_passed"] = not failed
    path.write_text(json.dumps(results, indent=2) + "\n")
    summary = ["## Verified cold build results", "", "Both builds ran in the archived benchmark. Maven 3.10 repository-key hash suffixes are normalized before validating origins.", "", "| Case | Build seconds | Maven CPU seconds | Artifact origins |", "| --- | ---: | ---: | --- |"]
    for result in results["measurements"]:
        counts = ", ".join(f"{key}: {value}" for key, value in sorted(result["origins"]["counts"].items()))
        summary.append(f"| {result['name']} | {result['seconds']} | {result['cpu_seconds']} | {counts} |")
    summary += ["", f"Origin validation passed: {not failed}.", ""]
    print("\n".join(summary))
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as stream:
            stream.write("\n".join(summary))
    return int(failed)


def cloudsmith_configuration(project, namespace, settings):
    tree = ET.parse(project / "pom.xml", parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True)))
    root = tree.getroot()

    def repository(parent, tag, identity, slug, snapshots):
        entry = ET.SubElement(parent, f"{{{POM_NS}}}{tag}")
        ET.SubElement(entry, f"{{{POM_NS}}}id").text = identity
        ET.SubElement(entry, f"{{{POM_NS}}}url").text = f"https://dl.cloudsmith.io/basic/{namespace}/{slug}/maven/"
        for policy, enabled in [("releases", True), ("snapshots", snapshots)]:
            element = ET.SubElement(entry, f"{{{POM_NS}}}{policy}")
            ET.SubElement(element, f"{{{POM_NS}}}enabled").text = str(enabled).lower()

    dependencies = root.find("m:repositories", NS)
    plugins = root.find("m:pluginRepositories", NS)
    dependencies.clear()
    plugins.clear()
    repository(dependencies, "repository", "cloudsmith-dependencies", "activiti-snapshots", True)
    repository(dependencies, "repository", "cloudsmith-releases", "activiti-releases", False)
    repository(plugins, "pluginRepository", "cloudsmith-dependencies", "activiti-snapshots", True)
    ET.register_namespace("", POM_NS)
    ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    tree.write(project / "pom.xml", encoding="utf-8", xml_declaration=True)
    settings.write_text(f'''<settings xmlns="{SETTINGS_NS}">
  <mirrors>
    <mirror>
      <id>cloudsmith-dependencies</id>
      <mirrorOf>*,!cloudsmith-dependencies,!cloudsmith-releases</mirrorOf>
      <url>https://dl.cloudsmith.io/basic/{namespace}/activiti-snapshots/maven/</url>
    </mirror>
  </mirrors>
</settings>
''')


def cache_origins(cache):
    origins = collections.Counter()
    records = []
    for tracking in cache.rglob("_remote.repositories"):
        for line in tracking.read_text().splitlines():
            if line.startswith("#") or ">" not in line or "=" not in line:
                continue
            key = line.split("=", 1)[0]
            artifact, repository = key.rsplit(">", 1)
            if repository:
                origins[repository_id(repository)] += 1
                records.append({"artifact": str(tracking.parent.relative_to(cache) / artifact), "repository": repository_id(repository), "repository_key": repository})
    return {"counts": dict(origins), "records": records}


def build(command, project, log_file):
    before = resource.getrusage(resource.RUSAGE_CHILDREN)
    started = time.perf_counter()
    with log_file.open("w") as log:
        environment = {**os.environ, "MAVEN_ARGS": ""}
        for name in ["CLOUDSMITH_USERNAME", "CLOUDSMITH_API_KEY", "MAVEN_USERNAME", "MAVEN_PASSWORD"]:
            environment.pop(name, None)
        with subprocess.Popen(command, cwd=project, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=environment) as process:
            for line in process.stdout:
                log.write(line)
                print(line, end="", flush=True)
            exit_code = process.wait()
    after = resource.getrusage(resource.RUSAGE_CHILDREN)
    return {
        "seconds": round(time.perf_counter() - started, 3),
        "cpu_seconds": round(after.ru_utime + after.ru_stime - before.ru_utime - before.ru_stime, 3),
        "exit_code": exit_code,
        "command": command,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--verify-results", type=Path)
    args = parser.parse_args()
    if args.verify_results:
        return verify_results(args.verify_results)
    if not args.source or not args.output:
        parser.error("--source and --output are required for a build benchmark")
    source, output = args.source.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    global_settings = output / "global-settings.xml"
    global_settings.write_text(f'<settings xmlns="{SETTINGS_NS}"/>\n')
    results = {
        "source_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip(),
        "measurements": [],
        "scope": "Full clean install with tests and Docker skipped. Maven dependency caches start empty; server caches are not reset.",
    }
    failed = False
    for name in ["current-nexus-and-central", "cloudsmith-only"]:
        project, cache = output / "case-project", output / "case-m2"
        shutil.copytree(source, project)
        settings = output / f"{name}-settings.xml"
        if name == "cloudsmith-only":
            cloudsmith_configuration(project, os.environ["CLOUDSMITH_NAMESPACE"], settings)
        else:
            shutil.copyfile(project / "settings.xml", settings)
        cache.mkdir()
        command = [
            "mvn", "--batch-mode", "--show-version", "--no-transfer-progress",
            "--settings", str(settings), "--global-settings", str(global_settings),
            f"-Dmaven.repo.local={cache}", "clean", "install", "-P!cloudsmith,!attach-sources",
            "-DskipTests", "-DskipITs", "-Dskip.common.tests=true", "-Ddocker.skip",
            "-Dlogging.root.level=off", "-Dspring.main.banner-mode=off",
        ]
        result = {"name": name, **build(command, project, output / f"{name}.log")}
        result["origins"] = cache_origins(cache)
        if name == "cloudsmith-only":
            result["unexpected_repositories"] = sorted(set(result["origins"]["counts"]) - {"cloudsmith-dependencies", "cloudsmith-releases"})
            failed |= bool(result["unexpected_repositories"])
        failed |= bool(result["exit_code"])
        results["measurements"].append(result)
        (output / "results.json").write_text(json.dumps(results, indent=2) + "\n")
        shutil.rmtree(project)
        shutil.rmtree(cache)
    summary = [
        "## Cold Maven build benchmark", "", results["scope"], "",
        f"Source: `{results['source_commit']}`. Maven 3.10.0, Java 25; the same runner, source and build flags.", "",
        "| Repositories | Build seconds | CPU seconds | Exit code | Artifact origin counts |",
        "| --- | ---: | ---: | ---: | --- |",
    ]
    for result in results["measurements"]:
        origins = ", ".join(f"{key}: {value}" for key, value in sorted(result["origins"]["counts"].items()))
        summary.append(f"| {result['name']} | {result['seconds']} | {result['cpu_seconds']} | {result['exit_code']} | {origins} |")
    summary += ["", "Cloudsmith serves Activiti releases from activiti-releases and third-party dependencies and plugins through activiti-snapshots. Every inherited repository is mirrored through Cloudsmith. Both cases use anonymous community access.", ""]
    print("\n".join(summary))
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as stream:
            stream.write("\n".join(summary))
    return int(failed)


if __name__ == "__main__":
    raise SystemExit(main())
