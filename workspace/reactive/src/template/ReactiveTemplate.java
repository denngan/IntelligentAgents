package template;

import java.util.*;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private List<State> stateSpace = new ArrayList<State>();
	private double epsilon = 0.0; // halting criterium for the computation of V
	private HashMap<State, City> strategy;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		// generate state space
		for (City i : topology.cities()) {
			for (City j : topology.cities()) {
				if (!i.equals(j)) {
					stateSpace.add(new State(i, j));
				}
			}
			stateSpace.add(new State(i, null));
		}

		strategy = computeStrategy(topology, discount, td);
		System.out.println(strategy);
	}

	/**
	 * Compute the optimal reactive strategy/policy for the agent
	 * For a state, return a the optimal action
	 * A city corresponds to a delivery action to this city, if there is a task available
	 * otherwise to a move action to this city
	 * @param topology
	 * @param discount
	 * @param td
	 */
	private HashMap<State, City> computeStrategy(Topology topology, double discount, TaskDistribution td) {
		// corresponds to V(S)
		HashMap<State, Double> v = new HashMap<State, Double>();
		HashMap<State, City> best = new HashMap<>();
		int iterations = 0;
		for (State s : stateSpace) {
			v.put(s, (double) 0);
		}
		boolean hasChanged = true;
		while (hasChanged) {
			hasChanged = false;
			iterations++;

			for (State s : stateSpace) {
				Map<City, Double> q = new HashMap<City, Double>();

				// Iterate over all actions: Moves (and Delivery)
				// i.e., move to City c (from s.currentCity)
				for (City c : s.currentCity.neighbors()) {
					if (!c.equals(s.currentCity)) {
						double value = discount * infiniteHorizzzon(v, td, c) - s.currentCity.distanceTo(c);
						// Consider Delivery Action instead of Move to the city
						// since rewards are non-negative
//						if (c == s.taskCity) {
//							value += td.reward(s.currentCity, c);
//						}
						q.put(c, value);
					}
				}
				// deliver to taskCity
				// replaces move to taskCity
				// taskCity is not necessarily a neighbor of current city
				if (s.taskCity != null) {
					double value = td.reward(s.currentCity, s.taskCity) - s.currentCity.distanceTo(s.taskCity) + discount*infiniteHorizzzon(v, td, s.taskCity);
					q.put(s.taskCity, value);
				}

				//calculate max
				City maxKey = Collections.max(q.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();
				double maxValue = q.get(maxKey);
				if (Math.abs(v.get(s) - maxValue) > epsilon) {
					hasChanged = true;
				}
//				assert maxValue == Collections.max(q.values());
				best.put(s, maxKey); // vlt in if
				v.put(s, maxValue);
			}
		}
		System.out.println("" + iterations + " iterations to compute the optimal strategy.");
		return best;
	}

	private City getStrategy(State currentState) {
		return strategy.get(currentState);
	}

	private double infiniteHorizzzon(Map<State, Double> v, TaskDistribution td, City c) {
		double returnValue = 0;

		for (State s : stateSpace) {
			if (s.currentCity == c) {
				returnValue += td.probability(c, s.taskCity) * v.get(s);
			}
		}

		return returnValue;
	}

	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		State currentState;
		City taskCity = null;

		// Task is available to deliveryCity
		if (availableTask != null) {
			taskCity = availableTask.deliveryCity;
		}

		currentState = new State(vehicle.getCurrentCity(), taskCity);

		// get target city from strategy for current state
		City optimalDestinationCity = getStrategy(currentState);
		assert optimalDestinationCity != null;
		// if city corresponds to target city of task, deliver task
		if (optimalDestinationCity == taskCity) {
			action = new Pickup(availableTask);
		} else { // otherwise move to city
			action = new Move(optimalDestinationCity);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;
		return action;
	}

	/**
	 *
	 */
	class State {
		/**
		 * City, the agent is in
		 */
		@NotNull
		public City currentCity;

		/**
		 * Contains the target of the task
		 * null iff there is no task
		 */
		@Nullable
		public City taskCity; //

		public State(City currentCity, City taskCity) {
			this.currentCity = currentCity;
			this.taskCity = taskCity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			State state = (State) o;

			if (!currentCity.equals(state.currentCity)) return false;
			return taskCity != null ? taskCity.equals(state.taskCity) : state.taskCity == null;
		}

		@Override
		public int hashCode() {
			int result = currentCity.hashCode();
			result = 31 * result + (taskCity != null ? taskCity.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "State{" +
					"currentCity=" + currentCity +
					", taskCity=" + taskCity +
					'}';
		}
	}

}
