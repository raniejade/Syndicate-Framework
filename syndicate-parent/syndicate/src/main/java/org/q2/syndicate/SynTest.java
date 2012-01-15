package org.q2.syndicate;


public class SynTest {
    private static SyndicateCore synCore = SyndicateCore.getInstance();

    public static void main(String[] args) {
        synCore.initialize();
        while (true) ;
    }
}
