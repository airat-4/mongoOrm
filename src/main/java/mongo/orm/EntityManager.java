package mongo.orm;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

    private static final Map<Class<? extends DTO>, Map<String, FieldMapper>> FIELD_MAPPER = new HashMap<>();
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
        Map<String, FieldMapper> fieldMapper = getFieldMapper(dto.getClass());
        for (DTO curDto : getNotNullInnerDto(dto, fieldMapper)) {
            save(curDto);
        }
        if (dto.getId() == null) {
            create(dto, fieldMapper);
        } else {
            update(dto, fieldMapper);
        }
    }

    private void update(DTO dto, Map<String, FieldMapper> fieldMapper) {
        BasicDBObject basicDBObject = toBasicDBObject(dto, fieldMapper);
        getCollection(dto.getClass()).update(new BasicDBObject(MONGO_ID, basicDBObject.getObjectId(MONGO_ID)), basicDBObject);
    }

    private List<DTO> getNotNullInnerDto(DTO dto, Map<String, FieldMapper> fieldMapper) {
        List<DTO> innerDto = new ArrayList<>();
        for (FieldMapper mapper : fieldMapper.values()) {
            if (mapper.isDTO()) {
                Object value = getFieldValue(dto, mapper.getFieldName());
                if (value != null) {
                    innerDto.add((DTO) value);
                }
            }
        }
        return innerDto;
    }

    private void create(DTO dto, Map<String, FieldMapper> fieldMapper) {
        BasicDBObject basicDBObject = toBasicDBObject(dto, fieldMapper);
        getCollection(dto.getClass()).insert(basicDBObject);
        dto.setId(basicDBObject.getObjectId(MONGO_ID));
    }

    private BasicDBObject toBasicDBObject(DTO dto, Map<String, FieldMapper> fieldMapper) {
        BasicDBObject basicDBObject = new BasicDBObject();
        for (FieldMapper mapper : fieldMapper.values()) {
            Object value = getFieldValue(dto, mapper.getFieldName());
            if (value != null) {
                if (mapper.isDTO()) {
                    basicDBObject.append(mapper.getDbName(), ((DTO) value).getId());
                } else {
                    basicDBObject.append(mapper.getDbName(), value);
                }
            }
        }
        return basicDBObject;
    }

    private Object getFieldValue(DTO dto, String fieldName) {
        try {
            Field field = getField(dto.getClass(), fieldName);
            return field.get(dto);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Field getField(Class<? extends DTO> clazz, String fieldName) throws NoSuchFieldException {
        clazz = MONGO_ID.equals(fieldName) ? (Class<DTO>) clazz.getSuperclass() : clazz;
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private Map<String, FieldMapper> getFieldMapper(Class<? extends DTO> clazz) {
        Map<String, FieldMapper> fieldMap = FIELD_MAPPER.get(clazz);
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(NoPersist.class)) {
                    FieldMapper mapping = new FieldMapper();
                    mapping.setDb(toDBFormatString(field.getName()));
                    mapping.setField(field.getName());
                    mapping.setDTO(DTO.class.isAssignableFrom(field.getType()));
                    mapping.setReference(ObjectId.class.isAssignableFrom(field.getType()));
                    fieldMap.put(field.getName(), mapping);
                }
            }
            FieldMapper mapping = new FieldMapper();
            mapping.setDb(MONGO_ID);
            mapping.setField(MONGO_ID);
            fieldMap.put(MONGO_ID, mapping);
            FIELD_MAPPER.put(clazz, fieldMap);
        }
        return fieldMap;
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
        BasicDBObject query = toBasicDBObject(dto, getFieldMapper(dto.getClass()));
        return findOne(query, (Class<T>) dto.getClass(), config);
    }

    public <T extends DTO> List<T> find(T dto) {
        return find(dto, DEFAULT_CONFIG);
    }

    public <T extends DTO> List<T> find(T dto, Config config) {
        BasicDBObject query = toBasicDBObject(dto, getFieldMapper(dto.getClass()));
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
        Map<String, FieldMapper> fieldMapper = getFieldMapper(clazz);
        for (DBObject dbObject : cursor) {
            T dto = toDto(dbObject, clazz, fieldMapper);
            answer.add(dto);
        }
        if (!config.isLazy()) {
            for (FieldMapper mapper : fieldMapper.values()) {
                if(mapper.isDTO()) {
                    resolveDependency(answer, mapper.getFieldName());
                }
            }
        }
        return answer;
    }

    private <T extends DTO> void resolveDependency(List<T> answer, String fieldName) {
        if (answer.isEmpty()) {
            return;
        }
        try {
            Field field = getField(answer.get(0).getClass(), fieldName);
            field.setAccessible(true);
            Set<ObjectId> objectIds = new HashSet<>();
            for (T dto : answer) {
                DTO joinedDto = (DTO) field.get(dto);
                if (joinedDto != null && joinedDto.getId() != null) {
                    objectIds.add(joinedDto.getId());
                }
            }
            List<? extends DTO> dependence = find(new BasicDBObject(MONGO_ID, new BasicDBObject("$in", objectIds)), (Class<? extends DTO>) field.getType(), EAGER_CONFIG);
            Map<ObjectId, DTO> dependenceMap = new HashMap<>();
            for (DTO dto : dependence) {
                dependenceMap.put(dto.getId(), dto);
            }
            for (T dto : answer) {
                DTO stabDto = (DTO) field.get(dto);
                if (stabDto != null && stabDto.getId() != null) {
                    DTO joinDto = dependenceMap.get(stabDto.getId());
                    if (joinDto != null) {
                        field.set(dto, joinDto);
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private <T extends DTO> T toDto(DBObject dbObject, Class<T> clazz, Map<String, FieldMapper> fieldMapper) {
        try {
            T dto = clazz.newInstance();
            for (FieldMapper mapper : fieldMapper.values()) {
                Object object = dbObject.get(mapper.getDbName());
                if (object != null) {
                    Field field = getField(clazz, mapper.getFieldName());
                    field.setAccessible(true);
                    if (object instanceof ObjectId && mapper.isDTO()) {
                        DTO joinDto = (DTO) field.getType().newInstance();
                        joinDto.setId((ObjectId) object);
                        joinDto.setStab(true);
                        object = joinDto;
                    }
                    field.set(dto, object);
                }
            }
            return dto;
        } catch (NoSuchFieldException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void delete(String id, Class<? extends DTO> clazz) {
        delete(new ObjectId(id), clazz);
    }

    public void delete(DTO dto) {
        getCollection(dto.getClass()).remove(new BasicDBObject(MONGO_ID, dto.getId()));
    }

    public void delete(ObjectId id, Class<? extends DTO> clazz) {
        getCollection(clazz).remove(new BasicDBObject(MONGO_ID, id));
    }
}
