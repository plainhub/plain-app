package com.ismartcoding.plain.lib.ahocorasick.trie.handler;

import com.ismartcoding.plain.lib.ahocorasick.trie.PayloadEmit;

public interface PayloadEmitHandler<T> {
    boolean emit(PayloadEmit<T> emit);
}
