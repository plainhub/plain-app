package com.ismartcoding.lib.ahocorasick.trie.handler;

import java.util.ArrayList;
import java.util.List;

import com.ismartcoding.lib.ahocorasick.trie.PayloadEmit;

public class DefaultPayloadEmitHandler<T> implements StatefulPayloadEmitHandler<T> {

    private final List<PayloadEmit<T>> emits = new ArrayList<>();

    @Override
    public boolean emit(final PayloadEmit<T> emit) {
        this.emits.add(emit);
        return true;
    }

    @Override
    public List<PayloadEmit<T>> getEmits() {
        return this.emits;
    }
}
