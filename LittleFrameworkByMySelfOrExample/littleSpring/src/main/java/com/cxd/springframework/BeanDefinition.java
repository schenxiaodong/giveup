package com.cxd.springframework;

public class BeanDefinition {
    private Class type; // 目标类Class

    private boolean isLazy; // 是否懒加载

    private String scope; // 作用域范围 singleton? prototype

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "type=" + type +
                ", isLazy=" + isLazy +
                ", scope='" + scope + '\'' +
                '}';
    }
}
