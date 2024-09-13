package sh.damon.fridamgr.listener;

public interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}
