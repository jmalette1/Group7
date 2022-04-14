package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*; //needed for Linked List and possibly other things, I know "*" is excessive

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
    	super();
	}

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     * 
     * Creates a global linked list of free physical pages (from 0 to numPhysPages)
     */
    public void initialize(String[] args) {
    	super.initialize(args);

    	console = new SynchConsole(Machine.console());
	
    	pageListLock = new Lock();
    	freePages = new LinkedList<Integer>();
    	int physPages = Machine.processor().getNumPhysPages();
    	for (int i = 0; i < physPages; i++)
    		freePages.add(i);
	
    	Machine.processor().setExceptionHandler(new Runnable() {
    		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
    	super.selfTest();

    	System.out.println("Testing the console device. Typed characters");
    	System.out.println("will be echoed until q is typed.");

    	char c;

    	do {
    		c = (char) console.readByte(true);
    		console.writeByte(c);
    	}
    	while (c != 'q');

    	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
    	if (!(KThread.currentThread() instanceof UThread))
    		return null;
	
    	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
    	Lib.assertTrue(KThread.currentThread() instanceof UThread);

    	UserProcess process = ((UThread) KThread.currentThread()).process;
    	int cause = Machine.processor().readRegister(Processor.regCause);
    	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
    	super.run();

    	UserProcess process = UserProcess.newUserProcess();
	
    	String shellProgram = Machine.getShellProgramName();	
    	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

    	KThread.finish(); //I had to remove "currentThread" for some reason? gave error message
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
    	super.terminate();
    }
    
    /**
     * NEW METHOD:
     * Allocates demanded number of pages. Physical pages might not necessarily be contiguous,
     * since the next method (releasePage) might cause gaps in the freePages linkedList.
     * 
     * However, in UserProcess.java, the method "load sections" will load contiguous virtual addresses
     * 
     * @param num : number of pages to allocate
     * 
     * @return : array of allocated pages (or null if insufficient pages)
     */
    public static int[] allocatePages(int num) {
    	pageListLock.acquire();
    
    	if (freePages.size() < num){
    		pageListLock.release();
    		return null;
    	}
    	
    	int[] result = new int[num];
    	
    	for(int i = 0; i<num; i++)
    		result[i] = freePages.remove();
    	
    	pageListLock.release();
    	
    	return result;
    }
    
    /**
     * NEW METHOD:
     * Releases pages from memory and adds it back to
     * the freePages list(only one page at a time).
     * 
     * @param physPageNum : the physical page number to remove
     */
    public static void releasePage(int physPageNum){
    	pageListLock.acquire();
    	freePages.add(physPageNum);
    	pageListLock.release();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;
    
    public static LinkedList<Integer> freePages;
    public static Lock pageListLock;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;

	//----------------Task 1 Variables-------------------
    	public static LinkedList<FileReference> globalFileArray = new LinkedList<FileReference>();
    	//----------------End Task 1-------------------------
}