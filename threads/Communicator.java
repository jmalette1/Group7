package nachos.threads;
import java.util.Queue;
import java.util.LinkedList;
import nachos.machine.*;

public class Communicator {

 private Queue<KThread> LWait = new LinkedList<KThread>();
 private Queue<KThread> SWait = new LinkedList<KThread>();
 int LCount, SCount,word;
 private Lock locks = new Lock();
 
 public Communicator(Lock locks) {
  this.locks =locks;
  LCount = 0;
  SCount = 0;
  word=0;
 }
 
    public void speak(int word) {
lock.acquire();
SCount++;
      while(LCount==0){
            LWait.sleep();
        }
    LCount--;
    int messenger = word;
    SWait.wake();
    lock.release();
    }
 

public int listen() {

        lock.acquire();
        LCount++;
        LWait.wake();
        while(SCount==0) {
            SWait.sleep();
        }
        SCount--;
        lock.release();
        return messenger;
    }

 
 public void SendMessage(int word) {
  Lib.assertTrue(locks.isHeldByCurrentThread());
  while(LCount > 1 && SCount > 1) {
   System.out.println(word);
   LCount--;
   SCount--;
   LWait.remove();
   SWait.remove();
  }
 }
 
 } 
