plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    tags.forEach { it.onlyIf { false } }
    tag("8")
  }
}
