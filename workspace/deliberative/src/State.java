import logist.task.TaskSet;
import logist.topology.Topology;

public class State {
	private Topology.City currentCity;

	private TaskSet carriedTasks;

	private TaskSet tasksToDo;

	public State(Topology.City currentCity, TaskSet carriedTasks, TaskSet tasksToDo) {
		this.currentCity = currentCity;
		this.carriedTasks = carriedTasks;
		this.tasksToDo = tasksToDo;
	}

	public boolean isGoalState(){
		return tasksToDo.isEmpty();
	}

	public Topology.City getCurrentCity() {
		return currentCity;
	}

	public void setCurrentCity(Topology.City currentCity) {
		this.currentCity = currentCity;
	}

	public TaskSet getCarriedTasks() {
		return carriedTasks;
	}

	public void setCarriedTasks(TaskSet carriedTasks) {
		this.carriedTasks = carriedTasks;
	}

	public TaskSet getTasksToDo() {
		return tasksToDo;
	}

	public void setTasksToDo(TaskSet tasksToDo) {
		this.tasksToDo = tasksToDo;
	}
}
