#!/bin/bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Usando Java: $(java -version 2>&1 | head -1)"
./mvnw spring-boot:run "$@"