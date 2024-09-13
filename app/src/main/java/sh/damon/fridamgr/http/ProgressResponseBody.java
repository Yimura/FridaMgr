package sh.damon.fridamgr.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import sh.damon.fridamgr.listener.ProgressListener;

public class ProgressResponseBody extends ResponseBody {
    private final ResponseBody responseBody;
    private final ProgressListener progressListener;

    public ProgressResponseBody(ResponseBody body, ProgressListener listener) {
        responseBody = body;
        progressListener = listener;
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    private BufferedSource bufferedSource = null;
    @NonNull
    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source src) {
        return new ForwardingSource(src) {
            long totalBytesRead = 0;

            @Override
            public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                final long bytesRead = super.read(sink, byteCount);
                final boolean done = bytesRead == -1;

                totalBytesRead += (done ? 0 : bytesRead);
                progressListener.update(totalBytesRead, responseBody.contentLength(), done);

                return bytesRead;
            }
        };
    }
}