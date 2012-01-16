#! /bin/sh

echo "Running Bluecove Emulator"

DEFINES="-Dbluecove.debug=true"

java $DEFINES -cp ./lib/bluecove-emu-2.1.0.jar:./lib/bluecove-2.1.0.jar com.intel.bluetooth.emu.EmuServer