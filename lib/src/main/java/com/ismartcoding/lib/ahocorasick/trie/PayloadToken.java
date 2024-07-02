package com.ismartcoding.lib.ahocorasick.trie;

/***
 * PayloadToken holds a text ("the fragment") an emits some output. If
 * {@link #isMatch()} returns {@code true}, the token matched a search term.
 *
 * @author Daniel Beck
 *
 * @param <T> The Type of the emitted payloads.
 */
public abstract class PayloadToken<T> {
    private String fragment;

    public PayloadToken(String fragment) {
        this.fragment = fragment;
    }

    public String getFragment() {
        return this.fragment;
    }

    /**
     * Return {@code true} if a search term matched.
     * @return {@code true} if this is a match
     */
    public abstract boolean isMatch();

    /**
     * @return the payload
     */
    public abstract PayloadEmit<T> getEmit();
}
