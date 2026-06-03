FROM maven:3.6.3-jdk-8

# Install OpenSSH Server
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y openssh-server && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Configure SSH: set root password to 'root', allow root login
RUN mkdir /var/run/sshd && \
    echo 'root:root' | chpasswd && \
    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config

# Set working directory to /app
WORKDIR /app

# Copy the project files
COPY . /app/

# Download dependencies and perform a basic build (skip tests to avoid DB/Redis requirement during build)
RUN mvn clean package -DskipTests || echo "Build failed, but continuing to allow SSH debugging"

# Expose SSH port and Spring Boot default port
EXPOSE 22 2086

# Start SSH daemon, attempt to run Spring Boot, and fallback to tail to keep container alive
CMD service ssh start && (java -jar target/*.jar || echo "Spring Boot failed to start") && tail -f /dev/null
