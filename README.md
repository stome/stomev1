STOME
=====

Stome is a stand-alone link tagging program written in Java, somewhat like the web-app Delicious. All you need to do is
download Stome.jar and run it.

DOWNLOADING STOME

Go here to download Stome.jar:

http://sourceforge.net/projects/stome/files/Stome.jar/download

RUNNING STOME

Windows XP/Vista/7 users should be able to launch Stome.jar just by double-clicking on it. If this doesn't work, you 
might not have the latest JRE installed. You can download and install it from here:

http://www.java.com/en/download/index.jsp

Linux/FreeBSD users can start Stome by running "java -jar Stome.jar" from the console in whatever directory they stored 
it in.

By default a Sqlite3 database (Stome.db) will be created in the directory Stome.jar resides in. If another Stome database
file is specified with the -d switch that database will be used instead of the default. This is where all your data is
stored, so don't delete this file if you want Stome to remember your links and tags.

For command-line options run "java -jar Stome.jar -h"

COMPILING STOME

For Linux users you just need to run "./build.sh" (minus the parentheses) in the project directory.
That should build Stome.jar. For Windows users, you're on your own. Sorry :/
