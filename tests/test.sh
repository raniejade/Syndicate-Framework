#! /bin/sh

DEFS="-Dbluecove.stack=emulator -Dsyndicate.debug=true -Dsyndicate.connections.type=l2cap"

CLASSPATH="./lib/bluecove-2.1.0.jar:./lib/bluecove-gpl-2.1.0.jar:./lib/bluecove-emu-2.1.0.jar:syndicate-1.0.jar"

java $DEFS -cp $CLASSPATH org.q2.syndicate.SynTest