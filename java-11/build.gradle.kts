plugins {
  id("org.hypertrace.docker-publish-plugin")
}

var javaVersion = "11.0.16.1-11.58.23"

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    buildArgs.put("JAVA_VERSION", javaVersion)
    tags.forEach { it.onlyIf { false } }
    tag(javaVersion)
    tag(javaVersion.split('-')[0])
    tag(javaVersion.split('.')[0])
  }
}
