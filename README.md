# Assignment 3

## How to Run

This project uses OpenJDK 18. You can download it [here](https://jdk.java.net/archive/).

- In the root directory, run `javac *.java` to compile the programs.
- Run `java MinotaurThankYou` to run the Minotaur thank you program.
- Run `java MarsRover` to run the Mars rover program.

## Minotaur Thank You

We have four servants that will help the Minotaur with his thank you cards. Unordered presents are placed in a bag, this can be thought of as a queue. The Minotaur wants to create a linked list of presents in increasing order. The servants will alternate between adding presents to the linked list and writing thank you cards. The linked list is implemented as a concurrent skip list. The thank you note writing is implemented as PrinterWriter.

### Existing Problem

The Minotaur has more presents than thank you notes. Because the servants are alternating between adding presents and writing thank you notes, there is a possibility that a present is added to the linked list but the next present is removed at the same time. This results in a present being added to the linked list but not having a thank you note written for it.

### Correctness

- The ConcurrentLinkedQueue is used to store the presents. This is a thread-safe queue that allows multiple threads to add and remove elements concurrently. Even we don't concurrently add to the queue, this helps to ensure that multiple threads can poll the queue without any duplication.
- The linked list is linearizable because the ConcurrentSkipListSet is linearizable. There are 2 operations that can be performed on the linked list: add and remove.
  1. Add operation
     - The insert operation is straightforward.\  
       Given: A -> B -> C -> D
     - If we want to insert E between B and C, we can do so by changing the pointers of E to point to C first.\
       A -> B -> C -> D\
       E --------^
     - Then we change the pointers of B to point to E.\
       A -> B -> E -> C -> D
     - The CAS operation ensures that the pointers are changed atomically.
  2. Remove operation
     - The remove operation is a little more complicated.\
       Given: A -> B -> C -> F -> G
     - If we just change the pointers of B to point to F.\
       A -> B -> F -> G\
       C --------^
     - And at the same time, another thread insert D between C and F.\
       A -> B -> F -> G\
       C -> D --^
     - The linked list is now broken as the CAS operation is not able to detect the change it.
     - To solve this, the ConcurrentSkipListSet is used a mark the nodes that it is logically removed. [Research Paper](https://www.cl.cam.ac.uk/research/srg/netos/papers/2001-caslists.pdf)

### Efficiency

- Both the ConcurrentLinkedQueue and ConcurrentSkipListSet are wait-free, which reduces contention between threads.
- The Minotaur want to keep presents in increasing order. The ConcurrentSkipListSet is a good choice because it is a concurrent data structure that allows for efficient insertion and removal of elements in log(n) time, instead of O(n) time for a normal linked list.

### Experimental Evaluation

<!-- You are tasked with the design of the module responsible for measuring the atmospheric
temperature of the next generation Mars Rover, equipped with a multicore CPU and 8
temperature sensors. The sensors are responsible for collecting temperature readings at
regular intervals and storing them in shared memory space. The atmospheric
temperature module has to compile a report at the end of every hour, comprising the top
5 highest temperatures recorded for that hour, the top 5 lowest temperatures recorded
for that hour, and the 10-minute interval of time when the largest temperature
difference was observed. The data storage and retrieval of the shared memory region
must be carefully handled, as we do not want to delay a sensor and miss the interval of
time when it is supposed to conduct temperature reading. Design and implement a
solution using 8 threads that will offer a solution for this task. Assume that the
temperature readings are taken every 1 minute. In your solution, simulate the operation
of the temperature reading sensor by generating a random number from -100F to 70F at
every reading. In your report, discuss the efficiency, correctness, and progress guarantee
of your program. -->

## Mars Rover

The idea is to have 8 temperature sensors that will collect temperature readings at regular intervals and we don't want to block any sensor from taking a reading. The sensors will store the readings in a shared memory space (ConcurrentLinkedQueue). The atmospheric temperature module will poll the readings from the shared memory space, get all the readings for the hour, and compile a report.

## Correctness and Efficiency

Because the ConcurrentLinkedQueue is used to store the readings, the sensors will not be blocked from writing to the shared memory space. The atmospheric temperature module will poll the readings from the shared memory space without blocking any sensor from taking a reading.

- No starvation, no deadlock
- Wait-free
- Linearizable


