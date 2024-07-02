package com.ismartcoding.lib.ahocorasick.trie;

public class MatchToken extends Token {

    private final Emit emit;

    public MatchToken(final String fragment, final Emit emit) {
        super(fragment);
        this.emit = emit;

    }

    @Override
    public boolean isMatch() {
        return true;
    }

    @Override
    public Emit getEmit() {
        return this.emit;
    }
}
