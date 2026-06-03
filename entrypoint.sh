#!/bin/bash

/usr/sbin/sshd || echo "[WARN] sshd start failed"

echo "============================================="
echo " SSH server started on port 22"
echo " Root password: trae"
echo " Workdir: /app"
echo "============================================="

if [ -f "/app/target/langhaiblogs~v0.0.3.jar" ]; then
    echo "Attempting to start Spring Boot application..."
    java -jar "/app/target/langhaiblogs~v0.0.3.jar" || echo "Spring Boot exited (expected if external services are unavailable). Container stays alive for debugging."
else
    echo "JAR not found at /app/target/langhaiblogs~v0.0.3.jar. Skipping Spring Boot startup."
fi

exec sleep infinity
