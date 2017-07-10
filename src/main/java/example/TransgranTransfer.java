package example;

import mongo.orm.DTO;

/**
 * Created by airat on 10.07.17.
 */
public class TransgranTransfer extends DTO {

    private User from;
    private User to;
    private Integer amount;

    public User getFrom() {
        return from;
    }

    public void setFrom(User from) {
        this.from = from;
    }

    public User getTo() {
        return to;
    }

    public void setTo(User to) {
        this.to = to;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
