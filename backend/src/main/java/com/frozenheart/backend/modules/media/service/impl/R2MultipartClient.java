package com.frozenheart.backend.modules.media.service.impl;

import io.minio.Http;
import io.minio.MinioAsyncClient;
import io.minio.Signer;
import io.minio.StatObjectArgs;
import io.minio.Time;
import io.minio.Xml;
import io.minio.errors.MinioException;
import io.minio.errors.XmlParserException;
import io.minio.messages.ListPartsResult;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletionException;

public final class R2MultipartClient extends MinioAsyncClient {
    public R2MultipartClient(MinioAsyncClient client) {
        super(client);
    }

    public String presignPart(String bucket, String key, String uploadId, int number, long size, int expires)
            throws MinioException {
        var region = getRegion(bucket, null).join();
        var credentials = provider.fetch();
        var params = new Http.QueryParameters("uploadId", uploadId, "partNumber", Integer.toString(number));
        if (credentials.sessionToken() != null) {
            params.put(Http.Headers.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
        }
        var url = baseUrl.buildUrl(Http.Method.PUT, bucket, key, region, params);
        // MinIO's presigner removes Content-Length. Bind each URL to its exact part size.
        var request = new Request.Builder().url(url)
                .header("Host", url.uri().getRawAuthority())
                .header("Content-Length", Long.toString(size))
                .header("X-Amz-Date", ZonedDateTime.now(ZoneOffset.UTC).format(Time.AMZ_DATE_FORMAT))
                .put(RequestBody.create(new byte[0])).build();
        return Signer.presignV4(request, region, credentials.accessKey(), credentials.secretKey(), expires).toString();
    }

    public ListPartsResult listParts(String bucket, String key, String uploadId, Integer marker) {
        // MinIO 9.0.3's ListPartsArgs.Builder cannot set the object key.
        var args = StatObjectArgs.builder().bucket(bucket).object(key).build();
        var params = new Http.QueryParameters("uploadId", uploadId);
        if (marker != null) {
            params.put("part-number-marker", marker.toString());
        }
        try (var response = executeGetAsync(args, null, params).join()) {
            return Xml.unmarshal(ListPartsResult.class, response.body().charStream());
        } catch (XmlParserException exception) {
            throw new CompletionException(exception);
        }
    }
}
