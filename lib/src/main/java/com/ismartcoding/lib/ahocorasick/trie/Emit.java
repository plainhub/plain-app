package com.ismartcoding.lib.ahocorasick.trie;


import com.ismartcoding.lib.ahocorasick.interval.Interval;
import com.ismartcoding.lib.ahocorasick.interval.Intervalable;

/**
 * Responsible for tracking the bounds of matched terms.
 */
public class Emit extends Interval implements Intervalable {
    private final String keyword;

    public Emit(final int start, final int end, final String keyword) {
        super(start, end);
        this.keyword = keyword;
    }

    public String getKeyword() {
        return this.keyword;
    }

    @Override
    public String toString() {
        return super.toString() + "=" + this.keyword;
    }

}
