import logist.task.Task;

import java.util.ArrayList;
import java.util.List;

public class AuctionAgent2 extends AuctionAgent {
	protected String name = "AA2";
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

		return bid;
	}

	@Override
	protected void printName() {
		println("I am AuctionAgent2");
	}

	@Override
	protected void updateStrategy() {
		if (ourBids.get(round) < enemiesBids.get(round)) {
			weightWinning += (estimatedEnemysCosts.get(round) - enemiesBids.get(round)) / Math.max(estimatedEnemysCosts.get(round), enemiesBids.get(round)) * (1 - weightWinning) / (round + 1);
		} else {

		}
	}
}
