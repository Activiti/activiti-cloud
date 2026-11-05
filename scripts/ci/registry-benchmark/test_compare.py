"""Guard benchmark isolation, snapshot comparisons and artifact integrity checks."""

from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

import compare


class BenchmarkTest(unittest.TestCase):
    def test_repository_configuration_uses_current_cloudsmith_and_central_settings(self):
        for registry in ("nexus", "cloudsmith"):
            with self.subTest(registry=registry), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / "pom.xml").write_text(f'''<project xmlns="{compare.POM_NS}">
                  <modelVersion>4.0.0</modelVersion><repositories><repository><id>central</id>
                  <url>https://repo.maven.apache.org/maven2/</url></repository><repository><id>activiti-releases</id>
                  <url>https://dl.cloudsmith.io/basic/hyland/activiti-releases/maven/</url></repository></repositories><pluginRepositories/>
                </project>''')
                (root / "settings.xml").write_text('''<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
                  <servers><server><id>cloudsmith</id></server></servers><mirrors><mirror>
                  <id>cloudsmith-activiti-releases</id><mirrorOf>activiti-releases</mirrorOf>
                  <url>https://dl.cloudsmith.io/basic/hyland/activiti-releases/maven/</url>
                  </mirror></mirrors></settings>''')
                original = (root / "pom.xml").read_bytes()
                settings = compare.configuration(root, root, registry, "hyland")
                pom = ET.parse(root / "pom.xml")
                urls = [entry.text for entry in pom.findall(".//m:url", compare.NS)]
                mirror = ET.parse(settings).getroot()[0][0]
                pattern = next(item.text for item in mirror if item.tag.endswith("mirrorOf"))
                self.assertFalse(any(item.tag.endswith("servers") for item in ET.parse(settings).getroot()))
                if registry == "cloudsmith":
                    self.assertEqual((root / "pom.xml").read_bytes(), original)
                    self.assertEqual(pattern, "activiti-releases")
                    self.assertIn("https://repo.maven.apache.org/maven2/", urls)
                else:
                    self.assertTrue(pattern.startswith("*,"))
                    self.assertTrue(all("cloudsmith" not in url for url in urls))
                    self.assertIn("https://repo.maven.apache.org/maven2/", urls)

    def test_origins_accept_maven_hash_suffix_and_reject_other_registry(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            artifact = root / "org/activiti/library/1.0"
            artifact.mkdir(parents=True)
            tracking = artifact / "_remote.repositories"
            (artifact / "library.jar").write_bytes(b"fixture")
            tracking.write_text(f"library.jar>cloudsmith-activiti-releases-{'a' * 40}=\n")
            self.assertTrue(compare.origins(root, "cloudsmith")["valid"])
            tracking.write_text("library.jar>nexus-releases=\n")
            self.assertFalse(compare.origins(root, "cloudsmith")["valid"])
            tracking.write_text("library.jar>central=\n")
            self.assertFalse(compare.origins(root, "cloudsmith")["valid"])
            tracking.write_text("")
            self.assertFalse(compare.origins(root, "cloudsmith")["valid"])

    def test_deployment_comparison_normalizes_remote_snapshot_timestamps(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            results = []
            for number in (1, 2):
                events = root / f"{number}.tsv"
                events.write_text(f"deployed\torg.example:library:jar:1.0-20261104.12000{number}-{number}\tbenchmark-upload\n"
                                  "mojo\torg.example:library:jar:1.0-SNAPSHOT\t1.25\n")
                results.append(compare.publish_events(events))
            self.assertNotEqual(results[0]["deployed"], results[1]["deployed"])
            self.assertEqual(results[0]["base_coordinates"], results[1]["base_coordinates"])
            self.assertEqual(results[0]["deploy_mojo_seconds"], 1.25)

    def test_manifest_detects_modified_artifact_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            artifact = root / "library.jar"
            artifact.write_bytes(b"original")
            manifest = root / "artifacts.xml"
            properties = ET.Element("properties")
            ET.SubElement(properties, "entry", key="project.main").text = str(artifact)
            ET.ElementTree(properties).write(manifest)
            before = compare.manifest_files(manifest)
            artifact.write_bytes(b"modified")
            self.assertNotEqual(before, compare.manifest_files(manifest))

    def test_production_commands_preserve_profiles_and_concurrency(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / ".github/actions/maven-deploy").mkdir(parents=True)
            (source / ".github/workflows").mkdir()
            (source / ".github/actions/maven-deploy/action.yml").write_text('''steps:
  - name: Nexus
    with:
      maven-command: >-
        deploy -P '!attach-sources' -DskipTests
  - name: Cloudsmith
    run: >-
      mvn --batch-mode deploy '-Pcloudsmith,!alfresco,!attach-sources'
      -Daether.connector.basic.upstreamThreads.cloudsmith=10
      ${{ env.MAVEN_CLI_OPTS }}
''')
            (source / ".github/workflows/main.yml").write_text('''env:
  MAVEN_CLI_OPTS: --show-version --no-transfer-progress --settings settings.xml
''')
            commands = compare.production_commands(source)
            self.assertEqual(commands["nexus"].count("deploy"), 1)
            self.assertEqual(commands["cloudsmith"].count("deploy"), 1)
            self.assertIn("-Pcloudsmith,!alfresco,!attach-sources", commands["cloudsmith"])
            self.assertFalse(any("upstreamThreads" in item for item in commands["nexus"]))
            self.assertIn("-Daether.connector.basic.upstreamThreads.cloudsmith=10", commands["cloudsmith"])


if __name__ == "__main__":
    unittest.main()
