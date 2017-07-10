package mongo.orm;

import org.bson.types.ObjectId;

/**
 * @author Айрат Гареев
 * @since 07.07.2017
 */
public abstract class DTO {
    private ObjectId id;
    private boolean stab;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @NoPersist
    public boolean isStab() {
        return stab;
    }

    @NoPersist
    public void setStab(boolean stab) {
        this.stab = stab;
    }

    @NoPersist
    public String getStringId() {
        if (id == null) {
            return null;
        }
        return id.toHexString();
    }

    @NoPersist
    public void setStringId(String stringId) {
        id = stringId != null ? new ObjectId(stringId) : null;
    }
}
