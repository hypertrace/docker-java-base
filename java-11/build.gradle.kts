plugins {
  id("org.hypertrace.docker-publish-plugin")
}

var javaVersion = "11.0.12-11.50.19"

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
