package org.pdgdiff.testclasses;

public class TestAdder2 {
    public static void main(String[] args) {
        TestAdder2 test = new TestAdder2();
        int number1 = 5;
        int number2 = 4;
        int result = test.addNumbers(number1, number2);
        System.out.println("Result: " + result);
    }

    public int addNumbers(int number, int number2) {
        int sum = number + number2;
        return sum;
    }
}
