"""Guard benchmark isolation, snapshot comparisons and artifact integrity checks."""

from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

import compare


class BenchmarkTest(unittest.TestCase):
    def test_repository_configuration_has_no_cross_registry_fallback(self):
        for registry in ("nexus", "cloudsmith"):
            with self.subTest(registry=registry), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / "pom.xml").write_text(f'''<project xmlns="{compare.POM_NS}">
                  <modelVersion>4.0.0</modelVersion><repositories/><pluginRepositories/>
                </project>''')
                settings = compare.configuration(root, root, registry, "hyland")
                pom = ET.parse(root / "pom.xml")
                urls = [entry.text for entry in pom.findall(".//m:url", compare.NS)]
                mirror = ET.parse(settings).getroot()[0][0]
                pattern = next(item.text for item in mirror if item.tag.endswith("mirrorOf"))
                self.assertTrue(pattern.startswith("*,"))
                self.assertNotIn("<servers>", settings.read_text())
                if registry == "cloudsmith":
                    self.assertTrue(all(url.startswith("https://dl.cloudsmith.io/") for url in urls))
                    self.assertNotIn("!nexus", pattern)
                else:
                    self.assertTrue(all("cloudsmith" not in url for url in urls))
                    self.assertIn("https://repo.maven.apache.org/maven2/", urls)

    def test_origins_accept_maven_hash_suffix_and_reject_other_registry(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tracking = root / "_remote.repositories"
            (root / "library.jar").write_bytes(b"fixture")
            tracking.write_text(f"library.jar>cloudsmith-releases-{'a' * 40}=\n")
            self.assertTrue(compare.origins(root, "cloudsmith")["valid"])
            tracking.write_text("library.jar>nexus-releases=\n")
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


if __name__ == "__main__":
    unittest.main()
