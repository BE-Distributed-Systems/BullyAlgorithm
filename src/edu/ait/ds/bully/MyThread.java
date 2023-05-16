package edu.ait.ds.bully;
import java.util.*;
import java.io.*;
import java.net.*;

public class MyThread implements Runnable {

	private Process process;
	private int totalProcesses;
	private static boolean messageFlag[];
	ServerSocket[] sock;
	Random r;

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public MyThread(Process process, int total_processes) {
		this.process = process;
		this.totalProcesses = total_processes;
		this.r = new Random();
		this.sock = new ServerSocket[total_processes];
		MyThread.messageFlag = new boolean[total_processes];
		for (int i = 0; i < total_processes; i++)
			MyThread.messageFlag[i] = false;
	}

	synchronized private void recovery() {
		while (Election.isElectionFlag()) {
			// wait;
		}
		
		System.out.println("P" + this.process.getProcessId() + ": I'm back!");

		try {
			Election.pingLock.lock();
			Election.setPingFlag(false);
			Socket outgoing = new Socket(InetAddress.getLocalHost(), 12345);
			Scanner scan = new Scanner(outgoing.getInputStream());
			PrintWriter out = new PrintWriter(outgoing.getOutputStream(), true);
			System.out.println("P" + this.process.getProcessId() + ": Who is the coordinator?");
			out.println("Who is the coordinator?");
			out.flush();
			
			String pid = scan.nextLine();
			if (this.process.getProcessId() > Integer.parseInt(pid)) {
				out.println("Resign");
				out.flush();
				System.out.println("P" + this.process.getProcessId() + ": Resign -> P" + pid);
				String resignStatus = scan.nextLine();
				if (resignStatus.equals("Successfully Resigned")) {
					this.process.setCoOrdinatorFlag(true);
					sock[this.process.getProcessId() - 1] = new ServerSocket(10000 + this.process.getProcessId());
					System.out.println("P" + this.process.getProcessId()
							+ ": P" + pid + ", get out of here, I'm the coordinator!");
				}
			} else {
				out.println("Don't Resign");
				out.flush();
			}

			Election.pingLock.unlock();
			return;
			
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}

	}

	synchronized private void pingCoOrdinator() {
		try {
			Election.pingLock.lock();
			if (Election.isPingFlag()) {
				System.out.println("P" + this.process.getProcessId() + ": Coordinator, are you there?");
				Socket outgoing = new Socket(InetAddress.getLocalHost(), 12345);
				outgoing.close();
			}
		} catch (Exception ex) {
			Election.setPingFlag(false);
			Election.setElectionFlag(true);
			Election.setElectionDetector(this.process);

			System.out.println("P" + this.process.getProcessId() + ": Coordinator is down, I'm  initiating election..");
		} finally {
			Election.pingLock.unlock();
		}
	}

	private void executeJob() {
		int temp = r.nextInt(20);
		for (int i = 0; i <= temp; i++) {
			try {
				Thread.sleep((temp + 1) * 100);
			} catch (InterruptedException e) {
				System.out.println("Error Executing Thread:" + process.getProcessId());
				System.out.println(e.getMessage());
			}
		}
	}

	@SuppressWarnings({ "static-access" })
	synchronized private boolean sendMessage() {
		boolean response = false;
		try {
			Election.electionLock.lock();
			if (Election.isElectionFlag() && !MyThread.isMessageFlag(this.process.getProcessId() - 1)
					&& this.process.processId >= Election.getElectionDetector().getProcessId()) {

				for (int i = this.process.getProcessId() + 1; i <= this.totalProcesses; i++) {
					try {
						Socket electionMessage = new Socket(InetAddress.getLocalHost(), 10000 + i);
						System.out.println("P" + i + ": Here");
						electionMessage.close();
						response = true;
					} catch (IOException ex) {
						System.out.println("P" + this.process.getProcessId() + ": P" + i
								+ " didn't respond");
					} catch (Exception ex) {
						System.out.println(ex.getMessage());
					}
				}
				
				this.setMessageFlag(true, this.process.getProcessId() - 1);
				Election.electionLock.unlock();
				return response;
			} else {
				throw new Exception();
			}
		} catch (Exception ex1) {
			Election.electionLock.unlock();
			return true;
		}
	}

	public static boolean isMessageFlag(int index) {
		return MyThread.messageFlag[index];
	}

	public static void setMessageFlag(boolean messageFlag, int index) {
		MyThread.messageFlag[index] = messageFlag;
	}

	synchronized private void serve() {
		try {
			boolean done = false;
			Socket incoming = null;
			ServerSocket s = new ServerSocket(12345);
			Election.setPingFlag(true);
			int temp = this.r.nextInt(5) + 5;
			
			for (int counter = 0; counter < temp; counter++) {
				incoming = s.accept();
				if (Election.isPingFlag())
					System.out.println("P" + this.process.getProcessId() + ": Yes");
				
				Scanner scan = new Scanner(incoming.getInputStream());
				PrintWriter out = new PrintWriter(incoming.getOutputStream(), true);
				while (scan.hasNextLine() && !done) {
					String line = scan.nextLine();
					if (line.equals("Who is the coordinator?")) {
						System.out.println("P" + this.process.getProcessId() + ": Me");
						out.println(this.process.getProcessId());
						out.flush();
					} else if (line.equals("Resign")) {
						this.process.setCoOrdinatorFlag(false);
						out.println("Successfully Resigned");
						out.flush();
						incoming.close();
						s.close();
						System.out.println("P" + this.process.getProcessId() + ": Successfully Resigned");
						return;
						
					} else if (line.equals("Don't Resign")) {
						done = true;
					}
				}
			}
			
			this.process.setCoOrdinatorFlag(false);
			this.process.setDownflag(true);
			try {
				incoming.close();
				s.close();
				sock[this.process.getProcessId() - 1].close();
				Thread.sleep(15000);
				recovery();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			sock[this.process.getProcessId() - 1] = new ServerSocket(10000 + this.process.getProcessId());
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		
		while (true) {
			if (process.isCoOrdinatorFlag()) {
				serve();
			} else {
				while (true) {
					executeJob();
					pingCoOrdinator();
					if (Election.isElectionFlag()) {
						if (!sendMessage()) {
							Election.setElectionFlag(false);
							System.out.println("New coordinator: P" + this.process.getProcessId());
							this.process.setCoOrdinatorFlag(true);
							for (int i = 0; i < totalProcesses; i++) {
								MyThread.setMessageFlag(false, i);
							}
							break;
						}
					}
				}
			}
		}
	}
}