ui:
	LEIN_ROOT=1 lein clean
	LEIN_ROOT=1 lein less
	LEIN_ROOT=1 lein with-profile rel cljsbuild once less

package: ui
	make -C rel/pkg package
