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
}
