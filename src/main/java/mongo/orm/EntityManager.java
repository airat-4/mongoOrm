package mongo.orm;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Айрат Гареев
 * @since 07.07.2017
 */
public class EntityManager {

    private static final Map<Class<? extends DTO>, Map<String, String>> GETTER_TO_DB_STRING = new HashMap<>();
    private static final Map<Class<? extends DTO>, Map<String, String>> SETTER_TO_DB = new HashMap<>();
    private static final Map<Class<? extends DTO>, Map<String, Class<? extends DTO>>> SETTER_TO_DTO = new HashMap<>();
    private static final String MONGO_ID = "_id";
    private static final Config DEFAULT_CONFIG = new Config();
    private static final Config EAGER_CONFIG = new Config(false);

    private DB db;

    public EntityManager(DB db) {
        this.db = db;
    }

    public void save(DTO dto) {
        if (dto.isStab()) {
            return;
        }
        Map<String, String> getterToDBString = getGetterToDBString(dto.getClass());
        for (DTO curDto : getAllInnerDto(dto, getterToDBString)) {
            save(curDto);
        }
        if (dto.getId() == null) {
            create(dto, getterToDBString);
        } else {
            update(dto, getterToDBString);
        }
    }

    private void update(DTO dto, Map<String, String> getterToDBString) {
        BasicDBObject basicDBObject = toBasicDBObject(dto, getterToDBString);
        getCollection(dto.getClass()).update(new BasicDBObject(MONGO_ID, basicDBObject.getObjectId(MONGO_ID)), basicDBObject);
    }

    private List<DTO> getAllInnerDto(DTO dto, Map<String, String> getterToDBString) {
        List<DTO> innerDto = null;
        for (Map.Entry<String, String> entry : getterToDBString.entrySet()) {
            Object value = getValue(dto, entry.getKey());
            if (value != null && value instanceof DTO && !((DTO) value).isStab()) {
                if (innerDto == null) {
                    innerDto = new ArrayList<>();
                }
                innerDto.add((DTO) value);
            }
        }
        return innerDto != null ? innerDto : Collections.emptyList();
    }

    private void create(DTO dto, Map<String, String> getterToDBString) {
        BasicDBObject basicDBObject = toBasicDBObject(dto, getterToDBString);
        getCollection(dto.getClass()).insert(basicDBObject);
        dto.setId(basicDBObject.getObjectId(MONGO_ID));
    }

    private BasicDBObject toBasicDBObject(DTO dto, Map<String, String> getterToDBString) {
        BasicDBObject basicDBObject = new BasicDBObject();
        for (Map.Entry<String, String> entry : getterToDBString.entrySet()) {
            Object value = getValue(dto, entry.getKey());
            if (value != null) {
                if (value instanceof DTO) {
                    basicDBObject.append(entry.getValue(), ((DTO) value).getId());
                } else {
                    basicDBObject.append(entry.getValue(), value);
                }
            }
        }
        return basicDBObject;
    }

    private Object getValue(DTO dto, String getterName) {
        Class<? extends DTO> clazz = dto.getClass();
        try {
            Method method = clazz.getMethod(getterName);
            return method.invoke(dto);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getGetterToDBString(Class<? extends DTO> clazz) {
        Map<String, String> dtoToCollectionMap = GETTER_TO_DB_STRING.get(clazz);
        if (dtoToCollectionMap == null) {
            dtoToCollectionMap = new HashMap<>();
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("get") && !method.isAnnotationPresent(NoPersist.class) && !method.getName().equals("getClass")) {
                    dtoToCollectionMap.put(method.getName(), toDBFormatString(method.getName().substring(3)));
                }
                if (method.getName().startsWith("is") && !method.isAnnotationPresent(NoPersist.class)) {
                    dtoToCollectionMap.put(method.getName(), toDBFormatString(method.getName().substring(2)));
                }
            }
            GETTER_TO_DB_STRING.put(clazz, dtoToCollectionMap);
        }
        return dtoToCollectionMap;
    }

