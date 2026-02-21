# Convenience targets for running Play Framework tests against specific JDK versions.
# JDK paths are resolved dynamically via /usr/libexec/java_home (macOS).
# On Linux, set JAVA_HOME explicitly: JAVA_HOME=/usr/lib/jvm/java-17-openjdk make test-jdk17
#
# Usage:
#   make test-jdk17        # full test suite (unit + integration) on JDK 17
#   make unittest-jdk17    # unit tests only on JDK 17
#   make test-jdk11        # full test suite on JDK 11
#   make test-jdk21        # full test suite on JDK 21

JDK11 := $(shell /usr/libexec/java_home -v 11 2>/dev/null)
JDK17 := $(shell /usr/libexec/java_home -v 17 2>/dev/null)
JDK21 := $(shell /usr/libexec/java_home -v 21 2>/dev/null)

.PHONY: test unittest test-jdk11 test-jdk17 test-jdk21 unittest-jdk11 unittest-jdk17 unittest-jdk21

# Default targets use JDK 17 (the primary supported version)
test: test-jdk17
unittest: unittest-jdk17

test-jdk11:
	@test -n "$(JDK11)" || (echo "JDK 11 not found. Install: brew install --cask temurin@11"; exit 1)
	cd framework && JAVA_HOME=$(JDK11) ant test

test-jdk17:
	@test -n "$(JDK17)" || (echo "JDK 17 not found. Install: brew install --cask temurin@17"; exit 1)
	cd framework && JAVA_HOME=$(JDK17) ant test

test-jdk21:
	@test -n "$(JDK21)" || (echo "JDK 21 not found. Install: brew install --cask temurin@21"; exit 1)
	cd framework && JAVA_HOME=$(JDK21) ant test

unittest-jdk11:
	@test -n "$(JDK11)" || (echo "JDK 11 not found. Install: brew install --cask temurin@11"; exit 1)
	cd framework && JAVA_HOME=$(JDK11) ant unittest

unittest-jdk17:
	@test -n "$(JDK17)" || (echo "JDK 17 not found. Install: brew install --cask temurin@17"; exit 1)
	cd framework && JAVA_HOME=$(JDK17) ant unittest

unittest-jdk21:
	@test -n "$(JDK21)" || (echo "JDK 21 not found. Install: brew install --cask temurin@21"; exit 1)
	cd framework && JAVA_HOME=$(JDK21) ant unittest
