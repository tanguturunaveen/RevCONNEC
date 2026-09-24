package org.revature.revconnect.dto.request;

import org.revature.revconnect.enums.PostType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequest {

    // @NotBlank(message = "Content is required") -- Removed to allow media-only
    // posts
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private PostType postType;

    private List<String> mediaUrls;
}
