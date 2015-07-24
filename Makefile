ui:
	-rm -r resources/public/js
	LEIN_ROOT=1 lein with-profile rel cljsbuild once less

package: ui
	make -C rel/pkg package
