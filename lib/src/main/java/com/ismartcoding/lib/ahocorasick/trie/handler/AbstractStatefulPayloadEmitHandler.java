package com.ismartcoding.lib.ahocorasick.trie.handler;

import java.util.ArrayList;
import java.util.List;

import com.ismartcoding.lib.ahocorasick.trie.PayloadEmit;

public abstract class AbstractStatefulPayloadEmitHandler<T> implements StatefulPayloadEmitHandler<T> {

    private final List<PayloadEmit<T>> emits = new ArrayList<>();

    public void addEmit(final PayloadEmit<T> emit) {
        this.emits.add(emit);
    }

    @Override
    public List<PayloadEmit<T>> getEmits() {
        return this.emits;
    }

}
