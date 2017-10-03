/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import uchicago.src.sim.space.Object2DGrid;

/**
 *
 * @author Dennis
 */
public class Space {
    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;
    private int grassEnergy;
    
    public Space(int xSize,int ySize,int gEnergy){
        grassSpace = new Object2DGrid(xSize,ySize);
        rabbitSpace = new Object2DGrid(xSize,ySize);
        grassEnergy=gEnergy;
        
        for(int i=0;i<xSize;i++){
            for(int j=0; j<ySize;j++){
                grassSpace.putObjectAt(i, j, new Integer(0));
            }
        }
    }
    
    //let the grass grow #420 
    public boolean growGrass(int grass){
        int setGrass=0;
        for (int i = 0; i< grass; i++){
            //choose coordinates randomly
            //int x = (int)(Math.random()*(grassSpace.getSizeX()));
            //int y = (int)(Math.random()*(grassSpace.getSizeY()));
            
            //get Value at coordinates
            //int currentValue = getGrassAt(x,y);
            //replace integer Object with new Value
            //grassSpace.putObjectAt(x, y, new Integer(currentValue+1));
            
        
        
        
        boolean retValue=false;
        int count = 0;
        int countLimit = 10 * grassSpace.getSizeX() * grassSpace.getSizeY();
        
        while((retValue==false)&&(count<countLimit)){
            int x =(int)(Math.random()*(grassSpace.getSizeX()));
            int y =(int)(Math.random()*(grassSpace.getSizeY()));
            
            if (getGrassAt(x,y)==0){
                grassSpace.putObjectAt(x, y, new Integer(1));
                setGrass++;
                retValue=true;
            }
            count++;
        }
        }
        
        if (setGrass>1){
            System.out.println("grew grass: "+setGrass);
        }
        return (setGrass==grass);
    
        
               
    }
    
    public int getGrassCount(int x, int y){
        int i;
            if (grassSpace.getObjectAt(x,y)!=null){
                i=(((Integer)grassSpace.getObjectAt(x, y)).intValue()); 
            }
            else{
               i=0;
            }
        return i;
    }
    
    
    public int getGrassAt(int x, int y){
       int i;
            if (grassSpace.getObjectAt(x,y)!=null){
                i=(((Integer)grassSpace.getObjectAt(x, y)).intValue())*grassEnergy; 
            }
            else{
               i=0;
            }
        return i;
    }
   
    
    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }
    
    public Object2DGrid getCurrentRabbitSpace(){
        return rabbitSpace;
    }
    
    public boolean isCellOccupied(int x, int y){
        return (rabbitSpace.getObjectAt(x, y)!=null);
    }
    
    public boolean addRabbit(Rabbit rabbit){ //adding randomly
        boolean retValue = false;
        int count = 0;
        int countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();
        
        while((retValue==false)&&(count<countLimit)){
            int x =(int)(Math.random()*(rabbitSpace.getSizeX()));
            int y =(int)(Math.random()*(rabbitSpace.getSizeY()));
            
            if (isCellOccupied(x,y)==false){
                rabbitSpace.putObjectAt(x, y, rabbit);
                rabbit.setXY(x,y);
                rabbit.setSpace(this);
                retValue=true;
            }
            count++;
        }        
        
        return retValue;
    }
    
    public void removeRabbitAt(int x, int y){
        rabbitSpace.putObjectAt(x, y, null);
    }
    
    public int takeGrassAt(int x,int y ){
        int grass=getGrassAt(x,y);
        grassSpace.putObjectAt(x, y, new Integer(0));
        return grass;
    }
    
    public boolean moveRabbitAt(int x,int y, int newX, int newY){ // move rabbit if cell is not occupied
        boolean retVal =false;
        if(!isCellOccupied(newX,newY)){
            Rabbit r=(Rabbit)rabbitSpace.getObjectAt(x, y);
            removeRabbitAt(x,y);
            r.setXY(newX, newY);
            rabbitSpace.putObjectAt(newX, newY, r);
            retVal=true;
        }
        return retVal;
    }
    
    public int getTotalGrass(){
        int forTwenty=0;
        for (int i=0; i<rabbitSpace.getSizeX();i++){
            for (int j=0; j<rabbitSpace.getSizeY();j++){
                forTwenty+=getGrassCount(i,j);
            }
        }
        return forTwenty;
    }
    
     public int getTotalRabbits(){
        int carrotEaters=0;
        for (int i=0; i<rabbitSpace.getSizeX();i++){
            for (int j=0; j<rabbitSpace.getSizeY();j++){
                if(isCellOccupied(i,j)){
                  carrotEaters++;  
                }
            }
        }
        return carrotEaters;
    }
    
    
}
