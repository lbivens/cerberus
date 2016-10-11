VERSION=0.8.3p2
COMPONENT=fifo-cerberus

include pkg.mk
.PHONY: package prepare clean

package: prepare $(FILE).tgz

prepare:
	-rm -r $(STAGE_DIR)/$(COMPONENT)
	-mkdir -p $(STAGE_DIR)/$(COMPONENT)
	cp -r ../../resources/public/* $(STAGE_DIR)/$(COMPONENT)
	cp ../../resources/index.html $(STAGE_DIR)/$(COMPONENT)
	cp ../../LICENSE* $(STAGE_DIR)/$(COMPONENT)

clean: clean-pkg
	-rm *.tgz
