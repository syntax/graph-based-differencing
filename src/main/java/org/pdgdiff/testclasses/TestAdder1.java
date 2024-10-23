package org.pdgdiff.testclasses;

public class TestAdder1 {
    public static void main(String[] args) {
        TestAdder1 test = new TestAdder1();
        int result = test.addNumbers(5, 10);
        System.out.println("Result: " + result);
        int res = test.minus(10, 5);
        System.out.println("Result: " + res);

        int complexRes = test.detailedComputation(5, 10);
        System.out.println("Detailed Computation Result: " + complexRes);
        int t = test.identical(5, 10);
        System.out.println("identical Result: " + t);
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
        return  result;
    }

    public int addNumbers(int a, int b) {
        int toadd1 = a;
        int toadd2 = b;
        int sum = toadd1 + toadd2;
        return sum;
    }

    public int minus(int a, int b) {
        int sum = a - b;
        return sum;
    }


    // added these more complex classes with more intense control flow and non-matching names to try and catch edge cases
    public int detailedComputation(int num1, int num2) {
        int result = 0;

        // Conditional statements
        if (num1 > num2) {
            result = num1 + num2;
        } else if (num1 < num2) {
            result = num1 - num2;
        } else {
            result = num1 * num2;
        }

        // Loop that performs additional operations
        for (int i = 0; i < 4; i++) {
            result -= i;
            if (result % 3 == 0) {
                result /= 3;
            } else {
                result += i * 2;
            }
        }

        // Nested conditional inside a loop
        for (int i = 0; i < 6; i++) {
            if (i % 2 == 1) {
                result *= i;
            }
        }

        return result;
    }

}
