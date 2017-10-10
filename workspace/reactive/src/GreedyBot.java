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

public class GreedyBot implements ReactiveBehavior {
	HashMap<City, Double> expectedReward = new HashMap<>();
	HashMap<City, City> bestNeighbor = new HashMap<>();
	private int numActions = 0;
	private Agent myAgent;
	@Override
	public void setup(Topology topology, TaskDistribution taskDistribution, Agent agent) {
		this.myAgent = agent;
		for (City city:
		     topology.cities()) {
			double expectedRewardTemp = 0;
			for (City target:
			     topology.cities()) {
				expectedRewardTemp += taskDistribution.reward(city, target) * taskDistribution.probability(city, target
				) - city.distanceTo(target);
			}
			this.expectedReward.put(city, expectedRewardTemp);
		}

		for (City city:
		     topology.cities()) {
			City bestNeighborTemp = city.neighbors().get(0);
			for (City neighbor:
			     city.neighbors()) {
				if (this.expectedReward.get(neighbor) > this.expectedReward.get(bestNeighborTemp)) {
					bestNeighborTemp = neighbor;
				}
			}
			this.bestNeighbor.put(city, bestNeighborTemp);
		}

		System.out.println("Greedy bot is ready!");
	}

	@Override
	public Action act(Vehicle vehicle, Task task) {
		Action action;
		if (task == null) {
			action = new Move(bestNeighbor.get(vehicle.getCurrentCity()));
		} else {
			action = new Pickup(task);
		}

		numActions++;
		if (numActions >= 1) {
			System.out.println("The total profit GREEDY after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}

		return action;
	}

}
