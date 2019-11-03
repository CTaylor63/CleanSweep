package edu.depaul.cdm;

import java.awt.Point;
import java.util.*;

/**
 * 
 * @author hamood
 * MoveRobot is responsible for all the movement. 
 * MoveRobot class takes in three arguments as follows: 
 * 		1. 2D-Array for the Floor Plan
 * 		2. x - coordinate of a row  
 * 		3. y - coordinate of a column  
 *		4. 2D-Array for the Dirt Level
 * 
 * The Robot Backtracks if it reaches a space where it cannot go further and there are still some units left to be traversed. 
 * The initial coordinates for x and y will be fetched from Locator
 * And after every successful move the Locator would be passed on the current coordinates.
 *   
 */

public class MoveRobot {
	
	static private int[][] floor;
	static private int[][] dirtFloor;
	private static int x;
	private static int y;
	static int colLength;
	static int rowLength;
	static int floorLength;  //The complete number of units for the Floor Plan 
	static Stack<Point> trail = new Stack<Point>(); //Robots Trail
	static HashSet<Point> visited = new HashSet<Point>(); //Visited Floor Units

	public int peek_x;
	public int peek_y;
	public int return_to_charger_counter;

	PowerManagement powerManagement = PowerManagement.getInstance();
	
	public MoveRobot (int[][] floor, int xCord, int yCord){  
		MoveRobot.floor = floor;
		MoveRobot.y = yCord;
		MoveRobot.x = xCord;
		MoveRobot.colLength = floor[0].length;
		MoveRobot.rowLength = floor.length;
		MoveRobot.floorLength = colLength * rowLength; 
		System.out.println("Floor Length: " + floorLength);

		this.peek_x = 0;
		this.peek_y = 0;
		this.return_to_charger_counter = 0;
	}
	
	//Gives the Current Coordinates (might not be called)
	public int[] getCords(){
		int[] cords = {x, y};
		return cords;
	}

	public void set_peek_values(int x_, int y_)
	{
		this.peek_x = x_;
		this.peek_y = y_;
	}

	private static boolean safePath(){	
		//1st Priority to Move rightward
		if (x+1 >=0 && x+1 <colLength && !visited.contains(new Point(x+1, y)))
		{ x++; return true; }
		//Clockwise Priority from here onward: 
		else if (y+1 >=0 && y+1 <rowLength && !visited.contains(new Point(x, y+1))) 
		{ y++; return true; }
		else if (x-1 >=0 && x-1 <colLength && !visited.contains(new Point(x-1, y)))
		{ x--; return true; }
		else if (y-1 >=0 && y-1 <rowLength && !visited.contains(new Point(x, y-1)))
		{ y--; return true; }		
		else{ return false; }
	}

	// To see what next move is
	private boolean peek_safe_path(){
		//1st Priority to Move rightward
		if (peek_x+1 >=0 && peek_x+1 <colLength && !visited.contains(new Point(peek_x+1, peek_y)))
		{ peek_x++; return true;}
		//Clockwise Priority from here onward:
		else if (peek_y+1 >=0 && peek_y+1 <rowLength && !visited.contains(new Point(peek_x, peek_y+1)))
		{ peek_y++; return true;}
		else if (peek_x-1 >=0 && peek_x-1 <colLength && !visited.contains(new Point(peek_x-1, peek_y)))
		{ peek_x--; return true;}
		else if (peek_y-1 >=0 && peek_y-1 <rowLength && !visited.contains(new Point(peek_x, peek_y-1)))
		{ peek_y--; return true;}
		else{return false;}
	}

	private static void backTrack(){
		trail.pop();
		x = trail.peek().x;
		y = trail.peek().y;
		System.out.println("Backtracked to: " + "y= " + y + " x= " + x);
	}
	
	public void move() throws InterruptedException{
		
		Locator locator = new Locator();
		
		dirtFloor = DirtLevel.getDirtLevel(floor);
		DirtLevelSensor dirtSensor = new DirtLevelSensor(dirtFloor);

		ObstacleSensor obsSensor = new ObstacleSensor(floor);

		int current_cell_cost = 0;
		int next_cell_cost = 0;
		int average_move_cost = 0;
		
		while (visited.size() < floorLength)
		{
			// Pass in current cell coords
			powerManagement.set_x_y_coords(x, y);

			//  Check if battery level is sufficient before move is made.
			// At 2,2 so check what units of energy use is.
			current_cell_cost = powerManagement.switch_floor_types(x, y);

			// Check what next move's units of energy use is. It's 3,2
			this.set_peek_values(x, y);
			if(peek_safe_path())
			{
				next_cell_cost = powerManagement.switch_floor_types(this.peek_x, this.peek_y);

				// Since next move is valid take average cost of two moves
				average_move_cost = powerManagement.average_cost(current_cell_cost, next_cell_cost);

				this.return_to_charger_counter += average_move_cost;
			}

			// Check that battery has enough power for next move.
			// Need to consider what battery amount is needed for return to
			// charger if power level goes below this point since it will
			// have to head back to charger.



			if (safePath()){

				System.out.println("Visited Y&X Cords: " + y + " | "+ x);
				visited.add(new Point(x, y));
				if ((obsSensor.checkObstacle(x, y)) == true){
					locator.setX(x);
					locator.setY(y);
					trail.push(new Point(x, y));
					
					if (dirtSensor.checkDirtLevel(x, y) == true)
					{
						System.out.println("Cleaning: Y&X " + y + " | " + x);
						Thread.sleep(1000); //1 second delay
						System.out.println();
					}
				}	
			}
			else {
				backTrack(); //pops the last element and assigns the last coordinates to x and y
			}			
		}
	}
}
