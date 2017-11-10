import java.util.*;
import java.util.concurrent.*;

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
	private int MAX_STEPS_WITHOUT_IMPROVEMENT = 20;
	private int MAX_STEPS = 1000;
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
		timeout_plan = 10000;
		System.out.println("Time for plan:" + timeout_plan);
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();

		Assignment assignment = stochasticRestart(tasks, timeout_plan);

		List<Plan> plans = generatePlan(assignment);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		System.out.println("The cost of the plan is " + assignmentCost(assignment) +".");
		return plans;
	}


	private Assignment stochasticRestart(TaskSet tasks, long timeOut) {
		final ExecutorService service = Executors.newSingleThreadExecutor();
		// maybe dont use currenttimemillis
		long deadline = System.currentTimeMillis() + timeOut - 100;

		Assignment bestAssignment = null;
		double bestCost = Double.MAX_VALUE;

		while (true) {
			Assignment currentAssignment;
			long timeLeft = deadline - System.currentTimeMillis();

			if (timeLeft <= 0) {
				return bestAssignment;
			}

			try {
				final Future<Assignment> f = service.submit(() -> SLS(tasks));
				// maybe give even less time
				currentAssignment = f.get(timeLeft, TimeUnit.MILLISECONDS);
			} catch (final TimeoutException e) {
				service.shutdown();
				return bestAssignment;
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}

			System.out.println(assignmentCost(currentAssignment));
			if (assignmentCost(currentAssignment) < bestCost) {
				bestAssignment = currentAssignment;
				bestCost = assignmentCost(currentAssignment);
			}
		}
	}

	/**
	 * @param tasks
	 * @return
	 */
	private Assignment SLS(TaskSet tasks) {
                long time_start = System.currentTimeMillis();
		int stepsTotal = 0;
		int stepsWithoutImprovement = 0;
		Assignment assignment = selectInitialSolution2(tasks); // A
		do {
			Assignment oldAssignment = assignment; // A_old
			Set<Assignment> neighbours = chooseNeighbours(oldAssignment, tasks);
			// add additional random solutions if search stagnates
			for (int i = 0; i < Math.sqrt(stepsWithoutImprovement); i++) {
				neighbours.add(selectInitialSolution2(tasks));
			}

			assignment = localChoice(neighbours, oldAssignment);

			stepsTotal++;
			if (assignmentCost(assignment) == assignmentCost(oldAssignment)) {
				stepsWithoutImprovement++;
			} else {
				stepsWithoutImprovement = 0;
			}
			// System.out.println("Step " + stepsTotal + "; without improvement: " + stepsWithoutImprovement);
		} while (stepsTotal < MAX_STEPS
				&& stepsWithoutImprovement < MAX_STEPS_WITHOUT_IMPROVEMENT && (System.currentTimeMillis()-time_start)<timeout_plan-5000 );
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

	private Assignment selectInitialSolution2(TaskSet tasks) {
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
			int i= random.nextInt(actionsLeft.size());
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

	private int load(Vehicle vehicle, VehicleAssignment vehicleAssignment) {
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
				for (int i = 0; i < oldAssignment.get(chosenVehicle).size(); i++) {
					PublicAction action = oldAssignment.get(chosenVehicle).get(i);
					if (action.actionType == PublicAction.ActionType.PICKUP
							&& action.task.weight <= vehicle.capacity()) {
						neighbours.add(changingVehicle(oldAssignment, chosenVehicle, vehicle, i));
					}
				}

			}
		});
		// System.out.println("Number of neighbours: " + neighbours.size());

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

		// System.out.println("Number of neighbours: " + neighbours.size());
		return neighbours;
	}

	/**
	 * Permutes
	 * @param oldAssignment
	 * @param vehicle
	 * @param indexTask
	 * @return
	 */
	private Set<Assignment> changingTaskOrder(Assignment oldAssignment, Vehicle vehicle, int indexTask) {
		Set<Assignment> neighbours = new HashSet<>();


		Assignment assignment = new Assignment(oldAssignment);
		VehicleAssignment vehicleAssignment = assignment.get(vehicle);

		PublicAction pickup = oldAssignment.get(vehicle).get(indexTask);
		Task toMove = pickup.task;
		PublicAction delivery = null;
		assert pickup.task != null;

		for (int i = indexTask +1; i < vehicleAssignment.size(); i++) {
			PublicAction current = vehicleAssignment.get(i);
			if (current.task == toMove) {
				assert current.actionType == PublicAction.ActionType.DELIVERY;
				delivery = current;
				assignment.get(vehicle).remove(i);
				assignment.get(vehicle).remove(indexTask);
				break;
			}
		}
		assert delivery != null;

		// precompute the space left after each action
		int weight = toMove.weight;
		int[] spaceLeft = new int[vehicleAssignment.size() + 2];
		spaceLeft[0] = vehicle.capacity();
		for (int j = 0; j < vehicleAssignment.size(); j++) {
			if (vehicleAssignment.get(j).actionType == PublicAction.ActionType.PICKUP) {
				spaceLeft[j+1] = spaceLeft[j] - vehicleAssignment.get(j).task.weight;
			} else { // delivery
				spaceLeft[j+1] = spaceLeft[j] + vehicleAssignment.get(j).task.weight;
			}
		}
		spaceLeft[spaceLeft.length-1] = weight;

		int stop = 0;
		for (int i = 0; i < spaceLeft.length - 1; i++) {
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
		for (int k = stop; k < spaceLeft.length-1; k++) {
			for (int l = k; l < spaceLeft.length-1; l++) {
				Assignment temp = new Assignment(assignment);
				temp.get(vehicle).add(k, pickup);
				temp.get(vehicle).add(l+1, delivery); // index + 1 since the pickup shifts the order
				neighbours.add(temp);
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

		//assignment.get(vehicle).removeIf(a -> a.task == toMove);
		PublicAction pickup = oldAssignment.get(vehicleFrom).get(0);
		Task toMove = pickup.task;
		PublicAction delivery = null;
		for (int i = 1; i < oldAssignment.get(vehicleFrom).size(); i++) {
			PublicAction current = oldAssignment.get(vehicleFrom).get(i);
			if (current.task == toMove) {
				delivery = current;
				assignment.get(vehicleFrom).remove(i);
				assignment.get(vehicleFrom).remove(0);
				break;
			}
		}
		assert delivery != null;
		// add infront of vehicleTo
		assignment.get(vehicleTo).add(0, pickup);
		assignment.get(vehicleTo).add(1, delivery);
		return assignment;
	}

	private Assignment changingVehicle(Assignment oldAssignment, Vehicle vehicleFrom, Vehicle vehicleTo, int index) {
		Assignment assignment = new Assignment(oldAssignment);
		PublicAction action = assignment.get(vehicleFrom).get(index);
		assert action.actionType == PublicAction.ActionType.PICKUP;
		assignment.get(vehicleFrom).remove(index);
		assignment.get(vehicleFrom).add(0, action);

		//return oldAssignment;
		return changingVehicle(assignment, vehicleFrom, vehicleTo);
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
			System.out.println("chose old assignment");
			return oldA;
		} else {
			if (Math.random() > CHANGEMENT_PROBABILITY) {
				return oldA;  //probability 1-p
			} else {
				return randomBestA; //probability p
			}
		}
//		if (Math.random() > CHANGEMENT_PROBABILITY) {
//			return oldA;  //probability 1-p
//		} else {
//			return randomBestA; //probability p
//		}
	}


	private double assignmentCost(Assignment A) {
		double costs = 0;

		for (Vehicle vehicle : vehicles) {
			costs += generatePlanForVehicle(A.get(vehicle), vehicle).totalDistance() * vehicle.costPerKm();
		}

		return costs;
	}


	private List<Plan> generatePlan(Assignment A) {

		List<Plan> plans = new LinkedList<>();

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
			for (City city : current.pathTo(action.moveTo)) {
				plan.appendMove(city);
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
