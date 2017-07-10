package mongo.orm;

import com.mongodb.BasicDBObject;

/**
 * @author Айрат Гареев
 * @since 10.07.2017
 */
public class Config {
    private int skip = 0;
    private int limit = 0;
    private boolean lazy = true;
    private BasicDBObject sort;

    public Config() {
    }

    public Config(int skip, int limit) {
        this.skip = skip;
        this.limit = limit;
    }

    public Config(boolean lazy) {
        this.lazy = lazy;
    }

    public Config(int skip, int limit, boolean lazy) {
        this.skip = skip;
        this.limit = limit;
        this.lazy = lazy;
    }

    public Config(int skip, int limit, boolean lazy, BasicDBObject sort) {
        this.skip = skip;
        this.limit = limit;
        this.lazy = lazy;
        this.sort = sort;
    }

    public Config(BasicDBObject sort) {
        this.sort = sort;
    }

    public int getSkip() {
        return skip;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isLazy() {
        return lazy;
    }

    public BasicDBObject getSort() {
        return sort;
    }
}
