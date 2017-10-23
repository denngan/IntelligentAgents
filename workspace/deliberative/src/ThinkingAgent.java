import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.time.Duration;
import java.util.*;

public class ThinkingAgent implements DeliberativeBehavior {
	enum Algorithm {BFS, ASTAR}

	/* Environment */
	private Topology topology;
	private TaskDistribution taskDistribution;

	/* the properties of the agent */
	private Agent agent;
	private int capacity;

	/* the planning class */
	private Algorithm algorithm;


	private Set<City> releventCities = new HashSet<>();

	/**
	 * The setup method is called exactly once, before the simulation starts and
	 * before any other method is called.
	 * <p>
	 * The <tt>agent</tt> argument allows you to access important information
	 * about your agent, most notably:
	 * <ul>
	 * <li>{@link Agent#vehicles()} the list of vehicles controlled
	 * by the agent</li>
	 * <li>{@link Agent#getTasks()} the set of tasks that the agent
	 * has accepted but not yet delivered</li>
	 * </ul>
	 *
	 * @param topology     The topology of the simulation
	 * @param distribution The task distribution of the simulation
	 * @param agent
	 */
	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		this.topology = topology;
		this.agent = agent;
		this.taskDistribution = distribution;

		// initialize the planner
		this.capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());


	}

	/**
	 * Computes the transportation plan for a vehicle. <br>
	 * In a single agent system, the agent can assume that the vehicle is
	 * carrying no tasks. In a multi-agent system this method might be called
	 * again during the execution of a plan (see
	 * {@link DeliberativeBehavior#planCancelled(TaskSet)}
	 * ).
	 *
	 * @param vehicle The vehicle that the agent is controlling
	 * @param tasks   The list of tasks to be handled
	 * @return The plan for the vehicle
	 */
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan = new Plan(vehicle.getCurrentCity());

		// Compute the plan with the selected algorithm.
		State state = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks() , tasks);
		long startTime = System.nanoTime();
		switch (algorithm) {
			case ASTAR:
				// ...
				plan = searchAStar(state);
				break;
			case BFS:
				// ...
				// plan = naivePlan(vehicle, tasks);
				state.setCapacity(vehicle.capacity());
				plan = BreadthFirst(state);
				break;
			default:
				throw new AssertionError("Algorithm not found.");
		}
		System.out.println("" + Duration.ofNanos(System.nanoTime() - startTime));
		return plan;
	}

	/**
	 * In a multi-agent system the plan of an agent might get 'stuck' in which
	 * case this method is called followed by a call to
	 * {@link DeliberativeBehavior#plan(Vehicle, TaskSet)}
	 * . The argument carriedTasks is the set of task that the vehicle has
	 * picked up but not yet delivered. These tasks have to be considered the
	 * next time a plan is computed.
	 * <p>
	 * You can also use {@link Vehicle#getCurrentTasks()} to
	 * obtain the set of tasks that the vehicle is holding.
	 *
	 * @param carriedTasks the tasks that the vehicle is holding
	 */
	@Override
	public void planCancelled(TaskSet carriedTasks) {

	}

	public class NodeComparator implements Comparator<Node<State>> {
		public int compare(Node<State> nodeFirst, Node<State> nodeSecond) {
			if (nodeFirst.getF() > nodeSecond.getF()) return 1;
			if (nodeSecond.getF() > nodeFirst.getF()) return -1;
			return 0;
		}
	}

	private Plan searchAStar(State startState) {
		releventCities = new HashSet<>();
		for (Task task :
				startState.getTasksToDo()) {
			releventCities.add(task.pickupCity);
			releventCities.add(task.deliveryCity);
		}
		for (Task task :
				startState.getCarriedTasks()) {
			releventCities.add(task.pickupCity);
			releventCities.add(task.deliveryCity);
		}

		PriorityQueue<Node<State>> openList = new PriorityQueue<>(11, new NodeComparator());
		HashSet<State> closedList = new HashSet<>();
		Map<State, State> parentState = new HashMap<>();
		Map<State, Node<State>> stateToNode = new HashMap<>();

		Node<State> startNode = new Node<State>(startState);
		openList.add(startNode);

		do {
			//System.out.println("schleife");
			Node<State> currentNode = openList.poll();

			State currentState = currentNode.getNodeData();
			if (currentState.isGoalState()) {
				return computePath(parentState, currentState, startState);
			}

			closedList.add(currentState);

			// expand node
			for (Node<State> successorNode : getSuccessorNodes(currentNode, stateToNode)) {
				State successorState = successorNode.getNodeData();

				if (closedList.contains(successorState)) {
					continue;
				}

				double tentativeG = currentNode.getG() + cost(currentState, successorState);

				if (openList.contains(successorNode) && tentativeG >= successorNode.getG()) {
					continue;
				}

				successorNode.setG(tentativeG);
				parentState.put(successorState, currentState);
				if (!openList.contains(successorNode)) {
					openList.add(successorNode);
				}
			}
		} while (!openList.isEmpty());
		System.out.println("no path found");
		return null;
	}


	private Plan computePath(Map<State, State> parentState, State goalState, State startState) {
		State currentState = goalState;
		Plan plan = new Plan(startState.getCurrentCity());

		List<Action> list = new LinkedList<>();

		while (currentState != startState) {
			State previousState = parentState.get(currentState);
			TaskSet deliveries = previousState.getCarriedTasks().clone();
			deliveries.removeAll(currentState.getCarriedTasks());

			for (Task delivery : deliveries) {
				list.add(new Action.Delivery(delivery));
			}
			if (currentState.getCurrentCity() == previousState.getCurrentCity()) {
				TaskSet newTask = previousState.getTasksToDo().clone();
				newTask.removeAll(currentState.getTasksToDo());
				assert newTask.size() == 1;

				list.add(new Action.Pickup((Task) newTask.toArray()[0]));
			} else {
				List<Action> newList = new LinkedList<>();
				for (City c :
						previousState.getCurrentCity().pathTo(currentState.getCurrentCity())) {
					newList.add(new Action.Move(c));
				}
				Collections.reverse(newList);
				list.addAll(newList);

			}
			currentState = previousState;
		}

		Collections.reverse(list);

		for (Action a :
				list) {
			plan.append(a);
		}
		System.out.println(plan);
		return plan;

	}

	List<Node<State>> getSuccessorNodes(Node<State> node, Map<State, Node<State>> stateToNodeMap) {
		List<Node<State>> returnList = new LinkedList<>();

		for (State successor : getSuccessorStates(node.getNodeData())) {
			Node<State> nodeTemp;
			if (!stateToNodeMap.containsKey(successor)) {
				nodeTemp = new Node<State>(successor);
				stateToNodeMap.put(successor, nodeTemp);
			} else {
				nodeTemp = stateToNodeMap.get(successor);
			}
			returnList.add(nodeTemp);
		}
		return returnList;
	}

	List<State> getSuccessorStates(State state) {
		List<State> list = new LinkedList<>();

		for (City city : topology.cities()) {
			if (city != state.getCurrentCity()) {
				TaskSet newTaskSet = state.getCarriedTasks().clone();
				for (Task task : state.getCarriedTasks()) {
					if (task.deliveryCity == city) {
						newTaskSet.remove(task);
					}
				}
				State stateTemp = new State(city, newTaskSet, state.getTasksToDo());
				list.add(stateTemp);
			}
		}

		for (Task task : state.getTasksToDo()) {
			if (task.pickupCity == state.getCurrentCity()) {
				TaskSet newTasksToDo = state.getTasksToDo().clone();
				newTasksToDo.remove(task);
	            TaskSet newCarriedTasks= state.getCarriedTasks().clone();
	            newCarriedTasks.add(task);
	            if (newCarriedTasks.weightSum() <= capacity){
		            list.add(new State(state.getCurrentCity(), newCarriedTasks, newTasksToDo));
	            }
			}
		}

		return list;
	}

	private double cost(State state1, State state2) {
		return state1.getCurrentCity().distanceTo(state2.getCurrentCity());
	}

	// heuristics
}
