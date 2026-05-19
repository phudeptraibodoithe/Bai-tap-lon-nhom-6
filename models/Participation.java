package models;

public class Participation {
    private String id;
    private User user;
    private AuctionSession session;
    private TransactionRole roleBehavior;

    public Participation(String id, User user, AuctionSession session, TransactionRole roleBehavior) {
        this.id = id;
        this.user = user;
        this.session = session;
        this.roleBehavior = roleBehavior;
    }

    public void executeAction(double amount) {
        this.roleBehavior.execute(this.user, this.session, amount);
    }
}