package mongo.orm;

/**
 * @author Айрат Гареев
 * @since 25.07.2017
 */
public class FieldMapper {
    private String field;
    private String db;
    private boolean isDTO;
    private boolean isReference;

    public String getFieldName() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDbName() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public boolean isDTO() {
        return isDTO;
    }

    public void setDTO(boolean DTO) {
        isDTO = DTO;
    }

    public boolean isReference() {
        return isReference;
    }

    public void setReference(boolean reference) {
        isReference = reference;
    }
}
