/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;



/**
 *
 * @author Dennis
 */
public class Rabbit implements Drawable {
    private int x;
    private int y;
    private int vX;
    private int vY;
    private Space space;
    private int energy; 
    private int grassEnergy;
    private int birthThreshold;
    private static int IDNumber=0;
    private int ID;
    private int minEng;
    private int maxEng;
    //rabbits in total as static var?
    
    public Rabbit(int bThreshold,int gEnergy, int maxStartEnergy, int minStartEnergy){
        x=-1;
        y=-1;
        energy= (int) ((Math.random() *(maxStartEnergy-minStartEnergy))+minStartEnergy);
        birthThreshold=bThreshold;
        grassEnergy=gEnergy;
        IDNumber++;
        ID=IDNumber;
        minEng=minStartEnergy;
        maxEng=maxStartEnergy;
    }
    
    public void setXY(int x, int y){
        this.x=x;
        this.y=y;
    }
    
    public void setVXVY(){
        vX=0;
        vY=0;
        while ((vX==0)&&(vY==0)){
            vX=(int)Math.floor(Math.random()*3)-1;
            vY=(int)Math.floor(Math.random()*3)-1;
        }
    }

    /**
     * @return the energy
     */
    public int getEnergy() {
        return energy;
    }

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	/**
     * @return the ID
     */
    public String getID() {
        return "R-"+ID;
    }
    
    public void report(){
        System.out.println(getID()+" at "+x+", "+y+" has "+getEnergy()+" energy points left.");
    }
    
    public int getX(){
        return x;
    }
    
    public int getY(){
        return y;
    }
    
    public void draw(SimGraphics G){
        if(energy<5){
            G.drawFastRoundRect(Color.red);
        }
        else{
        G.drawFastRoundRect(Color.white);
        }
    }
    
    public void step(){
        int newX=x+vX;
        int newY= y+vY;
        
        Object2DGrid grid = space.getCurrentRabbitSpace();
        newX=(newX+grid.getSizeX())%grid.getSizeX();
        newY=(newY+grid.getSizeY())%grid.getSizeY();
        
        if(tryMove(newX,newY)){
        energy+=space.takeGrassAt(x,y); // move if no rabbit there 
        }
        else{
            setVXVY();
        }
        energy--;
    }
    
    private boolean tryMove(int newX,int newY){
        return space.moveRabbitAt(x,y,newX,newY); 
    }
    
    public void giveBirth(){
        energy= (int) ((Math.random() *(maxEng-minEng))+minEng); //set energy back to birth energy
    }

    /**
     * @param space the space to set
     */
    public void setSpace(Space space) {
        this.space = space;
    }
   
}
