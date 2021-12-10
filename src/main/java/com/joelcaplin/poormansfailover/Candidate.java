package com.joelcaplin.poormansfailover;

import lombok.NonNull;
import lombok.Value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Value(staticConstructor = "of")
public class Candidate<T> implements Comparable<Candidate<T>> {
    @NonNull
    private T reference;

    @NonNull
    private Priority priority;

    @Override
    public int compareTo(Candidate<T> that) {
        return this.priority.compareTo(that.priority);
    }

    public Object invokeWithReference(Method method,
                                      Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(reference, args);
    }
}
