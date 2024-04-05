import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class MarsRover {
    private static final int NUM_SENSORS = 8;
    private static final int TEMPERATURE_READING_INTERVAL = 50;
    private static int numberOfReadings = 0;
    private static long startTime = System.nanoTime();
    private static long endTime;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long executionTime = (endTime - startTime) / 1000000000;
            System.out.println("\n\nTotal number of readings: " + numberOfReadings);
            System.out.println("Total realtime execution time: " + executionTime + " seconds");
        }));

        SharedMemory sharedMemory = new SharedMemory();
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_SENSORS);

        // Log how to stop the simulation
        System.out.println("\nWelcome to the Mars Rover Temperature Monitoring System!");
        System.out.println("This simulation will run indefinitely. To stop the simulation, press Ctrl+C.");
        System.out.println("1 minute in the simulation is equivalent to " + TEMPERATURE_READING_INTERVAL + " milliseconds in real time.");

        // Create NUM_SENSORS temperature sensor threads
        for (int i = 0; i < NUM_SENSORS; i++) {
            executorService.execute(new TemperatureSensor(sharedMemory, i, TEMPERATURE_READING_INTERVAL));
        }

        // Schedule hourly report generation
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            List<TemperatureReading> temperatureReadings = sharedMemory.getTemperatureReadings();
            // Process temperature readings for the last hour
            processHourlyReport(temperatureReadings, TEMPERATURE_READING_INTERVAL * 10);
        }, TEMPERATURE_READING_INTERVAL * 60, TEMPERATURE_READING_INTERVAL * 60, TimeUnit.MILLISECONDS);
    }

    private static void processHourlyReport(List<TemperatureReading> temperatureReadings, int differenceWindow) {
        // Log starting message
        System.out.println("\n--------------------------------------------");
        System.out.println("\nGenerating hourly report...");
        int size = temperatureReadings.size();
        endTime = System.nanoTime();
        numberOfReadings += size;

        // Sort temperature readings by time
        Collections.sort(temperatureReadings, (a, b) -> Long.compare(a.getTime(), b.getTime()));

        // Calculate the 10-minute interval with the largest temperature difference
        // using sliding window technique
        double maxDiff = Double.MIN_VALUE;
        int startIndex = 0;
        int left = 0;
        int right = 0;
        while (right < size) {
            if (temperatureReadings.get(right).getTime() - temperatureReadings.get(left).getTime() > differenceWindow) {
                if (temperatureReadings.get(right).getTemperature() - temperatureReadings.get(left).getTemperature() > maxDiff) {
                    maxDiff = temperatureReadings.get(right).getTemperature() - temperatureReadings.get(left).getTemperature();
                    startIndex = left;
                }
                left++;
            }
            right++;
        }

        System.out.println(
          "10-minute interval with largest temperature difference: " 
          + temperatureReadings.get(startIndex).getTime() 
          + " to " 
          + temperatureReadings.get(startIndex + 10).getTime()
        );
        System.out.println("Temperature difference: " + maxDiff);

        // Sort temperature readings
        Collections.sort(temperatureReadings, (a, b) -> Double.compare(a.getTemperature(), b.getTemperature()));

        // Calculate top 5 highest temperatures
        List<Double> top5Highest = new ArrayList<>();
        for (int i = size - 1; i >= size - 5; i--) {
            top5Highest.add(temperatureReadings.get(i).getTemperature());
        }

        // Calculate top 5 lowest temperatures
        List<Double> top5Lowest = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            top5Lowest.add(temperatureReadings.get(i).getTemperature());
        }

        // Print the report
        System.out.println("Top 5 Highest Temperatures: " + top5Highest);
        System.out.println("Top 5 Lowest Temperatures: " + top5Lowest);
    }
}

class TemperatureSensor implements Runnable {
    private final SharedMemory sharedMemory;
    private final int id;
    private final int temperatureReadingInterval;

    public TemperatureSensor(SharedMemory sharedMemory, int id, int temperatureReadingInterval) {
        this.sharedMemory = sharedMemory;
        this.id = id;
        this.temperatureReadingInterval = temperatureReadingInterval;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (true) {
            // Generate random temperature reading from -100F to 70F, make it normally distributed
            // 70 - (-100) = 170, so we devide 170 by 6 to get 1 standard deviation, = 28.33
            // Mean = 170 / 2 + (-100) = -15
            double temperature = random.nextGaussian() * 28.33 + (-15);

            if (temperature < -100) {
                temperature = -100;
            } else if (temperature > 70) {
                temperature = 70;
            }
            
            // Get current nanosecond time
            long currentTime = System.nanoTime();

            // Store temperature reading in shared memory
            sharedMemory.storeTemperature(currentTime, temperature);

            try {
                // Simulate temperature reading interval
                Thread.sleep(this.temperatureReadingInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class TemperatureReading {
    private final long time;
    private final double temperature;

    public TemperatureReading(long time, double temperature) {
        this.time = time;
        this.temperature = temperature;
    }

    public long getTime() {
        return time;
    }

    public double getTemperature() {
        return temperature;
    }
}

class SharedMemory {
    private final ConcurrentLinkedQueue<TemperatureReading> temperatureReadings = new ConcurrentLinkedQueue<>();

    public synchronized void storeTemperature(long time, double temperature) {
        temperatureReadings.add(new TemperatureReading(time, temperature));
    }

    public synchronized List<TemperatureReading> getTemperatureReadings() {
        // Get current nanosecond time
        long pollFrom = System.nanoTime() - 60 * 1000000000;

        // Pop all temperature readings from the queue
        List<TemperatureReading> readings = new ArrayList<>();
        while (!temperatureReadings.isEmpty() && temperatureReadings.peek().getTime() < pollFrom ) {
            readings.add(temperatureReadings.poll());
        }

        return readings;
    }
}