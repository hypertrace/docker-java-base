plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    tags.forEach { it.onlyIf { false } }
    tag("14.0.2-14.29.23")
    tag("14.0.2")
    tag("14")
  }
}
