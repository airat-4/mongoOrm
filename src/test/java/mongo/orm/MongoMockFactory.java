package mongo.orm;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Айрат Гареев
 * @since 24.07.2017
 */
public class MongoMockFactory {

    private static final com.mongodb.DB DB;
    private static final MockMongo MOCK_MONGO;

    static {
        DB = mock(com.mongodb.DB.class);
        MOCK_MONGO = new MockMongo();

        when(DB.getCollection(any(String.class))).thenAnswer(invocationOnMock -> {
            String collectionName = invocationOnMock.getArgumentAt(0, String.class);
            DBCollection dbCollection = mock(DBCollection.class);
            when(dbCollection.getName()).thenReturn(collectionName);
            when(dbCollection.insert(any(DBObject[].class))).thenAnswer((invocation -> {
                String name = ((DBCollection) invocation.getMock()).getName();
                MOCK_MONGO.insert(name, (DBObject) invocation.getArguments()[0]);
                return null;
            }));
            when(dbCollection.update(any(DBObject.class), any(DBObject.class))).thenAnswer((invocation -> {
                String name = ((DBCollection) invocation.getMock()).getName();
                MOCK_MONGO.update(name, invocation.getArgumentAt(0, DBObject.class), invocation.getArgumentAt(1, DBObject.class));
                return null;
            }));
            when(dbCollection.find(any(DBObject.class))).thenAnswer((invocation -> {
                String name = ((DBCollection) invocation.getMock()).getName();
                List<DBObject> dbObjects = MOCK_MONGO.find(name, invocation.getArgumentAt(0, DBObject.class));
                DBCursor mock = mock(DBCursor.class);
                when(mock.iterator()).thenReturn(dbObjects.iterator());
                return mock;
            }));

            when(dbCollection.remove(any(DBObject.class))).thenAnswer((invocation -> {
                String name = ((DBCollection) invocation.getMock()).getName();
                List<DBObject> dbObjects = MOCK_MONGO.find(name, invocation.getArgumentAt(0, DBObject.class));
                if(!dbObjects.isEmpty()){
                    MOCK_MONGO.remove(name, dbObjects.get(0));
                }
                return null;
            }));
            return dbCollection;
        });
    }


    public static com.mongodb.DB getMongoDBMock() {
        return DB;
    }

}

class MockMongo {
    private static final String MONGO_ID = "_id";
    private ConcurrentHashMap<String, List<DBObject>> mongoMock = new ConcurrentHashMap<>();

    void insert(String collectionName, DBObject[] dbObjects) {
        for (DBObject dbObject : dbObjects) {
            insert(collectionName, dbObject);
        }
    }

    void insert(String collectionName, DBObject dbObject) {
        List<DBObject> collection = getCollection(collectionName);
        dbObject.put(MONGO_ID, new ObjectId());
        collection.add(dbObject);
    }

    private List<DBObject> getCollection(String collectionName) {
        List<DBObject> collection = mongoMock.get(collectionName);
        if (collection == null) {
            collection = new ArrayList<>();
            mongoMock.put(collectionName, collection);
        }
        return collection;
    }

    void update(String collectionName, DBObject query, DBObject newObject) {
        DBObject dbObject = find(collectionName, query).get(0);
        newObject.put(MONGO_ID, dbObject.get(MONGO_ID));
        remove(collectionName, dbObject);
        getCollection(collectionName).add(newObject);
    }

     void remove(String collectionName, DBObject object) {
        getCollection(collectionName).remove(object);
    }

    List<DBObject> find(String collectionName, DBObject query) {
        List<DBObject> find = new ArrayList<>();
        List<DBObject> collection = getCollection(collectionName);
        for (DBObject dbObject : collection) {
            if (is(dbObject, query)) {
                find.add(dbObject);
            }
        }
        return find;
    }

    private boolean is(DBObject dbObject, DBObject query) {
        if(query == null){
            return true;
        }
        for (String key : query.keySet()) {
            if((query.get(key) instanceof DBObject && ((Collection)((DBObject) query.get(key)).get("$in")).contains(dbObject.get(key)))){
                continue;
            }
            if (!query.get(key).equals(dbObject.get(key))) {
                return false;
            }
        }
        return true;
    }
}