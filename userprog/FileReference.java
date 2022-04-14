package nachos.userprog;

/** This class is a reference to a file used to keep track of open files and their references. Contains the file name,
  * number of references to the file, and a boolean indicating whether or not the file is marked for unlinking.
  */
public class FileReference{

private String fileName;
private int numReferences;
private boolean unlinking;

//Constructor
	public FileReference(String fileName){
		this.fileName = fileName;
		numReferences = 1;
		unlinking = false;
	}

	//Add reference
	public void addReference(){
		++numReferences;
	}

	//Remove reference
	public void removeReference(){
		--numReferences;
	}

	//Get number of references
	public int getReferences(){
		return numReferences;
	}

	//Set name
	public void setName(String fileName){
		this.fileName = fileName;
	}

	//Get name
	public String getName(){
		return fileName;
	}

	//Set unlinking val
	public void setUnlinking(boolean bool){
		unlinking = bool;
	}


	//Get unlinking val
	public boolean getUnlinking(){
		return unlinking;
	}

}
