package com.ismartcoding.lib.ahocorasick.trie;

/**
 * Contains the matched keyword and some payload data.
 * 
 * @author Daniel Beck
 * @param <T> The type of the wrapped payload data.
 */
public class Payload<T> implements Comparable<Payload<T>> {

    private final String keyword;
    private final T data;

    public Payload(final String keyword, final T data) {
        super();
        this.keyword = keyword;
        this.data = data;
    }

    public String getKeyword() {
        return keyword;
    }

    public T getData() {
        return data;
    }

    @Override
    public int compareTo(Payload<T> other) {
        return keyword.compareTo(other.getKeyword());
    }
}
