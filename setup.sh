#/bin/bash

if ! command -v python3 &> /dev/null
then
    echo "python3 could not be found. Please make sure python3 is on the path."
else
    echo "python3 -- o.k."
fi

echo "Create new environment"

python3 -m venv ./graphbuilding_venv
source ./graphbuilding_venv/bin/activate
pip install -r ./requirements.txt
deactivate

echo "Successfully installed python environment"

if ! command -v sbt &> /dev/null
then
    echo "sbt could not be found. Please make sure sbt is on the path."
    exit
fi

if ! command -v verilator &> /dev/null
then
    echo "verilator could not be found. Please make sure verilator is on the path."
else
    echo "Verilator -- o.k."
fi

if ! command -v sbt &> /dev/null
then
    echo "SBT could not be found. Please make sure SBT is on the path."
else
    echo "SBT -- o.k"
fi