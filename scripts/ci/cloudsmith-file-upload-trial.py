"""Exercise the publishing PR's file uploader against Cloudsmith with an isolated snapshot."""

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("phase", choices=["build", "upload"])
parser.add_argument("--source", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--version", required=True)
args = parser.parse_args()
source, root = args.source.resolve(), args.output.resolve()
root.mkdir(parents=True, exist_ok=True)
project, settings, cache = root / "project", root / "settings.xml", root / "m2"
module_spec = importlib.util.spec_from_file_location(
    "publishing_tests", source / ".github/actions/maven-deploy/test_publish_built_artifacts.py",
)
tests = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(tests)
group = "org.activiti.cloud.ci.isolated"


def run(command, log_name, directory):
    environment = dict(os.environ, MAVEN_ARGS=f"--global-settings {settings}", MAVEN_SKIP_RC="true")
    started = time.monotonic()
    with (root / log_name).open("w") as log:
        result = subprocess.run(command, cwd=directory, env=environment, stdout=log, stderr=subprocess.STDOUT)
    print((root / log_name).read_text()[-6000:], flush=True)
    result.check_returncode()
    return round(time.monotonic() - started, 3)


if args.phase == "build":
    tests.MavenIntegrationTests().fixture(root, args.version)
    for pom in project.rglob("pom.xml"):
        pom.write_text(pom.read_text().replace("example.cloudsmith", group))
    settings.write_text("<settings/>\n")
    duration = run([
        "mvn", "--batch-mode", "--no-transfer-progress", "--settings", str(settings),
        f"-Dmaven.repo.local={cache}", "deploy", "-Pcloudsmith", "-DskipTests",
    ], "build.log", project)
    shutil.rmtree(cache / group.replace(".", "/"))
    (root / "build-results.json").write_text(json.dumps({"seconds": duration, "version": args.version}, indent=2) + "\n")
    sys.exit(0)

duration = run([
    "python3", str(source / ".github/actions/maven-deploy/publish-built-artifacts.py"),
    "--project-directory", str(project), "--version", args.version,
    "--namespace", os.environ["CLOUDSMITH_NAMESPACE"], "--settings", str(source / "settings.xml"),
    "--local-repository", str(cache),
], "upload.log", project)
base = f"https://dl.cloudsmith.io/basic/{os.environ['CLOUDSMITH_NAMESPACE']}/activiti-snapshots/maven/"
payloads = tests.MavenIntegrationTests.payloads(root / "nexus")
assert len(payloads) == 8, payloads
verified = []
for path, expected in payloads.items():
    relative = Path(path)
    artifact = relative.parent.parent.name
    prefix = f"{artifact}-{args.version}"
    suffix = relative.name[len(prefix):]
    classifier, extension = ("", suffix[1:]) if suffix.startswith(".") else suffix[1:].rsplit(".", 1)
    deadline = time.monotonic() + 120
    while True:
        try:
            with urllib.request.urlopen(base + str(relative.parent / "maven-metadata.xml"), timeout=30) as response:
                metadata = ET.fromstring(response.read())
            values = [
                item.findtext("value") for item in metadata.findall("./versioning/snapshotVersions/snapshotVersion")
                if item.findtext("extension") == extension and (item.findtext("classifier") or "") == classifier
            ]
            value = max(values, key=lambda value: (value.rsplit("-", 2)[-2], int(value.rsplit("-", 1)[-1])))
            name = artifact + "-" + value + ("-" + classifier if classifier else "") + "." + extension
            with urllib.request.urlopen(base + str(relative.parent / name), timeout=30) as response:
                checksum = hashlib.sha512(response.read()).hexdigest()
            assert checksum == expected, f"Checksum mismatch: {path}"
            verified.append(path)
            break
        except (urllib.error.HTTPError, urllib.error.URLError, ValueError, AssertionError):
            if time.monotonic() >= deadline:
                raise
            time.sleep(5)
skipped = group.replace(".", "/") + "/skipped/" + args.version + "/maven-metadata.xml"
try:
    urllib.request.urlopen(base + skipped, timeout=30)
except urllib.error.HTTPError as error:
    assert error.code == 404, error
else:
    raise AssertionError("Skipped module was published")
result = {"version": args.version, "modules": 3, "files": len(verified), "all_checksums_equal": True,
          "anonymous_downloads": True, "skipped_module_absent": True, "cloudsmith_upload_seconds": duration,
          "verified": verified}
(root / "results.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2), flush=True)
with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as stream:
    stream.write(f"\nLive Cloudsmith check: {len(verified)} identical files across 3 modules; anonymous downloads passed; skipped module absent.\n")
