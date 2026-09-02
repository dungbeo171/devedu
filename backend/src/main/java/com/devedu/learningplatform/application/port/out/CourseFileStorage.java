package com.devedu.learningplatform.application.port.out;

public interface CourseFileStorage {
    void store(String storageKey, byte[] content);
    byte[] load(String storageKey);
    void delete(String storageKey);
}
