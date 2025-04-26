import java.util.Random;

public abstract class Main {
    private static int[] array;

    private static int minElement;
    private static int minIndex;
    private static int completedThreads;

    private static final Random random = new Random();
    private static final Object lockObject = new Object();
    private static final Object completionLock = new Object();

    public static void main(String[] args) {
        int[] threadCounts = {Runtime.getRuntime().availableProcessors()};

        for (int threadCount : threadCounts) {
            System.out.println("\nRunning with " + threadCount + " threads:\n");
            findMinElement(threadCount);

            synchronized (completionLock) {
                while (completedThreads < threadCount) {
                    try {
                        completionLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static void findMinElement(int threadCount) {
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

            threads[i] = new Thread(() -> findMinInRange(startIndex, endIndex, threadCount));
            threads[i].start();
        }
    }

    private static void findMinInRange(int startIndex, int endIndex, int threadCount) {
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

            if (completedThreads == threadCount) {
                System.out.println("Minimum element: " + minElement);
                System.out.println("Index of minimum element: " + minIndex);
            }
        }
    }
}
