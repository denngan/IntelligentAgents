import logist.task.Task;

public class AdvancedCopyCat extends BalancedCopyCat {
	private long avgEnemyBid;
	@Override
	protected void printName() {
		println("Advanced Copy Cat");
	}

	@Override
	protected Long computeBid(Task task, long marginalCost) {
		//double enemiesCost = enemy.computeMarginalCost(task, timeOutBid);

		double bid = 0.5 * marginalCost + 0.25 * avgEnemyBid + 0.25 * enemy.computeMarginalCost(task, timeOutBid);
		if (round == 0) {
			bid = marginalCost;
		}
		bid *= 1.3 - 0.6*Math.exp(-0.19 * (round));
		return (long) bid;
	}

	@Override
	protected void updateStrategy() {
		avgEnemyBid += enemiesBids.get(round);
		if (round != 0) {
			avgEnemyBid /= 2;
		}
	}
}
