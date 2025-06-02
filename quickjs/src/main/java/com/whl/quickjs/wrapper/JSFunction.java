package com.whl.quickjs.wrapper;

public class JSFunction extends JSObject {

    private final long objPointer;

    public JSFunction(QuickJSContext context, long objPointer, long pointer) {
        super(context, pointer);
        this.objPointer = objPointer;
    }

    public Object call(Object... args) {
        try {
            if (isReleased || getContext() == null) {
                System.err.println("Warning: Attempting to call a function on a released JSObject or with null context");
                return null;
            }
            return getContext().call(this, objPointer, args);
        } catch (Exception e) {
            System.err.println("Error calling JSFunction: " + e.getMessage());
            return null;
        }
    }

    // 直接使用父类的 isReleased 字段，现在它是 protected 的

}
