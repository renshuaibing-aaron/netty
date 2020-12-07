package io.netty.example.aaron.zerocopy.server;

class FileEntry {

    long bodyLength;
    short nameLength;
    long id = -1;
}
