import hashlib
import importlib.util
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest
import zipfile


SCRIPT = Path(__file__).with_name("publish-built-artifacts.py")
SPEC = importlib.util.spec_from_file_location("publisher", SCRIPT)
publisher = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(publisher)


class ManifestTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.project = self.root / "project"
        self.destination = self.root / "staging"
        self.destination.mkdir()

    def manifest(self, packaging="jar", aggregate=True):
        folder = self.project / "library/target"
        folder.mkdir(parents=True)
        pom = f'<project xmlns="http://maven.apache.org/POM/4.0.0"><packaging>{packaging}</packaging></project>'
        payloads = {"library-1.0-SNAPSHOT.pom": pom.encode()}
        if packaging == "jar":
            payloads.update({
                "library-1.0-SNAPSHOT.jar": b"built jar",
                "library-1.0-SNAPSHOT-sources.jar": b"sources",
                "library-1.0-SNAPSHOT-api-docs.json": b'{"api":true}',
            })
        lines = [
            "buildinfo.version=1.0-SNAPSHOT", "version=1.0-SNAPSHOT",
            "group-id=example", "artifact-id=library",
        ]
        if aggregate:
            lines.append("outputs.0.coordinates=example:library")
        for index, (name, content) in enumerate(payloads.items()):
            source_name = "swagger.json" if name.endswith("api-docs.json") else name
            (folder / source_name).write_bytes(content)
            key = f"outputs.0.{index}" if aggregate else f"outputs.{index}"
            lines += [
                f"{key}.groupId=example", f"{key}.filename={name}", f"{key}.length={len(content)}",
                f"{key}.checksums.sha512={hashlib.sha512(content).hexdigest()}",
            ]
        manifest = self.root / "artifacts.buildinfo"
        manifest.write_text("\n".join(lines) + "\n")
        return manifest, folder, payloads

    def test_stages_only_recorded_files_and_preserves_classifiers(self):
        manifest, folder, payloads = self.manifest()
        (folder / "library-1.0-SNAPSHOT-stale.jar").write_bytes(b"old build")
        modules = publisher.stage_artifacts(manifest, self.project, self.destination, "1.0-SNAPSHOT")
        self.assertEqual(len(modules), 1)
        self.assertEqual({item["file"].name for item in modules[0]["files"]}, set(payloads))
        self.assertEqual(modules[0]["main"]["extension"], "jar")
        self.assertEqual([item["classifier"] for item in modules[0]["files"]], ["", "", "sources", "api-docs"])
        for item in modules[0]["files"]:
            self.assertEqual(item["file"].read_bytes(), payloads[item["file"].name])

    def test_rejects_changed_build_outputs(self):
        manifest, folder, _ = self.manifest()
        (folder / "library-1.0-SNAPSHOT.jar").write_bytes(b"wrong jar")
        with self.assertRaisesRegex(ValueError, "built artifacts matching the Maven manifest"):
            publisher.stage_artifacts(manifest, self.project, self.destination, "1.0-SNAPSHOT")

    def test_rejects_stale_manifest(self):
        manifest, _, _ = self.manifest()
        with self.assertRaisesRegex(ValueError, "different version"):
            publisher.stage_artifacts(manifest, self.project, self.destination, "2.0")

    def test_handles_single_module_pom_only_builds(self):
        manifest, _, _ = self.manifest(packaging="pom", aggregate=False)
        module = publisher.stage_artifacts(manifest, self.project, self.destination, "1.0-SNAPSHOT")[0]
        self.assertIs(module["main"], module["pom"])
        self.assertEqual(len(module["files"]), 1)


