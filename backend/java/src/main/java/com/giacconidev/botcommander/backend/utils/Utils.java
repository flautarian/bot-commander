package com.giacconidev.botcommander.backend.utils;

public class Utils {
    public static String generateRandomName() {
        String[] adjectives = {"Quick", "Lazy", "Happy", "Sad", "Brave", "Clever", "Silly", "Wise"};
        String[] nouns = {"Fox", "Dog", "Cat", "Bear", "Lion", "Tiger", "Elephant", "Monkey"};

        int randomAdjectiveIndex = (int) (Math.random() * adjectives.length);
        int randomNounIndex = (int) (Math.random() * nouns.length);

        return adjectives[randomAdjectiveIndex] + nouns[randomNounIndex];
    }
}
