package com.ismartcoding.lib.ahocorasick.interval;


/**
 * Responsible for tracking the start and end bounds, which are reused by
 * both {@link Emit} and {@link PayloadEmit}.
 */
public class Interval implements Intervalable {

    private final int start;
    private final int end;

    /**
     * Constructs an interval with a start and end position.
     *
     * @param start The interval's starting text position.
     * @param end   The interval's ending text position.
     */
    public Interval(final int start, final int end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the starting offset into the text for this interval.
     *
     * @return A number between 0 (start of text) and the text length.
     */
    @Override
    public int getStart() {
        return this.start;
    }

    /**
     * Returns the ending offset into the text for this interval.
     *
     * @return A number between getStart() + 1 and the text length.
     */
    @Override
    public int getEnd() {
        return this.end;
    }

    /**
     * Returns the length of the interval.
     *
     * @return The end position less the start position, plus one.
     */
    @Override
    public int size() {
        return end - start + 1;
    }

    /**
     * Answers whether the given interval overlaps this interval
     * instance.
     *
     * @param other the other interval to check for overlap
     * @return true The intervals overlap.
     */
    public boolean overlapsWith(final Interval other) {
        return this.start <= other.getEnd() &&
                this.end >= other.getStart();
    }

    public boolean overlapsWith(int point) {
        return this.start <= point && point <= this.end;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Intervalable)) {
            return false;
        }
        Intervalable other = (Intervalable) o;
        return this.start == other.getStart() &&
                this.end == other.getEnd();
    }

    @Override
    public int hashCode() {
        return this.start % 100 + this.end % 100;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof Intervalable)) {
            return -1;
        }
        Intervalable other = (Intervalable) o;
        int comparison = this.start - other.getStart();
        return comparison != 0 ? comparison : this.end - other.getEnd();
    }

    /**
     * Returns the starting offset and ending offset separated
     * by a full colon (:).
     *
     * @return A non-null String, never empty.
     */
    @Override
    public String toString() {
        return this.start + ":" + this.end;
    }
}