@unittest.skipUnless(os.environ.get("MAVEN_INTEGRATION"), "Set MAVEN_INTEGRATION to a Maven executable")
class MavenIntegrationTests(unittest.TestCase):
    def fixture(self, root, version):
        project = root / "project"
        project.mkdir()
        plugins = "".join(
            f"<plugin><artifactId>maven-{name}-plugin</artifactId><version>{number}</version></plugin>"
            for name, number in {
                "resources": "3.5.0", "compiler": "3.16.0", "jar": "3.5.0", "source": "3.4.0",
                "install": "3.2.0", "deploy": "3.2.0", "surefire": "3.6.0",
            }.items()
        )
        (project / "pom.xml").write_text(f'''<project xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion><groupId>example.cloudsmith</groupId><artifactId>parent</artifactId>
          <version>{version}</version><packaging>pom</packaging>
          <properties><maven.compiler.release>17</maven.compiler.release><timestamp>${{maven.build.timestamp}}</timestamp></properties>
          <modules><module>library</module><module>pom-only</module><module>skipped</module></modules>
          <distributionManagement>
            <repository><id>nexus</id><url>{(root / "nexus").as_uri()}</url></repository>
            <snapshotRepository><id>nexus</id><url>{(root / "nexus").as_uri()}</url></snapshotRepository>
          </distributionManagement>
          <build><pluginManagement><plugins>{plugins}</plugins></pluginManagement><plugins>
            <plugin><artifactId>maven-deploy-plugin</artifactId><configuration><deployAtEnd>true</deployAtEnd></configuration></plugin>
          </plugins></build>
          <profiles><profile><id>cloudsmith</id><build><plugins>
            <plugin><artifactId>maven-artifact-plugin</artifactId><version>3.7.0</version>
              <configuration><attach>false</attach><ignoreJavadoc>false</ignoreJavadoc></configuration>
              <executions><execution><phase>deploy</phase><goals><goal>buildinfo</goal></goals></execution></executions>
            </plugin>
          </plugins></build></profile></profiles>
        </project>''')
        for name in ["library", "pom-only", "skipped"]:
            folder = project / name
            folder.mkdir()
            packaging = "pom" if name == "pom-only" else "jar"
            extra = ""
            if name == "pom-only":
                extra = "<build><plugins><plugin><artifactId>maven-deploy-plugin</artifactId><configuration><deployAtEnd>false</deployAtEnd></configuration></plugin></plugins></build>"
            elif name == "skipped":
                extra = "<properties><maven.deploy.skip>true</maven.deploy.skip></properties>"
            else:
                (folder / "src/main/java/example").mkdir(parents=True)
                (folder / "src/main/java/example/Library.java").write_text("package example; public class Library {}\n")
                (folder / "src/main/resources").mkdir(parents=True)
                (folder / "src/main/resources/build-time.txt").write_text("${timestamp}\n")
                (folder / "api.json").write_text('{"test":true}\n')
                (folder / "api.yaml").write_text("test: true\n")
                with zipfile.ZipFile(folder / "documentation.jar", "w") as archive:
                    archive.writestr("index.html", "API documentation\n")
                extra = '''<build><resources><resource><directory>src/main/resources</directory><filtering>true</filtering></resource></resources><plugins>
                  <plugin><artifactId>maven-source-plugin</artifactId><executions><execution><goals><goal>jar-no-fork</goal></goals></execution></executions></plugin>
                  <plugin><groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId><version>3.6.1</version>
                    <executions><execution><phase>package</phase><goals><goal>attach-artifact</goal></goals><configuration><artifacts>
                      <artifact><file>${project.basedir}/api.json</file><type>json</type><classifier>api-docs</classifier></artifact>
                      <artifact><file>${project.basedir}/api.yaml</file><type>yaml</type><classifier>api-docs</classifier></artifact>
                      <artifact><file>${project.basedir}/documentation.jar</file><type>jar</type><classifier>javadoc</classifier></artifact>
                    </artifacts></configuration></execution></executions>
                  </plugin>
                </plugins></build>'''
            (folder / "pom.xml").write_text(f'''<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion><parent><groupId>example.cloudsmith</groupId><artifactId>parent</artifactId><version>{version}</version></parent>
              <artifactId>{name}</artifactId><packaging>{packaging}</packaging>{extra}
            </project>''')
        return project

    @staticmethod
    def payloads(repository):
        result = {}
        for path in repository.rglob("*"):
            if not path.is_file() or "maven-metadata" in path.name or path.suffix in {".md5", ".sha1", ".sha256", ".sha512"}:
                continue
            name = re.sub(r"-\d{8}\.\d{6}-\d+(?=[-.])", "-SNAPSHOT", path.name)
            result[str(path.parent.relative_to(repository) / name)] = hashlib.sha512(path.read_bytes()).hexdigest()
        return result

    def test_snapshot_and_release_uploads_match_nexus_without_rebuilding(self):
        maven = os.environ["MAVEN_INTEGRATION"]
        for version in ["1.0-SNAPSHOT", "1.0"]:
            with self.subTest(version=version), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                project = self.fixture(root, version)
                settings = root / "settings.xml"
                settings.write_text("<settings/>\n")
                repository = Path(os.environ.get("MAVEN_TEST_REPOSITORY", root / "m2"))
                environment = dict(os.environ, MAVEN_ARGS=f"--global-settings {settings}", MAVEN_SKIP_RC="true")
                for name in ["CLOUDSMITH_API_KEY", "CLOUDSMITH_USERNAME", "MAVEN_USERNAME", "MAVEN_PASSWORD"]:
                    environment.pop(name, None)
                build = subprocess.run([
                    maven, "--batch-mode", "--no-transfer-progress", "--settings", str(settings),
                    f"-Dmaven.repo.local={repository}", "deploy", "-Pcloudsmith", "-DskipTests",
                ], cwd=project, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
                self.assertEqual(build.returncode, 0, build.stdout[-6000:])
                self.assertEqual(build.stdout.count("Deferring deploy for"), 2)
                shutil.rmtree(repository / "example/cloudsmith")
                jar = project / "library/target" / f"library-{version}.jar"
                before = publisher.checksum(jar)
                upload = subprocess.run([
                    os.environ.get("PYTHON", "python3"), str(SCRIPT), "--version", version,
                    "--buildinfo", str(project / "target" / f"parent-{version}.buildinfo"),
                    "--local-repository", str(repository), "--settings", str(settings),
                    "--repository-url", (root / "cloudsmith").as_uri(), "--maven", maven,
                ], cwd=project, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
                self.assertEqual(upload.returncode, 0, upload.stdout[-6000:])
                self.assertEqual(publisher.checksum(jar), before)
                source, target = self.payloads(root / "nexus"), self.payloads(root / "cloudsmith")
                self.assertEqual(len(source), 8)
                self.assertEqual(target, source)
                self.assertFalse(any("/skipped/" in name for name in target))


if __name__ == "__main__":
    unittest.main()
