import logist.task.Task;

public class BalancedCopyCat extends AuctionAgent{
	private long lastEnemyBid;

	@Override
	protected void updateStrategy() {
		lastEnemyBid = enemiesBids.get(round);
	}

	@Override
	protected Long computeBid(Task task, long marginalCost) {
		if (round == 0) {
			return marginalCost;
		} else {
			return (long) (0.5 * marginalCost + 0.5 * lastEnemyBid);
		}
	}

	@Override
	protected void printName() {
		println("I am Balanced Copy Cat");
	}
}
