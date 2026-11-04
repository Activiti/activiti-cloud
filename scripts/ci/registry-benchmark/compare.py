#!/usr/bin/env python3
"""Compare Nexus and Cloudsmith with identical publishing and build settings."""

import argparse
import collections
import hashlib
import json
import os
from pathlib import Path
import re
import resource
import shutil
import statistics
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile

POM_NS = "http://maven.apache.org/POM/4.0.0"
NS = {"m": POM_NS}
DEPLOY_GOAL = "org.apache.maven.plugins:maven-deploy-plugin:3.2.0:deploy"
NEXUS = "https://artifacts.alfresco.com/nexus/content/"
ORDERS = (("nexus", "cloudsmith"), ("cloudsmith", "nexus"))


def configuration(project, directory, registry, namespace, publishing=False):
    """Select Nexus/Central or Cloudsmith and mirror inherited repositories consistently."""
    if registry == "nexus":
        dependencies = "https://repo.maven.apache.org/maven2/"
        releases = NEXUS + "repositories/activiti-releases/"
    else:
        dependencies = f"https://dl.cloudsmith.io/basic/{namespace}/activiti-snapshots/maven/"
        releases = f"https://dl.cloudsmith.io/basic/{namespace}/activiti-releases/maven/"
    tree = ET.parse(project / "pom.xml", parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True)))
    root = tree.getroot()
    for section, child in (("repositories", "repository"), ("pluginRepositories", "pluginRepository")):
        parent = root.find(f"m:{section}", NS)
        if parent is None:
            parent = ET.SubElement(root, f"{{{POM_NS}}}{section}")
        parent.clear()
        endpoints = [(f"{registry}-dependencies", dependencies, registry == "cloudsmith")]
        if section == "repositories":
            endpoints.append((f"{registry}-releases", releases, False))
            if registry == "nexus":
                endpoints.append(("nexus-snapshots", NEXUS + "repositories/activiti-snapshots/", True))
        elif registry == "nexus":
            endpoints.append(("nexus-public", NEXUS + "groups/public/", False))
        for identity, url, snapshots in endpoints:
            entry = ET.SubElement(parent, f"{{{POM_NS}}}{child}")
            ET.SubElement(entry, f"{{{POM_NS}}}id").text = identity
            ET.SubElement(entry, f"{{{POM_NS}}}url").text = url
            for policy, enabled in (("releases", True), ("snapshots", snapshots)):
                element = ET.SubElement(entry, f"{{{POM_NS}}}{policy}")
                ET.SubElement(element, f"{{{POM_NS}}}enabled").text = str(enabled).lower()
    ET.register_namespace("", POM_NS)
    ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    tree.write(project / "pom.xml", encoding="utf-8", xml_declaration=True)
    server = ""
    if publishing:
        server = """<servers><server><id>benchmark-upload</id>
          <username>${env.BENCHMARK_USER}</username><password>${env.BENCHMARK_PASSWORD}</password>
          </server></servers>"""
    settings = directory / f"{registry}-settings.xml"
    exclusions = f"!{registry}-dependencies,!{registry}-releases"
    if registry == "nexus":
        exclusions += ",!nexus-snapshots,!nexus-public"
    settings.write_text(f'''<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
      {server}
      <mirrors><mirror><id>{registry}-dependencies</id>
        <mirrorOf>*,{exclusions}</mirrorOf>
        <url>{dependencies}</url>
      </mirror></mirrors>
    </settings>
''')
    return settings


def command(settings, global_settings, cache):
    return [
        "mvn", "--batch-mode", "--show-version", "--no-transfer-progress",
        "--settings", str(settings), "--global-settings", str(global_settings),
        f"-Dmaven.repo.local={cache}", "-P!cloudsmith,!attach-sources",
        "-DskipTests", "-DskipITs", "-Dskip.common.tests=true", "-Ddocker.skip",
        "-Dlogging.root.level=off", "-Dspring.main.banner-mode=off",
        "-Dhttp.keepAlive=false", "-Dmaven.wagon.http.pool=false",
        "-Dmaven.wagon.httpconnectionManager.ttlSeconds=120",
    ]


