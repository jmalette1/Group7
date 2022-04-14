package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);

	//----------- Task 1 -----------
	processID = processNum;
	++processNum;

	localFileTable = new OpenFile[16];

	localFileTable[0] = UserKernel.console.openForReading();
	localFileTable[1] = UserKernel.console.openForWriting();

	UserKernel.globalFileArray.add(new FileReference(localFileTable[0].getName()));
	UserKernel.globalFileArray.add(new FileReference(localFileTable[1].getName()));

	//----------- End Task 1 -----------
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }
    
    /**
     * NEW METHOD:
     * 
     * This is a helper method used to transform a virtual page
     * into a TranslationEntry object.
     * 
     * @param vpn : virtual page number to be transformed
     * 
     * @param writeNotRead : a boolean indicating if the TranslationEntry will 
     * 						 be used for a write or a read (to set dirty bit = 
     * 						 true or not)
     */
    public TranslationEntry getTranslationEntry(int vpn, boolean writeNotRead){
    	if(vpn < 0 || vpn > numPages)
    		return null;				//invalid vpn
    	
    	TranslationEntry result = pageTable[vpn];
    	if(result == null)
    		return null;				//error, no entry in pageTable
    	
    	if(result.readOnly && writeNotRead)
    		return null;				//error, attempted write on readOnly entry
    	
    	if(writeNotRead)
    		result.dirty = true;
    	
    	result.used = true;
    	
    	return result;

    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
    return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    	byte[] memory = Machine.processor().getMemory();
    	
    	int vpn = Processor.pageFromAddress(vaddr);
    	int firstOffset = Processor.offsetFromAddress(vaddr);
    	int vpnEnd = Processor.pageFromAddress(vaddr + length);
	
    	TranslationEntry translatedEntry = getTranslationEntry(vpn, false);
	
    	if(translatedEntry == null)
    		return 0;   //0 bytes returned as specified instead of error
	
	
    	//Read from first offset to start of next page (or for length amount)
    	int amountRead = Math.min(length,  pageSize - firstOffset);
    	System.arraycopy(memory, Processor.makeAddress(translatedEntry.ppn, firstOffset), data, offset, amountRead);
    	offset += amountRead;
    
	
    	//Read the rest (one page at a time)
    	int amountRead2;
    	for(int i = vpn + 1; i <= vpnEnd; i++){
    		translatedEntry = getTranslationEntry(i, false);
    		
    		if(translatedEntry == null)
    			return amountRead;
		
    		amountRead2 = Math.min(length - amountRead, pageSize);
    		System.arraycopy(memory, Processor.makeAddress(translatedEntry.ppn, 0), data, offset, amountRead2);
    		offset += amountRead2;
    		amountRead += amountRead2;
    	}
    	return amountRead;	
	}

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    	
    	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    	byte[] memory = Machine.processor().getMemory();
	
    	int vpn = Processor.pageFromAddress(vaddr);
    	int firstOffset = Processor.offsetFromAddress(vaddr);
    	int vpnEnd = Processor.pageFromAddress(vaddr + length);

    	TranslationEntry translatedEntry = getTranslationEntry(vpn, true);

    	if(translatedEntry == null)
    		return 0;   //0 bytes returned as specified instead of error


    	//Write from first offset to start of next page (or for length amount)
    	int amountWritten = Math.min(length,  pageSize - firstOffset);
    	System.arraycopy(data, offset, memory, Processor.makeAddress(translatedEntry.ppn, firstOffset), amountWritten);
    	offset += amountWritten;


    	//Read the rest (one page at a time)
    	int amountWritten2;
    	for(int i = vpn + 1; i <= vpnEnd; i++){
    		translatedEntry = getTranslationEntry(i, true);
		
    		if(translatedEntry == null)
    			return amountWritten;
	
    		amountWritten2 = Math.min(length - amountWritten, pageSize);
    		System.arraycopy(data, offset, memory, Processor.makeAddress(translatedEntry.ppn, 0), amountWritten2);
    		offset += amountWritten2;
    		amountWritten += amountWritten2;
    	}
    	return amountWritten;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    	//Allocate physical page numbers, see UserKernel.java
    	int[] physicalPageNums = UserKernel.allocatePages(numPages); //numPages = lenght of memory needed by process + stackPages + 1 for args
    
    	if(physicalPageNums == null) {
    		coff.close();
    		Lib.debug(dbgProcess, "\tinsufficient physical memory");
    		return false;
    	}
    
		if(numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory2");
			return false;
		}

    	pageTable = new TranslationEntry[numPages];
    	
    	// load sections
    	for (int s=0; s<coff.getNumSections(); s++) {
    		CoffSection section = coff.getSection(s);
	    
    		Lib.debug(dbgProcess, "\tinitializing " + section.getName()
    				  + " section (" + section.getLength() + " pages)");

    		for (int i=0; i<section.getLength(); i++) {
    			int vpn = section.getFirstVPN()+i;
    			int ppn = physicalPageNums[vpn];
    			pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
    			section.loadPage(i, ppn);
    		}
    	}
    	//allocate free pages for stack and argv
    	for(int i = numPages - stackPages - 1; i < numPages; i++)
    		pageTable[i] = new TranslationEntry(i, physicalPageNums[i], true, false, false, false);

    	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	coff.close();
    	
    	for(int i = 0; i < numPages; i++)
    		UserKernel.releasePage(pageTable[i].ppn);
    	pageTable = null;
    }    
    
    public void selfTest(){
    	System.out.println("UserProcessTest");
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    // -------- Task 1 --------

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    	// if root process
    	if(processID == 0) {

	    	Machine.halt();
		
	    	Lib.assertNotReached("Machine.halt() did not halt machine!");
	    	return 0;
    	}
    	
    	return -1;
    }
    
    /**
     * Handle the creat system call. 
     * Returns a file descriptor 
     * @param addr memory location of the file
     * @return file descriptor on success, -1 if an error occured 
     */
    
    private int handleCreat(int addr) {
    	// argument validation 
    	int fileDescriptor = Processor.pageFromAddress(addr);
    	if(fileDescriptor < 0 || fileDescriptor > 15)
    		return -1;
    	
    	// get filename from memory 
    	String fileName = readVirtualMemoryString(addr, 256);
    	OpenFile openFile = UserKernel.fileSystem.open(fileName, true);   //attempt to open the file 
    	
    	if(openFile == null)
    		return -1;
    	
    	int localFileIndex = -1;
    	
    	// Iterate through the local file table to find file
    	if(fileName != null){
    		for(int i = 2; i < localFileTable.length; ++i){
    			if(localFileTable[i] == openFile) 
    				return localFileIndex = i;
    		}
    		
    		// If the open file isn't found, search for a free space in the local file table and add the file
    		if(localFileIndex == -1){
    			for(int i = 2; i < localFileTable.length; ++i){
    				if(localFileTable[i] == null){
    					localFileIndex = i;
    					localFileTable[i] = openFile;
    					updateGlobalFileArray(fileDescriptor, openFile, fileName);
    					return localFileIndex;
    				} else 
    					return -1; // error
    			}
    		}
    	}
    	
    	return -1; // error
    }
    
    /**
     * Attempts to open the specified file and returns a file descriptor
     * @param addr memory address of the file 
     * @return file descriptor on success, -1 if an error occured
     */
    
    private int handleOpen(int addr){
    	// argument validation 
    	int fileDescriptor = Processor.pageFromAddress(addr);
    	if(fileDescriptor < 0 || fileDescriptor > 15)
    		return -1;
    	
    	String fileName = readVirtualMemoryString(addr, 256);
    	OpenFile openFile = UserKernel.fileSystem.open(fileName, false);
    	
    	if(openFile == null)
    		return -1;
    	
    	int localFileIndex = -1;
    	
    	if(fileName != null){
    		for(int i = 2; i < localFileTable.length; ++i){
    			if(localFileTable[i] == openFile) 
    				return localFileIndex = i;
    		}
    		
    		if(localFileIndex == -1){
    			for(int i = 2; i < localFileTable.length; ++i){
    				if(localFileTable[i] == null){
    					localFileIndex = i;
    					localFileTable[i] = openFile;
    					updateGlobalFileArray(fileDescriptor, openFile, fileName);
    					return localFileIndex;
    				} else 
    					return -1;
    			}
    		}
    	}
    	
    	return -1; 
    }
    
    /**
     * Handles the read system call.
     * @param fileDescriptor The file descriptor
     * @param bufferAddr Buffer address
     * @param size Number of bytes to read
     * @return Returns the number of bytes read or -1 if an error occurs.
     */
    private int handleRead(int fileDescriptor, int bufferAddr, int size){
    	// argument validation 
    	if(fileDescriptor < 0 || fileDescriptor > 15 || localFileTable[fileDescriptor] == null || bufferAddr < 0 || size < 0)
    		return -1;
    	
    	
    	if(size == 0)
    		return 0;
    	
    	byte[] store = new byte[size];
    	OpenFile openFile = localFileTable[fileDescriptor];
    	int bytesRead = openFile.read(store, 0, size);
    	
    	if(bytesRead < 0)
    		return -1;
    	
    	int bytesWritten = writeVirtualMemory(bufferAddr, store, 0, bytesRead);
    	
    	if(bytesRead != bytesWritten)
    		return -1;
    	
    	return bytesRead;
    	
    }
    
    /**
     * Handles the write system call
     * @param fileDescriptor The file descriptor
     * @param bufferAddr Buffer address 
     * @param size Number of bytes read
     * @return Returns the number of bytes successfully written. 0 if nothing written and -1 if an error occured.
     */
    
    private int handleWrite(int fileDescriptor, int bufferAddr, int size){
    	// argument validation 
    	if(fileDescriptor < 0 || fileDescriptor > 15 || localFileTable[fileDescriptor] == null || bufferAddr < 0 || size < 0)
    		return -1;
    	
    	if(size == 0)
    		return 0;
    	
    	byte[] store = new byte[size];
    	OpenFile openFile = localFileTable[fileDescriptor];
    	
    	int bytesRead = readVirtualMemory(bufferAddr, store, 0, size);
    	if(bytesRead < 0)
    		return -1;
    	
    	int bytesWritten = openFile.write(store, 0, bytesRead);
    	
    	if(bytesWritten < 0){
    		return -1;
    	} else
    		return bytesWritten;
 
    }
    
    /**
     * Handles the close system call. Closes the file descriptor so it no longer refers to a file or stream. Releases allocated resources
     * @param fileDescriptor The file descriptor
     * @return Returns 0 on success or -1 if an error occured. 
     */
    private int handleClose(int fileDescriptor){
    	// argument validation 
    	if(fileDescriptor < 0 || fileDescriptor > 15 || localFileTable[fileDescriptor] == null)
    		return -1;
    	
    	String fileName = localFileTable[fileDescriptor].getName();
    	
    	
    	if(fileName != null){
    		localFileTable[fileDescriptor].close();
    		localFileTable[fileDescriptor] = null;
    	}
    	
    	int globalFileIndex = -1;
    	for(int i = 0; i < UserKernel.globalFileArray.size(); ++i){
    		if(UserKernel.globalFileArray.get(i).getName().equals(fileName)){
    			globalFileIndex = i;
    			break;
    		}
    	}
    	
    	if(globalFileIndex == -1){
    		return -1;
    	} else {
    		UserKernel.globalFileArray.get(globalFileIndex).removeReference();
    		if(UserKernel.globalFileArray.get(globalFileIndex).getReferences() == 0 && UserKernel.globalFileArray.get(globalFileIndex).getUnlinking() == true){
    			boolean closing = UserKernel.fileSystem.remove(fileName); //attempt to close
    			if(closing){
    				return 0; //success
    			} else {
    				return -1;
    			}
    		}
    	}
    	
    	return -1;
    }
    
    /**
     * Handles the unlink system call. Deletes a file from the system. If any processes have the file open, the file will remain in existence until the
     * last file descriptor referring to it is closed. 
     * @param addr Memory location of file
     * @return Returns 0 on success or -1 if an error occured 
     */
    private int handleUnlink(int addr){
    	// argument validation 
    	int fileDescriptor = Processor.pageFromAddress(addr);
    	if(fileDescriptor < 0 || fileDescriptor > 15)
    		return -1;
    	
    	String fileName = readVirtualMemoryString(addr, 256);
    	
    	for(int i = 2; i < localFileTable.length; ++i){
    		if(localFileTable[fileDescriptor] != null){
    			if(localFileTable[fileDescriptor].getName().equals(fileName)){
    				localFileTable[fileDescriptor].close();
    				localFileTable[fileDescriptor] = null;
    			}
    		}
    	}
    	
    	int globalFileIndex = -1;
    	for(int i = 0; i < UserKernel.globalFileArray.size(); ++i){
    		if(UserKernel.globalFileArray.get(i).getName().equals(fileName)){
    			globalFileIndex = i;
    			break;
    		}
    	}
    	
    	if(globalFileIndex == -1){
    		return -1;
    	} else {
    		UserKernel.globalFileArray.get(globalFileIndex).setUnlinking(true);
    		UserKernel.globalFileArray.get(globalFileIndex).removeReference();
    		if(UserKernel.globalFileArray.get(globalFileIndex).getReferences() == 0){
    			boolean closing = UserKernel.fileSystem.remove(fileName); //attempt to close
    			if(closing){
    				UserKernel.globalFileArray.remove(globalFileIndex);
    				return 0; //success
    			} else {
    				return -1;
    			}
    		}
    	}
    	
    	return -1;
    }
    
    /**
     * Updates the globalFileArray. Increments the number of references when called by handleOpen() or handleCreat()
     * @param fileDescriptor The file descriptor
     * @param openFile File for reading and writing
     * @param fileName File name
     */
    private void updateGlobalFileArray(int fileDescriptor, OpenFile openFile, String fileName) {
    	int globalFileIndex = -1;
    	
    	for(int i = 0; i< UserKernel.globalFileArray.size(); ++i){
    		if(UserKernel.globalFileArray.get(i).getName().equals(fileName)) {
    			globalFileIndex = i;
    			break;
    		}
    	}
    	
    	if(globalFileIndex == -1){
    		FileReference fileRef = new FileReference(fileName);  //create reference to the Open File
    		UserKernel.globalFileArray.add(fileRef);
    	} else {
    		if(UserKernel.globalFileArray.get(globalFileIndex).getUnlinking() == false)
    			UserKernel.globalFileArray.get(globalFileIndex).addReference();
    	}
    }
    
    // --------- End Task 1 --------


    // ---------- Task 3 -----------
    private int handleExit() {
    	UThread.stop();
    	file.close();
    	System.gc();
    	String username=System.getProperty("user.name");
    	if(username.equals("root"))
    			halt();
    	if(counter==1)
    			halt();
    	
    	Machine.halt();
    }
    
    private int handleExec(int file, int argc, int argv) {
    	//Checking if valid or not
    	if(file < 0 || argc < 0 || argv < 0) {
    		System.out.println("Not valid");
    		return -1;
    	}
    	
    	String filename = readVirtualMemoryString(file, 256);
    	
    	//Checking if valid or not
    	if(filename == null) {
    		System.out.println("Not valid");
    		return -1;
    	}
    	
    	//Checking if valid or not
    	if(filename.contains(".coff") == false) {
    		System.out.println("Not valid");
    		return -1;
    	}
    	
    	String[] fileString = filename.split("\\.");
    			String[] argue = new String[argc];
    	
    	//New Constructor for child
    	UserProcess child = UserProcess.newUserProcess();
    	
    	if(child.execute(filename, argue)) {
    		childProcess.add(child);
    		return child.processID;
    	}
    	else {
    		return -1;
    	}
    }
    
    private int handleJoin(int processID, int status) {
    	
    	//Checking if valid or not
    	if(processID < 0 || status < 0) {
    		System.out.println("Not valid");
    		return -1;
    	}
    	
    	UserProcess child = null;
    	
    	for (int i = 0; i < childProcess.size(); i++) {
    		if(childProcess.get(i).processID == processID) {
    			child = childProcess.get(i);
    		}
    	}
    	
    	//Checking if valid or not
    	if(child == null) {
    		return -1;
    	}
    	
    	//joining the child to parent
    	child.thread.join();
    	
    	//creating the status for the child process
    	Lock.acquire();
    	Integer Childstatus = Statuses.get(child.processID);
    	Lock.release();
    	
    	//Checking if valid or not
    	if(Childstatus == null) {
    		return -1;
    	}
    	
    	//Checking if valid or not
    	if(status != null) {
    		byte[] bytes = new byte[4];
    		Lib.bytesFromInt(bytes, 0, Childstatus);
    		int bytesWrite = writeVirtualMemory(status, bytes);
    		
    		if(bytesWrite == 4) {
    			return 1;
    		}
    		else {
    			return -1;
    		}
    	
    	}
    	
    	else {
    		return -1;
    	}
    }
    // ---------- End Task 3 -------
    

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    // -------- Task 1 --------
    
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallHalt:
            return handleHalt();
        case syscallCreate:
                return handleCreat(a0);
        case syscallOpen:
                return handleOpen(a0);
        case syscallRead:
                return handleRead(a0, a1, a2);
        case syscallWrite:
                return handleWrite(a0, a1, a2);
        case syscallClose:
              return handleClose(a0);
        case syscallUnlink:
              return handleUnlink(a0);
        case syscallExec:
              return handleExec(a0, a1, a2);
        case syscallJoin:
              return handleJoin(a0, a1);
        case syscallExit:
              return handleExit(a0);

        default:
            Lib.debug(dbgProcess, "Unknown syscall " + syscall);
            Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

	// ----------- Task 1 -----------
	
	private static int processNum = 0;
	public int processID;
	public OpenFile[] localFileTable;
	private static final char debugFlag = 'f';

	//----------- End Task 1 -----------
	
	public int counter;
}
