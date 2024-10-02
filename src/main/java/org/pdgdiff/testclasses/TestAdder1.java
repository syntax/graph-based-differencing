package org.pdgdiff.testclasses;

public class TestAdder1 {
    public static void main(String[] args) {
        TestAdder1 test = new TestAdder1();
        int result = test.addNumbers(5, 10);
        System.out.println("Result: " + result);
        int res = test.minus(10, 5);
        System.out.println("Result: " + res);
    }

    public int addNumbers(int a, int b) {
        int toadd1 = a;
        int toadd2 = b;
        int sum = toadd1 + toadd2;
        int minus = toadd1 - toadd2;
        return sum;
    }

    public int minus(int a, int b) {
        int sum = a - b;
        return sum;
    }
}
