package example;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import mongo.orm.EntityManager;

import java.util.List;

/**
 * Created by airat on 10.07.17.
 */
public class Main {
    public static void main(String[] args) {
        Mongo mongo = new MongoClient();
        EntityManager entityManager = new EntityManager(mongo.getDB("transfer"));
        User user = new User();
        user.setName("andrey");

        User airat = new User();
        airat.setName("airat");

        TransgranTransfer transfer = new TransgranTransfer();
        transfer.setFrom(airat);
        transfer.setFrom(user);
        transfer.setAmount(60);


        entityManager.save(transfer);
        List<TransgranTransfer> all = entityManager.findAll(TransgranTransfer.class);

    }
}
