import java.util.Random;

public class Main {
    private int[] array;

    private int minElement;
    private int minIndex;
    private int completedThreads;

    private final Random random = new Random();
    private final Object lockObject = new Object();
    private final Object completionLock = new Object();

    public static void main(String[] args) {
        Main main = new Main();
        int[] threadCounts = {4};

        for (int threadCount : threadCounts) {
            System.out.println("\nRunning with " + threadCount + " threads:\n");
            main.findMinElement(threadCount);

            synchronized (main.completionLock) {
                while (main.completedThreads < threadCount) {
                    try {
                        main.completionLock.wait();

                        if (main.completedThreads == threadCount) {
                            System.out.println("Minimum element: " + main.minElement);
                            System.out.println("Index of minimum element: " + main.minIndex);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void findMinElement(int threadCount) {
        int arraySize = 10_000_000;
        array = new int[arraySize];

        for (int i = 0; i < arraySize; i++) {
            array[i] = random.nextInt(1_000_000) + 1;
        }

        int randomIndex = random.nextInt(arraySize);
        array[randomIndex] = -random.nextInt(1_000) - 1;

        minElement = Integer.MAX_VALUE;
        minIndex = -1;
        completedThreads = 0;

        Thread[] threads = new Thread[threadCount];
        int chunkSize = arraySize / threadCount;

        for (int i = 0; i < threadCount; i++) {
            int startIndex = i * chunkSize;
            int endIndex = (i == threadCount - 1) ? arraySize : startIndex + chunkSize;

            threads[i] = new Thread(() -> findMinInRange(startIndex, endIndex));
            threads[i].start();
        }
    }

    private void findMinInRange(int startIndex, int endIndex) {
        int localMin = Integer.MAX_VALUE;
        int localMinIndex = -1;

        for (int i = startIndex; i < endIndex; i++) {
            if (array[i] < localMin) {
                localMin = array[i];
                localMinIndex = i;
            }
        }

        synchronized (lockObject) {
            if (localMin < minElement) {
                minElement = localMin;
                minIndex = localMinIndex;
            }
        }

        synchronized (completionLock) {
            completedThreads++;
            completionLock.notify();
        }
    }
}