def measure(args, project, log, credentials=None):
    environment = {**os.environ, "MAVEN_ARGS": "", "MAVEN_OPTS": ""}
    for name in ("CLOUDSMITH_USERNAME", "CLOUDSMITH_API_KEY", "MAVEN_USERNAME", "MAVEN_PASSWORD",
                 "NEXUS_USERNAME", "NEXUS_PASSWORD", "BENCHMARK_USER", "BENCHMARK_PASSWORD"):
        environment.pop(name, None)
    if credentials:
        environment.update(credentials)
    before = resource.getrusage(resource.RUSAGE_CHILDREN)
    start = time.perf_counter()
    with log.open("w") as stream:
        with subprocess.Popen(args, cwd=project, env=environment, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True) as process:
            for line in process.stdout:
                stream.write(line)
                print(line, end="", flush=True)
            code = process.wait()
    after = resource.getrusage(resource.RUSAGE_CHILDREN)
    return {
        "seconds": round(time.perf_counter() - start, 3),
        "cpu_seconds": round(after.ru_utime + after.ru_stime - before.ru_utime - before.ru_stime, 3),
        "exit_code": code, "command": args,
    }


def origins(cache, registry):
    records, counts = [], collections.Counter()
    for path in sorted(cache.rglob("_remote.repositories")):
        for line in path.read_text().splitlines():
            if line.startswith("#") or ">" not in line or "=" not in line:
                continue
            artifact, identity = line.split("=", 1)[0].rsplit(">", 1)
            identity = re.sub(r"-[0-9a-f]{40}$", "", identity)
            if identity:
                counts[identity] += 1
                records.append({"path": str(path.parent.relative_to(cache) / artifact), "repository": identity})
    allowed = {f"{registry}-dependencies", f"{registry}-releases"}
    if registry == "nexus":
        allowed.update(("nexus-snapshots", "nexus-public"))
    downloaded = {record["path"] for record in records}
    return {"counts": dict(counts), "records": records,
            "downloaded_bytes": sum((cache / path).stat().st_size for path in downloaded),
            "valid": bool(records) and not (set(counts) - allowed)}


def manifest_files(manifest):
    entries = {entry.attrib["key"]: entry.text for entry in ET.parse(manifest).getroot().findall("entry")}
    files = sorted({value for key, value in entries.items() if key.endswith((".file", ".main", ".pom"))})
    return [{"path": name, "bytes": Path(name).stat().st_size,
             "sha256": hashlib.sha256(Path(name).read_bytes()).hexdigest()} for name in files]


def compile_spy(directory):
    version = subprocess.check_output(["mvn", "--version"], text=True)
    home = re.search(r"^Maven home: (.+)$", version, re.MULTILINE).group(1)
    classes = directory / "spy-classes"
    classes.mkdir()
    source = Path(__file__).with_name("ArtifactSpy.java")
    subprocess.run(["javac", "-cp", str(Path(home) / "lib" / "*"), "-d", str(classes), str(source)], check=True)
    services = classes / "META-INF" / "plexus"
    services.mkdir(parents=True)
    (services / "components.xml").write_text('''<component-set><components><component>
      <role>org.apache.maven.eventspy.EventSpy</role><role-hint>registry-benchmark</role-hint>
      <implementation>benchmark.ArtifactSpy</implementation>
    </component></components></component-set>''')
    target = directory / "artifact-spy.jar"
    with zipfile.ZipFile(target, "w") as archive:
        for path in sorted(classes.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(classes))
    return target


def publish_events(path):
    events = [line.split("\t") for line in path.read_text().splitlines()] if path.exists() else []
    deployed = sorted(item[1] for item in events if item[0] == "deployed")
    return {"deploy_mojo_seconds": round(sum(float(item[2]) for item in events if item[0] == "mojo"), 3),
            "deployed": deployed,
            "base_coordinates": sorted(re.sub(r"-\d{8}\.\d{6}-\d+$", "-SNAPSHOT", item) for item in deployed),
            "repository_ids": sorted({item[2] for item in events if item[0] == "deployed"})}


