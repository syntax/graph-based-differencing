package org.example.testclasses;

public class TestClass {
    public static void main(String[] args) {
        TestClass test = new TestClass();
        int result = test.addNumbers(5, 10);
        System.out.println("Result: " + result);
    }

    public int addNumbers(int a, int b) {
        int sum = a + b;
        return sum;
    }
}
