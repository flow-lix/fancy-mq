package org.fancy.mq.client.producer;

/**
 * A callback interface that the user can implement to allow code to execute when the request is complete. This callback
 * will generally execute in the background I/O thread so it should be fast.
 */
public interface Callback {

    void onCompletion(RecordMetadata metadata, Exception exception);
}
