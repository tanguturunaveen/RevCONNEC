package org.revature.revconnect.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.revature.revconnect.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulePostRequest {

    // @NotBlank(message = "Content is required") -- Removed to allow media-only
    // scheduled posts
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private PostType postType;

    private List<String> mediaUrls;

    @NotNull(message = "publishAt is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishAt;
}
