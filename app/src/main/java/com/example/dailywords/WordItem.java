package com.example.dailywords;

public class WordItem {
    private String word;
    private String meaning;

    public WordItem(String word, String meaning) {
        this.word = word;
        this.meaning = meaning;
    }

    public String getWord() { return word; }
    public String getMeaning() { return meaning; }

    public WordItem() {}
}
