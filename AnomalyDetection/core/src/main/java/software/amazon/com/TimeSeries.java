package software.amazon.com;

import java.io.Serializable;
import java.util.ArrayList;

public class TimeSeries implements Serializable {

    private int size;
    private final int maxSize;
    private final int sequenceLength;
    private final int initializationPeriods;

    private final ArrayList<Double> contextWindow;

    private int currentIndex;

    public TimeSeries(int contextWindowSize, int sequenceLength, int initializationPeriods) {
        this.size = 0;
        this.sequenceLength = sequenceLength;
        this.maxSize = contextWindowSize*sequenceLength;
        this.initializationPeriods = initializationPeriods;

        this.contextWindow = new ArrayList<>(maxSize);

        for(int i = 0; i < maxSize; i++) {
            contextWindow.add(null);
        }

        this.currentIndex = 0;
    }

    private int getIndex(int currentIndex, int step) {
        return (currentIndex + maxSize + step) % maxSize;
    }

    /**
     * Adds an element to the TimeSeries.
     * If the contextWindow is full, overrides the oldest record.
     *
     * @param element the element to be added
     */
    public void add(Double element) {
        contextWindow.set(currentIndex, element);

        currentIndex = getIndex(currentIndex, 1);

        size = Math.min(size + 1, maxSize);
    }

    private int size() {
        return size;
    }

    public Boolean readyToCompute() {
        return size() > 2 * sequenceLength;
    }

    public Boolean readyToInfer() {
        return size() > initializationPeriods * sequenceLength;
    }

    /**
     * Finds the distance from the closest non-self-matching subsequence
     * in the context window and the current subsequence.
     *
     * @return the distance to the closest neighbour
     */
    public Double computeNearestNeighbourDistance() {
        double minDistance = Double.MAX_VALUE;

        int prevNonSelfMatching = getIndex(currentIndex, -(2*sequenceLength));

        while (prevNonSelfMatching != currentIndex) {
            Double currDistance = computeSquaredDistance(prevNonSelfMatching);
            minDistance = Math.min(currDistance, minDistance);

            prevNonSelfMatching = getIndex(prevNonSelfMatching, -1);
        }

        return minDistance;
    }

    /**
     * Computes the Euclidean distance between the last subsequence of length sequenceLength
     * and the subsequence of length sequenceLength starting in startingIndex
     *
     * @return the distance between the two subsequences
     */
    private Double computeSquaredDistance(int startingIndex) {
        double squaredDistance = 0.0;

        int otherVectorIndex = startingIndex;
        int currentVectorIndex = getIndex(currentIndex, -sequenceLength);

        for (int i = 0; i < sequenceLength && contextWindow.get(otherVectorIndex) != null; i++) {
            Double difference = contextWindow.get(otherVectorIndex) - contextWindow.get(currentVectorIndex);
            squaredDistance += difference * difference;

            otherVectorIndex = getIndex(otherVectorIndex, 1);
            currentVectorIndex = getIndex(currentVectorIndex, 1);
        }

        return squaredDistance;
    }
}