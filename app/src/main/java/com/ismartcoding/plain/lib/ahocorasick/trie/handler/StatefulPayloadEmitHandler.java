package com.ismartcoding.plain.lib.ahocorasick.trie.handler;

import com.ismartcoding.plain.lib.ahocorasick.trie.PayloadEmit;

import java.util.List;

public interface StatefulPayloadEmitHandler<T> extends PayloadEmitHandler<T>{
    List<PayloadEmit<T>> getEmits();
}
