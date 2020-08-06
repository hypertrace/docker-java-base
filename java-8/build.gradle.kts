plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    setTagNameTransform { tag -> "8-${tag.name}" }
  }
}
