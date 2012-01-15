package org.q2.syndicate;

public class DestinationUnreachableException extends Exception {
    public DestinationUnreachableException(String address) {
        super("Destination unreachable: " + address);
    }
}