FROM gcr.io/distroless/java:8 AS jre

# We extract JRE's hard dependencies, libz and SSL certs, from the fat JRE image.
FROM gcr.io/distroless/java:8-debug AS deps

FROM gcr.io/distroless/cc:debug

MAINTAINER Hypertrace "https://www.hypertrace.org/"

SHELL ["/busybox/sh", "-c"]

RUN ln -s /busybox/sh /bin/sh

COPY --from=deps /etc/ssl/certs/java /etc/ssl/certs/java

COPY --from=deps /lib/x86_64-linux-gnu/libz.so.1.2.8 /lib/x86_64-linux-gnu/libz.so.1.2.8
RUN ln -s /lib/x86_64-linux-gnu/libz.so.1.2.8 /lib/x86_64-linux-gnu/libz.so.1

COPY --from=jre /usr/lib/jvm/java-8-openjdk-amd64/jre /usr/lib/jvm/java-8-openjdk-amd64/jre
RUN ln -s /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java /usr/bin/java

# set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre

ENTRYPOINT ["/usr/bin/java", "-jar"]
