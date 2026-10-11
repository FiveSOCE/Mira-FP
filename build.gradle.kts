plugins { java }
group = "com.fivesoce.mira"
version = "0.1.0-dev"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies { compileOnly("io.papermc.paper:paper-api:26.2.build.+") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.jar { archiveBaseName.set("Mira-FP-26.2") }
