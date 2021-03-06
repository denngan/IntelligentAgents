import logist.task.Task;

import java.util.ArrayList;
import java.util.List;

public class AuctionAgent22 extends AuctionAgent {
	protected String name = "AA22";
	// weights for our Cost
	// case distinction for when the estimated enemy's cost is higher/lower than our cost
	protected double weightLoosing = 0.5;
	protected double weightWinning = 0.5;
	protected List<Long> estimatedEnemysCosts = new ArrayList<>();
	@Override
	protected Long computeBid(Task task, long marginalCost, long timeout) {
		long enemiesMarginalCost = (long) enemy.computeMarginalCost(task, timeout);
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
                if(round<=13){
		bid *= 1.3 - 0.6*Math.exp(-0.19 * (round));}
                
		return bid;
	}

	@Override
	protected void printName() {
		println("I am AuctionAgent22");
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
