#!/usr/bin/bash

AWK=/usr/bin/awk
SED=/usr/bin/sed

USER=jingles
GROUP=www


fail_if_error() {
    [ $1 != 0 ] && {
        unset PASSPHRASE
        exit 10
    }
}

case $2 in
    PRE-INSTALL)

        ;;
    POST-INSTALL)
        if [ ! -f /opt/local/fifo-cerberus/config/config.js ]
        then
            cp /opt/local/fifo-cerberus/config/config.js.example /opt/local/fifo-cerberus/config/config.js
        fi
        ;;
esac
