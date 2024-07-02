package com.ismartcoding.lib.ahocorasick.trie;

/**
 * Container for a token ("the fragment") that can emit a type of payload.
 * <p>
 * This token indicates a matching search term was found, so {@link #isMatch()}
 * always returns {@code true}.
 * </p>
 * 
 * @author Daniel Beck
 *
 * @param <T> The Type of the emitted payloads.
 */
public class PayloadMatchToken<T> extends PayloadToken<T> {

    private final PayloadEmit<T> emit;

    public PayloadMatchToken(final String fragment, final PayloadEmit<T> emit) {
        super(fragment);
        this.emit = emit;
    }

    @Override
    public boolean isMatch() {
        return true;
    }

    @Override
    public PayloadEmit<T> getEmit() {
        return this.emit;
    }
}
