package com.ismartcoding.lib.ahocorasick.interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntervalNode {

    private enum Direction {LEFT, RIGHT}

    private IntervalNode left;
    private IntervalNode right;
    private int point;
    private List<Intervalable> intervals = new ArrayList<>();

    public IntervalNode(final List<Intervalable> intervals) {
        this.point = determineMedian(intervals);

        final List<Intervalable> toLeft = new ArrayList<>();
        final List<Intervalable> toRight = new ArrayList<>();

        for (Intervalable interval : intervals) {
            if (interval.getEnd() < this.point) {
                toLeft.add(interval);
            } else if (interval.getStart() > this.point) {
                toRight.add(interval);
            } else {
                this.intervals.add(interval);
            }
        }

        if (toLeft.size() > 0) {
            this.left = new IntervalNode(toLeft);
        }
        if (toRight.size() > 0) {
            this.right = new IntervalNode(toRight);
        }
    }

    public int determineMedian(final List<Intervalable> intervals) {
        int start = -1;
        int end = -1;
        for (Intervalable interval : intervals) {
            int currentStart = interval.getStart();
            int currentEnd = interval.getEnd();
            if (start == -1 || currentStart < start) {
                start = currentStart;
            }
            if (end == -1 || currentEnd > end) {
                end = currentEnd;
            }
        }
        return (start + end) / 2;
    }

    public List<Intervalable> findOverlaps(final Intervalable interval) {
        final List<Intervalable> overlaps = new ArrayList<>();

        if (this.point < interval.getStart()) {
            // Tends to the right
            addToOverlaps(interval, overlaps, findOverlappingRanges(this.right, interval));
            addToOverlaps(interval, overlaps, checkForOverlapsToTheRight(interval));
        } else if (this.point > interval.getEnd()) {
            // Tends to the left
            addToOverlaps(interval, overlaps, findOverlappingRanges(this.left, interval));
            addToOverlaps(interval, overlaps, checkForOverlapsToTheLeft(interval));
        } else {
            // Somewhere in the middle
            addToOverlaps(interval, overlaps, this.intervals);
            addToOverlaps(interval, overlaps, findOverlappingRanges(this.left, interval));
            addToOverlaps(interval, overlaps, findOverlappingRanges(this.right, interval));
        }

        return overlaps;
    }

    protected void addToOverlaps(
            final Intervalable interval,
            final List<Intervalable> overlaps,
            final List<Intervalable> newOverlaps) {
        for (final Intervalable currentInterval : newOverlaps) {
            if (!currentInterval.equals(interval)) {
                overlaps.add(currentInterval);
            }
        }
    }

    protected List<Intervalable> checkForOverlapsToTheLeft(final Intervalable interval) {
        return checkForOverlaps(interval, Direction.LEFT);
    }

    protected List<Intervalable> checkForOverlapsToTheRight(final Intervalable interval) {
        return checkForOverlaps(interval, Direction.RIGHT);
    }

    protected List<Intervalable> checkForOverlaps(
            final Intervalable interval, final Direction direction) {
        final List<Intervalable> overlaps = new ArrayList<>();

        for (final Intervalable currentInterval : this.intervals) {
            switch (direction) {
                case LEFT:
                    if (currentInterval.getStart() <= interval.getEnd()) {
                        overlaps.add(currentInterval);
                    }
                    break;
                case RIGHT:
                    if (currentInterval.getEnd() >= interval.getStart()) {
                        overlaps.add(currentInterval);
                    }
                    break;
            }
        }

        return overlaps;
    }

    protected List<Intervalable> findOverlappingRanges(IntervalNode node, Intervalable interval) {
        return node == null
                ? Collections.<Intervalable>emptyList()
                : node.findOverlaps(interval);
    }
}
