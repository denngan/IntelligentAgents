import logist.task.TaskSet;
import logist.topology.Topology;

public class State {
	private Topology.City currentCity;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		State state = (State) o;

		if (currentCity != null ? !currentCity.equals(state.currentCity) : state.currentCity != null) return false;
		if (carriedTasks != null ? !carriedTasks.equals(state.carriedTasks) : state.carriedTasks != null) return false;
		return tasksToDo != null ? tasksToDo.equals(state.tasksToDo) : state.tasksToDo == null;
	}

	@Override
	public int hashCode() {
		int result = currentCity != null ? currentCity.hashCode() : 0;
		result = 31 * result + (carriedTasks != null ? carriedTasks.hashCode() : 0);
		result = 31 * result + (tasksToDo != null ? tasksToDo.hashCode() : 0);
		return result;
	}

	private TaskSet carriedTasks;

	private TaskSet tasksToDo;
        
        private int capacity;

	public State(Topology.City currentCity, TaskSet carriedTasks, TaskSet tasksToDo) {
		this.currentCity = currentCity;
		this.carriedTasks = carriedTasks;
		this.tasksToDo = tasksToDo;
	}
        
        public State(Topology.City currentCity, TaskSet carriedTasks, TaskSet tasksToDo,int capacity) {
		this.currentCity = currentCity;
		this.carriedTasks = carriedTasks;
		this.tasksToDo = tasksToDo;
                this.capacity= capacity;
	}
        

	public boolean isGoalState(){
		return tasksToDo.isEmpty() && carriedTasks.isEmpty();
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

    /**
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @param capacity the capacity to set
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
