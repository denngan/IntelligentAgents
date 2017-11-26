import logist.simulation.Vehicle;
import logist.task.Task;

public class CopyCat extends AuctionAgent {
	@Override
	protected Long computeBid(Task task, long marginalCost, long timeout) {
		if (round == 0) {
			return (long) (task.pickupCity.distanceTo(task.deliveryCity) * vehicles.stream().mapToInt(Vehicle::costPerKm).sum() / vehicles.size());
		}
		return enemiesBids.get(round - 1);
	}

	@Override
	protected void printName() {
		println("I am Copy Cat");
	}
}
