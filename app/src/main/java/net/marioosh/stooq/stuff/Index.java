package net.marioosh.stooq.stuff;

/**
 * @author marioosh
 */
public class Index {

    public enum Type {
        WIG("#aq_wig_c2"),
        WIG20("#aq_wig20_c2"),
        mWIG40("#aq_mwig40_c2"),
        sWIG80("#aq_swig80_c2");

        private final String cssSelector;

        Type(String cssSelector) {
            this.cssSelector = cssSelector;
        }

        public String getCssSelector() {
            return cssSelector;
        }

    }

    private Type type;
    private String value;
    private long time;
    private boolean updated;

    public Index(Type type, String value) {
        this.type = type;
        this.value = value;
        this.time = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean isUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Index) {
            Index o1 = (Index) o;
            if(getValue().equals(o1.getValue()) && getType() == o1.getType()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
