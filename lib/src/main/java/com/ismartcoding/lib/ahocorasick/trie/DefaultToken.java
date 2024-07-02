package com.ismartcoding.lib.ahocorasick.trie;

public class DefaultToken extends Token {

    private PayloadToken<String> payloadToken;

    public DefaultToken(PayloadToken<String> payloadToken) {
        super(payloadToken.getFragment());
        this.payloadToken = payloadToken;
    }

    public boolean isMatch() {
        return payloadToken.isMatch();
    }

    public Emit getEmit() {
        PayloadEmit<String> emit = payloadToken.getEmit();
        return new Emit(emit.getStart(), emit.getEnd(), emit.getKeyword());
    }

}
