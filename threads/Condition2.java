package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
    	this.conditionLock = conditionLock;
	
    	// Create waitingQueue
    	waitingQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	// Disable interrupt and release the lock
	boolean interruptStatus = Machine.interrupt().disable();
	conditionLock.release();

	// Add the current thread to waitQueue
	waitingQueue.add(KThread.currentThread());
	
	// Put thread to sleep
	KThread.sleep();
	
	// Aquire lock after wake up
	conditionLock.acquire();
	
	// Restore interrupts
	Machine.interrupt().restore(interruptStatus);
	
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	// Disable interrupts
	boolean interruptStatus = Machine.interrupt().disable();
	
	// Wake the first thread in the queue
	if(waitingQueue.isEmpty() == false)
		waitingQueue.remove().ready();
	
	// Restore interrupts
	Machine.interrupt().restore(interruptStatus);
	
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	// Disable interrupts
	boolean interruptStatus = Machine.interrupt().disable();
	
	// Wake all threads in the waitingQueue
	while(waitingQueue.isEmpty() == false)
		wake();
	
	// Restore interrupts
	Machine.interrupt().restore(interruptStatus);
	
    }
    private Lock conditionLock;
    private LinkedList<KThread> waitingQueue;
}
