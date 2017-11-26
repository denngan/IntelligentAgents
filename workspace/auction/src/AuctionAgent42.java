import logist.task.Task;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.regression.SimpleRegression;

public class AuctionAgent42 extends AuctionAgent {
	protected String name = "AA42";
	// weights for our Cost
	// case distinction for when the estimated enemy's cost is higher/lower than our cost
	protected double weightLoosing = 0.5;
	protected double weightWinning = 0.5;
	protected List<Long> estimatedEnemysCosts = new ArrayList<>();
        protected SimpleRegression enemyBidModel = new SimpleRegression();
        
        
	@Override
	protected Long computeBid(Task task, long marginalCost, long timeout) {
		long enemiesMarginalCost = (long) enemy.computeMarginalCost(task, timeout);
                //long enemiesBid= (long)(0.5* marginalCost+0.5*enemiesMarginalCost);
                
		

		String msg = "" + marginalCost + " | " + enemiesMarginalCost;
		if (marginalCost > enemiesMarginalCost) {
			msg = msg + " !!!";
		}
		println(msg);


		long bid;
                
                
                bid=bidWithPrediction(marginalCost,enemiesMarginalCost); 
                
                
		

//		if (round == 0) {
//			bid = (long) (task.pickupCity.distanceTo(task.deliveryCity) * vehicles.stream().mapToInt(Vehicle::costPerKm).sum() / vehicles.size());
//		}


		//
                //if(round<=13){
		bid *= 1.4 - 0.6*Math.exp(-0.21 * (round));//}
                
                
                estimatedEnemysCosts.add(enemiesMarginalCost);                
                
		return bid;
	}
        
        @Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		taskHistory.add(lastTask);
		ourBids.add(lastOffers[ourId]);
		enemiesBids.add(lastOffers[enemiesId]);
		winner.add(lastWinner);
                enemyBidModel.addData(estimatedEnemysCosts.get(estimatedEnemysCosts.size()-1), enemiesBids.get(enemiesBids.size()-1));

		if (lastWinner == ourId) {
			println("We won");
			cumulatedCostForAuction += lastOffers[ourId];
			ourWonTasks.add(lastTask);
			currentAssignment = temporaryAssignment;
			temporaryAssignment = null;
		} else {
			println("We lost");
			enemiesWonTasks.add(lastTask);
		}

		if (enemy != null) {
			enemy.auctionResult(lastTask, lastWinner, lastOffers);
			updateStrategy();
		}
	}

	@Override
	protected void printName() {
		println("I am AuctionAgent42");
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
        

        protected Long bidWithPrediction(Long marginalCost,Long enemMargCost){
            long bid;
            long enemBid=enemMargCost;
            
            if (round>=5){
            
                double oldMSE=enemyBidModel.getMeanSquareError();
                double oldMSE2=0;
                
                for (int i=0; i<enemiesBids.size();i++){
                    long x = enemiesBids.get(i)-estimatedEnemysCosts.get(i);
                    oldMSE2 += x*x;
                }
                println(""+oldMSE2);
                println(""+ (oldMSE2/enemiesBids.size()==oldMSE));
                
                double newMSE = 0;
            
                for (int i=0;i<=4;i++){
                    long x = enemiesBids.get(enemiesBids.size()-1-i)-(long)enemyBidModel.predict(estimatedEnemysCosts.get(estimatedEnemysCosts.size()-1-i));
                    newMSE+= x*x;
                }
            
                newMSE=newMSE/5;
                println("oMSE: "+oldMSE);
                println("newMSE: "+newMSE);
                if (oldMSE>newMSE){
                    enemBid=(long)enemyBidModel.predict(enemMargCost);
                    println("predicted: "+enemBid);
                }
                
                //todo change weights when MSE is too big, but how big?
                
            }
            
            
            if (marginalCost > enemBid) {
			bid = (long) (weightLoosing * marginalCost +  (1 - weightLoosing) * enemBid);
		} else {
			bid = (long) (weightWinning * marginalCost +  (1 - weightWinning) * enemBid) - 1;
		}
            
            
            return bid;
            
        }
}

