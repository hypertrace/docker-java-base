plugins {
  id("org.hypertrace.docker-publish-plugin")
}

var javaVersion = "21.0.0-21.28.85"

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
