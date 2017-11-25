package centralized;

import logist.plan.Plan;
import logist.simulation.Vehicle;
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

	public Plan generatePlanForVehicle(Vehicle vehicle) {
		Topology.City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (PublicAction action : this) {
			assert action.moveTo != null;
			if (current != action.moveTo) {
				for (Topology.City city : current.pathTo(action.moveTo)) {
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
