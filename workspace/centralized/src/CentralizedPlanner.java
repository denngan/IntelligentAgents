/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
*/


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.plan.Action;
import logist.plan.PublicAction.Delivery;
import logist.plan.PublicAction.Pickup;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


/**
 *
 * @author Dennis
 */
public class CentralizedPlanner implements CentralizedBehavior {
    
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private int maxImpSteps;
    private int maxSteps;
    
    List<Vehicle> vehicles;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config\\settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Map<Vehicle,List<PublicAction>> planSLS = SLS( tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");
        
        return plans;
    }

    private Map<Vehicle,List<PublicAction>> SLS(TaskSet tasks) {
        int stepsTotal=0;
        int stepsWithoutImprovement=0;        
        Map<Vehicle,List<PublicAction>> A = selectInitialSolution(tasks);
            do {
               Map<Vehicle,List<PublicAction>> Aold = A;
               Set<Map<Vehicle,List<PublicAction>>> N = chooseNeighbours(Aold, tasks );
               A = localChoice(N);
               stepsTotal++; 
               if (costFunction(A)==costFunction(Aold)){
                   stepsWithoutImprovement++;
               }
               else {
                   stepsWithoutImprovement=0;
               }
               
            } while (stepsTotal<maxSteps && stepsWithoutImprovement<maxImpSteps);
        return A;
    }
    
    
    private Map<Vehicle,List<PublicAction>> selectInitialSolution(TaskSet tasks){
    
        
    }

    private Set<Map<Vehicle, List<PublicAction>>> chooseNeighbours(Map<Vehicle, List<PublicAction>> Aold, TaskSet tasks) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Map<Vehicle, List<PublicAction>> localChoice(Set<Map<Vehicle, List<PublicAction>>> N) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private double costFunction(Map<Vehicle, List<PublicAction>> A) {
        double costs=0;
        
        for (Vehicle vehicle : vehicles ){
            costs+=generatePlanV(A.get(vehicle),vehicle).totalDistance()*vehicle.costPerKm();
        }
        
        
        return costs;
    }
    
    private List<Plan> generatePlan(Map<Vehicle,List<PublicAction>> A){
       
       List<Plan> plans = new LinkedList<>(); 
        
       for ( Vehicle vehicle : vehicles ){
      
        plans.add(generatePlanV(A.get(vehicle),vehicle));   
       } 
        
        return plans;
    }
   
    private Plan generatePlanV(List<PublicAction> actions, Vehicle vehicle){
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        
        for (PublicAction action : actions){
            
            for (City city : current.pathTo(action.moveTo)) {
                plan.appendMove(city);
            }
            
            
            if (action instanceof Pickup) {
                plan.appendPickup(action.task);
                
            } else if (action instanceof Delivery){
                plan.appendDelivery(action.task);
            } 
            
            current=action.moveTo;
            
            
    }
    return plan;
    }
}
