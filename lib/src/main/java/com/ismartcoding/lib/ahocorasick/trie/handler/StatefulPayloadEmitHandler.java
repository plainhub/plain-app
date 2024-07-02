package com.ismartcoding.lib.ahocorasick.trie.handler;

import com.ismartcoding.lib.ahocorasick.trie.PayloadEmit;

import java.util.List;

public interface StatefulPayloadEmitHandler<T> extends PayloadEmitHandler<T>{
    List<PayloadEmit<T>> getEmits();
}
