ARG JAVA_VERSION
FROM azul/zulu-openjdk-debian:${JAVA_VERSION} AS jre

# Needed for --strip-debug
RUN apt-get -y update && apt-get -y install binutils

# Included modules cherrypicked from https://docs.oracle.com/en/java/javase/21/docs/api/
#
# jdk.unsupported is undocumented but contains Unsafe, which is used by several dependencies to
# improve performance.
RUN cd / && jlink --no-header-files --no-man-pages --compress=0 --strip-debug \
    --add-modules java.base,java.logging,\
# java.desktop includes java.beans which is used by Spring
java.desktop,\
java.sql,\
# instrumentation
java.instrument,\
# we don't use JMX, but log4j2 errors without it: LOG4J2-716
java.management,\
# remote debug
jdk.jdwp.agent,\
# JVM metrics such as garbage collection
jdk.management,\
# JFR for live monitoring, adds < 1MB to size
jdk.jfr,jdk.management.jfr,\
# prevents us from needing a different base layer for kafka-zookeeper
# ZooKeeper needs jdk.management.agent, and adding it is 900K vs 200M for a different base layer
jdk.management.agent,\
# non-netty based DNS
java.naming,jdk.naming.dns,\
# TLS handehake with servers that use elliptic curve certificates
jdk.crypto.ec,\
# required for hadoop filesystem
java.security.jgss,\
jdk.security.auth,\
# sun.misc.Unsafe and friends
jdk.unsupported,\
# jdk specific network options
jdk.net,\
# built-in http client
java.net.http,\
# zip, jar file systems
jdk.zipfs\
 --output jre

# Extract java SSL certs
FROM gcr.io/distroless/java-base-debian12:debug AS ssl

FROM debian:bookworm-slim AS builder
RUN apt update && apt install -y libjemalloc-dev

# stage for amd64
FROM gcr.io/distroless/cc-debian12:debug@sha256:133c2e182e87c72c5efff9a3743f31542841425e9810b4a8371a9b95e06d1bd7 AS base-amd64

SHELL ["/busybox/sh", "-c"]
RUN ln -s /busybox/sh /bin/sh

COPY --from=jre /lib/x86_64-linux-gnu/libz.so.1.2.13 /lib/x86_64-linux-gnu/libz.so.1.2.13
RUN ln -s /lib/x86_64-linux-gnu/libz.so.1.2.13 /lib/x86_64-linux-gnu/libz.so.1

# change default memory allocator to jemalloc
COPY --from=builder /usr/lib/x86_64-linux-gnu/libjemalloc* /usr/lib/x86_64-linux-gnu/
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so

# stage for arm64
FROM gcr.io/distroless/cc-debian12:debug@sha256:133c2e182e87c72c5efff9a3743f31542841425e9810b4a8371a9b95e06d1bd7 AS base-arm64

SHELL ["/busybox/sh", "-c"]
RUN ln -s /busybox/sh /bin/sh

COPY --from=jre /lib/aarch64-linux-gnu/libz.so.1.2.13 /lib/aarch64-linux-gnu/libz.so.1.2.13
RUN ln -s /lib/aarch64-linux-gnu/libz.so.1.2.13 /lib/aarch64-linux-gnu/libz.so.1

# change default memory allocator to jemalloc
COPY --from=builder /usr/lib/aarch64-linux-gnu/libjemalloc* /usr/lib/aarch64-linux-gnu/
ENV LD_PRELOAD=/usr/lib/aarch64-linux-gnu/libjemalloc.so

# final image
ARG TARGETARCH
FROM base-${TARGETARCH}

COPY --from=ssl /etc/ssl/certs/java /etc/ssl/certs/java
COPY --from=jre /jre /usr/lib/jvm/zulu-21-slim

RUN ln -s /usr/lib/jvm/zulu-21-slim/bin/java /usr/bin/java
RUN ln -s /usr/lib/jvm/zulu-21-slim/bin/jfr /usr/bin/jfr

# set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/zulu-21-slim

ENTRYPOINT ["/usr/bin/java", "-jar"]
