package edu.ait.ds.bully;
public class Process {

	int processId;
	boolean isCoordinator, isDown;
	
	public int getProcessId() {
		return processId;
	}

	public void setProcessId(int pid) {
		this.processId = pid;
	}
		
	public boolean isCoOrdinatorFlag() {
		return isCoordinator;
	}

	public void setCoOrdinatorFlag(boolean isCoOrdinator) {
		this.isCoordinator = isCoOrdinator;
	}
	
	public boolean isDownflag() {
		return isDown;
	}

	public void setDownflag(boolean downflag) {
		this.isDown = downflag;
	}

	public Process() {
		
	}

	public Process(int pid) {
		this.processId = pid;
		this.isDown = false;
		this.isCoordinator = false;
	}
}