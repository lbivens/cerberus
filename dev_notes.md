#Cerberus Dev Environment

##Requirements

Java 1.7 or higher

    http://download.oracle.com/otn-pub/java/jdk/8u40-b27/jdk-8u40-macosx-x64.dmg
    install
    sudo rm -rf /System/Library/Java/JavaVirtualMachines/1.6.0.jdk


##To Start on local machine

        brew install leiningen
        git clone https://github.com/project-fifo/cerberus.git
        cd cerberus
        cp config.json.example config.json
        vi config.json
        lein repl
        cerberus.server=> (run)



snarl-admin init default dev users admin admin

##Upgrade

        rm -rf ~/.m2
        git pull
        lein upgrade
        lein repl
        cerberus.server=> (run)

##Overview

* source lives in src/cljs/cerberus/
* src/less/style.less for stylesheets
* data for each view lives in src/cljs/cerberus/<view>.cljs
* to look at the vms view you go to src/cljs/cerberus/vms.cljs
* there is a "def" config at the top that defines how data is displayed
* "fields map" explains the list view what fields exist and how to show them
* styling there is 2 places at the moment to look at
* * src/cljs/cerberus/core.cljs - function called main at the bottom
