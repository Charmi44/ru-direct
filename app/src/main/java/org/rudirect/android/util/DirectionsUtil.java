package org.rudirect.android.util;

import android.util.Log;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.rudirect.android.data.model.BusData;
import org.rudirect.android.data.model.BusRoute;
import org.rudirect.android.data.model.BusRouteEdge;
import org.rudirect.android.data.model.BusStop;
import org.rudirect.android.data.model.BusStopTime;

import java.util.ArrayList;
import java.util.HashMap;

public class DirectionsUtil {

    private static final String TAG = DirectionsUtil.class.getSimpleName();

    // The graph of active bus stops
    private static DirectedWeightedPseudograph<BusStop, BusRouteEdge> busStopsGraph;
    // Hash map of the bus stops
    private static HashMap<String, ArrayList<BusStop>> busStopsHashMap;
    // Initial wait
    private static double initialWait = -1;
    // Total path time
    private static double totalPathTime = -1;

    // Build the bus stop graph
    public static void setupBusStopsGraph() {
        busStopsGraph = new DirectedWeightedPseudograph<>(BusRouteEdge.class);
        busStopsHashMap = new HashMap<>();

        // Add all the active bus stops to the graph
        for (BusRoute activeRoute : BusData.getActiveRoutes()) {
            if (activeRoute != null) {
                // Log.d(TAG, "Active bus tag: " + activeBusTag + "\n_");
                BusStop[] busStops = activeRoute.getBusStops();
                BusStopTime prevTime = null;

                // Add vertex if the first bus stop is active
                if (busStops[0].isActive()) {
                    addVertex(activeRoute, busStops[0]);
                    prevTime = busStops[0].getTimes().get(0);
                }

                // Iterate through all the bus stops
                for (int i = 1; i < busStops.length; i++) {
                    // Add vertex if this bus stop is active
                    if (busStops[i].isActive()) {
                        addVertex(activeRoute, busStops[i]);
                        // Add edge between this bus stop and the previous bus stop if they are both active
                        if (busStops[i - 1].isActive()) {
                            prevTime = addEdge(activeRoute, busStops[i - 1], busStops[i], prevTime);
                        }
                    }
                }

                // Add edge from last bus stop to first bus stop
                if (busStops[busStops.length - 1].isActive() && busStops[0].isActive()) {
                    addEdge(activeRoute, busStops[busStops.length - 1], busStops[0], prevTime);
                }
            }
        }
        Log.d(TAG, "Bus stops graph has been set up.");
    }

    // Adds a weighted edge between two bus stops
    private static BusStopTime addEdge(BusRoute route, BusStop stop1, BusStop stop2, BusStopTime prevTime) {
        // Set previous time if it hasn't been set yet
        if (prevTime == null) {
            prevTime = stop2.getTimes().get(0);
        }

        // Add edge between stop1 and stop2
        BusRouteEdge edge = busStopsGraph.addEdge(stop1, stop2);
        ArrayList<BusStopTime> busStopTimes = stop2.getTimes();
        String vehicleId = prevTime.getVehicleId();
        edge.setRouteName(route.getTitle());
        edge.setRouteTag(route.getTag());

        // Iterate through all the times for the bus stop to get the lowest one
        for (int j = 0; j < busStopTimes.size(); j++) {
            BusStopTime time = busStopTimes.get(j);

            // Check to see that the time for this bus stop is greater than the time for the previous bus stop
            if (time.getMinutes() - prevTime.getMinutes() < 0) {
                continue;
            }

            // Staying on the same vehicle
            if (vehicleId.equals(time.getVehicleId())) {
                edge.setVehicleId(vehicleId);
                busStopsGraph.setEdgeWeight(edge, time.getMinutes() - prevTime.getMinutes());
                // Log.d(TAG, "Edge: " + stop1.getTitle() + " to " + stop2.getTitle());
                // Log.d(TAG, "Vehicle ID: " + time.getVehicleId() + ", Time (same vehicle): " + (time.getMinutes() - prevTime.getMinutes()));
                return time;
            }
            // Transfer to another vehicle
            else {
                edge.setVehicleId(prevTime.getVehicleId());
                busStopsGraph.setEdgeWeight(edge, time.getMinutes() - prevTime.getMinutes());
                // Log.d(TAG, "Edge: " + stop1.getTitle() + " to " + stop2.getTitle());
                // Log.d(TAG, "Vehicle ID: " + time.getVehicleId() + ", Time (vehicle transfer): " + (time.getMinutes() - prevTime.getMinutes()));
                return time;
            }
        }

        // Could not add edge to the graph, e.g. because the times for stop 2 were smaller than the times for stop 1
        busStopsGraph.removeEdge(edge);
        return null;
    }

