# GraphBuilding
Minimal viable example for online graph building on FPGAs for machine learning trigger applications in particle physics.

## Prerequiesites

This example is optimized for Ubuntu. To run the Graph Building Example the following install the following tools:

Python 3.9:

```
apt-get install python3.9 python3.9-venv
```

sbt (Simple Build Tool):

```
apt-get install -yq curl default-jdk
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
apt-get update
apt-get install -yq sbt
```

Verilator:

```
apt-get install -yq \
verilator git perl make autoconf g++ flex bison ccache \
libgoogle-perftools-dev numactl perl-doc \
libfl2 libfl-dev zlib1g zlib1g-dev
```

Install FIRRTL Compiler:

```
wget -q -O - https://github.com/llvm/circt/releases/download/firtool-1.38.0/firrtl-bin-ubuntu-20.04.tar.gz | tar -zx \
    && sudo mv firtool-1.38.0/bin/firtool /usr/local/bin/
```

Vivado:

Please install Vivado 2022.1 according to the [Xilinx Website](https://www.xilinx.com/support/download.html)


## Setup

Run the `setup.sh` script to generate a new virtual environment and check if all required applications are available.

## Running the Example

Configure the environment before running the example

```
export PYTHONPATH=$PWD/tool/src:$PYTHONPATH
export VIVADO=<path_to_vivado>
source ./graphbuilding_venv/bin/activate
```

Run the example by calling

```
python3 ./example.py
```

## Folder Structure

* backend: Contains the currently supported synthesis backend script for vivado
* generators: Includes the current Hardware Generator
* samples: Input files for all three stage described in the example
* tool: Python Code for Generation of Intermediate Graph Representation