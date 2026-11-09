package com.frozenheart.backend.modules.post.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LecturerNoteAccess {
    private final PostRepository postRepository;

    public Set<Long> findEditablePostIds(JwtPayload viewer, Collection<Post> posts) {
        if (viewer == null || viewer.getUserId() == null || !UserRole.LECTURER.name().equalsIgnoreCase(viewer.getRole())) {
            return Set.of();
        }

        List<Long> approvalPostIds = posts.stream()
                .filter(post -> post.getPostType() == PostType.QUESTION_APPROVED_NOTIFICATION)
                .map(Post::getId).toList();
        return approvalPostIds.isEmpty() ? Set.of()
                : Set.copyOf(postRepository.findApprovalPostIdsForLecturer(approvalPostIds, viewer.getUserId()));
    }
}
