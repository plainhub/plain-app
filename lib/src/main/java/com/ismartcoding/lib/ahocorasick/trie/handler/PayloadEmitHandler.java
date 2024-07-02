package com.ismartcoding.lib.ahocorasick.trie.handler;

import com.ismartcoding.lib.ahocorasick.trie.PayloadEmit;

public interface PayloadEmitHandler<T> {
    boolean emit(PayloadEmit<T> emit);
}
