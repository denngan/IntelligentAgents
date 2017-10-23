import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
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


	private Set<City> relevantCities = new HashSet<>();

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
		System.out.println("Using " + algorithmName);

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
		Plan plan;

		// Compute the plan with the selected algorithm.
		State state = new State(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), tasks);
		long startTime = System.nanoTime();

		relevantCities = computeRelevantCities(state);

		switch (algorithm) {
			case ASTAR:
				// ...
				plan = searchAStar(state);
				break;
			case BFS:
				// ...
				// plan = naivePlan(vehicle, tasks);
				state.setCapacity(vehicle.capacity());
				plan = myBFS(state);
				break;
			default:
				throw new AssertionError("Algorithm not found.");
		}
		System.out.println("" + Duration.ofNanos(System.nanoTime() - startTime));
		return plan;
	}

	private Set<City> computeRelevantCities(State state) {
		Set<City> cities = new HashSet<>();
		for (Task task :
				state.getTasksToDo()) {
			cities.add(task.pickupCity);
			cities.add(task.deliveryCity);
		}
		for (Task task :
				state.getCarriedTasks()) {
			cities.add(task.deliveryCity);
		}

		return cities;
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
		PriorityQueue<Node<State>> openList = new PriorityQueue<>(11, new NodeComparator());
		HashSet<State> closedList = new HashSet<>();

		Map<State, State> parentState = new HashMap<>();
		Map<State, Node<State>> stateToNode = new HashMap<>();

		Node<State> startNode = new Node<State>(startState, 0);
		openList.add(startNode);

		do {
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


	// BFS

	public Plan myBFS(State startState) {
		Queue<State> queue = new LinkedList<>();
		HashSet<State> visitedStates = new HashSet<>();
		Map<State, State> parentState = new HashMap<>();

		queue.add(startState);
		visitedStates.add(startState);

		while (!queue.isEmpty()) {
			State currentState = queue.poll();
			if (currentState.isGoalState()) {
				// compute path
				return computePath(parentState, currentState, startState);
			}

			// expand
			for (State successorState : getSuccessorStates(currentState)) {
				if (!visitedStates.contains(successorState)) {
					queue.add(successorState);
					visitedStates.add(successorState);
					parentState.put(successorState, currentState);
				}
			}
		}

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

		for (State successorState : getSuccessorStates(node.getNodeData())) {
			Node<State> nodeTemp;
			if (!stateToNodeMap.containsKey(successorState)) {
				nodeTemp = new Node<>(successorState, heuristicValue(successorState));
				stateToNodeMap.put(successorState, nodeTemp);
			} else {
				nodeTemp = stateToNodeMap.get(successorState);
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
				TaskSet newCarriedTasks = state.getCarriedTasks().clone();
				newCarriedTasks.add(task);
				if (newCarriedTasks.weightSum() <= capacity) {
					list.add(new State(state.getCurrentCity(), newCarriedTasks, newTasksToDo));
				}
			}
		}

		return list;
	}

	private double cost(State state1, State state2) {
		return state1.getCurrentCity().distanceTo(state2.getCurrentCity());
	}

	//________________________________________________________________
	private Plan BreadthFirst(State startState) {
		HashSet<NodeB> firstLevel = new HashSet<>();

		NodeB firstNode = new NodeB(startState, 0, new LinkedList<City>());
		firstLevel.add(firstNode);

		HashSet<NodeB> finals = BFS(firstLevel, new HashSet<NodeB>());
		//got back all possible routes to endpoint

		NodeB goal = Collections.min(finals);

		return getPlanFromPath(startState, goal);


	}

	private Plan getPlanFromPath(State startState, NodeB goal) {

		Plan plan = new Plan(startState.getCurrentCity());

		TaskSet carried = startState.getCarriedTasks();
		TaskSet ToDos = startState.getTasksToDo();


		LinkedList<City> path = goal.getPath();

		System.out.println(path);
		//System.out.println(ToDos);
		for (int i = 0; i < path.size(); i++) {

			plan = addAvailablePickUps(new State(path.get(i), carried, ToDos), plan);

			if (i == path.size() - 1) {
				//reached goal, do I need to travel back?

			} else {

				for (City c : path.get(i).pathTo(path.get(i + 1))) {
					plan.appendMove(c);
				}

			}


		}
		System.out.println(plan);
		return plan;
	}

	Plan addAvailablePickUps(State state, Plan plan) {
		City currentCity = state.getCurrentCity();

		for (Task task : state.getCarriedTasks()) {
			if (task.deliveryCity == currentCity) {
				plan.appendDelivery(task);
				state.getCarriedTasks().remove(task);
			}
		}
		for (Task task : state.getTasksToDo()) {
			if (task.pickupCity == currentCity) {
				plan.appendPickup(task);
				state.getTasksToDo().remove(task);
				state.getCarriedTasks().add(task);
			}
		}
		// deliveries are automatic
		return plan;
	}


	private HashSet<NodeB> BFS(HashSet<NodeB> level, HashSet<NodeB> finals) { //goal state is clear--> all with empty tasks

		HashSet<NodeB> allChildren = new HashSet<>();

		//get all child states, not in visited
		for (NodeB node : level) {
			//next states -> move either to city where there is atask to pick up or to city where there is a task to deliver
			//getSuccesorStates or nodes
			// iterate through successor states and remove visited ...there wont be visited as you only pick up or deliver tasks. moving is made on its own
			State state = node.getState();
			HashSet<State> children = getSuccessorStatesB(state);
			for (State child : children) {
				double cost = node.getCosts() + node.getState().getCurrentCity().distanceUnitsTo(child.getCurrentCity());
				LinkedList<City> path = (LinkedList<City>) node.getPath().clone();
				path.add(child.getCurrentCity());

				/*
				TaskSet delivered = node.getState().getCarriedTasks().clone();
				delivered.removeAll(child.getCarriedTasks());
				TaskSet pickups = node.getState().getTasksToDo();
				pickups.removeAll(child.getTasksToDo());
				*/
				//add to plan


				NodeB nextNode = new NodeB(child, cost, path);
				allChildren.add(nextNode);
				//added child node to the next level that needs to be checked

				if (child.isGoalState()) {
					finals.add(nextNode);
					//delete child from children
					allChildren.remove(nextNode);
				}

				//System.out.println(path);
			}

		}

		if (allChildren.isEmpty()) {
			return finals;
		} else {
			return BFS(allChildren, finals);
		}

		//create node mit liste der städte des paths (zeiger nach oberer stadt) und kosten des ganzen weges..
		//visited can be new for each level->next HashSet Nodes
		//when checking child add to visited/ nextas node
		//check if visited set has already Node that node,
		// if yes check costs, chose the one with smaller costs

		//when done go through visited/next states. get all with empty tasks
		// get the one with smallest cost --> end

		//if no empty task keep on searching recursively


	}

	/*
	return set of possible cities to got to
	*/
	private HashSet<City> getSuccessorCities(State state) {

		HashSet<City> successorCities = new HashSet();
		TaskSet toDos = state.getTasksToDo().clone();
		TaskSet carrying = state.getCarriedTasks().clone();

		for (Task task : state.getTasksToDo()) {


			//if (state.getCapacity() > task.weight) {
			{
				if (!successorCities.add(task.pickupCity)) {
					// can happen if two tasks maybe?return null; //could also return exception or just output
				}
			}
		}

		for (Task task : state.getCarriedTasks()) {


			if (!successorCities.add(task.deliveryCity)) {
				//return null; //could also return null or just output
			}


		}

		return successorCities;
	}


	//function to go through Successor city, for all cities make states:
	//remove deliverable tasks form carried and add capacity
	//add picked up task to carried, romeve for toDo and from capacity

	//!!only problem if two tasks to pick up from same city....
	private HashSet<State> getSuccessorStatesB(State state) {
		return new HashSet<>(getSuccessorStates(state));

		/*
		HashSet<State> successorStates = new HashSet<>();
		HashSet<City> cities = getSuccessorCities(state);
		if (cities == null) {
			return null;
		} else {
			for (City city : cities) {
				State tempState = new State(city, state.getCarriedTasks().clone(), state.getTasksToDo().clone(), state.getCapacity());

				for (Task delivery : state.getCarriedTasks()) {
					if (delivery.deliveryCity.equals(city)) {
						tempState.getCarriedTasks().remove(delivery);
						tempState.setCapacity(tempState.getCapacity() + delivery.weight);
					}
				}

				for (Task pickUp : state.getTasksToDo()) {
					if (pickUp.pickupCity.equals(city)) {

						if (pickUp.weight < tempState.getCapacity()) {
							tempState.getCarriedTasks().add(pickUp);
							tempState.getTasksToDo().remove(pickUp);
							tempState.setCapacity(tempState.getCapacity() - pickUp.weight);
						}

					}
				}

				successorStates.add(tempState);
			}
			return successorStates;
		}*/
	}

	// heuristics
	public double heuristicValue(State state) {
		return mst(state);
	}

	private double mst(State state) {
		return new Graph(topology, computeRelevantCities(state)).minimalSpanningTree();
	}

	private double constant() {
		return 1;
	}

}
