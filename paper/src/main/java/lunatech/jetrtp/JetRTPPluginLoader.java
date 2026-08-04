package lunatech.jetrtp;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class JetRTPPluginLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        PluginLibraries pluginLibraries = load();
        pluginLibraries.asRepositories().forEach(resolver::addRepository);
        pluginLibraries.asDependencies().forEach(resolver::addDependency);
        classpathBuilder.addLibrary(resolver);
    }

    /**
     * Load the plugin libraries from the `paper-libraries.json` file.
     * <p>
     * The file is parsed without external JSON libraries (like GSON) to prevent
     * classloader conflicts during PluginLoader execution.
     *
     * @return A {@link PluginLibraries} instance containing repositories and dependencies.
     */
    public PluginLibraries load() {
        try (
            final InputStream is = getClass().getResourceAsStream("/paper-libraries.json")
        ) {
            if (is == null)
                throw new RuntimeException("InputStream of \"/paper-libraries.json\" is null");

            try (
                final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr)
            ) {
                java.util.Map<String, String> repositories = new java.util.LinkedHashMap<>();
                java.util.List<String> dependencies = new java.util.ArrayList<>();

                String line;
                boolean inRepositories = false;
                boolean inDependencies = false;

                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("\"repositories\"")) {
                        inRepositories = true;
                        inDependencies = false;
                        continue;
                    }
                    if (line.startsWith("\"dependencies\"")) {
                        inDependencies = true;
                        inRepositories = false;
                        continue;
                    }

                    if (line.contains("}")) {
                        inRepositories = false;
                    }
                    if (line.contains("]")) {
                        inDependencies = false;
                    }

                    if (inRepositories) {
                        int firstQuote = line.indexOf('"');
                        if (firstQuote != -1) {
                            int secondQuote = line.indexOf('"', firstQuote + 1);
                            if (secondQuote != -1) {
                                String key = line.substring(firstQuote + 1, secondQuote);
                                int colon = line.indexOf(':', secondQuote + 1);
                                if (colon != -1) {
                                    int thirdQuote = line.indexOf('"', colon + 1);
                                    if (thirdQuote != -1) {
                                        int fourthQuote = line.indexOf('"', thirdQuote + 1);
                                        if (fourthQuote != -1) {
                                            String val = line.substring(thirdQuote + 1, fourthQuote);
                                            repositories.put(key, val);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (inDependencies) {
                        int firstQuote = line.indexOf('"');
                        if (firstQuote != -1) {
                            int secondQuote = line.indexOf('"', firstQuote + 1);
                            if (secondQuote != -1) {
                                String dep = line.substring(firstQuote + 1, secondQuote);
                                dependencies.add(dep);
                            }
                        }
                    }
                }

                return new PluginLibraries(repositories, dependencies);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Represents the plugin libraries as defined in the exported `paper-libraries.json` file.
     * This includes repositories and dependencies.
     * <p>
     * The repositories are a map of repository names to URLs, and the dependencies are a list of artifact definitions.
     * <p>
     * The {@link #asDependencies()} method converts the dependencies to a stream of {@link Dependency} instances,
     * and the {@link #asRepositories()} method converts the repositories to a stream of {@link RemoteRepository} instances.
     */
    public record PluginLibraries(@Nullable Map<String, String> repositories, @Nullable List<String> dependencies) {
        /**
         * These URLs will be replaced with the default mirror.
         */
        private static final Set<String> MAVEN_CENTRAL_URLS = Set.of(
            "https://repo1.maven.org/maven2",
            "http://repo1.maven.org/maven2",
            "https://repo.maven.apache.org/maven2",
            "http://repo.maven.apache.org/maven2"
        );
        /**
         * The default Maven Central mirror URL as configured by the Paper server.
         * This is used to replace any of the {@link #MAVEN_CENTRAL_URLS} in the repositories.
         *
         * @implNote This is obtained using reflection to support pre 1.21.7 versions of Paper that do not have this field.
         */
        private static final String MAVEN_CENTRAL_MIRROR = getMavenCentralMirror();
        /**
         * A fallback Maven Central mirror URL in case the Paper server does not define one.
         * This is used to replace any of the {@link #MAVEN_CENTRAL_URLS} in the repositories.
         */
        private static final String FALLBACK_MAVEN_CENTRAL_MIRROR = "https://maven-central.storage-download.googleapis.com/maven2";

        /**
         * Get the declared dependencies as a stream of {@link Dependency}.
         *
         * @return A stream of {@link Dependency} instances.
         */
        public Stream<Dependency> asDependencies() {
            if (dependencies == null)
                return Stream.empty();

            return dependencies.stream()
                .map(d -> new Dependency(new DefaultArtifact(d), null));
        }

        /**
         * Get the declared repositories as a stream of {@link RemoteRepository}.
         *
         * @return A stream of {@link RemoteRepository} instances.
         * @implNote This method filters out any maven central repositories as defined in {@link #MAVEN_CENTRAL_URLS}, and replaces them with {@link #MAVEN_CENTRAL_MIRROR}.
         */
        public Stream<RemoteRepository> asRepositories() {
            if (repositories == null)
                return Stream.empty();

            final boolean hasMavenCentral = repositories.values().stream()
                .anyMatch(url -> MAVEN_CENTRAL_URLS.stream().anyMatch(url::startsWith));

            final Stream<RemoteRepository> filtered = repositories.entrySet().stream()
                .filter(entry -> MAVEN_CENTRAL_URLS.stream().noneMatch(url -> entry.getValue().startsWith(url)))
                .map(entry -> new RemoteRepository.Builder(entry.getKey(), "default", entry.getValue()).build());

            if (hasMavenCentral)
                return Stream.concat(Stream.of(new RemoteRepository.Builder("MavenCentral", "default", MAVEN_CENTRAL_MIRROR).build()), filtered);

            return filtered;
        }

        /**
         * Get the default Maven Central mirror URL as configure by the Paper server, or fallback.
         *
         * @return A Maven Central mirror URL.
         * @implNote Uses reflection to support pre 1.21.7 versions of Paper that do not have this field.
         */
        private static String getMavenCentralMirror() {
            try {
                final Field field = MavenLibraryResolver.class.getDeclaredField("MAVEN_CENTRAL_DEFAULT_MIRROR");
                return (String) field.get(null);
            } catch (Exception e) {
                return FALLBACK_MAVEN_CENTRAL_MIRROR;
            }
        }
    }
}