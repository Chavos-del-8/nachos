package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
		listenerWaitingQueue = new Condition(lock);
		speakerWaitingQueue = new Condition(lock);
		listenerReceiving = new Condition(lock);
		speakerSending = new Condition(lock);
		listenerWaiting = false;
		speakerWaiting = false;
		received = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
		while(speakerWaiting){ 
			//Add speaker to waiting queue
			speakerWaitingQueue.sleep();
		}

		//Prevent other speakers from speaking
		speakerWaiting = true;

		holder = word;
		
		while(!listenerWaiting || !received){
			listenerReceiving.wake(); //wake up a potential partner
			speakerSending.sleep(); //put this speaker to the sending queue
		}

		listenerWaiting = false; //set it to false so other listener can get to the recievingQueue
		speakerWaiting = false; //set it to false so other speaker can get to the sendingQueue
		received = false;
		speakerWaitingQueue.wake(); //wake up a waiting speaker
		listenerWaitingQueue.wake();
		lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();
		while(listenerWaiting){
			listenerWaitingQueue.sleep();
		}

		listenerWaiting = true;
		// Until listenerWaiting is set to false, the process below is inaccessable to other listners	

		while(!speakerWaiting){ //no speaker, go into loop
			listenerReceiving.sleep(); //set this thread to be the first thread to recieve a message
		}

		//There is 1 speaker Sending message
		speakerSending.wake(); // wake up the sleeping speaker in sendingQueue
		received = true;
		lock.release();
		return holder;
    }

    public static void selfTest(){
		final Communicator commu = new Communicator();
	
		KThread thread2 = new KThread(new Runnable() {
		    public void run() {
		         System.out.println("Thread 2 begin listening");
			 commu.listen();
		         System.out.println("Thread 2 finished listening");
		    }
		});
		
		KThread thread1 = new KThread(new Runnable() {
		    public void run() {
			 System.out.println("Thread 1 begin speaking");
			 commu.speak(2);
		         System.out.println("Thread 1 finished speaking");
		    }
		});
		
		thread1.fork();
		thread2.fork();
		thread1.join();
		thread2.join();
	}

	private Condition 	speakerWaitingQueue; //speaker wait queue
	private Condition 	speakerSending; //speaker is sending word
	private Condition 	listenerWaitingQueue; //listener wait queue
	private Condition 	listenerReceiving;	//listener receiving word
	
	private boolean 	listenerWaiting; //indicates a waiting listener
	private boolean 	speakerWaiting; //indicates a waiting speaker
	private boolean 	received; //indicates a received message
	
	private int 		holder;
	private Lock 		lock;
}
