package com.ismartcoding.lib.ahocorasick.interval;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.sort;

public class IntervalTree {

    private final IntervalNode rootNode;

    public IntervalTree(List<Intervalable> intervals) {
        this.rootNode = new IntervalNode(intervals);
    }

    public List<Intervalable> removeOverlaps(final List<Intervalable> intervals) {

        // Sort the intervals on size, then left-most position
        sort(intervals, new IntervalableComparatorBySize());

        final Set<Intervalable> removeIntervals = new TreeSet<>();

        for (final Intervalable interval : intervals) {
            // If the interval was already removed, ignore it
            if (removeIntervals.contains(interval)) {
                continue;
            }

            // Remove all overallping intervals
            removeIntervals.addAll(findOverlaps(interval));
        }

        // Remove all intervals that were overlapping
        for (final Intervalable removeInterval : removeIntervals) {
            intervals.remove(removeInterval);
        }

        // Sort the intervals, now on left-most position only
        sort(intervals, new IntervalableComparatorByPosition());

        return intervals;
    }

    public List<Intervalable> findOverlaps(final Intervalable interval) {
        return rootNode.findOverlaps(interval);
    }

}