def summary(results, directory):
    lines = ["## Matched Nexus and Cloudsmith benchmark", "", f"Source: `{results['source_commit']}`.", "",
             "| Test | Round | Registry | Wall seconds | CPU seconds | Deploy plugin seconds | Passed |",
             "| --- | ---: | --- | ---: | ---: | ---: | --- |"]
    for item in results["measurements"]:
        lines.append(f"| {results['kind']} | {item['round']} | {item['registry']} | {item['seconds']} | "
                     f"{item['cpu_seconds']} | {item.get('deploy_mojo_seconds', '—')} | {item['valid']} |")
    for registry in ("nexus", "cloudsmith"):
        samples = [item["seconds"] for item in results["measurements"] if item["registry"] == registry and item["valid"]]
        if samples:
            lines.extend(["", f"{registry}: median wall time {statistics.median(samples):.3f}s ({len(samples)} successful samples)."])
    if "matching_download_paths" in results:
        lines.extend(["", f"All builds downloaded the same artifact paths: {results['matching_download_paths']}."])
    lines.extend(["", results["scope"], "",
                  "Order reverses in round 2. Remote server caches are not reset; two samples are exploratory measurements.", ""])
    text = "\n".join(lines)
    (directory / "summary.md").write_text(text)
    print(text)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as stream:
            stream.write(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("kind", choices=("build", "upload"))
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version-prefix")
    args = parser.parse_args()
    source, output = args.source.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    namespace = os.environ["CLOUDSMITH_NAMESPACE"]
    if not re.fullmatch(r"[a-z0-9-]+", namespace):
        parser.error("Invalid Cloudsmith namespace")
    if args.kind == "upload" and (not args.version_prefix or not re.fullmatch(r"0\.0\.1-PR-\d+-BENCH-[\d-]+", args.version_prefix)):
        parser.error("Uploads require an isolated PR benchmark version prefix")
    global_settings = output / "global-settings.xml"
    global_settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"/>\n')
    results = {"kind": args.kind, "source_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip(),
               "scope": "Tests and Docker are skipped in every case. Build downloads are anonymous. Nexus means the current Nexus + Maven Central setup; Cloudsmith serves both Activiti and third-party downloads. Inherited repositories are mirrored to prevent cross-registry fallback.",
               "setup": [], "measurements": []}

    def save():
        (output / "results.json").write_text(json.dumps(results, indent=2) + "\n")

    try:
        if args.kind == "build":
            for number, order in enumerate(ORDERS, 1):
                for registry in order:
                    case = output / f"round-{number}-{registry}"
                    case.mkdir()
                    project, cache = case / "project", case / "m2"
                    shutil.copytree(source, project)
                    cache.mkdir()
                    settings = configuration(project, case, registry, namespace)
                    item = {"round": number, "registry": registry,
                            **measure(command(settings, global_settings, cache) + ["clean", "install"], project,
                                      output / f"build-{number}-{registry}.log")}
                    item["origins"] = origins(cache, registry)
                    item["valid"] = item["exit_code"] == 0 and item["origins"]["valid"]
                    results["measurements"].append(item)
                    save()
                    shutil.rmtree(case)
            samples = [set(record["path"] for record in item["origins"]["records"]) for item in results["measurements"]]
            results["matching_download_paths"] = all(paths == samples[0] for paths in samples)
        else:
            results["scope"] += " Uploads use ten transfer threads, quiet logs, a single batched deployment execution and identical prebuilt files within each round. No lifecycle build runs during upload timing. Only isolated snapshots are published; no downstream propagation."
            spy = compile_spy(output)
            warm_cache = output / "warm-m2"
            for number, order in enumerate(ORDERS, 1):
                case = output / f"round-{number}"
                case.mkdir()
                project = case / "project"
                shutil.copytree(source, project)
                settings = configuration(project, case, "nexus", namespace, publishing=True)
                for pom in project.rglob("pom.xml"):
                    pom.write_text(pom.read_text().replace("<deployAtEnd>false</deployAtEnd>", "<deployAtEnd>true</deployAtEnd>"))
                base = command(settings, global_settings, warm_cache)
                manifest = case / "artifacts.xml"
                for name, goals in (
                    ("version", ["versions:set", f"-DnewVersion={args.version_prefix}-{number}-SNAPSHOT", "-DprocessAllModules=true", "-DgenerateBackupPoms=false"]),
                    ("build", ["clean", "install", f"-Dmaven.ext.class.path={spy}", "-Dbenchmark.mode=capture", f"-Dbenchmark.manifest={manifest}"]),
                    ("warm-deploy-plugin", ["--non-recursive", "org.apache.maven.plugins:maven-deploy-plugin:3.2.0:help"]),
                ):
                    setup = {"round": number, "name": name, **measure(base + goals, project, output / f"setup-{number}-{name}.log")}
                    results["setup"].append(setup)
                    save()
                    if setup["exit_code"]:
                        raise RuntimeError(f"Preparation failed: {name}")
                files = manifest_files(manifest)
                if not files:
                    raise RuntimeError("Completed build manifest is empty")
                for registry in order:
                    cache = case / "case-m2"
                    shutil.copytree(warm_cache, cache)
                    events = output / f"upload-{number}-{registry}.tsv"
                    # Dependency resolution is held constant for both upload cases.
                    upload_settings = case / f"upload-{registry}-settings.xml"
                    shutil.copyfile(settings, upload_settings)
                    if registry == "nexus":
                        destination = NEXUS + "repositories/activiti-snapshots/"
                        user, password = os.environ["MAVEN_USERNAME"], os.environ["MAVEN_PASSWORD"]
                    else:
                        destination = f"https://maven.cloudsmith.io/{namespace}/activiti-snapshots/"
                        user, password = os.environ["CLOUDSMITH_USERNAME"], os.environ["CLOUDSMITH_API_KEY"]
                    target = f"benchmark-upload::{destination}"
                    options = [DEPLOY_GOAL, f"-Dmaven.ext.class.path={spy}", "-Dbenchmark.mode=restore",
                               f"-Dbenchmark.manifest={manifest}", f"-Dbenchmark.events={events}",
                               "-Daether.connector.basic.upstreamThreads.benchmark-upload=10",
                               f"-DaltDeploymentRepository={target}", f"-DaltReleaseDeploymentRepository={target}",
                               f"-DaltSnapshotDeploymentRepository={target}"]
                    item = {"round": number, "registry": registry,
                            **measure(command(upload_settings, global_settings, cache) + options, project,
                                      output / f"upload-{number}-{registry}.log",
                                      {"BENCHMARK_USER": user, "BENCHMARK_PASSWORD": password}),
                            **publish_events(events)}
                    item["artifact_files"] = files
                    item["unchanged_files"] = manifest_files(manifest) == files
                    item["valid"] = (item["exit_code"] == 0 and item["unchanged_files"] and bool(item["deployed"])
                                     and item["repository_ids"] == ["benchmark-upload"])
                    results["measurements"].append(item)
                    save()
                    shutil.rmtree(cache)
                pair = results["measurements"][-2:]
                same = pair[0]["base_coordinates"] == pair[1]["base_coordinates"]
                for item in pair:
                    item["matching_deployed_artifacts"] = same
                    item["valid"] &= same
                save()
                shutil.rmtree(project)
    except Exception as error:
        results["error"] = str(error)
        print(f"::error::{error}", flush=True)
    finally:
        save()
        summary(results, output)
    return int("error" in results or len(results["measurements"]) != 4
               or not all(item["valid"] for item in results["measurements"])
               or not results.get("matching_download_paths", True))


if __name__ == "__main__":
    raise SystemExit(main())
