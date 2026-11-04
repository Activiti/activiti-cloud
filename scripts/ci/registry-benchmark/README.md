# Nexus and Cloudsmith comparison

Temporary experiment for AAE-55154; do not merge into develop.

- Both upload destinations receive the same prebuilt snapshot files. A small Maven EventSpy saves and restores the main and attached artifact files between Maven processes, so the timed `deploy:deploy` goal does not rebuild them.
- Upload settings match: Maven 3.10.0, Java 25, quiet logs, ten transfer threads and one deployment execution with `deployAtEnd=true` for every module. Each round uses a unique benchmark snapshot version, and records file hashes, deployed coordinates, process time and deployment plugin time.
- Each build starts with an empty local Maven cache. Nexus uses the existing Nexus/Maven Central combination; Cloudsmith serves Activiti artifacts and proxies third-party dependencies. Mirrors prevent cross-registry fallback, and downloaded artifact paths must match.
- Each comparison runs on one runner in Nexus/Cloudsmith order, then Cloudsmith/Nexus order. Server caches are not reset. Tests and Docker builds are skipped consistently.

The workflow targets only this experiment branch and PR base. It does not propagate versions or change the production workflow. Measurements, origin records and logs are archived and summarised in the Actions run.
