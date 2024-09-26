package org.example.testclasses;

public class AnotherClass {

    public void printMessage(String message) {
        if (message == null) {
            System.out.println("No message provided.");
        } else {
            System.out.println("Message: " + message);
        }
    }
}
