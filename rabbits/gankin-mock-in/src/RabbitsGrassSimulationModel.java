import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;

import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.OpenHistogram;

import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;


/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	public static void main(String[] args) {

		System.out.println("Rabbit start");
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		init.loadModel(model, "", false);
	}


	private OpenSequenceGraph amountGrass;
	private OpenHistogram energyDistr;


	public Schedule schedule;

	private Space rabbitSpace;

	private ArrayList rabbitList;

	private DisplaySurface displSurf;

	private int numRabbits = 20;
	private int worldXSize = 20;
	private int worldYSize = 20;
	private int grass = 42;
	private int grassGrowRate = 15; // New grass per tick
	private int birthThreshold = 15;
	private int grassEnergy = 4;
	private int maxStartEnergy = 12;//energy rabbits are born with
	private int minStartEnergy = 8;

	/**
	 * @return the birthThreshold
	 */
	public int getBirthThreshold() {
		return birthThreshold;
	}

	/**
	 * @param birthThreshold the birthThreshold to set
	 */
	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}


	//Graphs-------
	class grassInSpace implements DataSource, Sequence {
		public Object execute() {
			return getSValue();
		}

		public double getSValue() {
			return (double) rabbitSpace.getTotalGrass();
		}

	}

	class rabbitsInSpace implements DataSource, Sequence {
		public Object execute() {
			return getSValue();
		}

		public double getSValue() {
                 	return (double) rabbitSpace.getTotalRabbits();
		}

	}

	class rabbitEnergy implements BinDataSource {
		public double getBinValue(Object o) {
			Rabbit r = (Rabbit) o;
			return (double) r.getEnergy();
		}
	}

	//------------------------------------

	@Override
	public void begin() {
		// TODO Auto-generated method stub
		buildModel();
		buildSchedule();
		buildDisplay();

		displSurf.display();
		amountGrass.display(); //Graphs
		energyDistr.display();
	}

	@Override
	public String[] getInitParam() {
		// TODO Auto-generated method stub
		String[] initParams = {"NumRabbits", "WorldXSize", "WorldYSize", "Money", "grassGrowRate", "grassEnergy", "grass", "birthThreshold"};
		return initParams;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "RabitsGrassSimulationModel";
	}

	@Override
	public Schedule getSchedule() {
		// TODO Auto-generated method stub
		return schedule;
	}

	@Override
	public void setup() {
		// TODO Auto-generated method stub
		System.out.println("setup");
		rabbitSpace = null;
		rabbitList = new ArrayList();

		schedule = new Schedule(1);

		if (displSurf != null) {
			displSurf.dispose();
		}
		displSurf = null;

		if (amountGrass != null) {
			amountGrass.dispose();
		}
		amountGrass = null;

		if (energyDistr != null) {
			energyDistr.dispose();
		}
		energyDistr = null;


		displSurf = new DisplaySurface(this, "RabbitsGrassModel Window 1");
		amountGrass = new OpenSequenceGraph("Amount of Grass in Space", this);
		energyDistr = new OpenHistogram("Rabbit Energy", 8, 0);


		registerDisplaySurface("RabbitsGrassModel Window 1", displSurf);
		this.registerMediaProducer("Plot", amountGrass);
	}

	public void buildModel() {
		System.out.println("build Model");
		rabbitSpace = new Space(worldXSize, worldYSize, grassEnergy);
		rabbitSpace.growGrass(grass);

		for (int i = 0; i < numRabbits; i++) {
			addNewRabbit();
		}

		for (int i = 0; i < rabbitList.size(); i++) {
			Rabbit r = (Rabbit) rabbitList.get(i);
			r.report();
		}

	}

	public void buildSchedule() {
		System.out.println("build Schedule");

		class CarryDropStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(rabbitList);
				for (int i = 0; i < rabbitList.size(); i++) {
					Rabbit r = (Rabbit) rabbitList.get(i);
					r.step();
				}

				int deadRabbits = reapDeadRabbits();
				for (int i = 0; i < deadRabbits; i++) {
					//addNewRabbit();
				}


				displSurf.updateDisplay();
			}
		}

		schedule.scheduleActionBeginning(0, new CarryDropStep());

		class CarryDropCountLiving extends BasicAction {
			public void execute() {
				countLivingRabbits();
			}
		}
		class CarryDropCountBirth extends BasicAction {
			public void execute() {
				System.out.println(giveBirth() + " rabbits born");   //creating new rabbits
			}
		}

		class CarryDropCountGrass extends BasicAction {
			private int grassAmount = 1;

			public CarryDropCountGrass(int grassAmount) {
				this.grassAmount = grassAmount;
			}

			public void execute() {
				rabbitSpace.growGrass(grassAmount);   //creating new grass 
			}
		}

		class CarryDropPlotGrass extends BasicAction {
			public void execute() {
				amountGrass.step(); //grass and rabbit graph
			}
		}

		class CarryDropUpdateEnerDistr extends BasicAction {
			public void execute() {   //energy distribution graph
				if (countLivingRabbits() > 0) {
					energyDistr.step();
				} else {
					energyDistr.dispose();
				}
			}
		}


		schedule.scheduleActionAtInterval(10, new CarryDropPlotGrass());

		schedule.scheduleActionAtInterval(10, new CarryDropUpdateEnerDistr());

		schedule.scheduleActionAtInterval(10, new CarryDropCountLiving());
		schedule.scheduleActionAtInterval(5, new CarryDropCountBirth());
		schedule.scheduleActionAtInterval(1, new CarryDropCountGrass(grassGrowRate)); // grow some grass per tick 


	}

	public void buildDisplay() {
		System.out.println("build Display");

		ColorMap map = new ColorMap();

		map.mapColor(0, Color.black); //background
                
                //actually unecessary, only i=1 is enough, bc grass only has value 1
		/*for (int i = 1; i < 16; i++) {
			map.mapColor(i, Color.green);
		}*/
                //make grass green 
                map.mapColor(1,Color.green);


		Value2DDisplay displayGrass = new Value2DDisplay(rabbitSpace.getCurrentGrassSpace(), map);

		Object2DDisplay displayRabbits = new Object2DDisplay(rabbitSpace.getCurrentRabbitSpace());
		displayRabbits.setObjectList(rabbitList);

		displSurf.addDisplayable(displayGrass, "Grass");
		displSurf.addDisplayable(displayRabbits, "Rabbits");

		amountGrass.addSequence("Amount of Grass", new grassInSpace());
		amountGrass.addSequence("Amount of Rabbits", new rabbitsInSpace());
		energyDistr.createHistogramItem("Rabbit Energy", rabbitList, new rabbitEnergy());
	}

	public void addNewRabbit() {
		Rabbit a = new Rabbit(getBirthThreshold(), grassEnergy, getMinStartEnergy(), getMaxStartEnergy());

		if (rabbitSpace.addRabbit(a)) {
			rabbitList.add(a);
		}
	}

	private int reapDeadRabbits() {
		int count = 0;
		for (int i = (rabbitList.size() - 1); i >= 0; i--) {
			Rabbit r = (Rabbit) rabbitList.get(i);
			if (r.getEnergy() < 1) {
				rabbitSpace.removeRabbitAt(r.getX(), r.getY());
				rabbitList.remove(i);
				count++;
			}
		}
		System.out.println("removed " + count);
		return count++;
	}

	private int giveBirth() {
		int addRabbits = 0;
		for (int i = 0; i < rabbitList.size(); i++) {
			Rabbit r = (Rabbit) rabbitList.get(i);
			if (r.getEnergy() > getBirthThreshold()) {
				addRabbits++;
				r.setEnergy(minStartEnergy);
			}
		}
		for (int i = 0; i < addRabbits; i++) {
			Rabbit r = (Rabbit) rabbitList.get(i);
			addNewRabbit();
			r.giveBirth();
		}

		return addRabbits;
	}

	private int countLivingRabbits() {
		int livingRabbits = 0;
		for (int i = 0; i < rabbitList.size(); i++) {
			Rabbit r = (Rabbit) rabbitList.get(i);
			if (r.getEnergy() > 0) {
				livingRabbits++;
			}
		}
		System.out.println("livng rabbits: " + livingRabbits);
		return livingRabbits;
	}

	/**
	 * @return the numAgents
	 */
	public int getNumRabbits() {
		return numRabbits;
	}

	/**
	 * @param numRabbits the numAgents to set
	 */
	public void setNumRabbits(int numRabbits) {
		this.numRabbits = numRabbits;
	}

	/**
	 * @return the worldXSize
	 */
	public int getWorldXSize() {
		return worldXSize;
	}

	/**
	 * @param worldXSize the worldXSize to set
	 */
	public void setWorldXSize(int worldXSize) {
		this.worldXSize = worldXSize;
	}

	/**
	 * @return the worldYSize
	 */
	public int getWorldYSize() {
		return worldYSize;
	}

	/**
	 * @param worldYSize the worldYSize to set
	 */
	public void setWorldYSize(int worldYSize) {
		this.worldYSize = worldYSize;
	}

	/**
	 * @return the grass
	 */
	public int getGrass() {
		return grass;
	}

	/**
	 * @param grass the grass to set
	 */
	public void setGrass(int grass) {
		this.grass = grass;
	}

	/**
	 * @return the grassGrowRate
	 */
	public int getGrassGrowRate() {
		return grassGrowRate;
	}

	/**
	 * @param grassGrowRate the grassGrowRate to set
	 */
	public void setGrassGrowRate(int grassGrowRate) {
		this.grassGrowRate = grassGrowRate;
	}

	/**
	 * @return the birthTreshold
	 */
	public int getBirthTreshold() {
		return getBirthThreshold();
	}

	/**
	 * @param birthTreshold the birthTreshold to set
	 */
	public void setBirthTreshold(int birthTreshold) {
		this.setBirthThreshold(birthTreshold);
	}

	/**
	 * @return the grassEnergy
	 */
	public int getGrassEnergy() {
		return grassEnergy;
	}

	/**
	 * @param grassEnergy the grassEnergy to set
	 */
	public void setGrassEnergy(int grassEnergy) {
		this.grassEnergy = grassEnergy;
	}

	/**
	 * @return the maxStartEnergy
	 */
	public int getMaxStartEnergy() {
		return maxStartEnergy;
	}

	/**
	 * @param maxStartEnergy the maxStartEnergy to set
	 */
	public void setMaxStartEnergy(int maxStartEnergy) {
		this.maxStartEnergy = maxStartEnergy;
	}

	/**
	 * @return the minStartEnergy
	 */
	public int getMinStartEnergy() {
		return minStartEnergy;
	}

	/**
	 * @param minStartEnergy the minStartEnergy to set
	 */
	public void setMinStartEnergy(int minStartEnergy) {
		this.minStartEnergy = minStartEnergy;
	}


}
