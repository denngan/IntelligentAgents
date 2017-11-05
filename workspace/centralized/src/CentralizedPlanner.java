import java.util.*;

import logist.LogistSettings;

import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
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
	private int MAX_STEPS_WITHOUT_IMPROVEMENT = Integer.MAX_VALUE;
	private int MAX_STEPS = 10;
	private double CHANGEMENT_PROBABILITY = 0.4;


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

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();

		Assignment planSLS = SLS(tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans = generatePlan(planSLS);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");

		return plans;
	}


	/**
	 * @param tasks
	 * @return
	 */
	private Assignment SLS(TaskSet tasks) {
		int stepsTotal = 0;
		int stepsWithoutImprovement = 0;
		Assignment assignment = selectInitialSolution(tasks); // A
		System.out.println("Initial assignment found");
		do {
			Assignment oldAssignment = assignment; // A_old
			Set<Assignment> neighbours = chooseNeighbours(oldAssignment, tasks);
			assignment = localChoice(neighbours, oldAssignment);
			stepsTotal++;
			if (costFunction(assignment) == costFunction(oldAssignment)) {
				stepsWithoutImprovement++;
			} else {
				stepsWithoutImprovement = 0;
			}
			System.out.println("Step " + stepsTotal + "; without improvement: " + stepsWithoutImprovement);
		} while (stepsTotal < MAX_STEPS
				&& stepsWithoutImprovement < MAX_STEPS_WITHOUT_IMPROVEMENT);
		return assignment;
	}


	// TODO better random uniform
	private Assignment selectInitialSolution(TaskSet tasks) {
		int numVehicles = vehicles.size();
		Assignment a = new Assignment();

		//create Map with empty lists
		for (Vehicle vehicle : vehicles) {
			a.put(vehicle, new VehicleAssignment());
		}

		//add each task randomly to car
		for (Task task : tasks) {
			//chose car randomly
			Random random = new Random();
			int randomVehicleIndex = random.nextInt(numVehicles);

			//check that task fits
			//could be infinite loop! do differently (for loop for limited tries)
//			while (task.weight > vehicles.get(randomVehicleIndex).capacity()) {
//				random = new Random();
//				randomVehicleIndex = random.nextInt(numVehicles + 1);
//			}

			//add pickup action and delivery action to vehicle's list
			a.get(vehicles.get(randomVehicleIndex)).add(new PublicAction(task, PublicAction.ActionType.PICKUP));
			a.get(vehicles.get(randomVehicleIndex)).add(new PublicAction(task, PublicAction.ActionType.DELIVERY));
		}
		return a;
	}

	private Set<Assignment> chooseNeighbours(Assignment oldAssignment, TaskSet tasks) {
		Set<Assignment> neighbours = new HashSet<>();
		Random random = new Random();

		// choose a random vehicle with tasks
		Vehicle randomVehicle;
		do {
			randomVehicle = vehicles.get(random.nextInt(vehicles.size()));
		} while (oldAssignment.get(randomVehicle).isEmpty());

		final Vehicle chosenVehicle = randomVehicle;
		// Applying the changing vehicle operator :
		vehicles.forEach(vehicle -> {
			if (vehicle != chosenVehicle) {
				PublicAction action = oldAssignment.get(chosenVehicle).get(0);
				if (action.task.weight <= vehicle.capacity()) {
					neighbours.add(changingVehicle(oldAssignment, chosenVehicle, vehicle));
				}
			}
		});
		System.out.println("vehicle changed");
		// Applying the Changing task order operator :
		// compute the number of tasks of the vehicle
		VehicleAssignment vehicleAssignment = oldAssignment.get(chosenVehicle);
		int length = vehicleAssignment.size();
		if (length >= 4) { // 4 instead of 2 since every task appears twice: as pickup and delivery
			for (int i = 0; i < length - 1; i++) {
				if (oldAssignment.get(chosenVehicle).get(i).actionType == PublicAction.ActionType.PICKUP) {
					neighbours.addAll(changingTaskOrder(oldAssignment, chosenVehicle, i));
				}
			}
		}

		return neighbours;
	}

	/**
	 * @param oldAssignment
	 * @param vehicle
	 * @param indexTask
	 * @return
	 */
	private Set<Assignment> changingTaskOrder(Assignment oldAssignment, Vehicle vehicle, int indexTask) {
		Set<Assignment> neighbours = new HashSet<>();


		Assignment assignment = new Assignment(oldAssignment);
		VehicleAssignment vehicleAssignment = assignment.get(vehicle);

		//assignment.get(vehicle).removeIf(a -> a.task == toMove);
		PublicAction pickup = oldAssignment.get(vehicle).get(indexTask);
		Task toMove = pickup.task;
		PublicAction delivery = null;
		for (int i = indexTask +1; i < oldAssignment.get(vehicle).size(); i++) {
			PublicAction current = oldAssignment.get(vehicle).get(i);
			if (current.task == toMove) {
				assert current.actionType == PublicAction.ActionType.DELIVERY;
				delivery = current;
				assignment.get(vehicle).remove(i);
				assignment.get(vehicle).remove(indexTask);
				break;
			}
		}

		// precompute the space left after each action
		int weight = toMove.weight;
		int[] spaceLeft = new int[vehicleAssignment.size() + 1];
		spaceLeft[0] = vehicle.capacity();
		for (int j = 1; j < spaceLeft.length; j++) {
			if (vehicleAssignment.get(j-1).actionType == PublicAction.ActionType.PICKUP) {
				spaceLeft[j] = spaceLeft[j-1] - vehicleAssignment.get(j-1).task.weight;
			} else { // delivery
				spaceLeft[j] = spaceLeft[j-1] + vehicleAssignment.get(j-1).task.weight;
			}
		}

		int stop = 0;
		for (int i = 0; i < vehicleAssignment.size() + 1; i++) {
			if (spaceLeft[i] < weight) {
				for (int k = stop; k < i; k++) {
					for (int l = k; l <= i; l++) {
						Assignment temp = new Assignment(assignment);
						temp.get(vehicle).add(k, pickup);
						temp.get(vehicle).add(l+1, delivery); // index + 1 since the pickup shifts the order
						neighbours.add(temp);
					}
				}
				stop = i+1;
			}
		}

		return neighbours;
	}

	/**
	 * Returns a new assignment where the first task of the vehicleFrom is transferred to vehicleTo.
	 * TODO details
	 * Throws exception if vehicleFrom has no task
	 * @param oldAssignment
	 * @param vehicleFrom
	 * @param vehicleTo
	 * @return
	 */
	private Assignment changingVehicle(Assignment oldAssignment, Vehicle vehicleFrom, Vehicle vehicleTo) {
		Assignment assignment = new Assignment(oldAssignment);
		VehicleAssignment vehicleAssignment = assignment.get(vehicleFrom);

		//assignment.get(vehicle).removeIf(a -> a.task == toMove);
		PublicAction pickup = oldAssignment.get(vehicleFrom).get(0);
		Task toMove = pickup.task;
		PublicAction delivery = null;
		for (int i = 1; i < oldAssignment.get(vehicleFrom).size(); i++) {
			PublicAction current = oldAssignment.get(vehicleFrom).get(i);
			if (current.task == toMove) {
				delivery = current;
				assignment.get(vehicleFrom).remove(i);
				assignment.get(vehicleFrom).remove(vehicleFrom);
				break;
			}
		}
		// add infront of vehicleTo
		assignment.get(vehicleTo).add(0, new PublicAction(toMove, PublicAction.ActionType.PICKUP));
		assignment.get(vehicleTo).add(1, new PublicAction(toMove, PublicAction.ActionType.DELIVERY));
		return assignment;
	}


	private Assignment localChoice(Set<Assignment> neighbors, Assignment oldA) {
		List<Assignment> bestAssignments = new LinkedList<>();
		double bestCost = Double.MAX_VALUE;

		//get all the best results
		for (Assignment newA : neighbors) {
			if (costFunction(newA) < bestCost) {
				bestAssignments = new LinkedList<>();
				bestAssignments.add(newA);
				bestCost = costFunction(newA);
			} else if (costFunction(newA) == bestCost) {
				bestAssignments.add(newA);
			}
		}

		//chose randomly from list
		Random random = new Random();
		Assignment randomBestA = bestAssignments.get(random.nextInt(bestAssignments.size()));

		if (costFunction(oldA) > bestCost) {
			return randomBestA;
		} else if (costFunction(oldA) < bestCost) {
			//that should never happen
			assert false;
			return oldA;
		} else {
			if (Math.random() > CHANGEMENT_PROBABILITY) {
				return oldA;  //probability 1-p
			} else {
				return randomBestA; //probability p
			}
		}
	}


	private double costFunction(Assignment A) {
		double costs = 0;

		for (Vehicle vehicle : vehicles) {
			costs += generatePlanV(A.get(vehicle), vehicle).totalDistance() * vehicle.costPerKm();
		}

		return costs;
	}


	private List<Plan> generatePlan(Assignment A) {

		List<Plan> plans = new LinkedList<>();

		for (Vehicle vehicle : vehicles) {

			plans.add(generatePlanV(A.get(vehicle), vehicle));
		}

		return plans;
	}


	private Plan generatePlanV(VehicleAssignment actions, Vehicle vehicle) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (PublicAction action : actions) {
			for (City city : current.pathTo(action.moveTo)) {
				plan.appendMove(city);
			}

			if (action.actionType == PublicAction.ActionType.PICKUP) {
				plan.appendPickup(action.task);

			} else if (action.actionType == PublicAction.ActionType.DELIVERY) {
				plan.appendDelivery(action.task);
			}

			current = action.moveTo;
			assert current != null;
		}

		return plan;
	}
}
