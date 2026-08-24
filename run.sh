#!/bin/bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Usando Java: $(java -version 2>&1 | head -1)"
./mvnw spring-boot:run "$@"
