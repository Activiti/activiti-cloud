#!/usr/bin/env python3
"""Publish Maven's recorded artifacts without running another build lifecycle."""

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET


def properties(path):
    return {
        key: value
        for line in path.read_text().splitlines()
        if line and not line.startswith(("#", "!")) and "=" in line
        for key, value in [line.split("=", 1)]
    }


def checksum(path):
    digest = hashlib.sha512()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def find_artifacts(info, project):
    expected = {
        (int(info[key.removesuffix(".filename") + ".length"]),
         info[key.removesuffix(".filename") + ".checksums.sha512"])
        for key in info if key.startswith("outputs.") and key.endswith(".filename")
    }
    sizes = {length for length, _ in expected}
    found = {}
    for folder, directories, names in os.walk(project):
        directories[:] = [name for name in directories if name not in {".git", "node_modules", ".venv", "__pycache__"}]
        for name in names:
            path = Path(folder) / name
            if path.is_file() and path.stat().st_size in sizes:
                key = (path.stat().st_size, checksum(path))
                if key in expected:
                    found[key] = path
                    if len(found) == len(expected):
                        return found
    missing = expected - set(found)
    if missing:
        raise ValueError(f"Cannot find {len(missing)} built artifacts matching the Maven manifest")
    return found


def stage_artifacts(buildinfo, project, destination, version):
    info = properties(buildinfo)
    if info.get("buildinfo.version") != "1.0-SNAPSHOT" or info.get("version") != version:
        raise ValueError("The Maven build manifest is incompatible or has a different version")
    sources = find_artifacts(info, project)
    modules = {
        key.removeprefix("outputs.").removesuffix(".coordinates"): value
        for key, value in info.items()
        if key.startswith("outputs.") and key.endswith(".coordinates")
    }
    if not modules:
        modules = {"": f"{info['group-id']}:{info['artifact-id']}"}
    result = []
    for index, (module, coordinates) in enumerate(modules.items()):
        group, artifact = coordinates.split(":")
        prefix = f"outputs.{module}." if module else "outputs."
        entries = sorted(
            (key.removesuffix(".filename") for key in info if key.startswith(prefix) and key.endswith(".filename")),
            key=lambda key: int(key.rsplit(".", 1)[1]),
        )
        if not entries:
            raise ValueError(f"No artifacts recorded for {coordinates}")
        pom_name = info[entries[0] + ".filename"]
        if not pom_name.startswith(artifact + "-") or not pom_name.endswith(".pom"):
            raise ValueError(f"No primary POM recorded for {coordinates}")
        module_version = pom_name[len(artifact) + 1:-4]
        folder = destination / str(index)
        folder.mkdir()
        files = []
        for entry in entries:
            name = info[entry + ".filename"]
            if Path(name).name != name or info[entry + ".groupId"] != group:
                raise ValueError(f"Invalid artifact entry for {coordinates}: {name}")
            source = sources[(int(info[entry + ".length"]), info[entry + ".checksums.sha512"])]
            target = folder / name
            shutil.copyfile(source, target)
            if (
                target.stat().st_size != int(info[entry + ".length"])
                or checksum(target) != info[entry + ".checksums.sha512"]
            ):
                raise ValueError(f"Built artifact differs from the build manifest: {coordinates}:{name}")
            suffix = name[len(artifact + "-" + module_version):]
            if suffix.startswith("."):
                classifier, extension = "", suffix[1:]
            elif suffix.startswith("-") and "." in suffix:
                classifier, _, extension = suffix[1:].rpartition(".")
            else:
                raise ValueError(f"Invalid Maven artifact filename: {name}")
            files.append({"file": target, "classifier": classifier, "extension": extension})
        pom = files[0]
        packaging = ET.parse(pom["file"]).getroot().findtext("{http://maven.apache.org/POM/4.0.0}packaging", "jar")
        main = [item for item in files[1:] if not item["classifier"]]
        if packaging != "pom" and len(main) != 1:
            raise ValueError(f"Expected one main artifact for {coordinates}")
        result.append({
            "group": group, "artifact": artifact, "version": module_version,
            "pom": pom, "main": pom if packaging == "pom" else main[0], "files": files,
        })
    if not result:
        raise ValueError("The Maven build manifest contains no deployable artifacts")
    return result


