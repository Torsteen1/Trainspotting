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
	private int id;
	private Lab1 lab1;
	private int speed;
	private boolean inPlusCrossing = false;
	private boolean inTurnCrossing = false;
	private Direction direction;
	private boolean firstRound = true;
	
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
				int posX = event.getXpos();
				int posY = event.getYpos();
				if((posX >= 6 && posX <=10) && (posY>=5 && posY <= 8)) { //The Plus Junction
					if(inPlusCrossing) {
						lab1.getPlusJunctionAvailable().release();
						inPlusCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getPlusJunctionAvailable().acquire();
						tsi.setSpeed(id, speed);
						inPlusCrossing = true;
					}
					
				}else if((posX >= 14 && posX <=19) && (posY >= 7 && posY <= 8)){
					if(inTurnCrossing) {
						lab1.getTopTurnCrossingAvailable().release();
						inTurnCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getTopTurnCrossingAvailable().acquire();
						tsi.setSpeed(id, speed);
						inTurnCrossing = true;
						if(posX == 14 && posY == 7) {
							tsi.setSwitch(17, 7, 0x02);
						}else if(posX == 15 && posY == 8){
							tsi.setSwitch(17, 7, 0x01);
							lab1.getTopBottomRailAvailable().release();
						}
					}
				}else if(((posX + 2 >= 3 && posX<=3) || (posX - 3 <= 3 && posX>=3)) && (posY - 2 <= 11 && posY>=11)){
					if(posX == 3 && posY == 9) continue;
					if(inTurnCrossing) {
						lab1.getBottomTurnCrossingAvailable().release();
						inTurnCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getBottomTurnCrossingAvailable().acquire();
						tsi.setSpeed(id, speed);
						inTurnCrossing = true;
						if(posX == 6 && posY == 11) {
							tsi.setSwitch(3, 11, 0x01);
							lab1.getBottomTopRailAvailable().release();
						}else if(posX == 3 && posY == 13){
							tsi.setSwitch(3, 11, 0x02);
						}
					}
				}else if((posX==7 || posX==12) && (posY==9 || posY==10)){
					//Double rail
					
					if(posX==7 && direction==Direction.UP) {
						lab1.getBottomTurnCrossingAvailable().release();
						firstRound = false;
						continue;
					}
					if(posX==12 && direction==Direction.DOWN) {
						lab1.getTopTurnCrossingAvailable().release();
						firstRound = false;
						continue;
					}
					
					tsi.setSpeed(id, 0);
					
					if(posX==7) {
						lab1.getBottomTurnCrossingAvailable().acquire();
						
						if(posY==9) {
							tsi.setSwitch(4,9,0x01);
							lab1.getTopDoubleRailAvailable().release();
						}else if(posY==10) {
							tsi.setSwitch(4,9,0x02);
						}
					}
					
					if(posX==12) {
						lab1.getTopTurnCrossingAvailable().acquire();
						
						if(posY==9) {
							tsi.setSwitch(15,9,0x02);
							lab1.getTopDoubleRailAvailable().release();
						}else if(posY==10) {
							tsi.setSwitch(15,9,0x01);
						}
					}
					
					tsi.setSpeed(id, speed);
					inTurnCrossing = true;
					
										
				}else if(posY == 9 && (posX == 3 || posX == 16)) {
					//Decide which double rail to take logic
					if(lab1.getTopDoubleRailAvailable().availablePermits()==1) {
						if(posX==3 && direction==Direction.UP) {
							tsi.setSwitch(4, 9, 0x01);
							lab1.getTopDoubleRailAvailable().acquire();
						}else if(posX==16 && direction==Direction.DOWN) {
							tsi.setSwitch(15, 9, 0x02);
							lab1.getTopDoubleRailAvailable().acquire();
						}
					}else {
						if(posX==3 && direction==Direction.UP) {
							tsi.setSwitch(4, 9, 0x02);
						}else if(posX==16 && direction==Direction.DOWN) {
							tsi.setSwitch(15, 9, 0x01);
						}
					}
					
					if(posX==3 && direction==Direction.DOWN) {
						if(lab1.getBottomTopRailAvailable().availablePermits()==1) {
							tsi.setSwitch(3, 11, 0x01);
							lab1.getBottomTopRailAvailable().acquire();
						}else {
							tsi.setSwitch(3, 11, 0x02);
						}
					}
					
					if(posX==16 && direction==Direction.UP) {
						if(lab1.getTopBottomRailAvailable().availablePermits()==1) {
							tsi.setSwitch(17, 7, 0x01);
							lab1.getTopBottomRailAvailable().acquire();
						}else {
							tsi.setSwitch(17, 7, 0x02);
						}
					}
					
				}else if(posX==14 && ((posY == 13 || posY == 11) && direction==Direction.DOWN) || ((posY == 5 || posY == 3) && direction==Direction.UP)) {
					tsi.setSpeed(id, 0);
					Thread.sleep(2000);
					speed = -speed;
					tsi.setSpeed(id, speed);
					if(direction == Direction.UP) {
						direction = Direction.DOWN;
					}else{
						direction = Direction.UP;
					}
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