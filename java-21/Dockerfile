ARG JAVA_VERSION
FROM azul/zulu-openjdk-debian:${JAVA_VERSION} AS jre

# Needed for --strip-debug
RUN apt-get -y update && apt-get -y install binutils

# Included modules cherrypicked from https://docs.oracle.com/en/java/javase/11/docs/api/
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

FROM amd64/debian:bookworm-slim As builder
RUN apt update && apt install -y libjemalloc-dev

# Debian-12 image
FROM gcr.io/distroless/cc-debian12:debug

MAINTAINER Hypertrace "https://www.hypertrace.org/"

SHELL ["/busybox/sh", "-c"]

RUN ln -s /busybox/sh /bin/sh

COPY --from=ssl /etc/ssl/certs/java /etc/ssl/certs/java

COPY --from=jre /lib/x86_64-linux-gnu/libz.so.1.2.11 /lib/x86_64-linux-gnu/libz.so.1.2.11
RUN ln -s /lib/x86_64-linux-gnu/libz.so.1.2.11 /lib/x86_64-linux-gnu/libz.so.1

COPY --from=jre /jre /usr/lib/jvm/zulu-21-amd64-slim
RUN ln -s /usr/lib/jvm/zulu-21-amd64-slim/bin/java /usr/bin/java
RUN ln -s /usr/lib/jvm/zulu-21-amd64-slim/bin/jfr /usr/bin/jfr

# change rocksdb default memory allocator
# https://docs.confluent.io/platform/current/streams/developer-guide/memory-mgmt.html#rocksdb
COPY --from=builder /usr/lib/x86_64-linux-gnu/libjemalloc* /usr/lib/x86_64-linux-gnu/
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so

# set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/zulu-21-amd64-slim

ENTRYPOINT ["/usr/bin/java", "-jar"]