import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class Graph {
	private Set<Edge> edges = new HashSet<>();
	private List<City> cities;

	public Graph(Topology topology) {
		cities = topology.cities();
		for (City city : topology.cities()) {
			for (City neighbor : city.neighbors()) {
				edges.add(new Edge(city, neighbor, city.distanceTo(neighbor)));
			}
		}
	}

	// Computes the MST of graph induced by the set of cities.
	public double minimalSpanningTree() {
		Set<Edge> addedEdges = new HashSet<>();
		PriorityQueue<Edge> priorityQueue = new PriorityQueue<Edge>(edges);
		UnionFind<City> equivalenceClasses = new UnionFind<>(cities);
		double totalWeight = 0;

		while (!priorityQueue.isEmpty()) {
			Edge edge = priorityQueue.poll();
			if (equivalenceClasses.find(edge.cityIn) != equivalenceClasses.find(edge.cityOut)){
				equivalenceClasses.union(edge.cityIn, edge.cityOut);
				addedEdges.add(edge);
				totalWeight += edge.distance;
			}
		}

		return totalWeight;
	}

	private class Edge implements Comparable{
		City cityOut;
		City cityIn;
		double distance;

		public Edge(City cityOut, City cityIn, double distance) {
			this.cityOut = cityOut;
			this.cityIn = cityIn;
			this.distance = distance;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Edge edge = (Edge) o;
			return cityIn.equals(edge.cityIn) && cityOut.equals(edge.cityOut) ||
					cityIn.equals(cityOut) && cityOut.equals(cityIn);
		}

		@Override
		public int hashCode() {
			int result = cityOut.hashCode();
			result = 31 * result + cityIn.hashCode();
			return result;
		}


		@Override
		public int compareTo(Object o) {
			return compareTo((Edge) o);
		}

		public int compareTo(Edge that) {
			final int BEFORE = -1;
			final int EQUAL = 0;
			final int AFTER = 1;

			if (this == that) return EQUAL;

			if (this.distance < that.distance) return BEFORE;
			if (this.distance > that.distance) return AFTER;
			return EQUAL;
		}
	}
}
