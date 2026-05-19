.PHONY: production

production:
	./gradlew --no-build-cache clean test koverVerify buildPlugin
