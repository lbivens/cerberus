ui:
	LEIN_ROOT=1 lein with-profile rel cljsbuild once

package:
	make -C rel/pkg package
