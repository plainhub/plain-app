package com.ismartcoding.lib.ahocorasick.trie;

import java.util.*;

/**
 * <p>
 * A state has various important tasks it must attend to:
 * </p>
 * <ul>
 * <li>success; when a character points to another state, it must return that
 * state</li>
 * <li>failure; when a character has no matching state, the algorithm must be
 * able to fall back on a state with less depth</li>
 * <li>emits; when this state is passed and keywords have been matched, the
 * matches and their payloads must be 'emitted' so that they can be used later
 * on.</li>
 * </ul>
 * <p>
 * The root state is special in the sense that it has no failure state; it
 * cannot fail. If it 'fails' it will still parse the next character and start
 * from the root node. This ensures that the algorithm always runs. All other
 * states always have a fail state.
 * </p>
 *
 * @author Daniel Beck
 */
public class PayloadState<T> {

    /**
     * effective the size of the keyword
     */
    private final int depth;

    /**
     * only used for the root state to refer to itself in case no matches have been
     * found
     */
    private final PayloadState<T> rootState;

    /**
     * referred to in the white paper as the 'goto' structure. From a state it is
     * possible to go to other states, depending on the character passed.
     */
    private final Map<Character, PayloadState<T>> success = new HashMap<>();

    /**
     * if no matching states are found, the failure state will be returned
     */
    private PayloadState<T> failure;

    /**
     * whenever this state is reached, it will emit the matches keywords for future
     * reference
     */
    private Set<Payload<T>> emits;

    public PayloadState() {
        this(0);
    }

    public PayloadState(final int depth) {
        this.depth = depth;
        this.rootState = depth == 0 ? this : null;
    }

    private PayloadState<T> nextState(final Character character, final boolean ignoreRootState) {
        PayloadState<T> nextState = this.success.get(character);

        if (!ignoreRootState && nextState == null && this.rootState != null) {
            nextState = this.rootState;
        }

        return nextState;
    }

    public PayloadState<T> nextState(final Character character) {
        return nextState(character, false);
    }

    public PayloadState<T> nextStateIgnoreRootState(Character character) {
        return nextState(character, true);
    }

    public PayloadState<T> addState(Character character) {
        PayloadState<T> nextState = nextStateIgnoreRootState(character);
        if (nextState == null) {
            nextState = new PayloadState<>(this.depth + 1);
            this.success.put(character, nextState);
        }
        return nextState;
    }

    public int getDepth() {
        return this.depth;
    }

    /**
     * Adds a payload to be emitted for this state.
     * 
     * @param payload to be emitted.
     */
    public void addEmit(Payload<T> payload) {
        if (this.emits == null) {
            this.emits = new TreeSet<>();
        }
        this.emits.add(payload);
    }

    /**
     * Adds a collection of payloads to be emitted for this state.
     * 
     * @param emits Collection of payloads to be emitted.
     */
    public void addEmit(Collection<Payload<T>> emits) {
        for (Payload<T> emit : emits) {
            addEmit(emit);
        }
    }

    /**
     * Returns a collection of emitted payloads for this state.
     * 
     * @return Collection of emitted payloads.
     */
    public Collection<Payload<T>> emit() {
        return this.emits == null ? Collections.<Payload<T>>emptyList() : this.emits;
    }

    public PayloadState<T> failure() {
        return this.failure;
    }

    public void setFailure(PayloadState<T> failState) {
        this.failure = failState;
    }

    public Collection<PayloadState<T>> getStates() {
        return this.success.values();
    }

    public Collection<Character> getTransitions() {
        return this.success.keySet();
    }
}