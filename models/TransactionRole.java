package models;

public interface TransactionRole {
    boolean execute(User user, AuctionSession session, double amount);
}