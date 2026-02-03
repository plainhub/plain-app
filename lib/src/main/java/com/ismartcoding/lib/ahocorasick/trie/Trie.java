package com.ismartcoding.lib.ahocorasick.trie;

import java.util.ArrayList;
import java.util.Collection;

import com.ismartcoding.lib.ahocorasick.trie.PayloadTrie.PayloadTrieBuilder;
import com.ismartcoding.lib.ahocorasick.trie.handler.EmitHandler;
import com.ismartcoding.lib.ahocorasick.trie.handler.StatefulPayloadEmitDelegateHandler;
import com.ismartcoding.lib.ahocorasick.trie.handler.PayloadEmitDelegateHandler;
import com.ismartcoding.lib.ahocorasick.trie.handler.StatefulEmitHandler;

/**
 * Based on the <a href="http://cr.yp.to/bib/1975/aho.pdf">Aho-Corasick white
 * paper</a>, from Bell technologies.
 *
 * @author Robert Bor
 */
public class Trie {

    private final PayloadTrie<String> payloadTrie;

    private Trie(final PayloadTrie<String> payloadTrie) {
        this.payloadTrie = payloadTrie;
    }

    public Collection<Token> tokenize(final String text) {
        Collection<PayloadToken<String>> tokens = this.payloadTrie.tokenize(text);
        return asTokens(tokens);
    }

    private static Collection<Token> asTokens(Collection<PayloadToken<String>> tokens) {
        Collection<Token> result = new ArrayList<>();
        for (PayloadToken<String> payloadToken : tokens) {
            result.add(new DefaultToken(payloadToken));
        }
        return result;
    }

    private static Collection<Emit> asEmits(Collection<PayloadEmit<String>> emits) {
        Collection<Emit> result = new ArrayList<>();
        for (PayloadEmit<String> emit : emits) {
            result.add(asEmit(emit));
        }
        return result;
    }

    private static Emit asEmit(PayloadEmit<String> payloadEmit) {
        return new Emit(payloadEmit.getStart(), payloadEmit.getEnd(), payloadEmit.getKeyword());
    }

    public Collection<Emit> parseText(final CharSequence text) {
        Collection<PayloadEmit<String>> parsedText = this.payloadTrie.parseText(text);
        return asEmits(parsedText);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Collection<Emit> parseText( final CharSequence text, final StatefulEmitHandler emitHandler) {
        Collection<PayloadEmit<String>> parsedText = this.payloadTrie.parseText(text,
                new StatefulPayloadEmitDelegateHandler(emitHandler));
        return asEmits(parsedText);
    }

    public boolean containsMatch(final CharSequence text) {
        return firstMatch(text) != null;
    }

    public void parseText(final CharSequence text, final EmitHandler emitHandler) {
        this.payloadTrie.parseText(text, new PayloadEmitDelegateHandler(emitHandler));
    }

    /**
     * The first matching text sequence.
     *
     * @param text The text to search for keywords, must not be {@code null}.
     * @return {@code null} if no matches found.
     */
    public Emit firstMatch(final CharSequence text) {
        assert text != null;

        final PayloadEmit<String> payload = this.payloadTrie.firstMatch( text );
        return payload == null
          ? null
          : new Emit( payload.getStart(),
                      payload.getEnd(),
                      payload.getKeyword() );
    }

    /**
     * Provides a fluent interface for constructing Trie instances.
     *
     * @return The builder used to configure its Trie.
     */
    public static TrieBuilder builder() {
        return new TrieBuilder();
    }

    public static class TrieBuilder {

        private final PayloadTrieBuilder<String> delegate = PayloadTrie.builder();

        /**
         * Default (empty) constructor.
         */
        private TrieBuilder() {
        }

        /**
         * Configure the Trie to ignore case when searching for keywords in the text.
         * This must be called before calling addKeyword because the algorithm converts
         * keywords to lowercase as they are added, depending on this case sensitivity
         * setting.
         *
         * @return This builder.
         */
        public TrieBuilder ignoreCase() {
            delegate.ignoreCase();
//            this.trieConfig.setCaseInsensitive(true);
            return this;
        }

        /**
         * Configure the Trie to ignore overlapping keywords.
         *
         * @return This builder.
         */
        public TrieBuilder ignoreOverlaps() {
            delegate.ignoreOverlaps();
            return this;
        }

        /**
         * Adds a keyword to the Trie's list of text search keywords.
         *
         * @param keyword The keyword to add to the list.
         * @return This builder.
         * @throws NullPointerException if the keyword is null.
         */
        public TrieBuilder addKeyword(final String keyword) {
            delegate.addKeyword(keyword, null);
            return this;
        }

        /**
         * Adds a list of keywords to the Trie's list of text search keywords.
         *
         * @param keywords The keywords to add to the list.
         * @return This builder.
         */
        public TrieBuilder addKeywords(final String... keywords) {
            for (String keyword : keywords) {
                delegate.addKeyword(keyword, null);
            }
            return this;
        }

        /**
         * Adds a list of keywords to the Trie's list of text search keywords.
         *
         * @param keywords The keywords to add to the list.
         * @return This builder.
         */
        @SuppressWarnings("unused")
        public TrieBuilder addKeywords( final Collection<String> keywords ) {
            for (String keyword : keywords) {
                this.delegate.addKeyword(keyword, null);
            }
            return this;
        }

        /**
         * Configure the Trie to match whole keywords in the text.
         *
         * @return This builder.
         */
        public TrieBuilder onlyWholeWords() {
            this.delegate.onlyWholeWords();
            return this;
        }

        /**
         * Configure the Trie to match whole keywords that are separated by whitespace
         * in the text. For example, "this keyword thatkeyword" would only match the
         * first occurrence of "keyword".
         *
         * @return This builder.
         */
        public TrieBuilder onlyWholeWordsWhiteSpaceSeparated() {
            this.delegate.onlyWholeWordsWhiteSpaceSeparated();
            return this;
        }

        /**
         * Configure the Trie to stop after the first keyword is found in the text.
         *
         * @return This builder.
         */
        public TrieBuilder stopOnHit() {
            this.delegate.stopOnHit();
            return this;
        }

        /**
         * Configure the Trie based on the builder settings.
         *
         * @return The configured Trie.
         */
        public Trie build() {
            PayloadTrie<String> payloadTrie = this.delegate.build();
            return new Trie(payloadTrie);
        }

        /**
         * @return This builder.
         * @deprecated Use ignoreCase()
         */
        @Deprecated
        public TrieBuilder caseInsensitive() {
            return ignoreCase();
        }

        /**
         * @return This builder.
         * @deprecated Use ignoreOverlaps()
         */
        @Deprecated
        public TrieBuilder removeOverlaps() {
            return ignoreOverlaps();
        }
    }
}