    private Map<String, String> getSetterToDB(Class<? extends DTO> clazz) {
        Map<String, String> dtoToCollectionMap = SETTER_TO_DB.get(clazz);
        if (dtoToCollectionMap == null) {
            dtoToCollectionMap = new HashMap<>();
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("set") && !method.isAnnotationPresent(NoPersist.class)) {
                    dtoToCollectionMap.put(method.getName(), toDBFormatString(method.getName().substring(3)));
                }
            }
            SETTER_TO_DB.put(clazz, dtoToCollectionMap);
        }
        return dtoToCollectionMap;
    }

    private Map<String, Class<? extends DTO>> getSetterToDto(Class<? extends DTO> clazz) {
        Map<String, Class<? extends DTO>> dtoToCollectionMap = SETTER_TO_DTO.get(clazz);
        if (dtoToCollectionMap == null) {
            dtoToCollectionMap = new HashMap<>();
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("get")
                        && !method.getName().equals("getClass")
                        && DTO.class.isAssignableFrom(method.getReturnType())) {
                    dtoToCollectionMap.put("set" + method.getName().substring(3), (Class<? extends DTO>) method.getReturnType());
                }
            }
            SETTER_TO_DTO.put(clazz, dtoToCollectionMap);
        }
        return dtoToCollectionMap;
    }

    private DBCollection getCollection(Class<? extends DTO> clazz) {
        String name = clazz.getSimpleName();
        name = toDBFormatString(name);
        return db.getCollection(name);
    }

    String toDBFormatString(String camelCaseString) {
        boolean prevIsLowerCase = false;
        boolean prevIsDigit = false;
        StringBuilder DBFormatString = new StringBuilder();
        for (int i = 0; i < camelCaseString.length(); i++) {
            char ch = camelCaseString.charAt(i);
            boolean lowerCase = Character.isLowerCase(ch);
            boolean upperCase = Character.isUpperCase(ch);
            boolean digit = Character.isDigit(ch);
            if (upperCase && prevIsLowerCase || prevIsDigit != digit && i > 0) {
                DBFormatString.append("_");
            }
            DBFormatString.append(Character.toLowerCase(ch));
            prevIsDigit = digit;
            prevIsLowerCase = lowerCase;
        }
        String string = DBFormatString.toString();
        return string.equals("id") ? MONGO_ID : string;
    }

    public <T extends DTO> T findById(String id, Class<T> clazz) {
        return findById(id, clazz, DEFAULT_CONFIG);
    }

    public <T extends DTO> T findById(String id, Class<T> clazz, Config config) {
        return findById(new ObjectId(id), clazz, config);
    }

    public <T extends DTO> T findById(ObjectId id, Class<T> clazz) {
        return findById(id, clazz, DEFAULT_CONFIG);
    }

    public <T extends DTO> T findById(ObjectId id, Class<T> clazz, Config config) {
        try {
            T dto = clazz.newInstance();
            dto.setId(id);
            List<T> result = find(dto, config);
            if (!result.isEmpty()) {
                return result.get(0);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T extends DTO> T findOne(T dto) {
        return findOne(dto, DEFAULT_CONFIG);
    }

    public <T extends DTO> T findOne(T dto, Config config) {
        BasicDBObject query = toBasicDBObject(dto, getGetterToDBString(dto.getClass()));
        return findOne(query, (Class<T>) dto.getClass(), config);
    }

    public <T extends DTO> List<T> find(T dto) {
        return find(dto, DEFAULT_CONFIG);
    }

    public <T extends DTO> List<T> find(T dto, Config config) {
        BasicDBObject query = toBasicDBObject(dto, getGetterToDBString(dto.getClass()));
        return find(query, (Class<T>) dto.getClass(), config);
    }

    public <T extends DTO> T findOne(BasicDBObject query, Class<T> clazz) {
        return findOne(query, clazz, DEFAULT_CONFIG);
    }

    public <T extends DTO> T findOne(BasicDBObject query, Class<T> clazz, Config config) {
        config = new Config(config.getSkip(), 1, config.isLazy(), config.getSort());
        List<T> result = find(query, clazz, config);
        return result.isEmpty() ? null : result.get(0);
    }

    public <T extends DTO> List<T> findAll(Class<T> clazz) {
        return find(null, clazz, DEFAULT_CONFIG);
    }

    public <T extends DTO> List<T> findAll(Class<T> clazz, Config config) {
        return find(null, clazz, config);
    }

    public <T extends DTO> List<T> find(BasicDBObject query, Class<T> clazz) {
        return find(query, clazz, DEFAULT_CONFIG);
    }

    public <T extends DTO> List<T> find(BasicDBObject query, Class<T> clazz, Config config) {
        DBCursor cursor = getCollection(clazz).find(query);
        if (config.getSkip() > 0) {
            cursor.skip(config.getSkip());
        }
        if (config.getLimit() > 0) {
            cursor.limit(config.getLimit());
        }
        if (config.getSort() != null) {
            cursor.sort(config.getSort());
        }

        List<T> answer = new ArrayList<>();
        Map<String, String> setterToDB = getSetterToDB(clazz);
        Map<String, Class<? extends DTO>> setterToDto = getSetterToDto(clazz);
        for (DBObject dbObject : cursor) {
            T dto = toDto(dbObject, clazz, setterToDB, setterToDto);
            answer.add(dto);
        }
        if (!config.isLazy()) {
            for (Map.Entry<String, Class<? extends DTO>> entry : setterToDto.entrySet()) {
                resolveDependency(answer, entry.getKey(), entry.getValue());
            }
        }
        return answer;
    }

    private <T extends DTO> void resolveDependency(List<T> answer, String setterName, Class<? extends DTO> joinDtoClass) {
        if (answer.isEmpty()) {
            return;
        }
        try {
            String getterName = "get" + setterName.substring(3);
            Method getter = answer.get(0).getClass().getMethod(getterName);
            Method setter = answer.get(0).getClass().getMethod(setterName, joinDtoClass);
            Set<ObjectId> objectIds = new HashSet<>();
            for (T dto : answer) {
                DTO invoke = (DTO) getter.invoke(dto);
                if(invoke != null && invoke.getId() != null) {
                    objectIds.add(invoke.getId());
                }
            }
            List<? extends DTO> dependence = find(new BasicDBObject(MONGO_ID, new BasicDBObject("$in", objectIds)), joinDtoClass, EAGER_CONFIG);
            Map<ObjectId, DTO> dependenceMap = new HashMap<>();
            for (DTO dto : dependence) {
                dependenceMap.put(dto.getId(), dto);
            }
            for (T dto : answer) {
                DTO invoke = (DTO) getter.invoke(dto);
                if(invoke != null && invoke.getId() != null) {
                    DTO joinDto = dependenceMap.get(invoke.getId());
                    if (joinDto != null) {
                        setter.invoke(dto, joinDto);
                    }
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private <T extends DTO> T toDto(DBObject dbObject, Class<T> clazz, Map<String, String> setterToDB, Map<String, Class<? extends DTO>> setterToDto) {
        try {
            T dto = clazz.newInstance();
            for (Method method : clazz.getMethods()) {
                String dbString = setterToDB.get(method.getName());
                if (dbString != null) {
                    Object object = dbObject.get(dbString);
                    if (object != null) {
                        Class<? extends DTO> dtoClass = setterToDto.get(method.getName());
                        if (object instanceof ObjectId && dtoClass != null) {
                            DTO joinDto = dtoClass.newInstance();
                            joinDto.setId((ObjectId) object);
                            joinDto.setStab(true);
                            object = joinDto;
                        }
                        method.invoke(dto, object);
                    }
                }
            }
            return dto;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}
