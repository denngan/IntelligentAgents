package centralized;

import logist.plan.Plan;
import logist.simulation.Vehicle;

import java.util.*;

public class Assignment extends HashMap<Vehicle, VehicleAssignment> {
	public Assignment() {
		super();
	}

	public Assignment(Map<? extends Vehicle, ? extends VehicleAssignment> m) {
		this();
		m.forEach((vehicle, vehicleAssignment) -> this.put(vehicle, new VehicleAssignment(vehicleAssignment)));
	}

	public double cost() {
		double cost = 0.0;

		for (Vehicle vehicle : keySet()) {
			cost += get(vehicle).totalDistance() * vehicle.costPerKm();
		}

		return cost;
	}

	public List<Plan> generatePlans(List<Vehicle> vehicles) {
		List<Plan> plans = new LinkedList<>();
		for (Vehicle vehicle : vehicles) {
			plans.add(get(vehicle).generatePlanForVehicle(vehicle));
		}

		return plans;
	}
}
