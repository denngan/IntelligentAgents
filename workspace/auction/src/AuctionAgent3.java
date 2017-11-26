import logist.task.Task;

public class AuctionAgent3 extends AuctionAgent {
	@Override
	protected Long computeBid(Task task, long marginalCost, long timeout) {
		marginalCost += 100;
		double factor;
		long bid;
		if (round < 5) {
			factor = 0.8;
			bid = (long) (factor * marginalCost);
		} else {
			factor = 1.2;
			bid = (long) (factor * (marginalCost));
		}

		return (long) (factor * marginalCost);
	}

	protected void printName() {
		println("I am AuctionAgent3");
	}
}
