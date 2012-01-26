#! /bin/bash

java -Dbluecove.stack=emulator -Dbluecove.connection.type=l2cap -Dsyndicate.debug=true -cp syndicate-1.0.jar:qtransfer-1.0.jar:./lib/bluecove-2.1.0.jar:./lib/bluecove-emu-2.1.0.jar org.q2.qtransfer.QTransferGUI

