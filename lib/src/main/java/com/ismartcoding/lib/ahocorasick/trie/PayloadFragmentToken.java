package com.ismartcoding.lib.ahocorasick.trie;

/***
 * Container for a token ("the fragment") that can emit a type of payload.
 * <p>
 * This token indicates a matching search term was not found, so
 * {@link #isMatch()} always returns {@code false}.
 * </p>
 * 
 * @author Daniel Beck
 *
 * @param <T> The Type of the emitted payloads.
 */
public class PayloadFragmentToken<T> extends PayloadToken<T> {

    public PayloadFragmentToken(String fragment) {
        super(fragment);
    }

    @Override
    public boolean isMatch() {
        return false;
    }

    /**
     * Returns null.
     */
    @Override
    public PayloadEmit<T> getEmit() {
        return null;
    }
}
