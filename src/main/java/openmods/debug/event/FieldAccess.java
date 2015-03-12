package openmods.debug.event;

import java.lang.reflect.Field;

import com.google.common.base.Throwables;

public class FieldAccess<T> {

    public static class FieldAccessException extends RuntimeException {
        private static final long serialVersionUID = 3261757597754500600L;

        private static String createMessage(Field f, String action) {
            return "Failed to " + action + " field " + f;
        }

        public FieldAccessException(Field f, String action, Throwable cause) {
            super(createMessage(f, action), cause);
        }

        public FieldAccessException(Field f, String action) {
            super(createMessage(f, action));
        }

    }

    public final Field field;

    public FieldAccess(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    public T get(Object target) {
        try {
            return (T)field.get(target);
        } catch (Throwable t) {
            throw new FieldAccessException(field, "read", t);
        }
    }

    public void set(Object target, T value) {
        try {
            field.set(target, value);
        } catch (Throwable t) {
            throw new FieldAccessException(field, "set", t);
        }
    }

    public static Field getField(Class<?> klazz, String... fields) {
        for (String field : fields) {
            final Field f = getField(klazz, field);
            if (f != null)
                return f;
        }

        throw new IllegalArgumentException(String.format("%s:%s not found", klazz, fields));
    }

    private static Field getField(Class<?> klazz, String field) {
        Class<?> current = klazz;
        while (current != null) {
            try {
                Field f = current.getDeclaredField(field);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {} catch (Exception e) {
                throw Throwables.propagate(e);
            }
            current = current.getSuperclass();
        }

        return null;
    }

    public static <T> FieldAccess<T> create(Class<?> cls, String... names) {
        Field f = getField(cls, names);
        return new FieldAccess<T>(f);
    }

    public static <T> FieldAccess<T> create(Field f) {
        return new FieldAccess<T>(f);
    }
}
