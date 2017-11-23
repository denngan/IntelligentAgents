import logist.task.Task;

public class AuctionAgent2 extends AuctionAgent {
	protected String name = "AA2";
	@Override
	protected Long computeBid(Task task) {
		double marginalCost = computeMarginalCost(task, ourWonTasks, timeOutBid);
		double enemiesMarginalCost = enemy.computeMarginalCost(task, enemy.ourWonTasks, timeOutBid);
		println("estimate enemy bid: " + enemiesMarginalCost);
		long bid = (long) ((marginalCost + enemiesMarginalCost) / 2) - 1;
		return bid;
	}

	protected void printName() {
		println("I am AuctionAgent2");
	}
}
