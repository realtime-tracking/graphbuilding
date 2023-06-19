#!/bin/sh

while test $# -gt 0; do
	case "$1" in
		-h|--help)
		echo "Not Implemented"
		;;
	-m)
		shift
		if test $# -gt 0; then
			export MODULE=$1
		else
			echo "no project id specified"
			exit 1
		fi
		shift
		;;
	*)
		break
		;;
	esac
done

if [ -z ${MODULE+x} ]; then
	echo "Module name is not supplied";
	exit 2
fi

rm -r build
mkdir build
cd build

vivado -mode batch -source ../script/synthOocModule.tcl -tclargs $MODULE | tee synthLog$MODULE.txt

cd ..
