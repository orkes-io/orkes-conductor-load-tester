#!/bin/sh

echo "Starting Conductor Workers"
cd /app/libs
echo "Using config properties";
export config_file=/app/config/config.properties

if [[ -z "${JVM_MEMORY_SETTINGS}" ]]; then
  JVM_MEMORY="-Xms512M -Xmx750M"
else
  JVM_MEMORY="${JVM_MEMORY_SETTINGS}"
fi

echo "Starting Conductor with $JVM_MEMORY memory settings"

echo $CONDUCTOR_SERVER_URL

nohup java $JVM_MEMORY -jar -DCONDUCTOR_CONFIG_FILE=$config_file workers.jar