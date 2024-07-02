package com.ismartcoding.lib.ahocorasick.trie.handler;

import java.util.List;
import com.ismartcoding.lib.ahocorasick.trie.Emit;

public interface StatefulEmitHandler extends EmitHandler {
    List<Emit> getEmits();
}
