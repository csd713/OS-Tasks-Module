package osp.Tasks;

/**
 * Created by Chiru on 12-OCT-16.
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**
 * The student module dealing with the creation and killing of tasks. A task
 * acts primarily as a container for threads and as a holder of resources.
 * Execution is associated entirely with threads. The primary methods that the
 * student will implement are do_create(TaskCB) and do_kill(TaskCB). The student
 * can choose how to keep track of which threads are part of a task. In this
 * implementation, an array is used.
 * 
 * @OSPProject Tasks
 */
public class TaskCB extends IflTaskCB {
	/**
	 * The task constructor. Must have
	 * 
	 * super();
	 * 
	 * as its first statement.
	 * 
	 * @OSPProject Tasks
	 */

	private Vector<ThreadCB> threadsList;
	private Vector<PortCB> portsList;
	private Vector<OpenFile> openFilesList;

	public TaskCB() {

		// CSD
		super();
		threadsList = new Vector<ThreadCB>();
		portsList = new Vector<PortCB>();
		openFilesList = new Vector<OpenFile>();

	}

	/**
	 * This method is called once at the beginning of the simulation. Can be
	 * used to initialize static variables.
	 * 
	 * @OSPProject Tasks
	 */
	public static void init() {

	}

	/**
	 * Sets the properties of a new task, passed as an argument.
	 * 
	 * Creates a new thread list, sets TaskLive status and creation time,
	 * creates and opens the task's swap file of the size equal to the size (in
	 * bytes) of the addressable virtual memory.
	 * 
	 * @return task or null
	 * 
	 * @OSPProject Tasks
	 */
	static public TaskCB do_create() {
		// CSD

		// Creating task object
		TaskCB taskCB = new TaskCB();

		// Create a page table and associate it with the task object
		PageTable pageTable = new PageTable(taskCB);
		taskCB.setPageTable(pageTable);

		// ThreadCB threadCB = new ThreadCB();
		// PortCB portCB = new PortCB();

		// OpenFile openFile = new OpenFile(arg0, arg1);

		// Set task creation time
		taskCB.setCreationTime(HClock.get());
		System.out.println("Task " + taskCB + " created.");

		// Set task status to live
		taskCB.setStatus(TaskLive);

		// Set task priority to 5 by default (range is 5-1, 1 is the highest)
		taskCB.setPriority(5);

		/*
		 * A swap file contains the image of the task's virtual memory space and
		 * thus is equal to the maximal number of bytes in the virtual address
		 * space of the task.
		 */

		// Obtain task's virtual memory space size
		int swapFileSize = (int) Math.pow(2, MMU.getVirtualAddressBits());

		// Obtain Swap file name and path
		String swapFilePath = SwapDeviceMountPoint + taskCB.getID();

		// Create swap file and open it
		FileSys.create(swapFilePath, swapFileSize);
		OpenFile swapFile = OpenFile.open(swapFilePath, taskCB);

		// if open operation fails
		if (swapFile == null) {
			System.out.println("It failed");
			ThreadCB.dispatch();
			return null;
		}

		taskCB.setSwapFile(swapFile);
		ThreadCB.create(taskCB);

		return taskCB;
	}

	/**
	 * Kills the specified task and all of it threads.
	 * 
	 * Sets the status TaskTerm, frees all memory frames (reserved frames may
	 * not be unreserved, but must be marked free), deletes the task's swap
	 * file.
	 * 
	 * @OSPProject Tasks
	 */
	public void do_kill() {

		// CSD

		// Kill threads first
		for (int i = threadsList.size() - 1; i >= 0; i--) {
			threadsList.get(i).kill();
		}

		// Destroy the ports attached to task
		for (int i = portsList.size() - 1; i >= 0; i--) {
			portsList.get(i).destroy();
		}

		// Set the status of the task to terminated
		this.setStatus(TaskTerm);

		// Release the memory allocated to the task
		this.getPageTable().deallocateMemory();

		// Close all the opened files
		for (int i = openFilesList.size() - 1; i >= 0; i--) {
			if (openFilesList.get(i) != null) // Check this
				openFilesList.get(i).close();
		}

		// Delete the swap file
		FileSys.delete(SwapDeviceMountPoint + this.getID());

	}

	/**
	 * Returns a count of the number of threads in this task.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_getThreadCount() {

		// CSD
		// Return the number of threads
		return this.threadsList.size(); // Check this
	}

	/**
	 * Adds the specified thread to this task.
	 * 
	 * @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
	 *         SUCCESS otherwise.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_addThread(ThreadCB thread) {

		// CSD
		if (do_getPortCount() >= ThreadCB.MaxThreadsPerTask) {
			return FAILURE;
		}
		this.threadsList.add(thread);
		return SUCCESS;

	}

	/**
	 * Removes the specified thread from this task.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_removeThread(ThreadCB thread) {

		// CSD

		// remove the thread if it belongs to the task
		if (threadsList.size() == 0)
			return FAILURE;
		else if (threadsList.contains(thread)) {
			threadsList.remove(thread);
			return SUCCESS;
		}
		return FAILURE;
	}

	/**
	 * Return number of ports currently owned by this task.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_getPortCount() {

		// CSD

		// return the number of ports
		return portsList.size();

	}

	/**
	 * Add the port to the list of ports owned by this task.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_addPort(PortCB newPort) {

		// CSD
		if (do_getPortCount() >= PortCB.MaxPortsPerTask) {
			return FAILURE;
		}
		portsList.add(newPort);
		return SUCCESS;
	}

	/**
	 * Remove the port from the list of ports owned by this task.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_removePort(PortCB oldPort) {

		// CSD

		// remove ports
		if (portsList.size() == 0)
			return FAILURE;
		else if (portsList.contains(oldPort)) {
			portsList.remove(oldPort);
			return SUCCESS;
		}
		return FAILURE;
	}

	/**
	 * Insert file into the open files table of the task.
	 * 
	 * @OSPProject Tasks
	 */
	public void do_addFile(OpenFile file) {

		// CSD
		// Add new file to list
		openFilesList.add(file);

	}

	/**
	 * Remove file from the task's open files table.
	 * 
	 * @OSPProject Tasks
	 */
	public int do_removeFile(OpenFile file) {


		// CSD
		// Remove opened files

		if (openFilesList.size() == 0)
			return FAILURE;
		else if (openFilesList.contains(file)) {
			openFilesList.remove(file);
			return SUCCESS;
		}
		return FAILURE;
	}

	/**
	 * Called by OSP after printing an error message. The student can insert
	 * code here to print various tables and data structures in their state just
	 * after the error happened. The body can be left empty, if this feature is
	 * not used.
	 * 
	 * @OSPProject Tasks
	 */
	public static void atError() {

		// CSD
		System.out.println("Error occured");
	}

	/**
	 * Called by OSP after printing a warning message. The student can insert
	 * code here to print various tables and data structures in their state just
	 * after the warning happened. The body can be left empty, if this feature
	 * is not used.
	 * 
	 * @OSPProject Tasks
	 */
	public static void atWarning() {

		// CSD

		System.out.println("Warning is invoked");
	}

	/*
	 * Feel free to add methods/fields to improve the readability of your code
	 */

}

/*
 * Feel free to add local classes to improve the readability of your code
 */
