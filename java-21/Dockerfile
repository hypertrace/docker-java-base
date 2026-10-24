ARG JAVA_VERSION
ARG BASE_IMAGE
FROM azul/zulu-openjdk-debian:${JAVA_VERSION} AS jre

# Needed for --strip-debug
RUN apt-get -y update && apt-get -y upgrade libssl3 && apt-get -y install binutils

# Remove the eMudhra and Comodo CA certificates from the default JDK truststore
# as described in https://openjdk.org/emudhra.
# Check whether each alias exists before deletion to ensure compatibility with
# future JDK versions where these certificates may no longer be present.

RUN for alias in "emsigneccrootcag3 [jdk]" "comodoaaaca [jdk]"; do \
    if keytool -list -cacerts -storepass changeit -alias "$alias" >/dev/null 2>&1; then \
      keytool -delete -cacerts -storepass changeit -alias "$alias"; \
    else \
      echo "Certificate alias '$alias' not found; skipping deletion."; \
    fi; \
done

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

# TEMP: eMudhra/Comodo AAA distrust, see https://openjdk.org/emudhra
# Remove the base-ca and emudhra-clean stages and the "COPY --from=emudhra-clean" lines in the
# final image once the Zulu JDK, the distroless java-base image and BASE_IMAGE no longer ship
# these roots.
# jlink copies cacerts out of java.base.jmod, so the cleanup above (before jlink) does not
# reach /jre. The distroless store uses subject DN aliases and has no keytool.
# The PEM bundle in the final image comes from BASE_IMAGE and also has the eMudhra roots.
FROM ${BASE_IMAGE} AS base-ca

FROM jre AS emudhra-clean
COPY --from=ssl /etc/ssl/certs/java /etc/ssl/certs/java
COPY --from=base-ca /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
# Java stores: delete every entry whose owner is eMudhra or AAA Certificate Services (matched by
# subject, aliases differ between the two stores).
RUN for store in /jre/lib/security/cacerts /etc/ssl/certs/java/cacerts; do \
    keytool -list -v -keystore "$store" -storepass changeit \
      | awk '/^Alias name: /{a=substr($0,13)} /^Owner: /{if (tolower($0) ~ /emudhra|aaa certificate services/) print a}' \
      > /tmp/aliases; \
    while IFS= read -r alias; do \
      echo "Removing '$alias' from $store"; \
      keytool -delete -keystore "$store" -storepass changeit -alias "$alias"; \
    done < /tmp/aliases; \
done
# Drop every eMudhra root (emSign ECC Root CA - C3/G3, emSign Root CA - C1/G1) from the PEM bundle,
# matched by subject.
RUN mkdir /tmp/ca && cd /tmp/ca \
    && awk '/-----BEGIN CERTIFICATE-----/{n++} n{print > sprintf("c%04d.pem", n)}' /etc/ssl/certs/ca-certificates.crt \
    && for f in c*.pem; do \
      if openssl x509 -in "$f" -noout -subject | grep -qi 'eMudhra'; then \
        echo "Removing: $(openssl x509 -in "$f" -noout -subject)"; rm "$f"; \
      fi; \
    done \
    && cat c*.pem > /etc/ssl/certs/ca-certificates.crt

FROM debian:bookworm-slim AS builder
RUN apt update && apt install -y libjemalloc-dev

# stage for amd64
FROM ${BASE_IMAGE} AS base-amd64

SHELL ["/busybox/sh", "-c"]
RUN ln -s /busybox/sh /bin/sh

COPY --from=jre /lib/x86_64-linux-gnu/libz.so.1.2.13 /lib/x86_64-linux-gnu/libz.so.1.2.13
RUN ln -s /lib/x86_64-linux-gnu/libz.so.1.2.13 /lib/x86_64-linux-gnu/libz.so.1

# change default memory allocator to jemalloc
COPY --from=builder /usr/lib/x86_64-linux-gnu/libjemalloc* /usr/lib/x86_64-linux-gnu/
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so

# stage for arm64
FROM ${BASE_IMAGE} AS base-arm64

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
# TEMP: eMudhra distrust, see emudhra-clean stage above
COPY --from=emudhra-clean /jre/lib/security/cacerts /usr/lib/jvm/zulu-21-slim/lib/security/cacerts
COPY --from=emudhra-clean /etc/ssl/certs/java/cacerts /etc/ssl/certs/java/cacerts
COPY --from=emudhra-clean /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt

RUN ln -s /usr/lib/jvm/zulu-21-slim/bin/java /usr/bin/java
RUN ln -s /usr/lib/jvm/zulu-21-slim/bin/jfr /usr/bin/jfr

# set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/zulu-21-slim

ENTRYPOINT ["/usr/bin/java", "-jar"]
