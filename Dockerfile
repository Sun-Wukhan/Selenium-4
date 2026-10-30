# Headless Chrome image for the moisturizer TestNG suite.
# Google's Chrome package is amd64. On Apple Silicon, build with:
#   docker build --platform linux/amd64 -t moisturizer-tests .
FROM maven:3.9.10-eclipse-temurin-17

USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        wget \
        fonts-liberation \
        libasound2t64 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcups2 \
        libdbus-1-3 \
        libdrm2 \
        libgbm1 \
        libgtk-3-0 \
        libnspr4 \
        libnss3 \
        libx11-xcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxfixes3 \
        libxkbcommon0 \
        libxrandr2 \
        xdg-utils \
    && wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y /tmp/chrome.deb \
    && rm /tmp/chrome.deb \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml ./
COPY client/pom.xml client/pom.xml
COPY utilities/pom.xml utilities/pom.xml
COPY client/src client/src
COPY utilities/src utilities/src

RUN mvn -pl client -am test-compile

ENV HOME=/root
CMD ["mvn", "-pl", "client", "-am", "test", "-Dheadless=true", "-Dbrowser=chrome"]
