.PHONY: production

production:
	./gradlew clean test koverVerify buildPlugin
