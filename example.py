#!/usr/bin/python3

from pathlib import Path

from IntermediateGraphRepresentation import IntermediateGraphRepresentation

import pandas as pd
import numpy as np
import math
import json
import shutil
import subprocess

pathPwd = Path(__file__).parent

"""
Example Parameters
"""
detectorFilePath = (pathPwd / 'samples/CentralDriftChamberSuperlayer0.json').resolve()
epsilon = 14.0

if __name__ == "__main__":

        print("Online Grap Building Example")
        print("Author: Marc Neu")
        print("Date: 16.06.2023")
        print("----------------------------")

        """
        Phase 1:
        - Generate a IntermediateGraphRepresentation based on a formal detector description and the provided epsilon distance function.
        - Epsilon is set arbitraritly to 14.0mm
        - The length of an edge is added as an additional feature in the graph.
        - Normalization and Quantization of all features is performed when serializing.
        """
        print("Start Phase 1")

        igr = IntermediateGraphRepresentation()

        with open(detectorFilePath, 'r', encoding='utf-8') as f:
                detectorDatabase = json.load(f)
        igr.setDetector(detectorDatabase)

        # Generate the complete offline graph structure based on the e-NN building approach

        def eDistance(a: dict, b: dict, epsilon: float) -> bool:
                distance = (a["features"]["x"] - b["features"]["x"])**2 + (a["features"]["y"] - b["features"]["y"])**2
                result = distance < epsilon ** 2 and distance > 0
                return result

        igr.train(lambda a,b: eDistance(a,b,epsilon))

        # Add additional edge features

        def edgeDistance(a: dict, b:dict) -> float:
                return math.sqrt((a["features"]["x"] - b["features"]["x"]) ** 2 + (a["features"]["y"] - b["features"]["y"]) ** 2)

        edgeDefinition = {
                "name":"distance",
                "kind": "static",
                "resolution": 8,
                "sign": 0,
                "range": [0, 100]
        }

        igr.addEdgeFeature(edgeDefinition, edgeDistance)

        igr.serializeIGRDatabase((pathPwd / (f"samples/igr.json")).resolve())

        print(f"IntermediateGraphRepresentation with {igr.getVerticesNum()} vertices and {igr.getEdgeNum()} edges has been generated.")
        shutil.copyfile((pathPwd / (f"samples/igr.json")).resolve(), (pathPwd / (f"generators/GraphBuildingElementGenerator/src/main/resources/igr.json")).resolve())
        print(f"Copied IntermediateGraphRepresentation to resource folder in the respective Generator.")
        print("----------------------------")

        """
        Phase 2:
        - Run Test on GraphBuildingElementGenerator
        - Run GraphBuildingElementGenerator to generate SystemVerilog Code based on the previously generated IntermediateGraphRepresentation
            - Online Edge Condition is specified in the main scala source file under ./generators/GraphBuildingElementGenerator/src/main/scala/cor/VerilogGenerator.scala
            - Number of Output Queues is specified in the main scala source file under ./generators/GraphBuildingElementGenerator/src/main/scala/cor/VerilogGenerator.scala
        """
        print ("Start Phase 2")
        result = subprocess.run(['sbt', 'test'],cwd=(pathPwd / (f"generators/GraphBuildingElementGenerator")).resolve(), stdout=subprocess.PIPE)
        print(result.stdout.decode('utf-8'))
        print("Functional Test Completed")
        print("Run SystemVerilog Genertion")
        result = subprocess.run(['sbt', 'run'],cwd=(pathPwd / (f"generators/GraphBuildingElementGenerator")).resolve(), stdout=subprocess.PIPE)
        print(result.stdout.decode('utf-8'))
        print("SystemVerilog Genertion completed")
        print("Copy SystemVerilog To Vivado Backend")
        shutil.copyfile((pathPwd / (f"generators/GraphBuildingElementGenerator/rtl/GraphBuildingElement.sv")).resolve(), (pathPwd / (f"backend/vivado/src/hdl/GraphBuildingElement.sv")).resolve())
        print(f"Copied SystemVerilog source file to Vivado backend folder in.")
        print("----------------------------")

        """
        Phase 3:
        - 
        """
        print("Start Phase 3")
        print("Out of Context Synthesis of generated module")
        result = subprocess.run(['sh', './script/synthOocModule.sh','-m','GraphBuildingElement'],cwd=(pathPwd / (f"backend/vivado")).resolve(), stdout=subprocess.PIPE)
        print(result.stdout.decode('utf-8'))
        print("Vivado Checkpoint File and Reports are available in ./backend/vivado/build/")






