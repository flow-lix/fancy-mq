package org.fancy.mq.client.common.header;

public interface Headers {

    Headers addHeader(Header header);

    Headers addHeader(String key, byte[] value);

    Header[] toArray();

}
