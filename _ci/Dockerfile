FROM ubuntu:24.04
USER root
RUN apt update && apt install --yes openjdk-17-jdk curl
RUN mkdir -p /usr/local/bin
RUN chown ubuntu:ubuntu /usr/local/bin
USER ubuntu
RUN cd /home/ubuntu \
    && curl -O https://dlcdn.apache.org/maven/maven-4/4.0.0-rc-2/binaries/apache-maven-4.0.0-rc-2-bin.tar.gz \
    && tar -xzvf apache-maven-4.0.0-rc-2-bin.tar.gz && rm apache-maven-4.0.0-rc-2-bin.tar.gz \
    && ln -s /home/ubuntu/apache-maven-4.0.0-rc-2/bin/mvn /usr/local/bin/mvn
RUN java -version
RUN mvn --version
ENV PATH="/home/ubuntu/apache-maven-4.0.0-rc-2/bin:$PATH"
WORKDIR /home/ubuntu
