package benchmark;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryEvent;

/** Saves completed build outputs so separate deployment processes publish identical files. */
public final class ArtifactSpy extends AbstractEventSpy {

    private final Properties manifest = new Properties();
    private final Map<MojoExecution, Long> started = new IdentityHashMap<>();

    @Override
    public void init(Context context) throws Exception {
        if ("restore".equals(System.getProperty("benchmark.mode"))) {
            try (InputStream stream = Files.newInputStream(Path.of(System.getProperty("benchmark.manifest")))) {
                manifest.loadFromXML(stream);
            }
        }
    }

    @Override
    public void onEvent(Object value) throws Exception {
        if (value instanceof RepositoryEvent event && event.getType() == RepositoryEvent.EventType.ARTIFACT_DEPLOYED) {
            if (event.getException() != null) {
                throw event.getException();
            }
            append("deployed\t" + event.getArtifact() + "\t" + event.getRepository().getId());
        }
        if (!(value instanceof ExecutionEvent event)) {
            return;
        }
        MavenProject project = event.getProject();
        if (
            event.getType() == ExecutionEvent.Type.ProjectSucceeded &&
            "capture".equals(System.getProperty("benchmark.mode"))
        ) {
            String key = project.getId();
            if (project.getArtifact().getFile() != null) {
                manifest.setProperty(key + ".main", project.getArtifact().getFile().getAbsolutePath());
            }
            manifest.setProperty(key + ".pom", project.getFile().getAbsolutePath());
            manifest.setProperty(key + ".attachments", Integer.toString(project.getAttachedArtifacts().size()));
            int index = 0;
            for (Artifact artifact : project.getAttachedArtifacts()) {
                String prefix = key + ".attached." + index++;
                manifest.setProperty(prefix + ".file", artifact.getFile().getAbsolutePath());
                manifest.setProperty(prefix + ".type", artifact.getType());
                manifest.setProperty(
                    prefix + ".classifier",
                    artifact.getClassifier() == null ? "" : artifact.getClassifier()
                );
                manifest.setProperty(prefix + ".extension", artifact.getArtifactHandler().getExtension());
            }
        }
        MojoExecution mojo = event.getMojoExecution();
        if (mojo == null || !"maven-deploy-plugin".equals(mojo.getArtifactId())) {
            return;
        }
        if (event.getType() == ExecutionEvent.Type.MojoStarted) {
            if ("restore".equals(System.getProperty("benchmark.mode"))) {
                String key = project.getId();
                if (!manifest.containsKey(key + ".pom")) {
                    throw new IllegalStateException("Project missing from completed build: " + key);
                }
                String main = manifest.getProperty(key + ".main");
                if (main != null) {
                    project.getArtifact().setFile(Path.of(main).toFile());
                }
                project.getAttachedArtifacts().clear();
                int count = Integer.parseInt(manifest.getProperty(key + ".attachments"));
                for (int index = 0; index < count; index++) {
                    String prefix = key + ".attached." + index;
                    String type = manifest.getProperty(prefix + ".type");
                    DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
                    handler.setExtension(manifest.getProperty(prefix + ".extension"));
                    Artifact artifact = new DefaultArtifact(
                        project.getGroupId(),
                        project.getArtifactId(),
                        project.getVersion(),
                        null,
                        type,
                        manifest.getProperty(prefix + ".classifier"),
                        handler
                    );
                    artifact.setFile(Path.of(manifest.getProperty(prefix + ".file")).toFile());
                    project.addAttachedArtifact(artifact);
                }
            }
            started.put(mojo, System.nanoTime());
        } else if (
            event.getType() == ExecutionEvent.Type.MojoSucceeded || event.getType() == ExecutionEvent.Type.MojoFailed
        ) {
            Long start = started.remove(mojo);
            if (start != null) {
                append("mojo\t" + project.getId() + "\t" + (System.nanoTime() - start) / 1_000_000_000.0);
            }
        }
    }

    private void append(String line) throws Exception {
        String file = System.getProperty("benchmark.events");
        if (file != null) {
            Files.writeString(Path.of(file), line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    @Override
    public void close() throws Exception {
        if ("capture".equals(System.getProperty("benchmark.mode"))) {
            try (OutputStream stream = Files.newOutputStream(Path.of(System.getProperty("benchmark.manifest")))) {
                manifest.storeToXML(stream, "Completed Maven build artifacts");
            }
        }
    }
}
