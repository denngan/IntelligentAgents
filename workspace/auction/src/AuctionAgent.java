import centralized.Assignment;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.plan.PlanVerifier;
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
	protected CentralizedPlanner centralizedPlanner = new CentralizedPlanner();
	protected long cumulatedCostForAuction = 0;

	protected Assignment currentAssignment;
	protected Assignment temporaryAssignment;


	protected long timeOutSetup;
	protected long timeOutBid;
	protected long timeOutPlan;

	protected AuctionAgent enemy;
	protected int enemiesId;
	protected int ourId;

	protected List<Task> taskHistory = new ArrayList<>();
	protected List<Long> ourBids = new ArrayList<>();
	protected List<Long> ourCosts = new ArrayList<>();
	protected List<Long> enemiesBids = new ArrayList<>();
	protected List<Integer> winner = new ArrayList<>();

	protected Set<Task> ourWonTasks = new HashSet<>();
	protected Set<Task> enemiesWonTasks = new HashSet<>();

	protected Topology topology;
	protected TaskDistribution taskDistribution;
	protected Agent agent;
	protected List<Vehicle> vehicles;

	protected int round = -1;


	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		setup(topology, distribution, agent, false);
	}

	public void setup(Topology topology, TaskDistribution distribution, Agent agent, boolean isEnemy) {
		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config\\settings_auction.xml");
		} catch (Exception exc) {
			println("There was a problem loading the configuration file.");
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
		this.ourId = agent.id();
		enemiesId = 1 - ourId; // Assumption: Id = 0 or 1, 2 players
		this.vehicles = agent.vehicles();
		centralizedPlanner.setup(topology, distribution, agent, timeOutSetup, timeOutBid);

		if (!isEnemy) {
			enemy = new AuctionAgent();
			enemy.setup(topology, distribution, agent, true);
			enemy.ourId = enemiesId;
			enemy.enemiesId = ourId;
			enemy.enemy = null;
		}
		printName();
	}

	protected void printName() {
		println("I am AuctionAgent");
	}

	@Override
	public Long askPrice(Task task) {
		round++;
		long marginalCost = (long) computeMarginalCost(task, timeOutBid);
		ourCosts.add(marginalCost);
		long bid = computeBid(task, marginalCost);
		println("Our Bid is " + bid);
		return bid;
	}

	protected Long computeBid(Task task, long marginalCost) {
		return marginalCost - 1;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		taskHistory.add(lastTask);
		ourBids.add(lastOffers[ourId]);
		enemiesBids.add(lastOffers[enemiesId]);
		winner.add(lastWinner);

		if (lastWinner == ourId) {
			println("We won");
			cumulatedCostForAuction += lastOffers[ourId];
			ourWonTasks.add(lastTask);
			currentAssignment = temporaryAssignment;
			temporaryAssignment = null;
		} else {
			println("We lost");
			enemiesWonTasks.add(lastTask);
		}

		if (enemy != null) {
			enemy.auctionResult(lastTask, lastWinner, lastOffers);
			updateStrategy();
		}

		HashSet<Task> union = new HashSet<>(ourWonTasks);
		union.addAll(enemiesWonTasks);
		assert new HashSet<>(taskHistory).equals(union);
	}

	protected void updateStrategy() {
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		assert ourWonTasks.equals(new HashSet<>(tasks));

		println("Current costs: " + currentAssignment.cost());
		println("Bids: " + cumulatedCostForAuction);
		println("Enemy's bids: " + enemy.cumulatedCostForAuction);
		println("After auction: Computing plan...");
		centralizedPlanner = new CentralizedPlanner();
		centralizedPlanner.setup(topology, taskDistribution, agent, timeOutSetup, timeOutPlan);
		List<Plan> plans = centralizedPlanner.plan(vehicles, tasks);
		long reward = tasks.stream().mapToLong(task -> task.reward).sum();
		println("Our reward: " + reward);
		return plans;
		// TODO use currentassignment
	}

//	protected double computeMarginalCost(Task task, Set<Task> tasks, long timeOut) {
//		double result = 0;
//		double oldCost;
//		if (tasks.size() == 0) {
//			oldCost = 0;
//		} else
//			oldCost = centralizedPlanner.stochasticRestart(tasks, timeOut).cost();
//
//		Set<Task> tasks2;
//		if (tasks instanceof TaskSet) {
//			tasks2 = ((TaskSet) tasks).clone();
//		} else {
//			tasks2 = new HashSet<>(tasks);
//		}
//		tasks2.add(task);
//
//		temporaryAssignment = centralizedPlanner.stochasticRestart(tasks2, timeOut);
//
//		if (currentAssignment == null) {
//			return temporaryAssignment.cost();
//		}
//		//return temporaryAssignment.cost() - currentAssignment.cost();
//		return oldCost - currentAssignment.cost();
//	}

	protected double computeMarginalCost(Task task, Set<Task> tasks, long timeOut) {
		double result = 0;

		double oldCost;
		if (tasks.isEmpty()) {
			oldCost = 0;
		} else {
			oldCost = centralizedPlanner.stochasticRestart(tasks, timeOut).cost();
		}

		Set<Task> newTasks = new HashSet<>(tasks);
		newTasks.add(task);

		temporaryAssignment = centralizedPlanner.stochasticRestart(newTasks, timeOut);
		double newCost = temporaryAssignment.cost();

		return Math.max(0, newCost - oldCost);
	}

	protected double computeMarginalCost(Task task, long timeOut) {
		return computeMarginalCost(task, ourWonTasks, timeOut);
	}

	protected void println(String s) {
		if (enemy == null) {
			System.out.println("(" + ourId + "E) " + s);
		} else {
			System.out.println("(" + ourId + ") " + s);
		}
	}
}
