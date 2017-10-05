package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
        
        
                //State class
         class State {
             public City currentCity;
             public City taskCity;

             public State(City currentCity, City taskCity){

                 this.currentCity=currentCity;
                 this.taskCity=taskCity;
             }

         }
        
        //create list with all states
        List<State> stateList = new ArrayList<State>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
                
                

                
                //genereating S
                for(City i: topology.cities()){
                    for(City j: topology.cities()){
                        if (!i.equals(j)){
                            stateList.add(new State(i,j));   
                        }
                    }
                    stateList.add(new State(i,null));
                }
                
                
                Map<State,Double> v = new HashMap<State,Double>();
                
                while(true){
                   for(State s: stateList){
                       Map<City,Double> q = new HashMap<City,Double>();
                       
                       
                      // For all actions
                       //Move to c without package
                       for(City c: topology.cities()){
                           if(!c.equals(s.currentCity)){
                               double value= discount*infiniteHorizzzon(v,td,c,topology);
                               //if there is a task to the city moving to we always take the task, because there is no negative rewards(probably)
                               if(c == s.taskCity){
                                   value+=td.reward(s.currentCity, c);
                               }
                               q.put(c,value);
                           }
                       }
                       //calculate max
                       v.put(s, Collections.max(q.values()));
                   }
                }
                
                
                //call method for calculating V
                //we get taskdistribution-- that gives us 
                //td.probability(null, null)
                //td.reward(null, null)
                //td.weight(null, null) is that the cost?
                
	}
        
        private double infiniteHorizzzon(Map<State,Double> v,TaskDistribution td,City c,Topology topology){
            double returnv = 0;
            
            for(State s: stateList){
                if(s.currentCity==c){
                    returnv+=td.probability(c, s.taskCity)*v.get(s);
                }
                   
            }
            
            
            return returnv;
        }

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));   /// change that, one random one not random
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
        
        
}
