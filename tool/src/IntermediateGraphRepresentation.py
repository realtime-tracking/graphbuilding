from pathlib import Path

from typing import Callable
import json
import jsonschema

import random
import numpy as np
from math import floor,ceil

class IntermediateGraphRepresentation:

    def __init__(self):
        pathPwd = Path(__file__).parent
        schemaFilePath = (pathPwd / '../resources/detector_schema.json').resolve()
        with open(schemaFilePath,'r') as f:
            self.detectorSchema = json.load(f)

        self.detectorDatabase = None
        self.sensors = None
        self.edges = None
        self.edgeFeatures = []

        self.igrDatabase = {
            "vertexFeatures" : [],
            "edgeFeatures": [],
            "vertices": [],
            "edges": []
        }

    def _twosComplement(self,x: int, w: int) -> str:
        """
        Returns number in twos complement as hex literal.
        """
        if x >= 0:
            value = (x & ((1 << w)- 1))
        else:
            value = (((abs(x) ^ ((1 << w)- 1)) + 1) & ((1 << w)- 1))
        return f"b{value:0{w}b}"

    def _quantize(self, value: float, resolution: int, sign: bool) -> int:
        """
        Quantized value based on resolution and signedness.
        Arguments:
            value: floating point value in [-1.0,1.0]
            resolution: Number of available bits
            sign: true or false
        """
        if(value > 1.0 or value < -1.0):
            raise ValueError("For a correct conversion value must be between -1.0 and 1.0")

        if sign:
            if(value > 0):
                quantized = floor(value * 2 ** (resolution - 1) - 1)
            else:
                quantized = ceil(value * 2 ** (resolution - 1))
        else:
            quantized =  floor(value * 2 ** (resolution) - 1)
        return self._twosComplement(quantized,resolution)
    
    def _normalize(self, value:float, range: list[float], sign: bool) -> float:
        """
        Normalizes a value to [-1,1] or [0,1] based on a specified range and signedness. 
        """
        if sign:
            return 2 * (value - range[0])/(range[1] - range[0]) - 1
        else:
            return (value - range[0])/(range[1] - range[0])
    
    def getEdgeNum(self) -> int:
        return len(self.igrDatabase["edges"])
    
    def getVerticesNum(self) -> int:
        return len(self.igrDatabase["vertices"])
    
    def normalizeVertices(self) -> None:
        for i,feature in enumerate(feature for feature in self.igrDatabase["vertexFeatures"] if feature["kind"] == "static"):
            for vertex in self.igrDatabase["vertices"]:
                vertex["staticFeatures"][i] = self._normalize(vertex["staticFeatures"][i],feature["range"],feature["sign"])

    def normalizeEdges(self) -> None:
        for i,feature in enumerate(feature for feature in self.igrDatabase["edgeFeatures"] if feature["kind"] == "static"):
            for edge in self.igrDatabase["edges"]:
                edge["staticFeatures"][i] = self._normalize(edge["staticFeatures"][i],feature["range"],feature["sign"])
    
    def quantizeVertices(self) -> None:
        """
        Quantizes vertices features in intermediate graph representation.
        """
        resolutions = [feature["resolution"] for feature in self.igrDatabase["vertexFeatures"] if feature["kind"] == "static"]
        signs = [feature["sign"] for feature in self.igrDatabase["vertexFeatures"] if feature["kind"] == "static"]

        for vertex in self.igrDatabase["vertices"]:
            quantizedFeatures = []
            for value,resolution,sign in zip(vertex["staticFeatures"],resolutions,signs):
                quantizedFeatures.append(self._quantize(value,resolution,sign))
            vertex["staticFeatures"] = quantizedFeatures


    def quantizeEdges(self) -> None:
        """
        Quantizes edge features in intermediate graph representation.
        """
        resolutions = [feature["resolution"] for feature in self.igrDatabase["edgeFeatures"] if feature["kind"] == "static"]
        signs = [feature["sign"] for feature in self.igrDatabase["edgeFeatures"] if feature["kind"] == "static"]

        for edge in self.igrDatabase["edges"]:
            quantizedFeatures = []
            for value,resolution,sign in zip(edge["staticFeatures"],resolutions,signs):
                quantizedFeatures.append(self._quantize(value,resolution,sign))
            edge["staticFeatures"] = quantizedFeatures


    
    def setDetector(self,database: dict) -> None:
        """
        Addes vertex features to intermediate graph description based on detector database.
        Arguments:
            database: Dict as defined in detectorSchema containing sensor features which are consequently transformed into vertices.
        """

        jsonschema.validate(instance=database,schema=self.detectorSchema)

        self.detectorDatabase = database.copy()

        self.igrDatabase["vertexFeatures"] = self.detectorDatabase["features"]

        sensorList = list(self.detectorDatabase["sensors"])
        sensorIdList = [sensor["id"] for sensor in sensorList]

        sensorFeatureNames = [feature["name"] for feature in self.detectorDatabase["features"] if feature["kind"] == "static"]
        sensorFeatures = []
        for sensor in sensorList:
            features = []
            for name in sensorFeatureNames:
                features.append(sensor["features"][name])
            sensorFeatures.append(features)

        for id in range(len(sensorIdList)):
            self.igrDatabase["vertices"].append({
                "id": id,
                "staticFeatures": sensorFeatures[id]
            })

        self.sensorMap = dict(zip(sensorIdList,range(len(sensorList))))
    
    def addEdgeFeature(self,feature: dict, metric: Callable[[dict,dict], float]):
        """
        Adds feature to edges. Call after build().
        Arguments:
            feature: dict containing the description of the new feature. Must contain "name", "kind", "resolution"
            metric: function that returns a float value depending on edge parameters.
        """
        if self.edges is None:
            raise RuntimeError("Graph empty, make sure to run train() first before adding edge features")

        self.igrDatabase["edgeFeatures"].append(feature)

        for id,edge in enumerate(self.edges):
            a = self.sensorMap[int(edge[0])]
            b = self.sensorMap[int(edge[1])]
            newFeature = metric(self.detectorDatabase["sensors"][a],self.detectorDatabase["sensors"][b])

            self.edgeFeatures[id].append(newFeature)

    def train(self, metric: Callable[[dict,dict], bool]) -> set[tuple[int,int]]:
        """
        Trains Intermediate Graph Representation based on specified detector database and
        supplied metric.
        Arguments:
            metric: Function indicating adjacency of two sensors.
        Returns: Set of tuples containing two unique sensor ids. Each tuple represents
            a unidirectional edge
        """
        sensors = list(self.detectorDatabase["sensors"].copy())
        e = set()
        while sensors:
            sensori  = sensors.pop()
            for sensorj in sensors:
                if metric(sensori,sensorj):
                    e.add((sensori["id"],sensorj["id"]))
        self.edges = list(e)
        self.edgeFeatures = [ [] for _ in range(len(self.edges)) ]

        edgeList = list(self.edges)
        edgeList = [(self.sensorMap[a],self.sensorMap[b]) for (a,b) in edgeList]

        for id in range(len(edgeList)):
            self.igrDatabase["edges"].append({
                "id": id,
                "staticFeatures": self.edgeFeatures[id],
                "vertexA": edgeList[id][0],
                "vertexB": edgeList[id][1]
            })

        return e
                    
    def serializeEdges(self, path: str) -> None:
        """
        Stores original edges in numpy array.
        Dimension 0: Edge Id
        Dimension 1: Vertex Id
        """
        edges = np.array(self.edges)
        with open(path, 'wb') as f:
            np.save(f,edges)

    def deserializeEdges(self, path: str) -> None:
        """
        Loads original edges from numpy array
        Dimension 0: Edge Id
        Dimension 1: Vertex Id
        """
        with open(path, 'rb') as f:
            self.edges = np.load(f)

    def serializeIGRDatabase(self,path:str) -> None:
        """
        Serializes the intermediate graph description, importing and quantizing features from both the
        CDC Database and additional edge features. The resulting JSON is checked for correctness against
        the IGRSchema.
        """
        self.normalizeVertices()
        self.normalizeEdges()
        self.quantizeVertices()
        self.quantizeEdges()
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(self.igrDatabase, f, ensure_ascii=False, indent=4)

    def fit(self, events: list[int]) -> list[np.ndarray[int]]:
        """
        Evaluates IGR on dataset, returning the edges that are contained in the graph for a given
        event. Returns list of events, each containing an array of edges that are detected by the
        Intermediate Graph Representation.
        """
        maxEdges = 0
        self.evaluatedEvents = []

        for vertices in events:
            # Checks all vertices if they are existent in the selected event
            # Returns a 2D array [[True, False], [False, False], ...] indicating which vertices are valid
            selectedVertices = np.isin(self.edges, vertices)
            # Checks if both vertices of an edge are valid and returns a 1D array
            selectedEdges = selectedVertices[:,0] & selectedVertices[:,1]
            # Return edges that belong the the selected event
            eventEdges = self.edges[selectedEdges,:]
            self.evaluatedEvents.append(eventEdges)
            maxEdges = max(maxEdges,eventEdges.shape[0])
            
        for event in self.evaluatedEvents:
            event.resize((maxEdges,2),refcheck=False)       

        return self.evaluatedEvents
    
    def serializeEvents(self, path: str) -> None:
        if self.evaluatedEvents is None:
             raise RuntimeError("No Dataset has been evaluated on this model yet. Call fit() first.")
        with open(path, 'wb') as f:
                np.save(f,self.evaluatedEvents)