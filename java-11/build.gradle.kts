plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("java")
    setTagNameTransform { tag -> "11.0.8" }
  }
}
