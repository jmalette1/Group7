package nachos.threads;
import java.util.*;
import nachos.machine.*;

public class ReactWater {

	private Queue<KThread> hWait = new LinkedList<KThread>();
	private Queue<KThread> oWait = new LinkedList<KThread>();
	int hCount, oCount;
	int H;
	int O;
	private Lock locks = new Lock();
	
	public ReactWater(Lock locks) {
		this.locks =locks;
		hCount = 0;
		oCount = 0;
	}
	
	//When there's 1 H and 1 O it will form H2O
	public void hReady() {
		if(H== 1 && O == 1) {
		hCount++;
		Makewater();
		}
		//Waits for more Hydrogen or Oxygen
		hWait.add(KThread.currentThread());
	
	}
//When there's 2 H's it will form H2O	
	public void oReady() {
		if(H == 2) {
			oCount++;
			Makewater();
		}
		//Waits for Hydrogen or Oxygen
		oWait.add(KThread.currentThread());
	}
	
	public void Makewater() {
		Lib.assertTrue(locks.isHeldByCurrentThread());
		while(hCount > 1 && oCount > 0) {
			System.out.println("Water is made");
			hCount = hCount - 2;
			oCount--;
			oWait.remove();
			hWait.remove();
			hWait.remove();
		}
	}
}
