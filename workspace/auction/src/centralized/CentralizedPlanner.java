package centralized;

import java.util.*;
import java.util.concurrent.*;

import logist.LogistSettings;

import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.plan.IllegalPlanException;
import logist.plan.PlanVerifier;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class CentralizedPlanner implements CentralizedBehavior {

	private List<Vehicle> vehicles;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;
	private int MAX_STEPS_WITHOUT_IMPROVEMENT = 15;
	private int MAX_STEPS = 1000;
	private double CHANGEMENT_PROBABILITY = 0.4;

	private Map<Task, PublicAction> taskToPickup;
	private Map<Task, PublicAction> taskToDelivery;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
					  Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config\\settings_default.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		System.out.println("Time for plan:" + timeout_plan);
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
	}

	public void setup(Topology topology, TaskDistribution distribution,
					  Agent agent, long timeout_setup, long timeout_plan) {
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		this.timeout_setup = timeout_setup;
		this.timeout_plan = timeout_plan;
	}

	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		List<Plan> plans;

		if (tasks.isEmpty()) {
			plans = new LinkedList<>();
			vehicles.forEach(vehicle -> plans.add(new Plan(vehicle.getCurrentCity())));
		} else {
			Assignment assignment = stochasticRestart(tasks, timeout_plan);
			plans = generatePlans(assignment);

			long time_end = System.currentTimeMillis();
			long duration = time_end - time_start;
			System.out.println("The plan was generated in " + duration + " milliseconds.");
			System.out.println("The cost of the plan is " + assignmentCost(assignment) + ".");
		}

		return plans;
	}


	public Assignment stochasticRestart(Set<Task> tasks, long timeOut) {
		taskToPickup = new HashMap<>();
		taskToDelivery = new HashMap<>();
		tasks.forEach(task -> {
			taskToPickup.put(task, new PublicAction(task, PublicAction.ActionType.PICKUP));
			taskToDelivery.put(task, new PublicAction(task, PublicAction.ActionType.DELIVERY));
		});

		final ExecutorService service = Executors.newSingleThreadExecutor();
		// maybe dont use currenttimemillis
		long deadline = System.currentTimeMillis() + timeOut - 100;

		Assignment bestAssignment = null;
		double bestCost = Double.MAX_VALUE;

		int iteration = 0;
		while (true) {
			Assignment currentAssignment;
			long timeLeft = deadline - System.currentTimeMillis();

			if (timeLeft <= 0) {
				break;
			}

			try {
				final Future<Assignment> f = service.submit(() -> SLS(tasks));
				// maybe give even less time
				currentAssignment = f.get(timeLeft, TimeUnit.MILLISECONDS);
			} catch (final TimeoutException e) {
				service.shutdown();
				break;
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}

			iteration++;
			if (assignmentCost(currentAssignment) < bestCost) {
				bestAssignment = currentAssignment;
				bestCost = assignmentCost(currentAssignment);
			}
		}
		return bestAssignment;
	}


	private Assignment SLS(Set<Task> tasks) throws IllegalPlanException {
		long time_start = System.currentTimeMillis();
		int stepsTotal = 0;
		int stepsWithoutImprovement = 0;
		Assignment assignment = getRandomAssignment(tasks); // A
		do {
			Assignment oldAssignment = assignment; // A_old
			Set<Assignment> neighbours = new HashSet<>();
			for (int i = 0; i < 120; i++) {
				Assignment newAssignment = randomNeighbour(oldAssignment);
				if (newAssignment != null) {
					neighbours.add(newAssignment);
				}
			}

			assignment = localChoice(neighbours, oldAssignment);

			stepsTotal++;
			if (assignmentCost(assignment) == assignmentCost(oldAssignment)) {
				stepsWithoutImprovement++;
			} else {
				stepsWithoutImprovement = 0;
			}
		} while (stepsTotal < MAX_STEPS
				&& stepsWithoutImprovement < MAX_STEPS_WITHOUT_IMPROVEMENT
				&& (System.currentTimeMillis() - time_start) < timeout_plan - 100);

		return assignment;
	}


	// returns a random neighbour
	// steps: choose vehicle
	// choose which task to delete
	// choose where to add this task (vehicle, pos)
	private Assignment randomNeighbour(Assignment oldAssignment) {
		Random random = new Random();
		Assignment assignment = new Assignment(oldAssignment);

		Vehicle vehicleFrom;
		VehicleAssignment vehicleAssignment;
		do {
			vehicleFrom = vehicles.get(random.nextInt(vehicles.size()));
			vehicleAssignment = assignment.get(vehicleFrom);
		} while (vehicleAssignment.size() <= 0);

		int index = random.nextInt(vehicleAssignment.size());
		Task task = vehicleAssignment.removeTask(index);

		Vehicle vehicleTo = vehicles.get(random.nextInt(vehicles.size()));
		vehicleAssignment = assignment.get(vehicleTo);

		// precompute the space left after each action
		int weight = task.weight;
		int[] spaceLeft = new int[vehicleAssignment.size() + 2];
		spaceLeft[0] = vehicleTo.capacity();
		for (int j = 0; j < vehicleAssignment.size(); j++) {
			if (vehicleAssignment.get(j).actionType == PublicAction.ActionType.PICKUP) {
				spaceLeft[j + 1] = spaceLeft[j] - vehicleAssignment.get(j).task.weight;
			} else { // delivery
				spaceLeft[j + 1] = spaceLeft[j] + vehicleAssignment.get(j).task.weight;
			}
		}
		spaceLeft[spaceLeft.length - 1] = weight;

		int index1 = 0, index2 = 1;
		int p = 1;
		int stop = 0;
		for (int i = 0; i < spaceLeft.length - 1; i++) {
			if (spaceLeft[i] < weight) {
				for (int k = stop; k < i; k++) {
					for (int l = k; l < i; l++) { //TODO check bounds
						if (random.nextDouble() < 1d / p) {
							index1 = k;
							index2 = l + 1;
						}
						p++;
					}
				}
				stop = i + 1;
			}
		}
		for (int k = stop; k < spaceLeft.length - 1; k++) {
			for (int l = k; l < spaceLeft.length - 1; l++) {
				if (random.nextDouble() < 1d / p) {
					index1 = k;
					index2 = l+1;
				}
				p++;
			}
		}

		vehicleAssignment.add(index1, taskToPickup.get(task));
		vehicleAssignment.add(index2, taskToDelivery.get(task));


		return assignment;
	}


	private Assignment getRandomAssignment(Set<Task> tasks) {
		Assignment a = new Assignment();

		//create Map with empty lists
		for (Vehicle vehicle : vehicles) {
			a.put(vehicle, new VehicleAssignment());
		}

		List<PublicAction> actionsLeft = new LinkedList<>();
		tasks.forEach(task -> actionsLeft.add(new PublicAction(task, PublicAction.ActionType.PICKUP)));

		Map<Task, Vehicle> map = new HashMap<>();

		Random random = new Random();
		while (!actionsLeft.isEmpty()) {
			int i = random.nextInt(actionsLeft.size());
			PublicAction action = actionsLeft.get(i);
			Vehicle vehicle = vehicles.get(random.nextInt(vehicles.size()));
			VehicleAssignment vehicleAssignment = a.get(vehicle);

			if (action.actionType == PublicAction.ActionType.DELIVERY) {
				a.get(map.get(action.task)).add(action);
				actionsLeft.remove(i);
			} else if (action.actionType == PublicAction.ActionType.PICKUP
					&& action.task.weight <= vehicle.capacity() - load(vehicle, vehicleAssignment)) {
				vehicleAssignment.add(action);
				actionsLeft.remove(i);
				actionsLeft.add(new PublicAction(action.task, PublicAction.ActionType.DELIVERY));
				map.put(action.task, vehicle);
			}
		}
		return a;
	}

	// computes the load of the vehicle
	// after the vehicle assignment, how heavy are all tasks that in the vehicle (picked up, but not delivered)
	private int load(Vehicle vehicle, VehicleAssignment vehicleAssignment) {
		int[] load = new int[vehicleAssignment.size()];
		int i = 0;
		for (PublicAction publicAction : vehicleAssignment) {
			if (publicAction.actionType == PublicAction.ActionType.DELIVERY) {
				i -= publicAction.task.weight;
			} else {
				i += publicAction.task.weight;
			}
		}
		return i;
	}


	private Assignment localChoice(Set<Assignment> neighbors, Assignment oldA) {
		List<Assignment> bestAssignments = new LinkedList<>();
		double bestCost = Double.MAX_VALUE;

		//get all the best results
		for (Assignment newA : neighbors) {
			if (assignmentCost(newA) < bestCost) {
				bestAssignments.clear();
				bestAssignments.add(newA);
				bestCost = assignmentCost(newA);
			} else if (assignmentCost(newA) == bestCost) {
				bestAssignments.add(newA);
			}
		}

		//chose randomly from list
		Random random = new Random();
		Assignment randomBestA = bestAssignments.get(random.nextInt(bestAssignments.size()));

		if (assignmentCost(oldA) > bestCost) {
			return randomBestA;
		} else if (assignmentCost(oldA) < bestCost) {
			return oldA;
		} else {
			if (Math.random() > CHANGEMENT_PROBABILITY) {
				return oldA;  //probability 1-p
			} else {
				return randomBestA; //probability p
			}
		}
	}


	private double assignmentCost(Assignment A) {
		assert A!=null;
		double costs = 0.0;

		for (Vehicle vehicle : vehicles) {
			costs += A.get(vehicle).totalDistance() * vehicle.costPerKm();
		}

		return costs;
	}


	private List<Plan> generatePlans(Assignment A) {
		List<Plan> plans = new LinkedList<>();
		assert A != null;
		for (Vehicle vehicle : vehicles) {
			plans.add(generatePlanForVehicle(A.get(vehicle), vehicle));
		}

		return plans;
	}


	private Plan generatePlanForVehicle(VehicleAssignment actions, Vehicle vehicle) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (PublicAction action : actions) {
			assert action.moveTo != null;
			if (current != action.moveTo) {
				for (City city : current.pathTo(action.moveTo)) {
					plan.appendMove(city);
				}
			}

			if (action.actionType == PublicAction.ActionType.PICKUP) {
				plan.appendPickup(action.task);
			} else if (action.actionType == PublicAction.ActionType.DELIVERY) {
				plan.appendDelivery(action.task);
			}

			current = action.moveTo;
		}

		return plan;
	}
}
