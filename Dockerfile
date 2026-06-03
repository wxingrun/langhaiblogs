FROM maven:3.8.8-eclipse-temurin-8

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV ROOT_PASSWORD=trae-root

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-server procps net-tools \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /var/run/sshd /root/.ssh /var/log/langhaiblogs \
    && sed -i 's/^#\?PermitRootLogin .*/PermitRootLogin yes/' /etc/ssh/sshd_config \
    && sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication yes/' /etc/ssh/sshd_config \
    && sed -i 's/^#\?PubkeyAuthentication .*/PubkeyAuthentication yes/' /etc/ssh/sshd_config \
    && printf '\nUsePAM no\nClientAliveInterval 120\nClientAliveCountMax 3\n' >> /etc/ssh/sshd_config

COPY pom.xml /app/pom.xml
RUN mvn -B -ntp dependency:go-offline

COPY . /app
RUN mvn -B -ntp -DskipTests package

EXPOSE 22 2086 20202

CMD ["bash", "-lc", "set -e; mkdir -p /var/run/sshd /var/log/langhaiblogs; ssh-keygen -A; echo \"root:${ROOT_PASSWORD}\" | chpasswd; /usr/sbin/sshd; if ls /app/target/*.jar >/dev/null 2>&1; then (nohup java -jar /app/target/*.jar > /var/log/langhaiblogs/app.log 2>&1 &) || true; else echo 'No built jar found under /app/target' > /var/log/langhaiblogs/app.log; fi; tail -n +1 -F /var/log/langhaiblogs/app.log /dev/null"]
