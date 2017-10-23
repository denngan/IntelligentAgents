

import java.util.LinkedList;
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
public class NodeB implements Comparable<NodeB> {
    
    private State state;
    private double costs;
    private LinkedList<City> path;

    @Override
    public int compareTo(NodeB n){
        
            if (this.getCosts()<n.getCosts()){
                return -1;
            }
            else if (this.getCosts()>n.getCosts()){
                return 1;
            }
            else{
                return 0;
            }
            
        
        }
    
    
    public NodeB(State state, double costs, LinkedList<City> path){
        this.state=state;
        this.costs=costs;
        this.path=path;
    }
    
    
    /**
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * @return the costs
     */
    public double getCosts() {
        return costs;
    }

    /**
     * @param costs the costs to set
     */
    public void setCosts(int costs) {
        this.costs = costs;
    }

    /**
     * @return the path
     */
    public LinkedList<City> getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(LinkedList<City> path) {
        this.path = path;
    }
    
    
    
    
}
