import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1{
	private Semaphore plusJunctionAvailable = new Semaphore(1);
	private Semaphore topRailAvailable = new Semaphore(1);
	private Semaphore bottomRailAvailable = new Semaphore(1);
	private Semaphore topDoubleRailAvailable = new Semaphore(1);

	public Lab1(int speed1, int speed2) {
		Thread train1 = new Thread(new Train(1, speed1, this));
		Thread train2 = new Thread(new Train(2, speed2, this));
		
		train1.start();
		train2.start();
	}
	
	public Semaphore getPlusJunctionAvailable() {
		return plusJunctionAvailable;
	}

}

final class Train implements Runnable{
	TSimInterface tsi = TSimInterface.getInstance();
	private int id;
	private Lab1 lab1;
	private int speed;
	private boolean inCrossing = false;
	
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
				if(((posX + 2 >= 8 && posX<8) || (posX - 2 <= 8 && posX>8)) && ((posY + 2 >= 7 && posY<7) || (posY - 2 <= 7 && posY>7))) {
					//The plus-junction
					if(inCrossing) {
						lab1.getPlusJunctionAvailable().release();
						inCrossing = false;
					}else {
						tsi.setSpeed(id, 0);
						lab1.getPlusJunctionAvailable().acquire();
						tsi.setSpeed(id, speed);
						inCrossing = true;
					}
					
				}else if(((posX + 2 >= 17 && posX<17) || (posX - 2 <= 17 && posX>17)) && ((posY + 2 >= 7 && posY<7) || (posY - 2 <= 7 && posY>7))){
					//The top T-junction
					//If train coming from bottom has left double rail, train coming from left must wait.
					//Then if train coming from left the turn has to be switched to up, and if from right/down the turn must be switched down
				}else if(((posX + 2 >= 3 && posX<3) || (posX - 2 <= 3 && posX>3)) && ((posY + 2 >= 11 && posY<11) || (posY - 2 <= 11 && posY>11))){
					//The bottom T-junction
					//Coming from right: if train left double rail, wait
				}else if(false){
					//Double rail
					//If both trains have left/come into station, wait till the other train is on the other track 
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