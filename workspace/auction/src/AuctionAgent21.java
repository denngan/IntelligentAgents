import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.ArrayList;
import java.util.List;

public class AuctionAgent21 extends AuctionAgent {
	protected String name = "AA21";
	// weights for our Cost
	// case distinction for when the estimated enemy's cost is higher/lower than our cost
	protected double weightLoosing = 0.5;
	protected double weightWinning = 0.5;
	protected List<Long> estimatedEnemysCosts = new ArrayList<>();
	@Override
	protected Long computeBid(Task task, long marginalCost) {
		long enemiesMarginalCost = (long) enemy.computeMarginalCost(task, timeOutBid);
		estimatedEnemysCosts.add(enemiesMarginalCost);

		String msg = "" + marginalCost + " | " + enemiesMarginalCost;
		if (marginalCost > enemiesMarginalCost) {
			msg = msg + " !!!";
		}
		println(msg);


		long bid;
		if (marginalCost > enemiesMarginalCost) {
			bid = (long) (weightLoosing * marginalCost +  (1 - weightLoosing) * enemiesMarginalCost);
		} else {
			bid = (long) (weightWinning * marginalCost +  (1 - weightWinning) * enemiesMarginalCost) - 1;
		}

//		if (round == 0) {
//			bid = (long) (task.pickupCity.distanceTo(task.deliveryCity) * vehicles.stream().mapToInt(Vehicle::costPerKm).sum() / vehicles.size());
//		}


		//
		bid *= 1.2 - Math.exp(-0.17 * (round +1));
		return bid;
	}

	@Override
	protected void printName() {
		println("I am AuctionAgent21");
	}

	@Override
	protected void updateStrategy() {
		if (ourBids.get(round) < enemiesBids.get(round)) {
			if (Math.max(estimatedEnemysCosts.get(round), enemiesBids.get(round)) != 0) {
				weightWinning += (estimatedEnemysCosts.get(round) - enemiesBids.get(round)) / Math.max(estimatedEnemysCosts.get(round), enemiesBids.get(round)) * (1 - weightWinning) / (round + 1);
			}
		} else {

		}
	}
}