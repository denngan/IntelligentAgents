import centralized.Assignment;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import centralized.CentralizedPlanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuctionAgent implements AuctionBehavior {
	private CentralizedPlanner centralizedPlanner = new CentralizedPlanner();
	private Set<Task> auctionedTasks = new HashSet<>();
	private long cumulatedCostForAuction = 0;

	private Assignment currentAssignment;
	private Assignment temporaryAssignment;


	private long timeOutSetup;
	private long timeOutBid;
	private long timeOutPlan;

	List<Long> enemiesOffers = new ArrayList<>();
	int enemiesId;

	Topology topology;
	TaskDistribution taskDistribution;
	Agent agent;
	List<Vehicle> vehicles;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config\\settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeOutSetup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the bid method cannot execute more than timeout_bid milliseconds
		timeOutBid = ls.get(LogistSettings.TimeoutKey.BID);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeOutPlan = ls.get(LogistSettings.TimeoutKey.PLAN);

		timeOutBid = 500;
		timeOutPlan = 10000;

		this.topology = topology;
		this.taskDistribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		centralizedPlanner.setup(topology, distribution, agent, timeOutSetup, timeOutBid);
	}

	@Override
	public Long askPrice(Task task) {
		double marginalCost = computeMarginalCost(task, auctionedTasks, timeOutBid);
		System.out.println("Our Bid is " + marginalCost);
		return (long) marginalCost;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		enemiesOffers.add(lastOffers[enemiesId]);

		if (lastWinner == agent.id()) {
			System.out.println("We won");
			cumulatedCostForAuction += lastOffers[agent.id()];
			auctionedTasks.add(lastTask);
			currentAssignment = temporaryAssignment;
			temporaryAssignment = null;
		}
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("After auction: Computing plan...");
		centralizedPlanner = new CentralizedPlanner();
		centralizedPlanner.setup(topology, taskDistribution, agent, timeOutSetup, timeOutPlan);
		List<Plan> plans = centralizedPlanner.plan(vehicles, tasks);
		long reward = tasks.stream().mapToLong(task -> task.reward).sum();
		System.out.println("Our reward: " + reward);
		return plans;
		// TODO use currentassignment
	}

	private double computeMarginalCost(Task task, Set<Task> tasks, long timeOut) {
		if (tasks instanceof TaskSet) {
			tasks = ((TaskSet)tasks).clone();
		} else {
			tasks = new HashSet<>(tasks);
		}
		tasks.add(task);

		centralizedPlanner = new CentralizedPlanner();
		centralizedPlanner.setup(topology, taskDistribution, agent, timeOutSetup, timeOutPlan);
		temporaryAssignment = centralizedPlanner.stochasticRestart(tasks, timeOut);

		if (currentAssignment == null) {
			return temporaryAssignment.cost();
		}
		return temporaryAssignment.cost() - currentAssignment.cost();
	}


}
