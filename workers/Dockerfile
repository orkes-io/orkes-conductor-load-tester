FROM ghcr.io/orkes-io/orkes-saas/conductor-base-server-image:latest
MAINTAINER Orkes Inc <builds@orkes.io>

# Make app folders
RUN mkdir -p /app/config /app/logs /app/libs

# Startup script(s)
COPY workers/dockerFiles/startup.sh /app/

# Configs
COPY workers/dockerFiles/config.properties /app/config/

# JAR files
COPY workers/build/libs/orkes*.jar /app/libs/workers.jar

# Change permissions
RUN chmod +x /app/startup.sh

HEALTHCHECK --interval=60s --timeout=30s --retries=10 CMD curl -I -XGET http://localhost:8080/health || exit 1

# Expose required ports for UI and Backend
EXPOSE 8080

CMD ["/app/startup.sh"]
ENTRYPOINT ["/bin/sh"]
