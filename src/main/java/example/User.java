package example;

import mongo.orm.DTO;
import mongo.orm.NoPersist;


public class User extends DTO {
    private String name;
    private String password;
    private String hash;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NoPersist
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
