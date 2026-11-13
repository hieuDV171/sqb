package com.frozenheart.backend.modules.media.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.dto.LectureVideoUpload.StartRequest;
import com.frozenheart.backend.modules.media.service.impl.R2MultipartClient;
import io.minio.MinioAsyncClient;
import io.minio.messages.Part;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LectureVideoUploadServiceTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsStudentsAndOversizedFilesBeforeContactingStorage() {
        var service = new LectureVideoUploadService(new R2Properties(), null, null, null, null, null, null);
        var id = UUID.randomUUID();
        authenticate("STUDENT");
        var request = new StartRequest("lecture.mp4", "video/mp4", 1);
        assertEquals(ResponseCode.ACCESS_DENIED,
                assertThrows(AppException.class, () -> service.start(id, request)).getResponseCode());

        authenticate("LECTURER");
        var oversized = new StartRequest("lecture.mp4", "video/mp4", 2L * 1024 * 1024 * 1024 + 1);
        assertEquals(ResponseCode.FILE_SIZE_EXCEEDED,
                assertThrows(AppException.class, () -> service.start(id, oversized)).getResponseCode());
    }

    @Test
    void resumesOnlyPartsWithExpectedSizesAndSortsThemForCompletion() {
        long size = 2 * LectureVideoUploadService.PART_SIZE + 10;
        Part last = part(3, 10);
        Part first = part(1, LectureVideoUploadService.PART_SIZE);
        List<Part> actual = List.of(last, part(2, 10), first, part(4, 1));

        assertEquals(List.of(first, last), LectureVideoUploadService.validParts(actual, size));
    }

    @Test
    void signsObjectPathAndExactPartLength() throws Exception {
        try (var r2 = new R2MultipartClient(MinioAsyncClient.builder()
                .endpoint("https://example.com").region("auto").credentials("test", "test").build())) {
            var url = HttpUrl.get(r2.presignPart("sqb-media", "lecture-videos/test.mp4", "upload+/=", 2, 100, 900));
            assertEquals("/sqb-media/lecture-videos/test.mp4", url.encodedPath());
            assertEquals("upload+/=", url.queryParameter("uploadId"));
            assertEquals("2", url.queryParameter("partNumber"));
            assertEquals("content-length;host", url.queryParameter("X-Amz-SignedHeaders"));
            assertEquals("900", url.queryParameter("X-Amz-Expires"));
        }
    }

    private static Part part(int number, long size) {
        var part = mock(Part.class);
        when(part.partNumber()).thenReturn(number);
        when(part.partSize()).thenReturn(size);
        return part;
    }

    private static void authenticate(String role) {
        var auth = new UsernamePasswordAuthenticationToken("test", null, List.of());
        auth.setDetails(JwtPayload.builder().userId(7L).role(role).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
