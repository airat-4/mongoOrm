package mongo.orm;

import org.bson.types.ObjectId;

/**
 * @author Айрат Гареев
 * @since 07.07.2017
 */
public abstract class DTO {
    private ObjectId _id;
    @NoPersist
    private boolean stab;

    public ObjectId getId() {
        return _id;
    }

    public void setId(ObjectId id) {
        this._id = id;
    }

    public boolean isStab() {
        return stab;
    }

    public void setStab(boolean stab) {
        this.stab = stab;
    }

    public String getStringId() {
        if (_id == null) {
            return null;
        }
        return _id.toHexString();
    }

    public void setStringId(String stringId) {
        _id = stringId != null ? new ObjectId(stringId) : null;
    }
}
