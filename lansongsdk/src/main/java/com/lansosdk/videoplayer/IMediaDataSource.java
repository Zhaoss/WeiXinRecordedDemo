package com.lansosdk.videoplayer;

import java.io.IOException;

/**
 * 直接读取buffer中的裸数据来播放视频,
 * <p>
 * 使用在网络获取到的裸数据的场合.
 * <p>
 * 请不好优化掉.
 */
@SuppressWarnings("RedundantThrows")
public interface IMediaDataSource {
    int readAt(long position, byte[] buffer, int offset, int size)
            throws IOException;

    long getSize() throws IOException;

    void close() throws IOException;
}
