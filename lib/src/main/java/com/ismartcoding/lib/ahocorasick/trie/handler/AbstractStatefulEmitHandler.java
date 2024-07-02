package com.ismartcoding.lib.ahocorasick.trie.handler;

import com.ismartcoding.lib.ahocorasick.trie.Emit;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractStatefulEmitHandler implements StatefulEmitHandler {

    private final List<Emit> emits = new ArrayList<Emit>();

    public void addEmit(final Emit emit) {
        this.emits.add(emit);
    }

    @Override
    public List<Emit> getEmits() {
        return this.emits;
    }

}