    // Adds the bus stop to the graph while also handling duplicate bus stops
    private static void addVertex(BusRoute route, BusStop busStop) {
        ArrayList<BusStop> stopsArrayList;

        // Reset the bus stop ID to 0 if it isn't 0
        if (busStop.getId() != 0) {
            busStop.setId(0);
        }

        if (busStopsGraph.containsVertex(busStop)) { // If the bus stop already exists in the graph
            stopsArrayList = busStopsHashMap.get(busStop.getTitle());
            busStop.setId(stopsArrayList.size());
            busStopsGraph.addVertex(busStop);
            for (BusStop stop : stopsArrayList) {
                addEdge(route, busStop, stop, busStop.getTimes().get(0));
                addEdge(route, stop, busStop, stop.getTimes().get(0));
            }
            stopsArrayList.add(busStop);
        } else { // If the bus stop doesn't exist in the graph
            busStopsGraph.addVertex(busStop);
            stopsArrayList = new ArrayList<>();
            stopsArrayList.add(busStop);
            busStopsHashMap.put(busStop.getTitle(), stopsArrayList);
        }
    }

    // Calculate the shortest path from the origin to the destination
    public static GraphPath<BusStop, BusRouteEdge> calculateShortestPath(BusStop origin, BusStop destination)
            throws IllegalArgumentException {
        GraphPath<BusStop, BusRouteEdge> shortestPath = null;
        double shortestPathTime = Double.MAX_VALUE;
        outer:
        for (int i = 0; i < busStopsGraph.vertexSet().size(); i++) {
            origin.setId(i);
            for (int j = 0; j < busStopsGraph.vertexSet().size(); j++) {
                destination.setId(j);
                try {
                    GraphPath<BusStop, BusRouteEdge> newShortestPath
                            = new DijkstraShortestPath<>(busStopsGraph, origin, destination).getPath();

                    if (newShortestPath == null) {
                        break;
                    } else {
                        double newInitialWait = getInitialWait(newShortestPath);
                        double newTravelTime = getTravelTime(newShortestPath);
                        double newPathTime = newInitialWait + newTravelTime;
                        if (shortestPath == null || newPathTime < shortestPathTime) {
                            initialWait = newInitialWait;
                            totalPathTime = newPathTime;
                            shortestPath = newShortestPath;
                            shortestPathTime = newPathTime;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    if (i == 0 && j == 0) {
                        origin.setId(0);
                        destination.setId(0);
                        throw new IllegalArgumentException();
                    } else if (j == 0) {
                        break outer;
                    } else {
                        break;
                    }
                }
            }
        }
        origin.setId(0);
        destination.setId(0);
        return shortestPath == null ? null : shortestPath;
    }

    // Calculate and return the initial wait for a given path
    private static double getInitialWait(GraphPath<BusStop, BusRouteEdge> path) {
        ArrayList<BusStop> busStops = busStopsHashMap.get(path.getStartVertex().getTitle());
        return busStops.get(path.getStartVertex().getId()).getTimes().get(0).getMinutes();
    }

    // Calculate and return the travel time for a given path
    private static double getTravelTime(GraphPath<BusStop, BusRouteEdge> path) {
        return path.getWeight();
    }

    // Calculate and return the total travel time for a graph path
    private static double getPathTime(GraphPath<BusStop, BusRouteEdge> path) {
        return path.getWeight() + getInitialWait(path);
    }

    public static double getInitialWait() {
        return initialWait;
    }

    public static double getTotalPathTime() {
        return totalPathTime;
    }

    // For debugging: print out the vertices of the bus stops graph and their corresponding edges
    public static void printBusStopsGraph() {
        for (BusStop stop : busStopsGraph.vertexSet()) {
            Log.d(stop.toString(), busStopsGraph.outgoingEdgesOf(stop).toString());
        }
        Log.d(TAG, "Done printing out bus stops graph.");
    }
}