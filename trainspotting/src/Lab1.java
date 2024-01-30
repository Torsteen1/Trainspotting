import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1{
	private Semaphore plusJunctionAvailable = new Semaphore(1);
	private Semaphore topBottomRailAvailable = new Semaphore(1);
	private Semaphore bottomTopRailAvailable = new Semaphore(0);
	private Semaphore topTurnCrossingAvailable = new Semaphore(1);
	private Semaphore bottomTurnCrossingAvailable = new Semaphore(1);
	private Semaphore topDoubleRailAvailable = new Semaphore(1);

	public Lab1(int speed1, int speed2) {
		Thread train1 = new Thread(new Train(1, speed1, this, Direction.DOWN));
		Thread train2 = new Thread(new Train(2, speed2, this, Direction.UP));
		
		train1.start();
		train2.start();
	}
	
	public Semaphore getPlusJunctionAvailable() {
		return plusJunctionAvailable;
	}
	
	public Semaphore getTopTurnCrossingAvailable() {
		return topTurnCrossingAvailable;
	}
	
	public Semaphore getTopBottomRailAvailable() {
		return topBottomRailAvailable;
	}
	
	public Semaphore getBottomTurnCrossingAvailable() {
		return bottomTurnCrossingAvailable;
	}
	
	public Semaphore getBottomTopRailAvailable() {
		return bottomTopRailAvailable;
	}
	
	public Semaphore getTopDoubleRailAvailable() {
		return topDoubleRailAvailable;
	}
}

final class Train implements Runnable{
	TSimInterface tsi = TSimInterface.getInstance();
	private int id; //Train's id
	private Lab1 lab1; //lab1 in order to access semaphores
	private int speed; //The speed of the train
	private boolean inCrossing = false; //Boolean for whether the train is in a crossing or not
	private Direction direction; //The direction the train is moving in: up or down
	
	//Coordinates for the switches. These are predefined due to the map
	private final int TRSwitchX = 17;
	private final int TRSwitchY = 7;
	
	private final int BLSwitchX = 3;
	private final int BLSwitchY = 11;
	
	private final int DRRSwitchX = 15;
	private final int DRRSwitchY = 9;
	private final int DRLSwitchX = 4;
	private final int DRLSwitchY = 9;
	
