#Jingles 2 dev environment

##Requirements

Java 1.7 or higher  

		http://download.oracle.com/otn-pub/java/jdk/8u40-b27/jdk-8u40-macosx-x64.dmg
		install 
		sudo rm -rf /System/Library/Java/JavaVirtualMachines/1.6.0.jdk  

##Fifo configuration

wiggle compression needs to be disabled

        vi /opt/local/fifo-wiggle/etc/wiggle.conf
        compression = off
        svcadm disable wiggle
        svcadm enable wiggle


##To Start on local machine

        brew install leiningen
        git clone git@github.com:project-fifo/jingles2.git
        cd jingles2
        cp config.json.example config.json
        vi config.json
        lein repl
        jingles.server=> (run)



snarl-admin init default dev users admin admin

##Upgrade

        rm -rf ~/.m2
        git pull
        lein upgrade
        lein repl
        jingles.server=> (run)
        
##Overview

* source lives in src/cljs/jingles/
* src/less/style.less for stylesheets
* data for each view lives in src/cljs/jingles/<view>.cljs
* to look at the vms view you go to src/cljs/jingles/vms.cljs
* there is a "def" config at the top that defines how data is displayed
* "fields map" explains the list view what fields exist and how to show them
* styling there is 2 places at the moment to look at
* * src/cljs/jingles/core.cljs - function called main at the bottom that holds the page root
* * src/cljs/jingles/list.cljs - holds the view for list view data