package com.ismartcoding.lib.ahocorasick.trie.handler;

import com.ismartcoding.lib.ahocorasick.trie.Emit;
import com.ismartcoding.lib.ahocorasick.trie.PayloadEmit;

/**
 * Convenience wrapper class that delegates every method to an
 * instance of {@link EmitHandler}.
 */
public class PayloadEmitDelegateHandler implements PayloadEmitHandler<String> {

    private EmitHandler handler;

    public PayloadEmitDelegateHandler(EmitHandler handler) {
        this.handler = handler;

    }

    @Override
    public boolean emit(PayloadEmit<String> emit) {
        Emit newEmit = new Emit(emit.getStart(), emit.getEnd(), emit.getKeyword());
        return handler.emit(newEmit);
    }

}
