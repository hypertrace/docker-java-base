plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    tags.forEach { it.onlyIf { false } }
    tag("11.0.8-11.41.23")
    tag("11.0.8")
    tag("11")
  }
}