def deploy(module, settings, local_repository, url, maven):
    main, pom = module["main"], module["pom"]
    command = [
        maven, "--batch-mode", "--no-transfer-progress", "--settings", str(settings),
        f"-Dmaven.repo.local={local_repository}",
        "org.apache.maven.plugins:maven-deploy-plugin:3.2.0:deploy-file",
        "-DrepositoryId=cloudsmith", f"-Durl={url}",
        f"-DgroupId={module['group']}", f"-DartifactId={module['artifact']}", f"-Dversion={module['version']}",
        f"-Dfile={main['file']}", f"-DpomFile={pom['file']}",
        f"-Dpackaging={main['extension']}", f"-Dextension={main['extension']}", "-DgeneratePom=false",
        "-Daether.connector.basic.upstreamThreads.cloudsmith=2",
        "-Dhttp.keepAlive=false", "-Dmaven.wagon.http.pool=false",
        "-Dmaven.wagon.httpconnectionManager.ttlSeconds=120",
    ]
    extra = [item for item in module["files"] if item is not main and item is not pom]
    if extra:
        command += [
            "-Dfiles=" + ",".join(str(item["file"]) for item in extra),
            "-Dclassifiers=" + ",".join(item["classifier"] for item in extra),
            "-Dtypes=" + ",".join(item["extension"] for item in extra),
        ]
    process = subprocess.run(
        command, cwd=main["file"].parent, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )
    coordinates = f"{module['group']}:{module['artifact']}:{module['version']}"
    if process.returncode:
        raise RuntimeError(f"Cloudsmith upload failed for {coordinates}:\n{process.stdout}")
    print(f"Published {coordinates} ({len(module['files'])} files)", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--namespace")
    parser.add_argument("--settings", type=Path, default=Path("settings.xml"))
    parser.add_argument("--project-directory", type=Path, default=Path.cwd())
    parser.add_argument("--buildinfo", type=Path)
    parser.add_argument("--local-repository", type=Path, default=Path.home() / ".m2/repository")
    parser.add_argument("--repository-url", help="Override the destination for local integration tests")
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--maven", default="mvn")
    args = parser.parse_args()
    if not args.namespace and not args.repository_url:
        parser.error("--namespace is required")
    if args.workers < 1:
        parser.error("--workers must be positive")
    buildinfo = args.buildinfo
    if buildinfo is None:
        artifact = ET.parse(args.project_directory / "pom.xml").getroot().findtext("{http://maven.apache.org/POM/4.0.0}artifactId")
        buildinfo = args.project_directory / "target" / f"{artifact}-{args.version}.buildinfo"
    settings, repository = args.settings.resolve(), args.local_repository.resolve()
    with tempfile.TemporaryDirectory(prefix="cloudsmith-publish-", dir=os.environ.get("RUNNER_TEMP")) as temporary:
        modules = stage_artifacts(buildinfo, args.project_directory, Path(temporary), args.version)
        with ThreadPoolExecutor(max_workers=args.workers) as executor:
            futures = []
            for module in modules:
                snapshot = module["version"].endswith("-SNAPSHOT") or re.fullmatch(
                    r".*-\d{8}\.\d{6}-\d+", module["version"],
                )
                slug = "activiti-snapshots" if snapshot else "activiti-releases"
                url = args.repository_url or f"https://maven.cloudsmith.io/{args.namespace}/{slug}/"
                futures.append(executor.submit(deploy, module, settings, repository, url, args.maven))
            try:
                for future in as_completed(futures):
                    future.result()
            except Exception:
                for future in futures:
                    future.cancel()
                raise
    files = sum(len(module["files"]) for module in modules)
    summary = f"Published {len(modules)} modules and {files} files to Cloudsmith without rebuilding."
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as stream:
            stream.write(summary + "\n")


if __name__ == "__main__":
    main()
