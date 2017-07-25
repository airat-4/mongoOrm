package mongo.orm;

import example.TransgranTransfer;
import example.User;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Айрат Гареев
 * @since 07.07.2017
 */
public class EntityManagerTest {
    private EntityManager entityManager;

    @Before
    public void setUp() throws Exception {
        entityManager = new EntityManager(MongoMockFactory.getMongoDBMock());
    }

    @Test
    public void toDBFormatStringTest() throws Exception {
        Assert.assertEquals("qweqwe", entityManager.toDBFormatString("qweqwe"));
        Assert.assertEquals("qwe_qwe", entityManager.toDBFormatString("qweQwe"));
        Assert.assertEquals("qwe_qwe", entityManager.toDBFormatString("QweQwe"));
        Assert.assertEquals("qwe_qwe_qwe", entityManager.toDBFormatString("qweQweQwe"));
        Assert.assertEquals("qwe_qwe_qwe", entityManager.toDBFormatString("qwe_qwe_qwe"));
        Assert.assertEquals("qwe", entityManager.toDBFormatString("QWE"));
        Assert.assertEquals("qwe_745_qwe", entityManager.toDBFormatString("qwe745qwe"));
        Assert.assertEquals("qw_e_745_qwe", entityManager.toDBFormatString("qwE745Qwe"));
        Assert.assertEquals("qwe_745", entityManager.toDBFormatString("qwe745"));
        Assert.assertEquals("745_qwe", entityManager.toDBFormatString("745qwe"));
        Assert.assertEquals("_id", entityManager.toDBFormatString("ID"));
        Assert.assertEquals("_id", entityManager.toDBFormatString("_id"));
    }

    @Test
    public void insertTest() throws Exception {
        User user = new User();
        user.setName("name");
        entityManager.save(user);
        Assert.assertNotNull(user.getId());
    }

    @Test
    public void updateTest() throws Exception {
        User user = new User();
        user.setName("name");
        entityManager.save(user);
        Assert.assertNotNull(user.getId());
        user.setName("name2");
        entityManager.save(user);
        User newUser = entityManager.findById(user.getId(), User.class);
        Assert.assertEquals("name2", newUser.getName());
    }

    @Test
    public void relationTest(){
        User user = new User();
        user.setName("name");
        TransgranTransfer transfer = new TransgranTransfer();
        transfer.setAmount(5);
        transfer.setFrom(user);
        entityManager.save(transfer);
        ObjectId id = transfer.getId();

        transfer = entityManager.findById(id, TransgranTransfer.class);
        Assert.assertNotNull(transfer.getFrom().getId());
        Assert.assertNull(transfer.getFrom().getName());
        Assert.assertTrue(transfer.getFrom().isStab());
        Assert.assertNull(transfer.getTo());

        transfer = entityManager.findById(id, TransgranTransfer.class, new Config(false));
        Assert.assertNotNull(transfer.getFrom().getId());
        Assert.assertNotNull(transfer.getFrom().getName());
        Assert.assertFalse(transfer.getFrom().isStab());
        Assert.assertNull(transfer.getTo());
        Assert.assertNull(transfer.getOperator());
    }

    @Test
    public void deleteTest() throws Exception {
        User user = new User();
        user.setName("name");
        entityManager.save(user);
        Assert.assertNotNull(user.getId());
        entityManager.delete(user);
        user = entityManager.findById(user.getId(), User.class);
        Assert.assertNull(user);
    }

    @Test
    public void referenceTest() throws Exception {
        User user = new User();
        user.setName("name");
        entityManager.save(user);
        Assert.assertNotNull(user.getId());

        TransgranTransfer transfer = new TransgranTransfer();
        transfer.setOperator(user.getId());
        entityManager.save(transfer);
        transfer = entityManager.findById(transfer.getId(), TransgranTransfer.class);
        Assert.assertEquals(user.getId(), transfer.getOperator());
    }
}