	public Train(int id, int speed, Lab1 lab1, Direction direction) {
		try {
			this.id = id;
			this.lab1 = lab1;
			this.speed = speed;
			this.direction = direction;
			tsi.setSpeed(id, speed);
			
			
		} catch (CommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(true) {
			try {
				SensorEvent event = tsi.getSensor(id);
				if(event.getStatus()== 0x02) continue; //If sensor inactive continue
				
				int posX = event.getXpos(); //Get the coordinates of the sensor in order to determine which sensor is activated
				int posY = event.getYpos();
				
				if((posX >= 6 && posX <=10) && (posY>=5 && posY <= 8)) { //The Plus Junction
					
					//If the train has already passed the junction: release the semaphore
					if(inCrossing) {
						lab1.getPlusJunctionAvailable().release();
						inCrossing = false;
					}else {
					//Otherwise bring the train to a stop while waiting for the junction to be clear
					//Once the junction is clear(or it was clear to begin with), start the train again
						tsi.setSpeed(id, 0);
						lab1.getPlusJunctionAvailable().acquire();
						tsi.setSpeed(id, speed);
						inCrossing = true;
					}
					
				}else if((posX>=14 && posX<=19) && (posY >= 7 && posY <= 8)){ //Top turn junction					
					tJunction(posX, posY, lab1.getTopTurnCrossingAvailable(), 15, 8, 14, 7, TRSwitchX, TRSwitchY, lab1.getTopBottomRailAvailable());
					
				}else if((posX>=3 && posX<=6) && (posY<=13 && posY>=11)){ //Bottom turn junction
					tJunction(posX, posY, lab1.getBottomTurnCrossingAvailable(), 6, 11, 3, 13, BLSwitchX, BLSwitchY, lab1.getBottomTopRailAvailable());
					
				}else if((posX==7 || posX==12) && (posY==9 || posY==10)){ //Inside the double rail section
					//First, if the train is coming into the double rail section, we want to release the semaphore for the crossings they come from
					//The way we determine if the train is coming in or out is with the direction enum defined below
					if(posX==7 && direction==Direction.UP) {
						lab1.getBottomTurnCrossingAvailable().release();
						continue;
					}
					if(posX==12 && direction==Direction.DOWN) {
						lab1.getTopTurnCrossingAvailable().release();
						continue;
					}
					
					//If the train is leaving the section, we bring the train to a stop and wait for the section to be cleared
					//we then set the switches to the correct settings and start the train again
					//If the train has used the upper rail, we release that semaphore to allow other trains to use it
					tsi.setSpeed(id, 0);
					if(posX==7) {
						lab1.getBottomTurnCrossingAvailable().acquire();
						
						if(posY==9) {
							tsi.setSwitch(DRLSwitchX, DRLSwitchY,0x01);
							lab1.getTopDoubleRailAvailable().release();
						}else if(posY==10) {
							tsi.setSwitch(DRLSwitchX, DRLSwitchY,0x02);
						}
					}
					
					if(posX==12) {
						lab1.getTopTurnCrossingAvailable().acquire();
						
						if(posY==9) {
							tsi.setSwitch(DRRSwitchX,DRRSwitchY,0x02);
							lab1.getTopDoubleRailAvailable().release();
						}else if(posY==10) {
							tsi.setSwitch(DRRSwitchX,DRRSwitchY,0x01);
						}
					}
					tsi.setSpeed(id, speed);
					inCrossing = true;
					
										
				}else if(posY == 9 && (posX == 3 || posX == 16)) { //Sensors just outside the double rail section
					//When coming into the double rail section we must determine which rail to take
					//The switches are then changed to represent this decision
					
					//When leaving the double rail section, we must determine which rail to take in the next crossing
					//Similarly to above, the switches are then changed to represent this
					
					if(posX == 3 && direction == Direction.UP) { //Entering from the left
						doubleRailOutside(lab1.getTopDoubleRailAvailable(), DRLSwitchX, DRLSwitchY, 0x01, 0x02);
					}else if(posX == 16 && direction == Direction.DOWN) { //Entering from the right
						doubleRailOutside(lab1.getTopDoubleRailAvailable(), DRRSwitchX, DRRSwitchY, 0x02, 0x01);
					}else if(posX == 3 && direction == Direction.DOWN) { //Leaving to the left
						doubleRailOutside(lab1.getBottomTopRailAvailable(), BLSwitchX, BLSwitchY, 0x01, 0x02);
					}else if(posX == 16 && direction == Direction.UP) { //Leaving to the right
						doubleRailOutside(lab1.getTopBottomRailAvailable(), TRSwitchX, TRSwitchY, 0x01, 0x02);
					}
					
				}else if(posX==14 && 	((posY == 13 || posY == 11) && direction==Direction.DOWN) || 
										((posY == 5 || posY == 3) && direction==Direction.UP)) { //Stations
					//When the train arrives at the station, we bring the train to a stop
					//Once the train has stayed there a couple of seconds, we make the train go the other direction
					//The direction variable is updated to represent this
					tsi.setSpeed(id, 0);
					Thread.sleep(1500 + (speed * 20));
					speed = -speed;
					tsi.setSpeed(id, speed);
					if(direction == Direction.UP) {
						direction = Direction.DOWN;
					}else{
						direction = Direction.UP;
					}
				}
				
				
				
			} catch (CommandException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * posX : The X-coordinate for the activated sensor
	 * posY : The Y-coordinate for the activated sensor
	 * crossing : The semaphore determining whether the crossing is available or not
	 * sensLeftX : The X-coordinate for the sensor needing the switch to be set to the left
	 * sensLeftY : The Y-coordinate for the sensor needing the switch to be set to the left
	 * sensRightX : The X-coordinate for the sensor needing the switch to be set to the right
	 * sensRightY : The Y-coordinate for the sensor needing the switch to be set to the right
	 * switchX : The X-coordinate of the switch
	 * swithcY : The Y-coordinate of the switch
	 * defaultRail : The rail designated to as the default for that section of the map
	 */
	private void tJunction(int posX, int posY, Semaphore crossing, 
							int sensLeftX, int sensLeftY, int sensRightX, int sensRightY, 
							int switchX, int switchY, Semaphore defaultRail) {
		try {
			//If leaving the crossing, we release the semaphore and change the boolean to false
			if(inCrossing) {
				crossing.release();
				inCrossing = false;
			}else { //If entering, we need to stop the train to make sure the crossing is clear
				tsi.setSpeed(id, 0);
				crossing.acquire();
				tsi.setSpeed(id, speed); //Once the crossing is clear the train can move again, and then the switches are set to the right settings
				inCrossing = true;
				
				if(posX == sensRightX && posY == sensRightY) {
					tsi.setSwitch(switchX, switchY, 0x02);
				}else if(posX == sensLeftX && posY == sensLeftY){
					tsi.setSwitch(switchX, switchY, 0x01);
					defaultRail.release(); //If the train is leaving the default rail, the semaphore must be released
				}
			}
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * desiredRail : This is the rail that the train would prefer to go on
	 * switchX : The X-coordinate for the switch
	 * switchY : The Y-corrdinate for the switch
	 * desiredTurn : The setting for the switch which takes the train down the desired rail
	 * otherTurn : The setting for the switch which takes the train down the undesired rail
	 */
	private void doubleRailOutside(Semaphore desiredRail, int switchX, int switchY, int desiredTurn, int otherTurn) {
		try {
			if(desiredRail.availablePermits()==1) {
				tsi.setSwitch(switchX, switchY, desiredTurn);
				desiredRail.acquire();
			}else {
				tsi.setSwitch(switchX, switchY, otherTurn);
			}
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

//This is an enum which helps us determine which way the train is going at the moment
//This helps us decide what the sensors should do in that moment
enum Direction{
	UP,
	DOWN
}