package io.netty.example.aaron.flashnetty.attribute;

import io.netty.example.aaron.flashnetty.session.Session;
import io.netty.util.AttributeKey;

public interface Attributes {
    AttributeKey<Session> SESSION = AttributeKey.newInstance("session");
}
