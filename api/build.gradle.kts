import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.internal.extensions.stdlib.capitalized

plugins {
    alias(libs.plugins.publisher)
    signing
}

mavenPublishing {
    coordinates(
        groupId = "lunatech",
        artifactId = base.archivesName.get().lowercase(),
        version = version.toString().let { originalVersion ->
            if (!originalVersion.contains("-SNAPSHOT"))
                originalVersion
            else
                originalVersion.substringBeforeLast("-SNAPSHOT") + "-SNAPSHOT" // Force append just -SNAPSHOT if snapshot version
        }
    )

    pom {
        name.set(base.archivesName.get().split("-").map { it.capitalized() }.joinToString("-"))
        description.set(rootProject.description.orEmpty())
        url.set("https://github.com/lunarenzo/JetRTP")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("GNU General Public License Version 3")
                url.set("https://www.gnu.org/licenses/gpl-3.0.en.html#license-text")
                distribution.set("https://www.gnu.org/licenses/gpl-3.0.en.html#license-text")
            }
        }

        developers {
            developer {
                id.set("lunarenzo")
                name.set("lunarenzo")
                url.set("https://github.com/lunarenzo")
                organization.set("lunatech")
            }
        }

        scm {
            url.set("https://github.com/lunarenzo/JetRTP")
            connection.set("scm:git:git://github.com/lunarenzo/JetRTP.git")
            developerConnection.set("scm:git:ssh://github.com:lunarenzo/JetRTP.git")
        }
    }

    configure(JavaLibrary(
        javadocJar = JavadocJar.None(), // We want to use our own javadoc jar
    ))

    // Publish to Maven Central
    publishToMavenCentral(automaticRelease = true)

    // Sign all publications
    signAllPublications()
}

signing {
    isRequired = false // Skip signing if no credentials are provided, e.g. for local publishing
}