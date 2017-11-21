package centralized;

import logist.task.Task;
import logist.topology.Topology;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VehicleAssignment extends LinkedList<PublicAction> {
	public VehicleAssignment() {
	}

	public VehicleAssignment(Collection<? extends PublicAction> c) {
		super(c);
	}

	public double totalDistance() {
		if (size() <= 0) {
			return 0;
		}

		double costs = 0;
		assert this.get(0).actionType == PublicAction.ActionType.PICKUP;
		Topology.City currentCity = this.get(0).moveTo;

		for (int i = 1; i < this.size(); i++) {
			Topology.City nextCity = this.get(i).moveTo;
			costs += currentCity.distanceTo(nextCity);
			currentCity = nextCity;
		}

		return costs;
	}

	public Task removeTask(int index) {
		Task toRemove = get(index).task;
		this.removeIf(a -> a.task == toRemove);
		return toRemove;
	}
}
