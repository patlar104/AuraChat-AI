# ─────────────────────────────────────────────
# AuraChat Android Build Sandbox
# Usage:
#   docker build -t aurachat-build .
#   docker run --rm -v $(pwd):/project aurachat-build ./gradlew assembleDebug
# ─────────────────────────────────────────────

FROM eclipse-temurin:21-jdk-jammy

# Build args (override with --build-arg)
ARG ANDROID_SDK_VERSION=commandlinetools-linux-11076708_latest.zip
ARG ANDROID_COMPILE_SDK=36
ARG ANDROID_BUILD_TOOLS=35.0.0

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# Install system dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android command-line tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q "https://dl.google.com/android/repository/${ANDROID_SDK_VERSION}" -O /tmp/cmdtools.zip && \
    unzip -q /tmp/cmdtools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdtools.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager \
        "platform-tools" \
        "platforms;android-${ANDROID_COMPILE_SDK}" \
        "build-tools;${ANDROID_BUILD_TOOLS}"

WORKDIR /project

# Default: run a debug build
CMD ["./gradlew", "assembleDebug"]
