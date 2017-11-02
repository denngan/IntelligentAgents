
import logist.task.Task;
import logist.topology.Topology.City;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Dennis
 */
public class PublicAction {
    Task task;
    
    ActionType actionType; 
    
    public enum ActionType{
        PICKUP,DELIVERY
    }
    
    City moveTo;
    
    
    public PublicAction (Task task, ActionType actionType){
        this.task=task;
        this.actionType=actionType;
        if (actionType==ActionType.PICKUP){
            moveTo=task.pickupCity;       
        }    else {
            moveTo=task.deliveryCity;
        }
        
    }
    
}