plugins { java }
group = "com.fivesoce.mira"
version = "0.1.0-dev"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies { compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
tasks.jar { archiveBaseName.set("Mira-FP-1.18.2") }
