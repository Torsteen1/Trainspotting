import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1{
	private Semaphore plusJunctionAvailable = new Semaphore(1);
	private Semaphore topRailAvailable = new Semaphore(1);
	private Semaphore bottomRailAvailable = new Semaphore(1);
	private Semaphore topDoubleRailAvailable = new Semaphore(1);
	private Semaphore topTurnCrossingAvailable = new Semaphore(1);
	private Semaphore bottomTurnCrossingAvailable = new Semaphore(1);

	public Lab1(int speed1, int speed2) {
		Thread train1 = new Thread(new Train(1, speed1, this));
		Thread train2 = new Thread(new Train(2, speed2, this));
		
		train1.start();
		train2.start();
	}
	
	public Semaphore getPlusJunctionAvailable() {
		return plusJunctionAvailable;
	}
	
	public Semaphore getTopTurnCrossingAvailable() {
		return topTurnCrossingAvailable;
	}
	
	public Semaphore getTopRailAvailable() {
		return topRailAvailable;
	}
	
	public Semaphore getBottomRailAvailable() {
		return bottomTurnCrossingAvailable;
	}
}

final class Train implements Runnable{
	TSimInterface tsi = TSimInterface.getInstance();
	private int id;
	private Lab1 lab1;
	private int speed;
	private boolean inPlusCrossing = false;
	private boolean inTurnCrossing = false;
	
	public Train(int id, int speed, Lab1 lab1) {
		try {
			this.id = id;
			this.lab1 = lab1;
			this.speed = speed;
			tsi.setSpeed(id, speed);
			
			
		} catch (CommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		//TODO put in boolean for if it is the first round or not
		while(true) {
			try {
				SensorEvent event = tsi.getSensor(id);
				if(event.getStatus()== 0x02) continue; //If sensor inactive continue
				int posX = event.getXpos();
				int posY = event.getYpos();
				if(((posX + 2 >= 8 && posX<8) || (posX - 2 <= 8 && posX>8)) && ((posY + 2 >= 7 && posY<7) || (posY - 2 <= 7 && posY>7))) { //The Plus Junction
					if(inPlusCrossing) {
						lab1.getPlusJunctionAvailable().release();
						inPlusCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getPlusJunctionAvailable().acquire();
						tsi.setSpeed(id, speed);
						inPlusCrossing = true;
					}
					
				}else if((posX - 2 <= 17 && posX>17) && (posY + 2 >= 7 && posY<7)){
					if(inTurnCrossing) {
						lab1.getTopTurnCrossingAvailable().release();
						inTurnCrossing = false;
					}else {
						if(posX == 16 && posY == 9) continue;
						tsi.setSpeed(id, 0);
						lab1.getTopTurnCrossingAvailable().acquire();
						tsi.setSpeed(id, speed);
						inTurnCrossing = true;
						if(posX == 15 && posY == 7) {
							tsi.setSwitch(17, 7, 0x01);//Unsure if correct direction for the switch
						}else if(posX == 15 && posY == 8){
							tsi.setSwitch(17, 7, 0x02);//Unsure if correct direction for the switch
						}
					}
				}else if(((posX + 2 >= 3 && posX<3) || (posX - 2 <= 3 && posX>3)) && ((posY + 2 >= 11 && posY<11) || (posY - 2 <= 11 && posY>11))){
					if(inTurnCrossing) {
						lab1.getBottomTurnCrossingAvailable().release();
						inTurnCrossing = false;
					}else {
						if(posX == 3 && posY == 9) continue;
						tsi.setSpeed(id, 0);
						lab1.getBottomTurnCrossingAvailable().acquire();
						tsi.setSpeed(id, speed);
						inTurnCrossing = true;
						if(posX == 5 && posY == 11) {
							tsi.setSwitch(17, 7, 0x01);//Unsure if correct direction for the switch
						}else if(posX == 3 && posY == 13){
							tsi.setSwitch(17, 7, 0x02);//Unsure if correct direction for the switch
						}
					}
				}else if(false){
					//Double rail
					//If both trains have left/come into station, wait till the other train is on the other track 
					//Take semaphores for the T-junctions
					//Take semaphores for Top rail and bottom rail
				}else if(false) {
					//stations
				}
				
				
				
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}

enum Direction{
	UP,
	DOWN
}