# Nexus and Cloudsmith comparison

Temporary experiment for AAE-55154; do not merge into develop.

- Both upload destinations receive the same prebuilt snapshot files. A small Maven EventSpy saves and restores the main and attached artifact files between Maven processes, so the timed `deploy:deploy` goal does not rebuild them.
- Maven 3.10.0 and Java 25 match the publishing PR. Upload commands are read from its deployment action, retaining its repository profiles, concurrency and existing `deployAtEnd`/BOM settings. Each round uses an isolated benchmark snapshot version and records hashes, deployed coordinates, process time and deployment plugin time.
- Each cold build starts with an empty Maven cache. The baseline consumes Activiti from Nexus and other dependencies from Central; the current setup uses the publishing PR's POM/settings unchanged, consuming Activiti from Cloudsmith while keeping Central direct. Origin checks reject Activiti artifacts from the wrong registry, and downloaded artifact paths must match.
- Preview mode times both complete production Maven commands in their normal Nexus-then-Cloudsmith order. It warms dependencies outside timing, cleans workspace targets before Nexus, and lets Cloudsmith reuse that workspace/cache. It excludes GitHub checkout, tool setup, authentication and version preparation, so its sum is not the full GitHub job duration.
- Upload-only and cold-build comparisons reverse destination order in round two. Preview mode retains production order. Server caches are not reset. Tests and Docker builds are skipped consistently.

The workflow targets only this experiment branch and PR base. It does not propagate versions or change the production workflow. Measurements, origin records and logs are archived and summarised in the Actions run.
