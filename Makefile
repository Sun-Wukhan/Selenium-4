# Moisturizer Selenium suite. Run `make` or `make help` for targets.
MVN      ?= mvn
BROWSER  ?= chrome
HEADLESS ?= false
URL      ?= https://weathershopper.pythonanywhere.com/moisturizer
IMAGE    ?= moisturizer-tests
DOCKER_PLATFORM ?= linux/amd64

.DEFAULT_GOAL := help

.PHONY: help test test-headless test-chrome test-firefox test-edge \
	test-firefox-headless test-edge-headless compile clean report \
	docker-build docker-test

help: ## Show available targets
	@echo "Moisturizer suite"
	@echo ""
	@echo "  make test                  Run in Chrome with a visible window"
	@echo "  make test-headless         Run in headless Chrome"
	@echo "  make test-firefox          Run in Firefox"
	@echo "  make test-edge             Run in Edge"
	@echo "  make test-firefox-headless Run in headless Firefox"
	@echo "  make test-edge-headless    Run in headless Edge"
	@echo "  make compile               Compile main and test sources"
	@echo "  make clean                 Delete Maven build output"
	@echo "  make report                Open the Extent HTML report"
	@echo "  make docker-build          Build the headless test image"
	@echo "  make docker-test           Run the suite inside Docker"
	@echo ""
	@echo "Overrides: BROWSER=$(BROWSER) HEADLESS=$(HEADLESS)"
	@echo "           URL=$(URL)"

test: ## Run the suite (BROWSER, HEADLESS, and URL can be overridden)
	$(MVN) -pl client -am test -Dbrowser=$(BROWSER) -Dheadless=$(HEADLESS) -Durl=$(URL)

test-headless: ## Run the suite in headless Chrome
	$(MAKE) test BROWSER=chrome HEADLESS=true

test-chrome: ## Run the suite in Chrome
	$(MAKE) test BROWSER=chrome

test-firefox: ## Run the suite in Firefox
	$(MAKE) test BROWSER=firefox

test-edge: ## Run the suite in Edge
	$(MAKE) test BROWSER=edge

test-firefox-headless: ## Run the suite in headless Firefox
	$(MAKE) test BROWSER=firefox HEADLESS=true

test-edge-headless: ## Run the suite in headless Edge
	$(MAKE) test BROWSER=edge HEADLESS=true

compile: ## Compile the reactor without running tests
	$(MVN) -pl client -am test-compile

clean: ## Remove build output, including Extent reports under target/
	$(MVN) -pl client -am clean

report: ## Open the latest Extent report in a browser
	@test -f client/target/extent-report/index.html || { echo "No report yet. Run make test-headless first."; exit 1; }
	@if command -v open >/dev/null 2>&1; then open client/target/extent-report/index.html; \
	elif command -v xdg-open >/dev/null 2>&1; then xdg-open client/target/extent-report/index.html; \
	else echo "Report written to client/target/extent-report/index.html"; fi

docker-build: ## Build an image that can run the suite headless
	docker build --platform $(DOCKER_PLATFORM) -t $(IMAGE) .

docker-test: ## Run the headless suite in Docker and keep the report on the host
	docker run --rm --platform $(DOCKER_PLATFORM) --shm-size=1g \
		-v "$(CURDIR)/client/target:/app/client/target" \
		$(IMAGE)
