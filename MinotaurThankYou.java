import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;
import java.util.ArrayList;

public class MinotaurThankYou {
	private static void shufflePresents(int[] ar) {
		Random rnd = new Random(0);
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}


  public static void main(String[] args) {
    // Unordered bag of presents
    int[] presents = new int[500000];
    for (int i = 0; i < 500000; i++) {
      presents[i] = i;
    }
    shufflePresents(presents);

    // Push the presents into ConcurrentLinkedQueue
    ConcurrentLinkedQueue<Integer> unorderedBag = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < 500000; i++) {
      unorderedBag.add(presents[i]);
    }

    // Create a concurrent linked list
    ConcurrentSkipListSet<Integer> chain = new ConcurrentSkipListSet<>();

    // Create a file to write thank you notes
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new File("thankYouNotes.txt"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Start the simulation
    long startTime = System.nanoTime();

    // Create 4 threads, one for each servant
    ArrayList<Thread> threads = new ArrayList<>();
    for (int i = 1; i <= 4; i++) {
      Thread t = new Thread(new Servant(i, unorderedBag, chain, writer));
      t.start();
      threads.add(t);
    }
    // Wait for the threads to finish
    try {
      for (Thread t : threads) {
        t.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      writer.close();
    }

    // Print the number of presents and thank you notes
    System.out.println("Number of presents in the chain: " + chain.size());

    // Print the time taken to complete the simulation
    long endTime = System.nanoTime();
    System.out.println("Time taken to complete the simulation: " + (endTime - startTime) / 1000000 + "ms");
  }
}

class Servant implements Runnable {
	private ConcurrentLinkedQueue<Integer> unorderedBag;
	private ConcurrentSkipListSet<Integer> chain;
	private int servantId;
	private PrintWriter writer;
  private static boolean isDone = false;

	public Servant(
		int servantId,
    ConcurrentLinkedQueue<Integer> unorderedBag,
		ConcurrentSkipListSet<Integer> chain,
    PrintWriter writer
	) {
    this.unorderedBag = unorderedBag;
    this.chain = chain;
		this.servantId = servantId;
    this.writer = writer;
	}

  private void log(String message) {
    System.out.println("Servant " + servantId + ": " + message);
  }

  private void addGiftToChain() {
    Integer gift = unorderedBag.poll();
    if (gift != null) {
      chain.add(gift);
    }
  }

  private void addThankYouNotes() {
    Integer gift = chain.pollFirst();
    if (gift != null) {
      writer.println("Thank you " + gift);
    }
  }

  private void checkGift() {
    int randomGift = new Random().nextInt(500000);
    if (chain.contains(randomGift)) {
      writer.println("Gift " + randomGift + " is present in the chain");
    } else {
      // Might be taken by another servant
      writer.println("Gift " + randomGift + " is not present in the chain");
    }
  }

	@Override
	public void run() {
    while (!isDone) {
      if (chain.isEmpty() && unorderedBag.isEmpty()) {
        isDone = true;
        return;
      }

      // Random a number between 1 and 3
      int random = new Random().nextInt(3) + 1;
      if (random == 1) {
        addGiftToChain();
      } else if (random == 2) {
        addThankYouNotes();
      } else {
        checkGift();
      }
    }
	}
}
