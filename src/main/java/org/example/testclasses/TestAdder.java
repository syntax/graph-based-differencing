package org.example.testclasses;

public class TestAdder {
    public static void main(String[] args) {
        TestAdder test = new TestAdder();
        int result = test.addNumbers(5, 10);
        System.out.println("Result: " + result);
    }

    public int addNumbers(int a, int b) {
        int sum = a + b;
        return sum;
    }
}
