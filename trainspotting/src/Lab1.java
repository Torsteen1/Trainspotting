import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1{
	private Semaphore plusJunctionAvailable = new Semaphore(1);
	private Semaphore topRailAvailable = new Semaphore(1);
	private Semaphore bottomRailAvailable = new Semaphore(1);
	private Semaphore topTurnCrossingAvailable = new Semaphore(1);
	private Semaphore bottomTurnCrossingAvailable = new Semaphore(1);
	private Boolean topDoubleRailAvailable = true;

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
	
	public Semaphore getTopRailAvailable() {
		return topRailAvailable;
	}
	
	public Semaphore getBottomTurnCrossingAvailable() {
		return bottomTurnCrossingAvailable;
	}
	
	public Semaphore getBottomRailAvailable() {
		return bottomRailAvailable;
	}
	
	public Boolean getTopDoubleRailAvailable() {
		return topDoubleRailAvailable;
	}
	
	public void setTopDoubleRailAvailable(Boolean bool) {
		topDoubleRailAvailable = bool;
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
				if((posX >= 6 && posX <=10) && (posY>=6 && posY <= 8)) { //The Plus Junction
					if(inPlusCrossing) {
						lab1.getPlusJunctionAvailable().release();
						inPlusCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getPlusJunctionAvailable().acquire();
						tsi.setSpeed(id, speed);
						inPlusCrossing = true;
					}
					
				}else if((posX >= 15 && posX <=19) && (posY >= 5 && posY <= 9)){
					if(posX == 16 && posY == 9) continue;
					if(inTurnCrossing) {
						lab1.getTopTurnCrossingAvailable().release();
						inTurnCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getTopTurnCrossingAvailable().acquire();
						tsi.setSpeed(id, speed);
						inTurnCrossing = true;
						if(posX == 15 && posY == 7) {
							tsi.setSwitch(17, 7, 0x02);
						}else if(posX == 15 && posY == 8){
							tsi.setSwitch(17, 7, 0x01);
						}
					}
				}else if(((posX + 2 >= 3 && posX<3) || (posX - 2 <= 3 && posX>3)) && ((posY + 2 >= 11 && posY<11) || (posY - 2 <= 11 && posY>11))){
					if(posX == 3 && posY == 9) continue;
					if(inTurnCrossing) {
						lab1.getBottomTurnCrossingAvailable().release();
						inTurnCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getBottomTurnCrossingAvailable().acquire();
						tsi.setSpeed(id, speed);
						inTurnCrossing = true;
						if(posX == 5 && posY == 11) {
							tsi.setSwitch(3, 11, 0x01);
						}else if(posX == 3 && posY == 13){
							tsi.setSwitch(3, 11, 0x02);
						}
					}
				}else if(posX==9 && (posY==9 || posY==10)){
					//Double rail
					//Setting top double rail available
					//Switches for leaving double rail
										
					tsi.setSpeed(id, 0);
					if(direction == Direction.UP) {
						lab1.getBottomTurnCrossingAvailable().release();
						if(!firstRound) lab1.getBottomRailAvailable().release();
						
						lab1.getTopRailAvailable().acquire();
						lab1.getTopTurnCrossingAvailable().acquire();
						tsi.setSwitch(17, 7, 0x01);
					}else if(direction == Direction.DOWN){
						lab1.getTopTurnCrossingAvailable().release();
						if(!firstRound) lab1.getTopRailAvailable().release();
						
						lab1.getBottomRailAvailable().acquire();
						lab1.getBottomTurnCrossingAvailable().acquire();
						tsi.setSwitch(3, 11, 0x02);
					}
					
					inTurnCrossing = true;
					tsi.setSpeed(id, speed);
					firstRound = false;
					
					if(posY==9) {//if on top double rail
						lab1.setTopDoubleRailAvailable(true);
						if(direction == Direction.UP) {
							tsi.setSwitch(15, 9, 0x02);
						}else {
							tsi.setSwitch(4, 9, 0x01);
						}
					}else {//if on bottom rail
						if(direction == Direction.UP) {
							tsi.setSwitch(15, 9, 0x01);
						}else {
							tsi.setSwitch(4, 9, 0x02);
						}
					}
					
										
				}else if(posY == 9 && (posX == 3 || posX == 16)) {
					//Affect semaphore for top/bottom rail and T-junctions
					
					switch(direction) {
					case UP : if(posX==3) tsi.setSwitch(4,9,0x02); break;
					case DOWN : if(posX==16) tsi.setSwitch(15,9,0x01); break;
					}
					
				}else if((posX == 14 && posY == 13) || (posX == 14 && posY == 5)) {
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