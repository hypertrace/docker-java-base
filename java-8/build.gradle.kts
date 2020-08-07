plugins {
  id("org.hypertrace.docker-publish-plugin")
}

var javaVersion = "8"

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    buildArgs.put("JAVA_VERSION", javaVersion)
    tags.forEach { it.onlyIf { false } }
    tag(javaVersion)
  }
}
