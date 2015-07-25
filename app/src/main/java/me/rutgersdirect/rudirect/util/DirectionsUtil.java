package me.rutgersdirect.rudirect.util;

import android.util.Log;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import me.rutgersdirect.rudirect.data.constants.AppData;
import me.rutgersdirect.rudirect.data.constants.RUDirectApplication;
import me.rutgersdirect.rudirect.data.model.BusStop;
import me.rutgersdirect.rudirect.data.model.RouteEdge;

public class DirectionsUtil {

    // The graph of active bus stops
    private static DirectedWeightedPseudograph<BusStop, RouteEdge> busStopsGraph;

    // Build the bus stop graph
    public static void setupBusStopsGraph() {
        busStopsGraph = new DirectedWeightedPseudograph<>(RouteEdge.class);
        // Add all the active bus stops to the graph
        for (String activeBusTag : AppData.activeBuses) {
            String busName = RUDirectApplication.getBusData().getBusTagToBusTitle().get(activeBusTag);
            BusStop[] busStops = RUDirectApplication.getBusData().getBusTagToBusStops().get(activeBusTag);
            if (busStops[0].isActive()) {
                busStopsGraph.addVertex(busStops[0]);
            }
            for (int i = 1; i < busStops.length; i++) {
                if (busStops[i].isActive()) {
                    busStopsGraph.addVertex(busStops[i]);
                    if (busStops[i - 1].isActive()) {
                        RouteEdge edge = busStopsGraph.addEdge(busStops[i - 1], busStops[i]);
                        edge.setRouteName(busName);
                        busStopsGraph.setEdgeWeight(edge, 1); // Set edge weight
                    }
                }
            }
            if (busStops[busStops.length - 1].isActive() && busStops[0].isActive()) {
                busStopsGraph.addEdge(busStops[busStops.length - 1], busStops[0]);
            }
        }
    }

    // Calculate the shortest path from the origin to the destination
    public static String calculateShortestPath(BusStop origin, BusStop destination) throws IllegalArgumentException {
        DijkstraShortestPath<BusStop, RouteEdge> shortestPath = new DijkstraShortestPath<>(busStopsGraph, origin, destination);
        return shortestPath.getPath().toString();
    }

    // Print out the vertices of the bus stops graph and their corresponding edges
    public static void printBusStopsGraph() {
        for (BusStop stop : busStopsGraph.vertexSet()) {
            Log.d(stop.toString(), busStopsGraph.outgoingEdgesOf(stop).toString());
        }
    }
}