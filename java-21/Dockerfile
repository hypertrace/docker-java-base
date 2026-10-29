# Zulu build name, e.g. zulu21.54.109-ca-jdk21.0.13.0.3 (see java.version)
ARG JAVA_VERSION
ARG BASE_IMAGE
# The azul/zulu-openjdk-debian image has no tag with the eMudhra blocked.certs fix yet, so fetch the
# Zulu CA build straight from Azul's CDN. The SHA-256 pins the download. jlink copies blocked.certs
# from the JDK it runs, so it must be the fixed one.
# TEMP: go back to azul/zulu-openjdk-debian once it publishes a tag with the fix.
FROM debian:trixie-slim AS jre
ARG JAVA_VERSION
ARG TARGETARCH
# Update these together with java.version.
ARG ZULU_SHA256_AMD64=abe544103f1c4f0cc4b8997c0cac847eb1e842382eef57ce361c982fb0a3de11
ARG ZULU_SHA256_ARM64=eb6b89b23b9eb5661b29a4e8c09e8e91952cbdfa87045d36e9078e37388c2dc3

# Needed for --strip-debug
RUN apt-get -y update && apt-get -y upgrade libssl3 && apt-get -y install binutils curl ca-certificates

RUN case "${TARGETARCH}" in \
      amd64) ZULU_ARCH=x64; ZULU_SHA256="${ZULU_SHA256_AMD64}" ;; \
      arm64) ZULU_ARCH=aarch64; ZULU_SHA256="${ZULU_SHA256_ARM64}" ;; \
      *) echo "unsupported arch ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL -o /tmp/zulu.tar.gz "https://cdn.azul.com/zulu/bin/${JAVA_VERSION}-linux_${ZULU_ARCH}.tar.gz" \
    && echo "${ZULU_SHA256}  /tmp/zulu.tar.gz" | sha256sum -c - \
    && mkdir -p /usr/lib/jvm \
    && tar -xzf /tmp/zulu.tar.gz -C /usr/lib/jvm \
    && ln -s "/usr/lib/jvm/${JAVA_VERSION}-linux_${ZULU_ARCH}" /usr/lib/jvm/zulu \
    && rm /tmp/zulu.tar.gz
ENV JAVA_HOME=/usr/lib/jvm/zulu
ENV PATH=${JAVA_HOME}/bin:${PATH}

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

# Fail the build if the fixed blocklist did not make it through jlink.
# 7BA62FEC... is one of the fingerprints added by the 2026-10-21 out-of-cycle release.
RUN grep -q 7BA62FEC54E9D040556FCA70A6A88F03761C178FA1F7CB8390A73076849F55A2 /jre/lib/security/blocked.certs

# Extract java SSL certs
FROM gcr.io/distroless/java-base-debian13:debug AS ssl

FROM debian:trixie-slim AS builder
RUN apt update && apt install -y libjemalloc-dev

# stage for amd64
FROM ${BASE_IMAGE} AS base-amd64

SHELL ["/busybox/sh", "-c"]
RUN ln -s /busybox/sh /bin/sh

# libz.so.1 is already present in the base image
RUN test -e /lib/x86_64-linux-gnu/libz.so.1

# change default memory allocator to jemalloc
COPY --from=builder /usr/lib/x86_64-linux-gnu/libjemalloc* /usr/lib/x86_64-linux-gnu/
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so

# stage for arm64
FROM ${BASE_IMAGE} AS base-arm64

SHELL ["/busybox/sh", "-c"]
RUN ln -s /busybox/sh /bin/sh

# libz.so.1 is already present in the base image
RUN test -e /lib/aarch64-linux-gnu/libz.so.1

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
