package nachos.threads;
import nachos.machine.*;
import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	
    public Alarm() {
    	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * Takes wake-up time and sleeping KThread as arguments
     * We merge these parameters into a singl object, to be used
     * by other methods (ex. putting the object into a wait queue)
     */
    final class WaitThread implements Comparable<WaitThread>{
    	private final long wakeTime;
    	private final KThread waitThread;
    	
    	public WaitThread(long wakeTime, KThread waitThread){
    		this.wakeTime = wakeTime;
    		this.waitThread = waitThread;
    	}
    	
    	public long getWakeTime(){
    		return wakeTime;
    	}
    	
    	public KThread getWaitThread(){
    		return waitThread;
    	}
    	
    	@Override
    	public int compareTo(WaitThread wT){
    		return Long.compare(this.getWakeTime(), wT.getWakeTime());
    	}
    }
    
    /**
     * Set up "sleep queue" priority queue to store sleeping threads.
     * Uses "WaitThread" objects, described above.
     * Beware that since this is unbounded, adding too many tasks
     * could lead to an out-of-memory exception.
     */
    PriorityQueue<WaitThread> sleepQueue = new PriorityQueue<>();
    
    
    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     * 
     * Threads in the sleepQueue will be put into the ready queue (and removed
     * from the sleep queue) if their wake-up time is earlier than the currentTime.
     */
    public void timerInterrupt() {
    	while(true) //ends when queue is empty or head's wake-up-time > currentTime
    	{
    		if(sleepQueue.peek() == null)
    			break;
    		else if(Machine.timer().getTime() > sleepQueue.peek().getWakeTime())
    			sleepQueue.poll().getWaitThread().ready();
    		else
    			break;
    	}
    	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread is put
     * in a "WaitThread" object, and that object is put in a priority queue.
     * All threads in the priority queue will be woken up (placed in the scheduler
     * ready set) during the first timer interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    	long wakeTime = Machine.timer().getTime() + x;
    	
    	boolean intState = Machine.interrupt().disable(); //disabling interrupts seems to be necessary
	
    	WaitThread newWaitThread = new WaitThread(wakeTime, KThread.currentThread());
    	sleepQueue.add(newWaitThread);
    	KThread.currentThread().sleep();
    	
    	Machine.interrupt().restore(intState); //reenabling interrupts
    }
    
    /**
     * Used to test alarm class
     *
     * When looking at output, you can see that multiple threads can be woken at 
     * once (timestamp 5090, followed by 5100, etc.), or multiples of 500 ticks, 
     * as expected (since the kernel calls timerInterrupt method every 500 ticks).
     */ 
    private static class AlarmTest implements Runnable {
    	private long sleepTime;
    	private int iteration;
    	AlarmTest(long x, int i){
    		sleepTime = x;
    		iteration = i;
    	}
    	
    	@Override
    	public void run() {
    		long storeTime = Machine.timer().getTime();
    		System.out.println("Thread " + iteration + " stored at timestamp: " + storeTime);
    		System.out.println("delay = " + sleepTime);
    		ThreadedKernel.alarm.waitUntil(sleepTime); //Thread goes to sleep
    		long readyTime = Machine.timer().getTime(); //Thread is woken up
    		System.out.println("Thread " + iteration + " woken up at: " + readyTime + ", and " + readyTime + " >= " + storeTime + "+" + sleepTime + "(wake-up time)");
    	}
    }
    
    //Tests Alarm's waitUntil method by creating threads and using random wait values (between 0 and 9999)
    public static void selfTest(){
    	System.out.println("testing Alarm");
    	KThread[] threadArray = new KThread[15];
    	for(int i = 0; i < 15; i++){
    		threadArray[i] = new KThread(new AlarmTest(((long)(Math.random() * 10000)),i)); //when 10000 is replaced by -10000, threads seem to wake up immediately
    		threadArray[i].fork();
    	}
    
    ThreadedKernel.alarm.waitUntil(100000); //stops kernel from stopping nachos
    }
}