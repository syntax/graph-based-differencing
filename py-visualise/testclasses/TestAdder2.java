package org.pdgdiff.testclasses;

public class TestAdder2 {

    private int another;

    public static void main(String[] args) {
        TestAdder2 test = new TestAdder2();
        int number1 = 5;
        int number2 = 4;
        int result = test.addNumbers(number1, number2);
        System.out.println("Result: " + result);
        int product = test.multiplyNumbers(number1, number2);
        System.out.println("Product: " + product);



        int complexResult = test.complexCalculation(number1, number2);
        int meep = test.identical(3,10);
        System.out.println("Complex Calculation Result: " + complexResult);
        System.out.println("identical Result: " + meep);
    }

    public int addNumbers(int number, int number2) {
        int sum = number - number2;
        return sum;
    }

    public int multiplyNumbers(int number, int number2) {
        int product = number * number2;
        return product;
    }

    // added these more complex classes with more intense control flow and non-matching names to try and catch edge cases
    public int complexCalculation(int num1, int num2) {
        int result = 0;

        // Conditional statements
        if (num1 > num2) {
            result = num1 - num2;
        } else if (num1 < num2) {
            result = num1 + num2;
        } else {
            result = num1 * num2;
        }

        // Loop that performs additional operations
        for (int i = 0; i < 3; i++) {
            result += i;
            if (result % 2 == 0) {
                result /= 2;
            } else {
                result += 3;
            }
        }

        return result;
    }

    public int identical(int num1, int num2) {
        int result = 0;

        // Conditional statements
        if (num1 > num2) {
            result = num1 + num2;
        } else if (num1 < num2) {
            result = num1 - num2;
        } else {
            result = num1 * num2;
        }
        return result;
    }
}
